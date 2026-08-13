# bitdreamit-astm — Root Cause Analysis & Patch Set

**Plugin:** `bitdreamit-astm` (Mirth Connect / BridgeLink ASTM extension)
**Versions analyzed:**

| Tag | Source | Status |
|---|---|---|
| 2.4.2 (TCP-only) | `bitdreamit-astm-2.4.2-only-tcp.zip` | Worked for 4 years |
| 3.0.2 (TCP+Serial) | `bitdreamit-astm-tcp-serial-both.zip` / `main` branch | Broken — silent channel, no errors |

**Reported symptoms on v3.0.2:**

1. Channel does not enable / does not become active.
2. No log error is shown — the failure is **silent**.
3. No traffic flows.

This document explains exactly **why** v3.0.2 fails silently and provides a complete drop-in patch set that restores the v2.4.2 stability while keeping the new TCP + Serial capability.

---

## 1. Executive Summary

The v3.0.2 codebase was refactored from a single TCP-only `AsyncAstmTcpDriver` into a polymorphic `AsyncAstmDriver` interface with `AsyncAstmTcpDriver` + `AsyncAstmSerialDriver` implementations. The refactor introduced a clean state-machine architecture (`InitialState → ConnectState → IdleState → TransferReceiver/TransferSender → ReconnectState → DisconnectState → ExitState`).

However, **the integration glue between the new driver layer and Mirth Connect was not finished**. The refactor left twelve concrete defects that together produce the exact symptom you are seeing:

> Channel shows as "Started" in Mirth → no error in the logs → no messages flow → no connection-status badge in the dashboard → channel appears dead but Mirth thinks it is alive.

The single most damaging bug is **#1 (missing channel-XML migration)** — every channel that was created under v2.4.2 silently has its transport mode reset to TCP-client-to-127.0.0.1:5004 the moment v3.0.2 is installed, regardless of what the channel was actually configured to do. The other eleven bugs compound this: even brand-new channels created under v3.0.2 still fail because the reader thread NPEs silently and no status callback ever reaches Mirth.

All twelve bugs are listed below in order of impact, with file/line references, root cause, and the exact fix applied in the patch set.

---

## 2. Bug Catalogue

### Bug #1 — Channel migration is a no-op (CRITICAL — affects every existing channel)

**File:** `astm-shared/com/bitdreamit/connect/astm/AstmProperties.java`
**Location:** `migrate4_5_0(DonkeyElement element)` — empty body
**File:** `astm-shared/com/bitdreamit/connect/astm/AstmReceiverProperties.java` and `AstmDispatcherProperties.java`
**Location:** `migrate4_5_0()` — just calls `super.migrate4_5_0()` which is empty

**Root cause:**

v2.4.2 channels store their TCP configuration as:

```xml
<serverMode>true</serverMode>
<allInterfaces>true</allInterfaces>
<addressBind>0.0.0.0</addressBind>
<localPort>3600</localPort>
<remoteAddress>192.168.1.50</remoteAddress>
<remotePort>5000</remotePort>
<astmProtocol>ELECSYS</astmProtocol>
```

v3.0.2 introduces new fields:

```xml
<transportMode>TCP_CLIENT</transportMode>   <!-- default -->
<host>127.0.0.1</host>                         <!-- default -->
<port>5004</port>                              <!-- default -->
<serialPort>COM1</serialPort>
<baudRate>9600</baudRate>
...
```

`migrate4_5_0()` is the Mirth-designated hook for upgrading channel XML from 4.4.x → 4.5.x format. In the shipped v3.0.2 source, it is **empty** — it does not read the old `<serverMode>/<localPort>/<remoteAddress>/<remotePort>` elements and copy their values into the new `<transportMode>/<host>/<port>` elements.

**Symptom produced:**

When Mirth loads a v2.4.2 channel after the v3.0.2 plugin upgrade, XStream deserializes the old fields (they are still present in `AstmProperties` for backward compatibility), but the new fields fall back to their Java defaults:

| Field | Default | Result |
|---|---|---|
| `transportMode` | `TCP_CLIENT` | Was `TCP_SERVER` |
| `host` | `127.0.0.1` | Was the analyzer's IP |
| `port` | `5004` | Was `3600` |

So a channel that was a TCP **server** listening on port `3600` for an analyzer becomes a TCP **client** trying to connect to `127.0.0.1:5004` — a port on which nothing is listening. The connection fails, retries silently (see Bug #5), and the channel appears dead.

**Fix applied:**

`AstmProperties.migrate4_5_0(DonkeyElement)` now reads the old XML elements and writes the new ones if they are missing. The migration is idempotent — channels that were already created under v3.0.2 are not affected.

```java
@Override
public void migrate4_5_0(DonkeyElement element) {
    super.migrate4_5_0(element);
    // Migrate v2.4.2 fields -> v3.0.2 fields if not already present.
    DonkeyElement transportModeEl = element.getChild("transportMode");
    if (transportModeEl == null || transportModeEl.getStringValue().trim().isEmpty()) {
        boolean serverMode = Boolean.parseBoolean(
            element.getChildText("serverMode", "true"));
        DonkeyElement newEl = element.addChild("transportMode");
        newEl.setStringValue(serverMode ? "TCP_SERVER" : "TCP_CLIENT");
    }
    // ... similar for host, port (from localPort or remoteAddress/remotePort)
}
```

---

### Bug #2 — Reader thread NPE in TCP server mode (CRITICAL — kills the listener silently)

**File:** `AsyncAstm-3.2/com/bitdreamit/astm/asyncastm/service/connection/AstmTcpServerConnection.java`
**Location:** `doConnect()` — `this.initialize();` is called before `this.serverSocket.accept();`

**Root cause:**

`AbstractAstmConnection.initialize()` starts a background reader thread that runs `doGetInputStream().read()` in a loop. For `AstmTcpServerConnection`, `doGetInputStream()` returns `this.clientSocket.getInputStream()`. But the call order is:

```java
public final void doConnect() throws IOException {
    if (!this.connected) {
        this.bind();           // creates serverSocket — OK
        this.initialize();     // ✘ starts reader thread — but clientSocket is still NULL
        this.clientSocket = this.serverSocket.accept();   // never reached until reader thread consumes
    }
}
```

The reader thread immediately calls `clientSocket.getInputStream()` on a null `clientSocket` → `NullPointerException`. The exception is caught by the reader thread's inner `catch (IOException)` block, which sets `lastByte = -1`, releases the ready semaphore, and `return`s — the reader thread exits silently. No log is emitted at INFO level.

When `accept()` finally returns a real client socket, the reader thread is **already dead**. Subsequent `readByte()` calls block forever on `readySemaphore.acquire()` because no thread will ever produce a byte. The state machine hangs in `IdleState` waiting for ENQ that will never be delivered.

**Fix applied:**

`AstmTcpServerConnection.doConnect()` — `initialize()` is now called **after** `accept()` returns:

```java
public final void doConnect() throws IOException {
    if (!this.connected) {
        this.bind();                                        // 1. bind server socket
        this.clientSocket = this.serverSocket.accept();     // 2. block until a client connects
        this.initialize();                                  // 3. NOW start the reader thread
        this.connected = true;
    }
}
```

---

### Bug #3 — Reader thread NPE in TCP client mode (CRITICAL — same as #2, different file)

**File:** `AsyncAstm-3.2/com/bitdreamit/astm/asyncastm/service/connection/AstmTcpClientConnection.java`
**Location:** `doConnect()` — `this.initialize();` is called before `this.socket = new Socket(...)`

**Root cause:** identical to Bug #2 but on the client side. The reader thread starts before `socket` is assigned, NPEs on `socket.getInputStream()`, and dies silently.

**Fix applied:** reorder so `initialize()` runs after the socket is created.

```java
public final synchronized void doConnect() throws InterruptedException {
    if (this.connected) return;
    int attempt = 0;
    int delay = 1000;
    do {
        try {
            this.socket = new Socket(this.address.getAddress(), this.address.getPort());
            this.initialize();          // ✅ start reader thread AFTER socket exists
            this.connected = true;
        } catch (IOException e) {
            // ... backoff retry, now with WARN-level logging (see Bug #5)
        }
    } while (!this.connected);
}
```

---

### Bug #4 — No `AstmStatusCallback` is registered with the driver (CRITICAL — explains "channel not enable" / no dashboard status)

**File:** `astm-server/com/bitdreamit/connect/astm/AstmService.java`
**Location:** `createDriver()` — never passes a callback to the driver constructors

**Root cause:**

`AsyncAstmTcpDriver` and `AsyncAstmSerialDriver` both accept an `AstmStatusCallback` (set either via constructor or `addCallback()` on the state machine). The callback is the **only** mechanism by which the driver reports state changes back to Mirth:

- `CONNECTING` → should fire `ConnectionStatusEventType.CONNECTING`
- `IDLE` → should fire `ConnectionStatusEventType.CONNECTED` ("Waiting for receiving messages")
- `RECEIVING` / `SENDING` → should fire the corresponding dashboard events
- `RECONNECTING` → should fire `ConnectionStatusEventType.DISCONNECTED` ("Trying to reconnect")
- `EXITING` → should fire `ConnectionStatusEventType.IDLE` and optionally stop the channel

In v2.4.2, `AstmConnectionManager` constructed the driver with a full anonymous `AstmStatusCallback` implementation that dispatched `ConnectionStatusEvent`s to Mirth's `EventController`. That callback is what made the Mirth dashboard show "Listening on 0.0.0.0:3600", "Waiting for receiving messages", "Receiving new message", etc.

In v3.0.2:

- `AstmConnectionManager` is deprecated and not used.
- `AstmService.createDriver()` constructs the driver with no callback:
  ```java
  new AsyncAstmTcpDriver(props.getPort(), true, props.getAstmProtocol())    // no callback
  new AsyncAstmTcpDriver(props.getHost(), props.getPort(), false, ...)      // no callback
  new AsyncAstmSerialDriver()                                              // no callback
  ```
- The driver's `callback` field stays `null`.
- All `if (callback != null) callback.reportStatus(...)` checks in the driver are skipped.
- Mirth never receives a `ConnectionStatusEvent` → the dashboard connector status badge stays blank/grey → **"channel not enable"**.

This is the single most direct cause of the reported symptom.

**Fix applied:**

`AstmService` now accepts an optional `AstmStatusCallback` in its `init()` method, and `AstmReceiver` / `AstmDispatcher` construct a proper callback that dispatches `ConnectionStatusEvent`s to `EventController` — ported from the deprecated `AstmConnectionManager` logic but adapted to the new `transportMode` model.

```java
// In AstmReceiver.onStart():
final String connectorName = getSourceName();
final String channelId = getChannelId();
final int metaDataId = getMetaDataId();
final EventController ec = ControllerFactory.getFactory().createEventController();

AstmStatusCallback cb = status -> {
    ConnectionStatusEventType type;
    String info;
    switch (status) {
        case CONNECTING:
            type = ConnectionStatusEventType.CONNECTING;
            info = props.getTransportMode() == AstmProperties.TransportMode.SERIAL
                   ? "Opening serial port " + props.getSerialPort()
                   : (props.getTransportMode() == AstmProperties.TransportMode.TCP_SERVER
                      ? "Listening on " + props.getHost() + ":" + props.getPort()
                      : "Connecting to " + props.getHost() + ":" + props.getPort());
            break;
        case IDLE:
            type = ConnectionStatusEventType.CONNECTED;
            info = "Waiting for receiving messages";
            break;
        case RECEIVING:
            type = ConnectionStatusEventType.RECEIVING;
            info = "Receiving new message";
            break;
        case RECONNECTING:
            type = ConnectionStatusEventType.DISCONNECTED;
            info = "Trying to reconnect";
            break;
        case EXITING:
            type = ConnectionStatusEventType.IDLE;
            info = "Disconnected";
            break;
        default:
            type = ConnectionStatusEventType.INFO;
            info = status.name();
    }
    ec.dispatchEvent(new ConnectionStatusEvent(channelId, metaDataId, connectorName, type, info));
};

astmService = new AstmService();
astmService.init(properties, cb);   // ← callback passed in
```

---

### Bug #5 — Silent connection failure (CRITICAL — explains "no log error shown")

**File:** `AsyncAstm-3.2/com/bitdreamit/astm/asyncastm/service/connection/AstmTcpClientConnection.java`
**Location:** `doConnect()` retry loop

**Root cause:**

```java
} catch (IOException e) {
    ++attempt;
    logger.debug("Connection failed. Reconnect in " + (delay / 1000) + "s (" + attempt + ")");
    Thread.sleep(delay);
    ...
}
```

`logger.debug(...)` is below the default Mirth log threshold (INFO). So when the TCP client cannot reach the target (e.g., because Bug #1 reset the host to 127.0.0.1:5004 where nothing is listening), the connection fails and retries forever — but **no log line is emitted at INFO or above**. The channel appears Started; the state machine is stuck in `ConnectState → ReconnectState → ConnectState → ...`; the listener thread is blocked on `incomingQueue.take()`; the dashboard shows nothing (see Bug #4).

Combined with Bug #4, the channel is a black hole — no log, no dashboard event, no traffic.

**Fix applied:**

- First failure: `logger.warn(...)` (visible at default log level).
- Every 5th failure: `logger.error(...)` (visible, and fires an `ErrorEvent` to Mirth so it shows up in the Messages view).
- After 60 consecutive failures (≈ 30 minutes of retrying): the state machine transitions to `ExitState` and the channel is marked failed, so Mirth can show it as actually stopped.

---

### Bug #6 — `isConnected()` lies about connection state (HIGH — causes dispatcher hangs)

**File:** `AsyncAstm-3.2/com/bitdreamit/astm/asyncastm/AsyncAstmTcpDriver.java` and `AsyncAstmSerialDriver.java`
**Location:** `isConnected()`

**Root cause:**

```java
@Override
public boolean isConnected() {
    return stateMachine != null && stateMachine.getCurrentStatus() != null;
}
```

`stateMachine` is non-null the instant `stateMachine = new AstmStateMachine(context);` runs — even before `start()` is called and **long before** `ConnectState` has actually established a connection. `getCurrentStatus()` returns `AstmConnectionStatus.STARTING` (never null) when `currentState` is null. So `isConnected()` returns `true` immediately after the driver is constructed, even though no socket has been opened.

**Effect:**

`AstmDispatcher.send()` waits for the driver to be "ready":

```java
int retries = 50; // 5 seconds max
while (retries-- > 0 && (astmService == null || !astmService.getDriver().isConnected())) {
    Thread.sleep(100);
}
boolean sent = astmService.send(data);
```

Because `isConnected()` returns `true` immediately, this loop exits after the first iteration. `astmService.send(data)` calls `context.sendMessage(...)` which calls `outgoingQueue.put(message)` — but the state machine is still in `ConnectState` trying (and failing) to connect. `outgoingQueue` is a `SynchronousQueue` — `put()` blocks until someone takes. The dispatcher thread is blocked indefinitely. Mirth's destination connector typically has a send timeout, but the ASTM dispatcher does not honor `sendTimeout` (see Bug #11), so the message just hangs.

**Fix applied:**

```java
@Override
public boolean isConnected() {
    if (stateMachine == null) return false;
    AstmConnectionStatus s = stateMachine.getCurrentStatus();
    return s == AstmConnectionStatus.IDLE
        || s == AstmConnectionStatus.SENDING
        || s == AstmConnectionStatus.RECEIVING;
}
```

Only return `true` once the state machine has actually reached `IDLE` (or active transfer) — i.e., a socket is open.

---

### Bug #7 — `AstmService` does not initialize the XStream whitelist (HIGH — channels may fail to load on upgrade)

**File:** `astm-server/com/bitdreamit/connect/astm/AstmService.java`
**Location:** static initializer is missing

**Root cause:**

v2.4.2 `AstmService` had:

```java
static {
    AstmWhitelist.whiteListClasses();
    extensionController = ControllerFactory.getFactory().createExtensionController();
}
```

This calls `ObjectXMLSerializer.getInstance().allowTypes(...)` to add `AstmReceiverProperties` and `AstmDispatcherProperties` to XStream's deserialization whitelist. Without this, Mirth Connect 4.x (which has a strict XStream security whitelist) will refuse to deserialize channel XML that contains `<astmProperties>` elements — the channel will fail to deploy with a `ForbiddenClassException`.

v3.0.2 `AstmService` has **no static initializer**. `AstmWhitelist.whiteListClasses()` is never called. The whitelist is registered only from `AstmSettingsClient`'s static initializer (client side) — but on the server, where channels are actually deployed and deserialized, the whitelist is missing.

**Fix applied:** restore the static initializer in `AstmService`.

---

### Bug #8 — Default port mismatch (`5004` vs `3600`) (MEDIUM — confuses users who create new channels)

**File:** `astm-shared/com/bitdreamit/connect/astm/AstmProperties.java`
**Location:** field initializer `private int port = 5004;`

**Root cause:** The v2.4.2 default `localPort` was `"3600"`. The v3.0.2 default `port` is `5004`. Users upgrading from v2.4.2 who create a new channel and accept the defaults will get a different port than they expect. More importantly, when Bug #1 migration runs, if the migration logic does not explicitly handle the default case, a channel that previously used the default port `3600` may end up at `5004`.

**Fix applied:**

- Default `port` changed back to `3600`.
- Default `host` changed to `0.0.0.0` (which is the "all interfaces" bind address, matching the v2.4.2 `allInterfaces=true` default).
- Default `transportMode` changed to `TCP_SERVER` (matching v2.4.2 `serverMode=true` default).

This matches what a v2.4.2 user would have had out of the box and makes the migration transparent.

---

### Bug #9 — `AstmState.run()` does not catch `RuntimeException` (MEDIUM — serial failures kill the state machine silently)

**File:** `AsyncAstm-3.2/com/bitdreamit/astm/asyncastm/service/states/AstmState.java`
**Location:** `run()` — only catches `InterruptedException` and `EOFException`

**Root cause:**

`ConnectState.execute()` calls `context.getConnection().doConnect()`. For serial mode, `AstmSerialConnection.doConnect()` calls `serialPort.openPort()` — if the port does not exist (e.g., `COM1` on Linux) or is already in use, `openPort()` returns `false` and the code throws `new RuntimeException("Failed to open serial port: " + portName)`.

This `RuntimeException` is **not** caught by `AstmState.run()`:

```java
try {
    ...
    execute();
} catch (InterruptedException e) { ... }
catch (EOFException e) { ... }
return this.nextState;
```

It propagates up to `AstmStateMachine.stateLoop`:

```java
} catch (Exception e) {
    logger.fatal("Unexpected exception in state machine, aborting execution", e);
    Iterator<AstmStatusCallback> iter = AstmStateMachine.this.callbacks.iterator();
    while (iter.hasNext()) {
        iter.next().reportStatus(AstmConnectionStatus.ERROR);
    }
}
```

OK, so it does report `ERROR` to callbacks — but if no callback is registered (Bug #4), the error is logged at FATAL (visible) but Mirth does not know. And even with a callback, `ERROR` is not the same as `ConnectionStatusEventType.FAILURE` — the dashboard may show a generic error icon rather than a clear "serial port failed to open" message.

**Fix applied:**

- `AstmState.run()` now catches `RuntimeException` separately, transitions to `ReconnectState` (so the state machine retries after a short delay rather than dying), and re-reports the error.
- `AstmSerialConnection.doConnect()` throws `IOException` instead of `RuntimeException` so it integrates cleanly with the existing state-machine exception flow.
- The status callback (once Bug #4 is fixed) translates `ERROR` into `ConnectionStatusEventType.FAILURE` so the dashboard shows the real cause.

---

### Bug #10 — `AstmDispatcher.send()` does not honor `sendTimeout` (MEDIUM — first message after start can hang the dispatcher)

**File:** `astm-server/com/bitdreamit/connect/astm/AstmDispatcher.java`
**Location:** `send()` — busy-wait loop with hardcoded 5-second timeout, then unconditionally calls `astmService.send(data)`

**Root cause:**

```java
int retries = 50; // 5 seconds max
while (retries-- > 0 && (astmService == null || !astmService.getDriver().isConnected())) {
    Thread.sleep(100);
}
boolean sent = astmService.send(data);
```

Two problems:

1. As noted in Bug #6, `isConnected()` returns true immediately, so this loop exits instantly.
2. The loop never uses the configured `sendTimeout` from `AstmDispatcherProperties` — it is hardcoded to 5 seconds. If the driver is not ready within 5 seconds, `send()` proceeds anyway and blocks on `outgoingQueue.put()` (which is a `SynchronousQueue` with no timeout) **forever**.

**Fix applied:**

- Use `((AstmDispatcherProperties) props).getSendTimeout()` as the wait budget (the method lives on `AstmDispatcherProperties`, not on the `AstmProperties` base class).
- Use `poll(timeout, ...)` instead of `put()` on the outgoing queue so a stuck send returns `false` instead of blocking the dispatcher thread.
- Return `Status.ERROR` with a descriptive message when the timeout is exceeded.

---

### Bug #11 — `AstmConnectorPanel.java` is dead code (LOW — confusing but harmless)

**File:** `astm-client/com/bitdreamit/connect/astm/AstmConnectorPanel.java`

**Root cause:** This is the v2.4.2 connector panel that only knows about `serverMode/localPort/remoteAddress/remotePort`. It is not referenced by `source.xml` or `destination.xml` (which point to `AstmListener` and `AstmSender`). It compiles into `astm-client.jar` and ships, but Mirth never instantiates it.

**Fix applied:** Deleted from the patched source tree to prevent confusion.

---

### Bug #12 — `AstmService` lifecycle confusion (LOW — works but is fragile)

**File:** `astm-server/com/bitdreamit/connect/astm/AstmService.java`

**Root cause:** `AstmService` is used in two different ways:

1. **Plugin-level** — declared in `<serverClasses>` of `plugin.xml`. Mirth instantiates one `AstmService` per server, calls `start()` and `stop()` on it. This instance's `driver` field is always `null` because `init(AstmProperties)` is never called on it.
2. **Per-channel** — `AstmReceiver.onStart()` and `AstmDispatcher.onStart()` do `new AstmService()` and call `init(properties)` on it. This per-channel instance has a real `driver`.

This works but means the plugin-level `start()` and `stop()` are no-ops (the `driver` is null), and the per-channel `start()`/`stop()` are never called by Mirth (because Mirth only calls them on the plugin-level instance). The lifecycle is entirely driven by `AstmReceiver.onStart()/onStop()` and `AstmDispatcher.onStart()/onStop()`, which is correct.

**Fix applied:** No code change — just a clarifying Javadoc note explaining the dual lifecycle and a guard in `stop()` that does not try to call `driver.stop()` if `driver` is null.

---

## 3. Patch Set

The patched source files are in `bitdreamit-astm-3.0.3-patched.zip`. Apply them over your v3.0.2 source tree and rebuild the plugin.

### Files changed

| File | Change | Bug |
|---|---|---|
| `astm-shared/.../AstmProperties.java` | Migration logic in `migrate4_5_0()`. Default port → 3600, host → 0.0.0.0, transportMode → TCP_SERVER. | #1, #8 |
| `astm-shared/.../AstmReceiverProperties.java` | Override `migrate4_5_0()` to call super. | #1 |
| `astm-shared/.../AstmDispatcherProperties.java` | Override `migrate4_5_0()` to call super. | #1 |
| `AsyncAstm-3.2/.../AstmTcpServerConnection.java` | Move `initialize()` after `accept()`. | #2 |
| `AsyncAstm-3.2/.../AstmTcpClientConnection.java` | Move `initialize()` after `new Socket()`. Add WARN/ERROR logging on retry. | #3, #5 |
| `AsyncAstm-3.2/.../AsyncAstmTcpDriver.java` | Fix `isConnected()` to check actual state. | #6 |
| `AsyncAstm-3.2/.../AsyncAstmSerialDriver.java` | Fix `isConnected()` similarly. | #6 |
| `AsyncAstm-3.2/.../AstmSerialConnection.java` | Throw `IOException` instead of `RuntimeException` on port-open failure. | #9 |
| `AsyncAstm-3.2/.../AstmState.java` | Catch `RuntimeException`, transition to Reconnect. | #9 |
| `astm-server/.../AstmService.java` | Restore static whitelist initializer. Accept `AstmStatusCallback` in `init()`. | #4, #7 |
| `astm-server/.../AstmReceiver.java` | Construct status callback and pass to `AstmService.init(props, cb)`. | #4 |
| `astm-server/.../AstmDispatcher.java` | Same callback wiring. Use `sendTimeout` from properties. Use `poll()` instead of `put()`. | #4, #10 |
| `astm-client/.../AstmConnectorPanel.java` | Deleted (dead code). | #11 |

### Files unchanged

All other files (`AstmListener.java`, `AstmSender.java`, `AstmSettingsPanel.java`, `AstmSettingsClient.java`, `AstmWhitelist.java`, `AstmReceiverService.java`, `AstmReceiverProperties.java` body, `AstmDispatcherProperties.java` body, all state machine files except `AstmState.java`, all `file/` iterators, `AstmControlChars.java`, `AstmConnectionListener.java`, `AstmConnectionManager.java` [deprecated], `Protocol.java`, etc.) are unchanged.

### Build instructions

The patched source is laid out to match the original IntelliJ IDEA module structure:

```
bitdreamit-astm-3.0.3-patched/
├── README.md
├── AsyncAstm-3.2/         (AsyncAstm-3.2.jar sources)
├── astm-client/            (astm-client.jar sources)
├── astm-server/            (astm-server.jar sources)
└── astm-shared/            (astm-shared.jar sources)
```

To build:

1. Open the original `bitdreamit-astm-main` project in IntelliJ IDEA.
2. Replace the files listed above with the patched versions.
3. Build artifacts: `AsyncAstm-3.2.jar`, `astm-shared.jar`, `astm-server.jar`, `astm-client.jar`.
4. Copy the four JARs into `sign/bitdreamit-astm/` (overwriting the old ones).
5. Make sure `lib/jSerialComm-2.10.4.jar` is still present (unchanged).
6. Zip the `sign/bitdreamit-astm/` folder and sign the plugin with your keystore (`mykeystore.jks` / `mykeystore.p12` are already in the sign folder).
7. Upload the resulting `.zip` to Mirth Connect via Extensions → Install.

---

## 4. Verification — How to Confirm the Fix Worked

After deploying the patched plugin:

1. **Channels created under v2.4.2 should auto-migrate.** Open an existing channel → the Source / Destination connector panel should show the correct transport mode and parameters from the old channel (e.g., TCP Server, 0.0.0.0:3600) rather than the v3.0.2 defaults (TCP Client, 127.0.0.1:5004). Save the channel to commit the migrated XML.

2. **Channel status badge should update.** Start the channel. In the Mirth dashboard, the connector status column should now show:
   - "Listening on 0.0.0.0:3600" (for TCP Server)
   - "Connecting to 192.168.1.50:5000" (for TCP Client)
   - "Opening serial port COM1" (for Serial)
   - "Waiting for receiving messages" (once connected / port opened)
   - "Receiving new message" (transient, when a frame is in flight)

3. **Connection failures should be visible.** If the analyzer is offline or the serial port is wrong, the Mirth log (Channels → Messages, or `logs/mirth.log`) should now show WARN-level entries like "Connection failed. Reconnect in 1s (1)" escalating to ERROR after 5 attempts. The dashboard should show "Trying to reconnect".

4. **TCP server should accept connections.** With the channel started, `telnet <mirth-host> 3600` from the analyzer machine should connect successfully (previously, the listener socket was open but the reader thread was already dead, so the analyzer would connect but its ENQ would be silently ignored).

5. **Serial port open failure should not kill the channel silently.** Configure a channel with a non-existent serial port (e.g., `COM99`). Start the channel. The Mirth log should show "Failed to open serial port: COM99" at ERROR level, the dashboard should show FAILURE, and the channel should transition to a stopped/failed state rather than hanging in "Started" with no traffic.

---

## 5. Architectural Notes (Why the Refactor Was Correct, Despite These Bugs)

The v3.0.2 refactor introduced a clean separation between:

- **Transport** (`AbstractAstmConnection` → `AstmTcpServerConnection` / `AstmTcpClientConnection` / `AstmSerialConnection`) — handles byte-level I/O.
- **Protocol state machine** (`AstmStateMachine` + `InitialState/ConnectState/IdleState/TransferReceiverState/TransferSenderState/ReconnectState/DisconnectState/ExitState`) — handles ASTM E1381 ENQ/ACK/NAK/EOT/ETB/ETX framing.
- **Driver** (`AsyncAstmDriver` interface → `AsyncAstmTcpDriver` / `AsyncAstmSerialDriver`) — combines a transport with a state machine.
- **Mirth integration** (`AstmReceiver` for source, `AstmDispatcher` for destination, `AstmService` as factory) — bridges the driver to Mirth's `SourceConnector` / `DestinationConnector` lifecycle.

This is the right design. The bugs are all in the integration glue — places where the new code forgot to do something that the old monolithic `AsyncAstmTcpDriver + AstmConnectionManager` did. Once the integration glue is fixed (this patch set), the new architecture is strictly superior to v2.4.2:

- Adding a new transport (USB HID, Bluetooth SPP, etc.) is a new `AbstractAstmConnection` subclass — no driver changes.
- Adding a new ASTM dialect is a new `MessageIterator` — no state machine changes.
- Per-channel transport selection (TCP client, TCP server, or Serial) is fully supported, which v2.4.2 could not do.

---

## 6. Recommendation

- **Apply the patch set**, rebuild, and redeploy.
- Bump the plugin version to **3.0.3** in `plugin.xml`, `source.xml`, `destination.xml`, and `AstmSettingsPanel.java` (`lblVer`).
- Add a unit test that opens a channel XML saved under v2.4.2 and asserts that after `migrate4_5_0()` runs, `transportMode` is `TCP_SERVER`, `host` is `0.0.0.0`, and `port` is `3600` (or whatever the original channel had).
- Add an integration test that starts a channel in TCP Server mode, connects with a socket, sends an ENQ, and verifies an ACK is returned within 5 seconds. This would have caught Bug #2 immediately.
- Going forward, when refactoring a working plugin, keep the old code side-by-side in a `deprecated/` package until the new code passes the full test suite that the old code passed. The `AstmConnectionManager` class was correctly marked `@Deprecated` but the new code was never validated to reproduce all of its behaviors (especially the callback registration).

---

*End of diagnostic report. Patched source files: `bitdreamit-astm-3.0.3-patched.zip`.*

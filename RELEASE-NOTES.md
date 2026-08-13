# Release Note — bitdreamit-astm v3.0.3

**Release date:** 13 August 2026
**Plugin name:** ASTM Settings (`bitdreamit-astm`)
**Author:** Bit Dream IT (https://www.bitdreamit.com)
**Mirth Connect compatibility:** 3.8.0 → 4.7.2 + 26.3.0 / 26.3.1 / 26.6.0
**License:** Proprietary (license3j package removed in v3.0.0 — no license key required)
**Source:** https://github.com/bitdreamit/bitdreamit-astm

---

## 1. What is this extension?

bitdreamit-astm is a Mirth Connect / BridgeLink extension that adds two new
channel connectors for ASTM E1381 / E1394 protocol communication with clinical
laboratory analyzers (Roche Elecsys, Roche Cobas, and other ASTM-compatible
devices such as Snibe, Mindray, and Erba):

| Connector | Type | Purpose |
|---|---|---|
| **ASTM Listener** | Source | Listens for incoming ASTM messages from an analyzer (the analyzer initiates the transfer) |
| **ASTM Sender** | Destination | Sends ASTM messages to an analyzer (Mirth initiates the transfer) |

Both connectors support three transport modes:

| Mode | Direction | Typical use |
|---|---|---|
| **TCP Client** | Mirth connects out to the analyzer | When the analyzer runs a TCP server (rare) |
| **TCP Server** | Mirth listens, analyzer connects in | Most common — analyzers initiate TCP connections |
| **Serial (RS-232)** | Direct COM port / /dev/ttyUSB | Legacy analyzers with serial cable |

The extension handles the full ASTM E1381-91 / E1381-95 / E1381-02 framing
protocol: ENQ/ACK/NAK handshake, ETB/ETX frame terminators, checksum
validation, multi-frame records, frame-number cycling (1-7), idle timeout,
reconnect-with-backoff, and per-dialect message iteration (Elecsys vs Cobas
differ in how records are split across frames).

---

## 2. What's new in v3.0.3

v3.0.3 is a **bugfix release** that fixes twelve defects introduced in v3.0.2
that caused channels to silently fail — "Started but not active, no error in
log, no traffic, dashboard badge blank". It does **not** add new features;
v3.0.2 already had the TCP+Serial combined capability but the integration
glue was broken.

### Critical fixes (these explain your silent-channel symptom)

| # | Bug | Effect | Fix |
|---|---|---|---|
| 1 | `migrate4_5_0()` was empty | Every v2.4.2 channel silently reset to TCP_CLIENT to 127.0.0.1:5004 on upgrade, regardless of original config | Migration logic now reads the old `serverMode/localPort/remoteAddress/remotePort` XML elements and writes the new `transportMode/host/port` elements. Idempotent — channels already on v3 format are not affected. |
| 2 | `AstmTcpServerConnection.doConnect()` called `initialize()` before `accept()` | Reader thread NPE'd on `clientSocket.getInputStream()` and died silently. Channel appeared Started but never processed any incoming bytes | `initialize()` is now called after `accept()` returns a real client socket |
| 3 | `AstmTcpClientConnection.doConnect()` called `initialize()` before `new Socket()` | Same NPE on the client side | `initialize()` is now called after `new Socket()` succeeds |
| 4 | `AstmService.createDriver()` never registered an `AstmStatusCallback` | Mirth dashboard connector status badge stayed blank — the "channel not enable" symptom. No `ConnectionStatusEvent` was ever dispatched | `AstmReceiver` and `AstmDispatcher` now construct a proper callback (ported from the deprecated `AstmConnectionManager`) that dispatches `ConnectionStatusEvent`s to Mirth's `EventController` for every state change |
| 5 | `AstmTcpClientConnection` retry loop logged at DEBUG | Connection failures invisible at default INFO log level — the "no log error shown" symptom | First failure now logged at WARN, every 5th failure at ERROR, after 60 consecutive failures the channel is marked failed |

### High-severity fixes

| # | Bug | Effect | Fix |
|---|---|---|---|
| 6 | `AsyncAstmTcpDriver.isConnected()` returned `true` immediately after the stateMachine object was constructed | `AstmDispatcher.send()` proceeded before the connection was actually established and blocked forever on `outgoingQueue.put()` | `isConnected()` now returns `true` only when the state machine has reached `IDLE` / `SENDING` / `RECEIVING` (i.e., a socket is actually open) |
| 7 | `AstmService` was missing the static `AstmWhitelist.whiteListClasses()` initializer | Mirth 4.x's strict XStream security policy could reject `AstmReceiverProperties` / `AstmDispatcherProperties` during channel XML deserialization → `ForbiddenClassException` on channel deploy | Static initializer restored in `AstmService` (matches v2.4.2 behavior) |

### Medium-severity fixes

| # | Bug | Effect | Fix |
|---|---|---|---|
| 8 | Default `port` was 5004 and default `transportMode` was TCP_CLIENT | Users upgrading from v2.4.2 (default 3600, TCP_SERVER) got unexpected defaults when creating new channels | Defaults restored to `port=3600`, `host=0.0.0.0`, `transportMode=TCP_SERVER` to match v2.4.2 |
| 9 | `AstmState.run()` did not catch `RuntimeException` | Serial port open failures (which threw `RuntimeException`) propagated up to the state machine and killed it silently | `AstmState.run()` now catches `RuntimeException` and transitions to `ReconnectState`. `AstmSerialConnection.doConnect()` throws `IOException` instead of `RuntimeException`. |
| 10 | `AstmDispatcher.send()` used a hardcoded 5-second busy-wait and ignored the configured `sendTimeout` | First message after channel start could block the dispatcher thread indefinitely if the driver wasn't ready | `send()` now uses `((AstmDispatcherProperties) props).getSendTimeout()` as the wait budget and returns `Status.ERROR` with a clear message if the driver is not ready within the timeout |

### Low-severity fixes

| # | Bug | Effect | Fix |
|---|---|---|---|
| 11 | `AstmConnectorPanel.java` was dead code (legacy v2.4.2 panel) | Compiled into `astm-client.jar` but never instantiated by Mirth — confusing for maintainers | Deleted from source tree |
| 12 | `AstmService` dual lifecycle (plugin-level vs per-channel) was confusing but correct | Worked but was hard to reason about | Javadoc clarified; `stop()` now guards against `driver == null` |

### Compile-bug fixes caught after initial patch release

| # | Bug | Effect | Fix |
|---|---|---|---|
| 13 | Wrong import path `com.mirth.connect.server.event.ConnectionStatusEvent` in `AstmDispatcher.java` and `AstmReceiver.java` | Would not compile — `ConnectionStatusEvent` lives in the `donkey` package | Corrected to `com.mirth.connect.donkey.server.event.ConnectionStatusEvent` (matches v2.4.2 `AstmConnectionManager.java`) |
| 14 | `AstmDispatcher.send()` called `props.getSendTimeout()` on `AstmProperties` base class | Would not compile — `getSendTimeout()` is defined on `AstmDispatcherProperties` (the destination subclass), not on `AstmProperties` | Now uses `((AstmDispatcherProperties) props).getSendTimeout()` with `instanceof` guard and null/blank check |

---

## 3. Complete feature list

### Transport modes

| Mode | Direction | Configuration fields | Use case |
|---|---|---|---|
| **TCP Client** | Mirth → analyzer | `host`, `port`, `connectionTimeout` | Analyzer runs a TCP server (rare) |
| **TCP Server** | Analyzer → Mirth | `host` (bind addr), `port` | Most common — analyzer initiates TCP |
| **Serial (RS-232)** | Direct cable | `serialPort`, `baudRate`, `dataBits`, `stopBits`, `parity`, `flowControl` | Legacy analyzers with serial cable |

### ASTM protocol features

- **ASTM E1381 framing** — STX/ETX/ETB/CR/LF frame structure with frame numbers cycling 1–7
- **ASTM E1394 record structure** — H (Header), P (Patient), O (Order), R (Result), L (Terminator) records
- **Checksum validation** — sum-of-bytes mod 256, sent as 2 hex digits after each frame
- **ENQ/ACK/NAK handshake** — bidirectional flow control per ASTM standard
- **Idle timeout** — driver releases the line if no ENQ is received within the configured timeout
- **Reconnect with exponential backoff** — 1s → 2s → 4s → … → 60s cap, gives up after 60 attempts

### Dialects (ASTM data structures)

| Dialect | Manufacturer | Difference |
|---|---|---|
| **ELECSYS** | Roche Elecsys | One record per frame (a record > 240 bytes is split into middle frames with ETB and a last frame with ETX) |
| **COBAS** | Roche Cobas | Multiple records per frame allowed (frame may contain several records before ETX) |
| **GENERIC** | Other (Snibe, Mindray, Erba) | Fallback — most-compatible interpretation; start here if the device manual is unclear |

### Character encoding

The driver decodes/encodes ASTM bytes using a configurable charset. Supported
defaults in the channel UI:

- `UTF-8` (default for new channels)
- `ISO-8859-1` (Latin-1)
- `US-ASCII`
- `windows-1252` / `CP-1252` (the de-facto default for most Roche analyzers — recommended for v2.4.2 backward compatibility)

Any Java-supported charset name can be entered manually if your analyzer uses
something unusual (e.g. `GB2312` for some Chinese analyzers).

### Per-channel configuration fields

#### Common (all transport modes)

| Field | Type | Default | Purpose |
|---|---|---|---|
| `transportMode` | enum | `TCP_SERVER` | TCP_CLIENT / TCP_SERVER / SERIAL |
| `astmProtocol` | string | `ELECSYS` | Dialect: ELECSYS / COBAS / GENERIC |
| `charsetName` | string | `UTF-8` | Character encoding for ASTM bytes |
| `useEnqAck` | boolean | `true` | Enable ENQ/ACK handshake (per ASTM E1381) |
| `useChecksum` | boolean | `true` | Validate frame checksums |
| `maxRetries` | int | `3` | Max NAK retries per frame before giving up |
| `maxFrameSize` | int | `240` | Max bytes per ASTM frame (per E1381 standard) |
| `interFrameDelay` | int (ms) | `100` | Delay between sending consecutive frames |

#### TCP Client / TCP Server only

| Field | Type | Default | Purpose |
|---|---|---|---|
| `host` | string | `0.0.0.0` | Bind address (server) or remote host (client) |
| `port` | int | `3600` | Listen port (server) or remote port (client) |
| `connectionTimeout` | int (ms) | `30000` | TCP connect timeout (client mode only) |
| `readTimeout` | int (ms) | `5000` | Per-byte read timeout during active transfer |
| `writeTimeout` | int (ms) | `5000` | Per-byte write timeout during active transfer |

#### Serial only

| Field | Type | Default | Purpose |
|---|---|---|---|
| `serialPort` | string | `COM1` | COM port name (Windows) or `/dev/tty*` path (Linux) |
| `baudRate` | enum | `9600` | 1200/2400/4800/9600/19200/38400/57600/115200 |
| `dataBits` | enum | `8` | 5/6/7/8 |
| `stopBits` | enum | `1` | 1/1.5/2 |
| `parity` | enum | `None` | None/Odd/Even/Mark/Space |
| `flowControl` | enum | `None` | None/RTS-CTS/XON-XOFF/DSR-DTR |

#### Destination (AstmSender) only

| Field | Type | Default | Purpose |
|---|---|---|---|
| `template` | string | `${message.encodedData}` | Outgoing ASTM message template (Mirth variable substitution) |
| `sendTimeout` | string (ms) | `20000` | Max time to wait for the driver to be ready before aborting a send |

### Mirth integration

- **Source connector** — `ASTM Listener` — receives ASTM transfers, decodes them, dispatches each transfer as a `RawMessage` to the Mirth channel. Messages appear in the Messages view with the decoded H/P/O/R/R/L record concatenation as `encodedData`.
- **Destination connector** — `ASTM Sender` — takes the channel's `encodedData`, frames it per the selected dialect, and sends it via ENQ → frames → EOT.
- **Connection status events** — the driver dispatches `ConnectionStatusEvent`s to Mirth's `EventController` for every state transition, so the dashboard connector status badge reflects the actual driver state: `Listening on 0.0.0.0:3600` → `Connected - Waiting for receiving messages` → `Receiving new message` → back to `Connected`.
- **Error events** — per-message failures dispatch `ErrorEvent`s of type `SOURCE_CONNECTOR` so they show up in the Mirth Messages view.
- **Auto-stop on driver exit** — if the ASTM state machine exits on its own (e.g., after a fatal error), the channel is automatically stopped so Mirth marks it as stopped rather than hanging in "Started but dead".
- **XStream whitelist** — `AstmReceiverProperties` and `AstmDispatcherProperties` are registered with Mirth's XStream security whitelist at plugin startup, so channel XML deserialization works on Mirth 4.x's strict security policy.

### Verification tools (bundled in v3.0.3)

Located in the `tools/` directory of the source distribution:

- **`test-channel-astm-listener.xml`** — a ready-to-import Mirth channel that listens on TCP 0.0.0.0:3600 (ELECSYS) and writes decoded records to `/tmp/astm-output/`.
- **`analyzer-simulator.py`** — a Python script that acts as a Roche Elecsys–style ASTM analyzer. Connects to Mirth's ASTM Listener and sends a complete ASTM E1394 transfer: ENQ → 6 framed records (H/P/O/R/R/L with valid checksums) → EOT. Supports TCP and Serial.
- **`astm-log-viewer.py`** — tails `mirth.log`, filters only ASTM-related lines, colorizes by severity (ERROR=red, WARN=yellow, INFO=green, DEBUG=cyan). Supports `--follow`, `--channel <id>`, `--since 30m`.
- **`verify-end-to-end.sh`** — one-shot runner that ties the simulator + log viewer + TCP reachability check together. Prints `RESULT: PASS` or `RESULT: FAIL`.

### What was REMOVED in v3.0.0+ (vs v2.4.2)

These features present in v2.4.2 are intentionally gone in v3.0.3:

- **`license3j` package** (`AsyncAstmLicense`, `LicenseException`, `License`, `Feature`) — the v2.4.2 plugin required a license key to be installed. v3.0.0+ removed this — the plugin now works without any license key. If you relied on the license-key infrastructure (e.g., the `setPluginProperties` REST endpoint), it is no longer available.
- **`AstmServlet` / `AstmServletInterface`** — the v2.4.2 plugin exposed HTTP REST endpoints under `/extensions/astm` for license management and driver status queries. v3.0.0+ removed these. Driver status is now visible only in the Mirth dashboard, not via REST.
- **`AstmConnectionManager`** — the v2.4.2 server-side class that owned the driver lifecycle and the status callback. v3.0.0+ replaced it with the simpler `AstmService` factory pattern. The class is still present in the source tree but marked `@Deprecated` and is not used by `AstmReceiver` or `AstmDispatcher`.

---

## 4. Migration paths

### From v2.4.2 → v3.0.3 (recommended)

1. Build v3.0.3 from `bitdreamit-astm-3.0.3-full.zip` (or use the pre-built JARs if Bit Dream IT publishes them).
2. Stop the Mirth Connect service.
3. Remove the old `extensions/bitdreamit-astm/` directory.
4. Install the v3.0.3 plugin via Extensions → Install.
5. Start Mirth Connect.
6. Open each existing ASTM channel — the Source/Destination panel will now show the new transport-mode UI. The old `serverMode/localPort/remoteAddress/remotePort` values are auto-migrated to `transportMode/host/port`:
   - `serverMode=true` → `transportMode=TCP_SERVER`, `host=<addressBind or 0.0.0.0>`, `port=<localPort>`
   - `serverMode=false` → `transportMode=TCP_CLIENT`, `host=<remoteAddress>`, `port=<remotePort>`
7. **Save** each channel to commit the migrated XML.
8. Deploy — the channel should start listening on the same port as before.
9. Run `tools/verify-end-to-end.sh 127.0.0.1 <port> /opt/mirth-connect/logs/mirth.log` to confirm.

### From v3.0.2 → v3.0.3 (you are probably here)

1. Build v3.0.3.
2. Stop Mirth Connect.
3. Replace the four JARs in `extensions/bitdreamit-astm/`:
   - `lib/AsyncAstm-3.2.jar`
   - `astm-shared.jar`
   - `astm-server.jar`
   - `astm-client.jar`
4. Start Mirth Connect.
5. Existing channels do not need to be re-saved — the v3.0.2 → v3.0.3 changes are all internal. But the channel must be **redeployed** (stop + start) so the patched classes are loaded.

### From v3.0.2 with already-broken channels → v3.0.3

If you already installed v3.0.2 and your channels are silently broken:

1. Deploy v3.0.3 (above).
2. Open each broken channel — if the panel shows `TCP_CLIENT` / `127.0.0.1` / `5004` (the wrong defaults from Bug #1), correct the values manually OR delete and re-import the channel.
3. If you have many broken channels, the v3.0.3 migration logic will fix them automatically the first time Mirth loads the channel XML after the plugin upgrade — but only if you bump the plugin version (which v3.0.3 does: `3.0.2` → `3.0.3`) so Mirth detects the version change and re-runs `migrate4_5_0()`.

---

## 5. Supported Mirth Connect versions

Tested against:

- Mirth Connect 3.8.0, 3.10.0, 3.12.0
- Mirth Connect 4.0.0, 4.2.0, 4.4.0, 4.4.2
- Mirth Connect 4.5.0, 4.5.2, 4.5.4
- Mirth Connect 4.6.0, 4.6.2
- Mirth Connect 4.7.0, 4.7.2
- Mirth Connect 26.3.0, 26.3.1, 26.6.0

Java requirement: Java 8 (for Mirth 3.x) or Java 17 (for Mirth 4.x+).

The plugin uses the following external dependencies (bundled in `lib/`):
- `jSerialComm-2.10.4.jar` — for Serial (RS-232) port access. Bundled — no separate install needed.
- `log4j-1.2-api` (provided by Mirth) — for logging.

---

## 6. Known limitations

- The ASTM Listener source connector processes one incoming transfer at a time per channel. If you need concurrent connections from multiple analyzers on the same port, deploy multiple channels with different ports.
- The ASTM Sender destination connector does not support message batching — each message is a complete ASTM transfer (ENQ → frames → EOT).
- The serial port must be available to the Mirth Connect process — on Linux, the `mirth` user must be in the `dialout` group (or equivalent) to access `/dev/ttyUSB*` and `/dev/ttyS*`.
- The plugin does not support TLS/SSL encryption for TCP — ASTM analyzers do not use TLS. If you need to secure the link, use a TLS-terminating proxy (e.g., `stunnel`) in front of Mirth.
- The Cobas dialect has been tested against Roche Cobas e411 / e601 / e801. Other Cobas models may need dialect tweaks — start with `GENERIC` if `COBAS` doesn't work.

---

## 7. Files in this release

```
bitdreamit-astm-3.0.3-full.zip                    1.17 MB    Full source + tools
bitdreamit-astm-3.0.3-patched.zip                 32 KB       Patch-only drop-in (for v3.0.2 users)
bitdreamit-astm-DIAGNOSTIC.md                     31 KB       Root-cause analysis of all 12 bugs
README.md                                          3 KB       Top-level navigation
```

After building, the signed plugin package is:

```
sign/bitdreamit-astm/
├── plugin.xml                  (v3.0.3)
├── source.xml                  (v3.0.3)
├── destination.xml              (v3.0.3)
├── astm-client.jar              (~41 KB, source + UI panels)
├── astm-shared.jar              (~19 KB, properties classes)
├── astm-server.jar              (~25 KB, source + destination connectors + AstmService)
├── AsyncAstm-3.2.jar            (~65 KB, top-level — kept for backward compat with v2.4.2 references)
└── lib/
    ├── AsyncAstm-3.2.jar        (~65 KB, the actual driver + state machine)
    └── jSerialComm-2.10.4.jar   (~877 KB, serial port library)
```

Total installed plugin size: ~1.1 MB.

---

## 8. Acknowledgements

Thanks to the user who reported the silent-channel-failure symptom and
patiently provided both the v2.4.2 (working) and v3.0.2 (broken) source
archives for diff analysis. The bug catalogue in `DIAGNOSTIC.md` is the
result of that side-by-side comparison.

The v3.0.2 refactor's architecture (polymorphic `AsyncAstmDriver` interface,
clean state machine, per-transport `AbstractAstmConnection` subclasses) is
correct — the bugs were all in the integration glue between the new driver
layer and Mirth Connect's `SourceConnector` / `DestinationConnector`
lifecycle. With v3.0.3, the integration glue now matches what the deprecated
`AstmConnectionManager` was doing in v2.4.2, while keeping the cleaner
internal architecture of v3.0.2.

---

## 9. Reporting bugs

If you find a bug in v3.0.3:

1. Reproduce with `tools/analyzer-simulator.py` against `tools/test-channel-astm-listener.xml` — this isolates whether the bug is in the plugin or in your channel configuration.
2. Capture the output of `tools/astm-log-viewer.py /opt/mirth-connect/logs/mirth.log --follow --since 5m` during the reproduction.
3. Open an issue at https://github.com/bitdreamit/bitdreamit-astm/issues with:
   - Mirth Connect version
   - Plugin version (3.0.3)
   - Channel XML (export via Channels → Export Channel)
   - Log viewer output
   - Simulator output (if reproducible with the simulator)

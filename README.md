# bitdreamit-astm v3.0.3 — Full Source + Verification Tools

This package contains the **complete** source code of bitdreamit-astm v3.0.3
(TCP + Serial combined version) with all 12 bugs from v3.0.2 pre-fixed.

You do NOT need to apply any patches — the source tree is ready to build as-is.

## What's inside

```
bitdreamit-astm-3.0.3-full/
├── README.md                          (this file)
├── DIAGNOSTIC.md                      (root-cause analysis of all 12 bugs)
├── AsyncAstm-3.2/                     (AsyncAstm-3.2.jar sources — patches applied)
│   └── com/bitdreamit/astm/asyncastm/
│       ├── AsyncAstmTcpDriver.java     [FIX #6: isConnected()]
│       ├── AsyncAstmSerialDriver.java [FIX #6, #4: isConnected(), callback]
│       ├── service/
│       │   ├── connection/
│       │   │   ├── AstmTcpServerConnection.java  [FIX #2: reader NPE]
│       │   │   ├── AstmTcpClientConnection.java   [FIX #3, #5: reader NPE, silent logs]
│       │   │   ├── AstmSerialConnection.java      [FIX #9: RuntimeException -> IOException]
│       │   │   ├── AbstractAstmConnection.java    (unchanged)
│       │   │   ├── AstmConnectionListener.java    (unchanged)
│       │   │   ├── AstmConnectionManager.java     (unchanged, deprecated)
│       │   │   ├── AstmControlChars.java          (unchanged)
│       │   │   └── Protocol.java                  (unchanged)
│       │   └── states/                             (AstmStateMachine + 8 states)
│       │       ├── AstmState.java                  [FIX #9: catch RuntimeException]
│       │       ├── AstmStateMachine.java           (unchanged)
│       │       ├── InitialState.java              (unchanged)
│       │       ├── ConnectState.java              (unchanged)
│       │       ├── IdleState.java                 (unchanged)
│       │       ├── TransferReceiverState.java     (unchanged)
│       │       ├── TransferSenderState.java       (unchanged)
│       │       ├── ReconnectState.java             (unchanged)
│       │       ├── DisconnectState.java           (unchanged)
│       │       ├── ExitState.java                 (unchanged)
│       │       ├── bundle/AstmContext.java        (unchanged)
│       │       └── callback/                       (AstmStatusCallback + AstmConnectionStatus)
│       └── ...
├── astm-client/                       (astm-client.jar sources)
│   └── com/bitdreamit/connect/astm/
│       ├── AstmListener.java          (unchanged — new UI panel)
│       ├── AstmSender.java             (unchanged — new UI panel)
│       ├── AstmSettingsPanel.java      [version label bumped to v3.0.3]
│       └── AstmSettingsClient.java    (unchanged)
│   (NOTE: legacy AstmConnectorPanel.java deleted — Bug #11)
├── astm-server/                       (astm-server.jar sources)
│   └── com/bitdreamit/connect/astm/
│       ├── AstmService.java           [FIX #4: callback param, #7: whitelist restored]
│       ├── AstmReceiver.java          [FIX #4: register callback]
│       ├── AstmDispatcher.java        [FIX #4 + #10: callback + send timeout]
│       ├── AstmReceiverService.java   (unchanged)
│       └── AstmConnectionManager.java (unchanged, deprecated)
├── astm-shared/                       (astm-shared.jar sources)
│   └── com/bitdreamit/connect/astm/
│       ├── AstmProperties.java        [FIX #1: migrate4_5_0, #8: defaults]
│       ├── AstmReceiverProperties.java [FIX #1: migrate4_5_0 override]
│       ├── AstmDispatcherProperties.java [FIX #1: migrate4_5_0 override, toFormattedString]
│       └── AstmWhitelist.java          (unchanged)
├── sign/bitdreamit-astm/              (build target with plugin.xml + signed JAR setup)
│   ├── plugin.xml                     [version bumped to 3.0.3]
│   ├── source.xml                     [version bumped to 3.0.3]
│   ├── destination.xml                [version bumped to 3.0.3]
│   ├── mykeystore.jks                 (your existing keystore)
│   ├── mykeystore.p12                 (your existing keystore)
│   └── lib/
│       ├── AsyncAstm-3.2.jar          (built from AsyncAstm-3.2/ above)
│       └── jSerialComm-2.10.4.jar     (unchanged)
└── tools/                              (end-to-end verification toolset)
    ├── README.md                       (verification workflow)
    ├── test-channel-astm-listener.xml  (Mirth channel — TCP Server, port 3600)
    ├── analyzer-simulator.py           (Python ASTM analyzer — sends ENQ+frames+EOT)
    ├── astm-log-viewer.py              (tails mirth.log, filters ASTM lines, colorized)
    └── verify-end-to-end.sh            (one-shot runner that ties it all together)
```

## Quick start

### 1. Build the plugin

Open this directory in IntelliJ IDEA. The project should load with all the
existing `.iml` module files. Build all four artifacts:

- `AsyncAstm-3.2.jar`
- `astm-shared.jar`
- `astm-server.jar`
- `astm-client.jar`

Copy them into `sign/bitdreamit-astm/` (replacing the old ones). Make sure
`sign/bitdreamit-astm/lib/jSerialComm-2.10.4.jar` is still present.

### 2. Sign the plugin

Use the existing keystore (`sign/bitdreamit-astm/mykeystore.jks` or
`.p12`) to sign the plugin package. The exact signing command depends on
your existing workflow — typically:

```bash
cd sign/
zip -r bitdreamit-astm.zip bitdreamit-astm/
jarsigner -keystore bitdreamit-astm/mykeystore.jks bitdreamit-astm.zip <alias>
```

### 3. Install in Mirth Connect

In Mirth Administrator: Extensions → Install → select the signed `bitdreamit-astm.zip`.
Restart the Mirth service.

### 4. Import the test channel

Channels → Import Channel → select `tools/test-channel-astm-listener.xml`.
Deploy the channel.

### 5. Run the end-to-end verification

Open a terminal on a machine that can reach Mirth's TCP port 3600:

```bash
# Full one-shot verification (with log viewer in background)
./tools/verify-end-to-end.sh 127.0.0.1 3600 /opt/mirth-connect/logs/mirth.log

# OR manually:
python3 tools/analyzer-simulator.py --host 127.0.0.1 --port 3600

# In a second terminal — watch the Mirth log in real time:
python3 tools/astm-log-viewer.py /opt/mirth-connect/logs/mirth.log --follow
```

Expected output from the simulator:
```
[sim] Connected to 127.0.0.1:3600
[sim] → sending ENQ
[sim] ← ACK received (Mirth is ready to receive frames)
[sim] → frame 1/6: '1\rH|\^&||PS|||bitdreamit-sim^1.0||...'
[sim] ← ACK for frame 1
[sim] → frame 2/6: '2\rP|1||P001||Doe^John^A||...'
[sim] ← ACK for frame 2
...
[sim] → sending EOT
[sim] Transfer complete.
```

Expected log viewer output:
```
2026-08-13 14:23:45,123 INFO  c.b.connect.astm.AstmReceiver       - AstmReceiver started: mode=TCP_SERVER, channel=...
2026-08-13 14:23:45,124 INFO  c.b.a.a.s.c.AstmTcpServerConnection - Listening inbound ASTM on TCP port 3600 on 0.0.0.0
2026-08-13 14:23:45,125 INFO  c.b.a.a.s.c.AstmTcpServerConnection - Waiting for inbound TCP client to connect on port 3600 ...
2026-08-13 14:24:01,234 INFO  c.b.a.a.s.c.AstmTcpServerConnection - Client [127.0.0.1] connected
2026-08-13 14:24:01,235 INFO  c.b.a.a.s.states.AstmStateMachine    - Executing state Idle
2026-08-13 14:24:01,236 DEBUG c.b.a.a.s.states.IdleState           - Received ENQ request
2026-08-13 14:24:01,237 DEBUG c.b.a.a.s.states.IdleState           - Trying to receive message
```

Expected Mirth Messages view: a new message with `encodedData` containing
the decoded ASTM records (`H|...` `P|...` `O|...` `R|...` `R|...` `L|...`).
The File Writer destination will write it to `/tmp/astm-output/<timestamp>.txt`.

Expected dashboard connector status:
1. `Listening on 0.0.0.0:3600` (initially)
2. `Connected - Waiting for receiving messages` (when simulator connects)
3. `Receiving new message` (during transfer)
4. back to `Connected - Waiting for receiving messages` (after EOT)

## If something is wrong

If the dashboard badge is BLANK, or the simulator reports "Timeout waiting
for ACK", or no message appears in the Mirth Messages view — read
`DIAGNOSTIC.md`. It walks through all 12 bugs with file/line references so
you can verify each fix is actually applied.

Common gotchas:
- **Old JAR still loaded** — stop Mirth, delete the old plugin JARs from
  `extensions/bitdreamit-astm/`, restart, then install v3.0.3 fresh.
- **Channel not redeployed after upgrade** — restart Mirth Connect after
  installing the plugin so it reloads the channel XML and runs `migrate4_5_0()`.
- **Firewall blocking port 3600** — `telnet <mirth-host> 3600` should
  succeed; if not, open the port.
- **Wrong host in channel config** — if Mirth and the analyzer are on
  different machines, the analyzer must point to Mirth's IP, not 127.0.0.1.

## Version history

| Version | Status | Notes |
|---|---|---|
| 2.4.2 | TCP only, obfuscated | Worked for 4 years |
| 3.0.2 | TCP + Serial, refactored | Silent channel-failure bug (12 root causes) |
| **3.0.3** | **TCP + Serial, patched** | **All 12 bugs fixed — this package** |

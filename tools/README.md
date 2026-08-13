# tools/ — End-to-end Verification Toolset

This directory contains four tools that together verify the patched
bitdreamit-astm v3.0.3 plugin works correctly:

| File | Purpose |
|---|---|
| `test-channel-astm-listener.xml` | A Mirth Connect channel definition that uses the patched ASTM Listener (TCP Server, port 3600, ELECSYS dialect). Imports via Channels → Import Channel. |
| `analyzer-simulator.py` | A Python script that acts as a Roche Elecsys–style ASTM analyzer. Connects to Mirth's ASTM Listener and sends a complete ASTM E1394 transfer: ENQ → 6 framed records (Header, Patient, Order, Result, Result, Terminator) → EOT. |
| `astm-log-viewer.py` | A Python script that tails `mirth.log` and shows only ASTM-related lines, colorized by severity. Useful for watching the state machine transition in real time. |
| `verify-end-to-end.sh` | A bash runner that ties the above three together: starts the log viewer in the background, runs the simulator, and reports PASS/FAIL. |

## Quick verification flow

```bash
# 1. Build the patched plugin (see parent README.md) and install in Mirth.
# 2. Import the test channel via Mirth Administrator:
#       Channels -> Import Channel -> select test-channel-astm-listener.xml
# 3. Deploy the channel (or it auto-deploys based on initialState=Started).
# 4. From this directory, run the end-to-end verification:
./verify-end-to-end.sh 127.0.0.1 3600 /opt/mirth-connect/logs/mirth.log
```

## What you should see if the patched plugin works

### Simulator output (stdout)

```
============================================================
 bitdreamit-astm v3.0.3 - Analyzer Simulator
============================================================
[sim] Connecting to ASTM Listener at 127.0.0.1:3600 (TCP)...
[sim] Connected to 127.0.0.1:3600

[sim] === Sending message 1/1 ===
[sim] → sending ENQ
[sim] ← ACK received (Mirth is ready to receive frames)
[sim] → frame 1/6: '1\rH|\^&||PS|||bitdreamit-sim^1.0||...'
[sim] ← ACK for frame 1
[sim] → frame 2/6: '2\rP|1||P001||Doe^John^A||19700101|M||||||'
[sim] ← ACK for frame 2
[sim] → frame 3/6: '3\rO|1|S0001||^^^GLU^GLUCOSE|R||...'
[sim] ← ACK for frame 3
[sim] → frame 4/6: '4\rR|1|^^^GLU^GLUCOSE|110|mg/dL|70-110|N...'
[sim] ← ACK for frame 4
[sim] → frame 5/6: '5\rR|2|^^^HGB^HEMOGLOBIN|14.5|g/dL|13.0-17.0|N...'
[sim] ← ACK for frame 5
[sim] → frame 6/6: '6\rL|1|N'
[sim] ← ACK for frame 6
[sim] → sending EOT
[sim] Transfer complete.
[sim] Done. 1 message(s) sent successfully.
```

### Log viewer output (colorized in real terminal)

```
2026-08-13 14:23:45,123 INFO  c.b.c.astm.AstmReceiver             - AstmReceiver started: mode=TCP_SERVER
2026-08-13 14:23:45,124 INFO  c.b.a.a.s.c.AstmTcpServerConnection - Listening inbound ASTM on TCP port 3600 on 0.0.0.0
2026-08-13 14:23:45,125 INFO  c.b.a.a.s.c.AstmTcpServerConnection - Waiting for inbound TCP client to connect on port 3600 ...
2026-08-13 14:24:01,234 INFO  c.b.a.a.s.c.AstmTcpServerConnection - Client [127.0.0.1] connected
2026-08-13 14:24:01,235 INFO  c.b.a.a.s.states.AstmStateMachine    - Executing state Idle
2026-08-13 14:24:01,236 DEBUG c.b.a.a.s.states.IdleState           - Received ENQ request
2026-08-13 14:24:01,237 DEBUG c.b.a.a.s.states.IdleState           - Trying to receive message
2026-08-13 14:24:01,500 INFO  c.b.a.a.s.s.bundle.AstmContext       - (received frame 1, decoded H record)
2026-08-13 14:24:01,700 INFO  c.b.a.a.s.s.bundle.AstmContext       - (received frame 6, decoded L record, message complete)
2026-08-13 14:24:01,800 INFO  c.b.c.astm.AstmReceiverService      - ASTM listener thread: dispatched message to Mirth channel
```

### Mirth Administrator UI

- **Channels view**: channel "Test ASTM Listener v3.0.3" shows Started (green).
- **Dashboard status badge for the source connector** transitions through:
  1. `Listening on 0.0.0.0:3600` (initial)
  2. `Connected - Waiting for receiving messages` (after simulator connects)
  3. `Receiving new message` (during the transfer)
  4. `Connected - Waiting for receiving messages` (after EOT)
- **Messages view**: a new message appears with:
  - Source: `ASTM Listener`
  - Status: `RECEIVED` → `SENT` (after the File Writer destination runs)
  - Encoded data: the full decoded ASTM records (`H|...` `P|...` `O|...` `R|...` `R|...` `L|...`)
- **Files written**: `/tmp/astm-output/<timestamp>-<messageId>.txt` containing the same decoded records.

## What you will see if the patched plugin is NOT installed (still v3.0.2)

This is exactly your reported symptom. The simulator will hang:

```
[sim] Connecting to ASTM Listener at 127.0.0.1:3600 (TCP)...
[sim] Connected to 127.0.0.1:3600

[sim] === Sending message 1/1 ===
[sim] → sending ENQ
[sim] ERROR: timeout waiting for ACK after ENQ
[sim] Message 1 failed, aborting.
```

The log viewer shows nothing (because the failures are logged at DEBUG level
which is below the default Mirth log threshold). The dashboard badge is blank.
The Messages view shows no new message. This is the silent failure.

If you see this, the patched plugin is not installed — go back and rebuild /
redeploy v3.0.3.

## Running the simulator against a Serial channel

If you want to test Serial mode, you need:

1. A physical null-modem cable between two serial ports (e.g., COM1 ↔ COM2
   on Windows, or `/dev/ttyUSB0` ↔ `/dev/ttyUSB1` on Linux).
2. The Mirth channel configured with `transportMode=SERIAL` and the
   serial port that Mirth should listen on (e.g., `/dev/ttyUSB0`).
3. The simulator pointed at the OTHER port (e.g., `/dev/ttyUSB1`):

   ```bash
   python3 analyzer-simulator.py --serial /dev/ttyUSB1 --baud 9600
   ```

If you do not have a physical null-modem cable, you can use a virtual
serial port pair (`socat` on Linux, `com0com` on Windows):

```bash
# Linux — create a virtual serial port pair
socat -d -d pty,raw,echo=0,link=/tmp/vserial0 pty,raw,echo=0,link=/tmp/vserial1

# Configure Mirth channel: serialPort=/tmp/vserial0
# Run simulator: python3 analyzer-simulator.py --serial /tmp/vserial1
```

## Simulator CLI options

```
usage: analyzer-simulator.py [-h] [--host HOST] [--port PORT]
                              [--count COUNT] [--delay DELAY]
                              [--serial SERIAL] [--baud BAUD]

optional arguments:
  --host HOST        Mirth ASTM Listener host (TCP mode) (default: 127.0.0.1)
  --port PORT        Mirth ASTM Listener port (TCP mode) (default: 3600)
  --count COUNT      Number of messages to send (default: 1)
  --delay DELAY      Seconds between messages (default: 2.0)
  --serial SERIAL    Serial port path (e.g. /dev/ttyUSB0 or COM3) for serial mode
  --baud BAUD        Serial baud rate (default: 9600)
```

## Log viewer CLI options

```
usage: astm-log-viewer.py [-h] [--follow] [--channel CHANNEL]
                          [--since SINCE] [--debug]
                          logfile

positional arguments:
  logfile            Path to mirth.log (typically /opt/mirth-connect/logs/mirth.log)

optional arguments:
  --follow, -f       Follow the log file (like tail -f)
  --channel CHANNEL  Filter by Mirth channel ID (e.g. abc123 - partial match)
  --since SINCE      Only show lines from the last N seconds/minutes/hours
                     (e.g. --since 30m, --since 2h, --since 15s)
  --debug            Show TRACE-level lines (very verbose)
```

## Requirements

- Python 3.7+ (for the simulator and log viewer — only standard library).
- For Serial mode: `pip install pyserial`.
- Mirth Connect 3.8.0 or later (matches the plugin's `<mirthVersion>` list).
- The patched bitdreamit-astm v3.0.3 plugin installed and the test channel deployed.

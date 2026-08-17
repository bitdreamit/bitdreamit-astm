#!/usr/bin/env python3
"""
USB-COM Unplug / Hang Recovery Simulator — bitdreamit-astm v3.0.3 verification tool.

This script tests that the patched plugin can RECOVER AUTOMATICALLY when a
USB-RS232 adapter is unplugged or hangs, then plugged back in — and that
LIS data flow resumes without manual channel restart.

SCENARIO TESTED:
    1. Open the serial port that Mirth ASTM Listener is listening on
       (use a virtual serial port pair like socat, or a real null-modem cable).
    2. Send one ASTM message (H/P/O/R/R/L) — verify it's received by Mirth.
    3. SIMULATE USB UNPLUG by abruptly closing the serial port without sending EOT.
       In the patched plugin: the reader thread will hit EOF, throw EOFException,
       transition to ReconnectState (5s backoff).
    4. Wait 15 seconds (long enough for first reconnect attempt to fail).
    5. RECONNECT — open the serial port again (simulate USB replug).
    6. Send another ASTM message — verify Mirth receives it WITHOUT manual restart.

USAGE:
    # Linux — first create a virtual serial port pair (one-time):
    socat -d -d pty,raw,echo=0,link=/tmp/vserial0 pty,raw,echo=0,link=/tmp/vserial1 &

    # Configure Mirth ASTM Listener channel: serialPort=/tmp/vserial0
    # Run this simulator against /tmp/vserial1:

    python3 usb-unplug-simulator.py --serial /tmp/vserial1 --baud 9600

WHAT YOU SHOULD SEE IN MIRTH LOGS (via astm-log-viewer.py):

    t=0s    INFO   AstmSerialConnection - Opening serial port /tmp/vserial0
    t=0s    INFO   AstmSerialConnection - Serial port /tmp/vserial0 opened successfully
    t=1s    INFO   AstmReceiverService  - dispatched message to Mirth channel  (← first message OK)
    t=15s   DEBUG  IdleState            - End of stream reached, connection was closed
    t=15s   WARN   ReconnectState       - Connection lost — will retry in 5s (attempt #1)
    t=20s   INFO   AstmSerialConnection - Opening serial port /tmp/vserial0
    t=20s   ERROR  AstmSerialConnection - Failed to open serial port /tmp/vserial0 (still unplugged)
    t=20s   WARN   ReconnectState       - Reconnect attempt #2 — waiting 10s
    t=30s   INFO   AstmSerialConnection - Opening serial port /tmp/vserial0
    t=30s   INFO   AstmSerialConnection - Serial port /tmp/vserial0 opened successfully  (← USB back!)
    t=30s   INFO   ConnectState         - Connection established — transitioning to Idle
    t=31s   INFO   AstmReceiverService  - dispatched message to Mirth channel  (← second message OK!)

If the second message is received, the recovery worked.
If only the first message is received and the channel stays "dead", the
recovery logic is broken — paste the log output into a bug report.

NOTE: This script requires pyserial. Install with: pip install pyserial
"""
from __future__ import annotations

import argparse
import socket
import sys
import time
from datetime import datetime

# Reuse the message builder from analyzer-simulator.py
import importlib.util
import os
SIM_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "analyzer-simulator.py")
spec = importlib.util.spec_from_file_location("analyzer_sim", SIM_PATH)
analyzer_sim = importlib.util.module_from_spec(spec)
spec.loader.exec_module(analyzer_sim)
# Now we have access to: analyzer_sim.build_message, analyzer_sim.ENQ, analyzer_sim.ACK, etc.

# ASTM control chars
ENQ = analyzer_sim.ENQ
ACK = analyzer_sim.ACK
NAK = analyzer_sim.NAK
EOT = analyzer_sim.EOT


def log(stage, msg):
    ts = datetime.now().strftime("%H:%M:%S")
    print(f"[{ts}] [{stage:8}] {msg}")


def send_message_over_serial(ser) -> bool:
    """Send one ASTM message over an open serial port."""
    log("SEND", "→ ENQ")
    ser.write(bytes([ENQ]))
    # Wait for ACK
    b = ser.read(1)
    if not b:
        log("FAIL", "  no response to ENQ (timeout)")
        return False
    if b[0] != ACK:
        log("FAIL", f"  expected ACK (0x06) but got 0x{b[0]:02X}")
        return False
    log("OK", "  ← ACK (Mirth is ready)")

    frames = analyzer_sim.build_message("RECOVERY", "TEST-002")
    for i, frame in enumerate(frames, 1):
        log("SEND", f"→ frame {i}/{len(frames)}")
        ser.write(frame)
        b = ser.read(1)
        if not b or b[0] != ACK:
            log("FAIL", f"  no ACK for frame {i}")
            return False
        log("OK", f"  ← ACK for frame {i}")

    log("SEND", "→ EOT")
    ser.write(bytes([EOT]))
    log("OK", "  message complete")
    return True


def run_serial_test(port: str, baud: int, unplug_delay: float) -> int:
    try:
        import serial
    except ImportError:
        log("ERR", "pyserial not installed. Run: pip install pyserial")
        return 2

    print("=" * 70)
    print(" bitdreamit-astm v3.0.3 — USB-COM Unplug Recovery Simulator")
    print("=" * 70)
    print(f" Serial port: {port}")
    print(f" Baud rate:   {baud}")
    print(f" Unplug delay: {unplug_delay}s")
    print("=" * 70)
    print()

    # === PHASE 1: Connect + send first message ===
    log("PHASE1", f"Opening serial port {port}...")
    try:
        ser = serial.Serial(port, baudrate=baud, timeout=10.0)
    except serial.SerialException as e:
        log("ERR", f"Cannot open {port}: {e}")
        return 1
    log("OK", "Port opened — this simulates USB-COM being plugged in")

    log("PHASE1", "Sending first ASTM message...")
    if not send_message_over_serial(ser):
        log("ERR", "First message failed — cannot proceed with unplug test")
        ser.close()
        return 1
    log("OK", "First message sent successfully. Mirth should have received it.")
    print()

    # === PHASE 2: Simulate USB unplug ===
    log("PHASE2", f"SIMULATING USB UNPLUG — abruptly closing serial port (no EOT)")
    log("", "       In the patched plugin, this triggers:")
    log("", "         - reader thread sees EOF (read() returns -1)")
    log("", "         - EOFException propagates to AstmState.run()")
    log("", "         - transition to ReconnectState (5s backoff)")
    log("", "         - WARN-level log: 'Connection lost — will retry in 5s'")
    log("", "         - Mirth dashboard: 'Trying to reconnect'")
    ser.close()
    log("OK", f"Serial port closed (simulated unplug). Waiting {unplug_delay}s...")

    # Wait long enough for at least 1-2 reconnect attempts to fail
    time.sleep(unplug_delay)
    print()

    # === PHASE 3: Reconnect + send second message ===
    log("PHASE3", f"SIMULATING USB REPLUG — reopening serial port")
    try:
        ser = serial.Serial(port, baudrate=baud, timeout=10.0)
    except serial.SerialException as e:
        log("ERR", f"Cannot reopen {port}: {e}")
        log("", "  If this fails, the patched plugin's ReconnectState may have left the port in a bad state.")
        log("", "  Check mirth.log for AstmSerialConnection errors.")
        return 1
    log("OK", "Port reopened — this simulates USB-COM being plugged back in")

    # Give Mirth a moment to detect the new connection
    time.sleep(2)

    log("PHASE3", "Sending second ASTM message (should succeed WITHOUT channel restart)...")
    if not send_message_over_serial(ser):
        log("ERR", "Second message failed — automatic recovery did NOT work")
        log("", "  Check mirth.log — ReconnectState should have transitioned to ConnectState")
        log("", "  and ConnectState should have called doConnect() successfully.")
        ser.close()
        return 1
    log("OK", "Second message sent successfully — AUTOMATIC RECOVERY WORKED")
    ser.close()

    print()
    print("=" * 70)
    print(" RESULT: PASS — USB-COM unplug/replug recovery verified")
    print("=" * 70)
    print()
    print(" In Mirth you should see TWO messages in the Messages view:")
    print("   1. Sample ID TEST-001 (sent before unplug)")
    print("   2. Sample ID TEST-002 (sent after replug)")
    print()
    print(" The channel should show as Started (green) throughout the test.")
    print(" The dashboard status badge should have transitioned:")
    print("   Connected → Reconnecting (5s) → Reconnecting (10s) → Connected")
    return 0


def run_tcp_test(host: str, port: int, unplug_delay: float) -> int:
    """Same test but for TCP — simulates network drop instead of USB unplug."""
    print("=" * 70)
    print(" bitdreamit-astm v3.0.3 — TCP Drop Recovery Simulator")
    print("=" * 70)
    print(f" Target: {host}:{port}")
    print(f" Drop delay: {unplug_delay}s")
    print("=" * 70)
    print()

    # PHASE 1
    log("PHASE1", f"Connecting to {host}:{port}...")
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(10.0)
        sock.connect((host, port))
    except (socket.error, OSError) as e:
        log("ERR", f"Cannot connect: {e}")
        return 1
    log("OK", "Connected")

    log("PHASE1", "Sending first ASTM message...")
    if not analyzer_sim.send_one_message(sock, "RECOVERY", "TEST-001"):
        log("ERR", "First message failed")
        sock.close()
        return 1
    log("OK", "First message sent successfully")
    print()

    # PHASE 2: simulate network drop (close socket without sending EOT)
    log("PHASE2", f"SIMULATING NETWORK DROP — abruptly closing socket (no EOT)")
    sock.close()
    log("OK", f"Socket closed. Waiting {unplug_delay}s for ReconnectState to kick in...")
    time.sleep(unplug_delay)
    print()

    # PHASE 3: reconnect
    log("PHASE3", "Reconnecting...")
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(10.0)
        sock.connect((host, port))
    except (socket.error, OSError) as e:
        log("ERR", f"Cannot reconnect: {e}")
        return 1
    log("OK", "Reconnected")

    time.sleep(2)

    log("PHASE3", "Sending second ASTM message (should succeed WITHOUT channel restart)...")
    if not analyzer_sim.send_one_message(sock, "RECOVERY", "TEST-002"):
        log("ERR", "Second message failed — automatic recovery did NOT work")
        sock.close()
        return 1
    log("OK", "Second message sent successfully — AUTOMATIC RECOVERY WORKED")
    sock.close()

    print()
    print("=" * 70)
    print(" RESULT: PASS — TCP drop recovery verified")
    print("=" * 70)
    return 0


def main() -> int:
    p = argparse.ArgumentParser(description="USB-COM / TCP drop recovery simulator")
    p.add_argument("--serial", help="Serial port path (e.g. /tmp/vserial1 or COM3)")
    p.add_argument("--baud", type=int, default=9600)
    p.add_argument("--host", default="127.0.0.1", help="Mirth host (TCP mode)")
    p.add_argument("--port", type=int, default=3600, help="Mirth port (TCP mode)")
    p.add_argument("--unplug-delay", type=float, default=15.0,
                   help="Seconds to wait between unplug and replug (default 15s)")
    args = p.parse_args()

    if args.serial:
        return run_serial_test(args.serial, args.baud, args.unplug_delay)
    return run_tcp_test(args.host, args.port, args.unplug_delay)


if __name__ == "__main__":
    sys.exit(main())

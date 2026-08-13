#!/usr/bin/env python3
"""
ASTM Analyzer Simulator — bitdreamit-astm v3.0.3 verification tool.

Acts as a lab analyzer (Roche Elecsys / Cobas compatible) that connects to
the Mirth Connect ASTM Listener (TCP Server mode) and sends a complete
ASTM E1394 message with patient + order + result records.

USAGE:
    python3 analyzer-simulator.py --host 127.0.0.1 --port 3600
    python3 analyzer-simulator.py --host 192.168.1.10 --port 3600 --count 5
    python3 analyzer-simulator.py --serial /dev/ttyUSB0          # serial mode

WHAT IT DOES:
    1. Connects to the ASTM Listener (TCP client mode).
    2. Sends ENQ (0x05).
    3. Waits for ACK (0x06) from Mirth.
    4. Sends a framed ASTM message: H|... P|... O|... R|... R|... L|...
       Each frame is <STX>FN<CR>payload<CR>checksum<ETX><CR><LF>
       Middle frames use <ETB>, last frame uses <ETX>.
    5. Waits for ACK after each frame.
    6. Sends EOT (0x04) to close the transfer.
    7. Optionally repeats for --count messages.

This is the simplest possible end-to-end test that the patched plugin
can:
    - Accept a TCP connection (Bug #2 — accept() before initialize())
    - Start the reader thread without NPE (Bug #2)
    - Reach IDLE state and report it via callback (Bug #4)
    - Receive ENQ and respond with ACK
    - Receive framed ASTM messages
    - Dispatch them as RawMessage to the Mirth channel

If the patched plugin works, you will see:
    - In the simulator stdout: "ACK received" after ENQ, and "ACK received"
      after each frame, and the channel will show "Receiving new message".
    - In the Mirth Messages view: a new message with the ASTM payload.
    - In the dashboard: status transitions Listening → Connected → Receiving → Connected.

If the patched plugin is NOT installed (i.e. original v3.0.2), you will see:
    - "Timeout waiting for ACK" — because the reader thread is dead and no
      ACK is ever sent in response to ENQ. This reproduces your "silent"
      symptom precisely.
"""
from __future__ import annotations

import argparse
import socket
import struct
import sys
import time
from datetime import datetime


# ASTM control characters
ENQ = 0x05
ACK = 0x06
NAK = 0x15
EOT = 0x04
STX = 0x02
ETX = 0x03
ETB = 0x17
CR  = 0x0D
LF  = 0x0A


def calc_checksum(payload_with_fn: bytes) -> str:
    """
    ASTM checksum: sum of all bytes (including frame number and payload,
    excluding STX/ETX/ETB/CR/LF and the checksum itself), mod 256,
    formatted as two uppercase hex digits.
    """
    total = sum(payload_with_fn) & 0xFF
    return f"{total:02X}"


def build_frame(frame_no: int, payload: str, last: bool) -> bytes:
    """
    Build one ASTM frame:
        <STX>FN<CR>payload<CR>checksum<ETX or ETB><CR><LF>

    Args:
        frame_no: 1-7 (cycles 1-7 after 7)
        payload: the record string, e.g. "H|\\^&||..."
        last: True if this is the last frame (ETX), False if middle (ETB)
    """
    # Frame number cycles 1..7 then back to 1
    fn = ((frame_no - 1) % 7) + 1
    body = f"{fn}\r".encode("ascii") + payload.encode("cp1252") + b"\r"
    checksum = calc_checksum(body)
    terminator = ETX if last else ETB
    return bytes([STX]) + body + checksum.encode("ascii") + bytes([terminator, CR, LF])


def build_message(patient_id: str, sample_id: str) -> list[bytes]:
    """
    Build a complete ASTM E1394 message for a Roche Elecsys-style analyzer.
    Returns a list of frames (one per record). All frames except the last
    use ETB; the last uses ETX.
    """
    now = datetime.now().strftime("%Y%m%d%H%M%S")

    # Header record
    header = (
        "H|\\^&||PS|||bitdreamit-sim^1.0||{ts}|||P|E1394-97|{ts}|{ts}"
    ).format(ts=now)

    # Patient record
    patient = f"P|1||{patient_id}||Doe^John^A||19700101|M||||||"

    # Order record
    order = f"O|1|{sample_id}||^^^GLU^GLUCOSE|R||{now}||||N"

    # Result records (glucose = 110 mg/dL, hemoglobin = 14.5 g/dL)
    result1 = f"R|1|^^^GLU^GLUCOSE|110|mg/dL|70-110|N|||F||{now}"
    result2 = f"R|2|^^^HGB^HEMOGLOBIN|14.5|g/dL|13.0-17.0|N|||F||{now}"

    # Terminator record
    terminator = "L|1|N"

    records = [header, patient, order, result1, result2, terminator]
    frames = []
    for i, rec in enumerate(records):
        frames.append(build_frame(i + 1, rec, last=(i == len(records) - 1)))
    return frames


def send_tcp(host: str, port: int, count: int, delay: float) -> int:
    """
    Send `count` ASTM messages to a TCP ASTM Listener.
    Returns 0 on success, non-zero on failure.
    """
    print(f"[sim] Connecting to ASTM Listener at {host}:{port} (TCP)...")
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(10.0)
        sock.connect((host, port))
    except (socket.error, OSError) as e:
        print(f"[sim] ERROR: cannot connect to {host}:{port} — {e}")
        print("[sim] Is the Mirth channel Started? Is the firewall open?")
        return 1
    print(f"[sim] Connected to {host}:{port}")

    try:
        for i in range(1, count + 1):
            print(f"\n[sim] === Sending message {i}/{count} ===")
            if not send_one_message(sock, f"P{i:03d}", f"S{i:04d}"):
                print(f"[sim] Message {i} failed, aborting.")
                return 1
            if i < count:
                print(f"[sim] Waiting {delay}s before next message...")
                time.sleep(delay)
    finally:
        sock.close()
        print("\n[sim] Connection closed.")

    print(f"[sim] Done. {count} message(s) sent successfully.")
    return 0


def send_one_message(sock: socket.socket, patient_id: str, sample_id: str) -> bool:
    """Send one complete ASTM transfer: ENQ -> frames -> EOT."""
    # 1. Send ENQ
    print("[sim] → sending ENQ")
    sock.sendall(bytes([ENQ]))

    # 2. Wait for ACK
    if not expect_byte(sock, ACK, "ACK after ENQ", timeout=10.0):
        return False
    print("[sim] ← ACK received (Mirth is ready to receive frames)")

    # 3. Send frames
    frames = build_message(patient_id, sample_id)
    for idx, frame in enumerate(frames, 1):
        preview = frame[1:].decode("cp1252", errors="replace")[:60]
        print(f"[sim] → frame {idx}/{len(frames)}: {preview!r}...")
        sock.sendall(frame)
        if not expect_byte(sock, ACK, f"ACK after frame {idx}", timeout=10.0):
            return False
        print(f"[sim] ← ACK for frame {idx}")

    # 4. Send EOT
    print("[sim] → sending EOT")
    sock.sendall(bytes([EOT]))
    print("[sim] Transfer complete.")
    return True


def expect_byte(sock: socket.socket, expected: int, label: str, timeout: float) -> bool:
    """Read one byte from the socket and verify it matches `expected`."""
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            sock.settimeout(max(0.5, deadline - time.time()))
            b = sock.recv(1)
        except socket.timeout:
            continue
        if not b:
            print(f"[sim] ERROR: socket closed while waiting for {label}")
            return False
        byte = b[0]
        if byte == expected:
            return True
        if byte == NAK:
            print(f"[sim] ERROR: received NAK (0x15) instead of {label}")
            return False
        print(f"[sim] WARN: expected {label} (0x{expected:02X}) but got 0x{byte:02X}")
        return False
    print(f"[sim] ERROR: timeout waiting for {label}")
    return False


def send_serial(port: str, baud: int) -> int:
    """Send one ASTM message over a serial port."""
    try:
        import serial
    except ImportError:
        print("[sim] ERROR: pyserial not installed. Run: pip install pyserial")
        return 2

    print(f"[sim] Opening serial port {port} @ {baud} baud...")
    try:
        ser = serial.Serial(port, baudrate=baud, timeout=10.0)
    except serial.SerialException as e:
        print(f"[sim] ERROR: cannot open {port} — {e}")
        return 1

    try:
        # Same flow as TCP, but write byte-by-byte to serial
        print("[sim] → sending ENQ")
        ser.write(bytes([ENQ]))
        if not expect_byte_serial(ser, ACK, "ACK after ENQ"):
            return 1

        frames = build_message("P001", "S0001")
        for idx, frame in enumerate(frames, 1):
            print(f"[sim] → frame {idx}/{len(frames)}")
            ser.write(frame)
            if not expect_byte_serial(ser, ACK, f"ACK after frame {idx}"):
                return 1

        print("[sim] → sending EOT")
        ser.write(bytes([EOT]))
        print("[sim] Transfer complete.")
    finally:
        ser.close()
    return 0


def expect_byte_serial(ser, expected: int, label: str) -> bool:
    """Read one byte from serial and verify."""
    b = ser.read(1)
    if not b:
        print(f"[sim] ERROR: timeout waiting for {label}")
        return False
    if b[0] == expected:
        return True
    if b[0] == NAK:
        print(f"[sim] ERROR: NAK instead of {label}")
        return False
    print(f"[sim] ERROR: expected {label} but got 0x{b[0]:02X}")
    return False


def main() -> int:
    p = argparse.ArgumentParser(description="ASTM analyzer simulator")
    p.add_argument("--host", default="127.0.0.1", help="Mirth ASTM Listener host (TCP mode)")
    p.add_argument("--port", type=int, default=3600, help="Mirth ASTM Listener port (TCP mode)")
    p.add_argument("--count", type=int, default=1, help="Number of messages to send")
    p.add_argument("--delay", type=float, default=2.0, help="Seconds between messages")
    p.add_argument("--serial", help="Serial port path (e.g. /dev/ttyUSB0 or COM3) for serial mode")
    p.add_argument("--baud", type=int, default=9600, help="Serial baud rate")
    args = p.parse_args()

    print("=" * 60)
    print(" bitdreamit-astm v3.0.3 — Analyzer Simulator")
    print("=" * 60)

    if args.serial:
        return send_serial(args.serial, args.baud)
    return send_tcp(args.host, args.port, args.count, args.delay)


if __name__ == "__main__":
    sys.exit(main())

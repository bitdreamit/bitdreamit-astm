#!/usr/bin/env bash
# ============================================================================
# bitdreamit-astm v3.0.3 — End-to-end Verification Script
# ============================================================================
# This script runs a complete end-to-end test of the patched plugin:
#
#   1. Checks that the simulator and log viewer scripts exist.
#   2. Optionally tails the Mirth log (if it can find mirth.log).
#   3. Sends one ASTM message to the ASTM Listener.
#   4. Reports whether the round-trip succeeded.
#
# USAGE:
#   ./verify-end-to-end.sh                          # default: localhost:3600
#   ./verify-end-to-end.sh 192.168.1.50 3600        # custom host:port
#   ./verify-end-to-end.sh 192.168.1.50 3600 /var/log/mirth/mirth.log
#
# PREREQUISITES:
#   - Mirth Connect is running and the patched plugin v3.0.3 is installed.
#   - The test channel (test-channel-astm-listener.xml) is imported and started.
#   - Python 3.7+ is on PATH.
# ============================================================================
set -euo pipefail

HOST="${1:-127.0.0.1}"
PORT="${2:-3600}"
MIRTH_LOG="${3:-/opt/mirth-connect/logs/mirth.log}"

# Locate script directory
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SIM="$SCRIPT_DIR/analyzer-simulator.py"
VIEWER="$SCRIPT_DIR/astm-log-viewer.py"

echo "============================================================"
echo " bitdreamit-astm v3.0.3 — End-to-end Verification"
echo "============================================================"
echo " Target: $HOST:$PORT"
echo " Mirth log: ${MIRTH_LOG:-<not found>}"
echo "============================================================"
echo ""

# Sanity checks
[ -f "$SIM" ]    || { echo "ERROR: $SIM not found"; exit 1; }
[ -f "$VIEWER" ] || { echo "ERROR: $VIEWER not found"; exit 1; }

command -v python3 >/dev/null 2>&1 || { echo "ERROR: python3 not found"; exit 1; }

# Start the log viewer in the background (if log file exists)
if [ -f "$MIRTH_LOG" ]; then
    echo "[verify] Starting ASTM log viewer in background (PID stored in astm-log-viewer.pid)..."
    python3 "$VIEWER" "$MIRTH_LOG" --follow --since 1m &
    VIEWER_PID=$!
    echo "$VIEWER_PID" > astm-log-viewer.pid
    trap "kill $VIEWER_PID 2>/dev/null || true" EXIT
    sleep 1
    echo "[verify] Log viewer started. ASTM lines will appear below as the test runs."
    echo ""
else
    echo "[verify] WARN: mirth.log not found at $MIRTH_LOG — log viewer skipped."
    echo "         To enable, pass the log path as the third argument."
    echo ""
fi

# Quick TCP reachability check (without sending any ASTM bytes)
echo "[verify] Checking TCP reachability of $HOST:$PORT ..."
if ! python3 -c "import socket,sys; s=socket.socket(); s.settimeout(3); s.connect(('$HOST', $PORT)); s.close()" 2>/dev/null; then
    echo "[verify] FAIL: cannot reach $HOST:$PORT within 3 seconds."
    echo "[verify] Possible causes:"
    echo "          - Mirth channel is not Started"
    echo "          - Firewall is blocking port $PORT"
    echo "          - Plugin v3.0.3 is not deployed"
    echo "          - Host/port is wrong"
    exit 2
fi
echo "[verify] OK: TCP port $HOST:$PORT is reachable."
echo ""

# Send one ASTM message
echo "[verify] Sending one ASTM message via the simulator..."
echo ""
if python3 "$SIM" --host "$HOST" --port "$PORT" --count 1; then
    echo ""
    echo "[verify] SUCCESS: simulator reports the message was sent and ACKed."
    echo "[verify] Check Mirth Messages view for a new message with patient P001 / sample S0001."
    echo "[verify] Check /tmp/astm-output/ for the raw decoded message file."
    echo ""
    echo "============================================================"
    echo " RESULT: PASS"
    echo "============================================================"
    exit 0
else
    echo ""
    echo "[verify] FAIL: simulator could not complete the ASTM transfer."
    echo "[verify] Look at the log viewer output above for clues."
    echo ""
    echo "[verify] If you see 'Timeout waiting for ACK' — the patched plugin is"
    echo "         NOT installed (the reader thread is dead, no ACK is sent)."
    echo "         Rebuild and redeploy v3.0.3."
    echo ""
    echo "[verify] If you see 'ERROR: cannot connect' — the channel is not"
    echo "         Started or the port is wrong. Check Mirth Administrator."
    echo ""
    echo "============================================================"
    echo " RESULT: FAIL"
    echo "============================================================"
    exit 1
fi

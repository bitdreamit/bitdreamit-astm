#!/usr/bin/env python3
"""
ASTM Log Viewer — bitdreamit-astm v3.0.3 verification tool.

Tails the Mirth Connect log file and shows only ASTM-related lines, with
color highlighting by severity. Helps you verify in real time that the
patched plugin is working correctly.

USAGE:
    python3 astm-log-viewer.py /opt/mirth-connect/logs/mirth.log
    python3 astm-log-viewer.py /opt/mirth-connect/logs/mirth.log --channel abc123
    python3 astm-log-viewer.py /opt/mirth-connect/logs/mirth.log --follow
    python3 astm-log-viewer.py /opt/mirth-connect/logs/mirth.log --since 30m

WHAT IT FILTERS:
    - Any line containing: AstmReceiver, AstmDispatcher, AstmService,
      AsyncAstmTcpDriver, AsyncAstmSerialDriver, AstmStateMachine, AstmState,
      AstmTcpServerConnection, AstmTcpClientConnection, AstmSerialConnection,
      AstmConnectionManager, "ASTM", "astmProperties"
    - Lines containing your channel ID (if --channel is given)

COLOR LEGEND:
    - RED     — ERROR / FATAL (bugs you should worry about)
    - YELLOW  — WARN  (transient failures like TCP reconnect attempts)
    - GREEN   — INFO  (normal operation: "Listening on...", "Client connected")
    - CYAN    — DEBUG (state transitions, byte-level trace)
    - GRAY    — TRACE (very verbose, only shown with --debug)
"""
from __future__ import annotations

import argparse
import os
import re
import sys
import time
from datetime import datetime, timedelta

# ANSI color codes
class C:
    RESET   = "\033[0m"
    RED     = "\033[31m"
    YELLOW  = "\033[33m"
    GREEN   = "\033[32m"
    CYAN    = "\033[36m"
    GRAY    = "\033[90m"
    BOLD    = "\033[1m"
    MAGENTA = "\033[35m"


# Patterns to match (case-insensitive)
ASTM_PATTERNS = [
    r"AstmReceiver",
    r"AstmDispatcher",
    r"AstmService",
    r"AsyncAstmTcpDriver",
    r"AsyncAstmSerialDriver",
    r"AstmStateMachine",
    r"AstmState\b",
    r"AstmTcpServerConnection",
    r"AstmTcpClientConnection",
    r"AstmSerialConnection",
    r"AstmConnectionManager",
    r"AstmListener",
    r"AstmSender",
    r"AstmConnectorPanel",
    r"AstmStatusCallback",
    r"AstmConnectionStatus",
    r"ASTM\s+(?:Listener|Sender|driver|state|connection|connector|properties)",
    r"astmProperties",
    r"Opening\s+serial\s+port",
    r"Listening\s+inbound\s+ASTM",
    r"Client\s+\[.*\]\s+connected",
    r"AsyncAstm\s+-",
    r"Driver\s+Status",
]

ASTM_RE = re.compile("|".join(ASTM_PATTERNS), re.IGNORECASE)

LEVEL_RE = re.compile(r"\b(ERROR|FATAL|WARN(?:ING)?|INFO|DEBUG|TRACE)\b", re.IGNORECASE)

# Standard Mirth log4j pattern: "2024-08-13 14:23:45,123 [Thread] LEVEL  Class  - message"
LINE_RE = re.compile(
    r"^(?P<ts>\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}[,.]\d+)\s+"
    r"\[(?P<thread>[^\]]+)\]\s+"
    r"(?P<level>\w+)\s+"
    r"(?P<logger>[\w.\$]+)\s+-\s+"
    r"(?P<msg>.*)$"
)


def color_for_level(level: str) -> str:
    L = level.upper()
    if L in ("ERROR", "FATAL"):
        return C.RED + C.BOLD
    if L in ("WARN", "WARNING"):
        return C.YELLOW
    if L == "INFO":
        return C.GREEN
    if L == "DEBUG":
        return C.CYAN
    if L == "TRACE":
        return C.GRAY
    return ""


def is_astm_related(line: str, channel_filter: str | None) -> bool:
    if not ASTM_RE.search(line):
        return False
    if channel_filter and channel_filter.lower() not in line.lower():
        return False
    return True


def parse_since(since: str) -> datetime | None:
    """Parse strings like '30m', '2h', '15s' into a datetime threshold."""
    m = re.match(r"^(\d+)([smh])$", since, re.IGNORECASE)
    if not m:
        return None
    n = int(m.group(1))
    unit = m.group(2).lower()
    delta = {"s": timedelta(seconds=n), "m": timedelta(minutes=n),
             "h": timedelta(hours=n)}[unit]
    return datetime.now() - delta


def parse_log_ts(ts: str) -> datetime | None:
    """Parse '2024-08-13 14:23:45,123' style timestamps."""
    for fmt in ("%Y-%m-%d %H:%M:%S,%f", "%Y-%m-%d %H:%M:%S.%f"):
        try:
            return datetime.strptime(ts, fmt)
        except ValueError:
            continue
    return None


def format_line(line: str) -> str:
    """Apply color highlighting to a log line."""
    line = line.rstrip()
    m = LINE_RE.match(line)
    if not m:
        # Fallback: just color by level if present
        lm = LEVEL_RE.search(line)
        if lm:
            color = color_for_level(lm.group(1))
            return f"{color}{line}{C.RESET}"
        return line

    color = color_for_level(m.group("level"))
    return (
        f"{C.GRAY}{m.group('ts')}{C.RESET} "
        f"{color}{m.group('level'):5}{C.RESET} "
        f"{C.MAGENTA}{m.group('logger'):40}{C.RESET} "
        f"- {color}{m.group('msg')}{C.RESET}"
    )


def tail_file(path: str, follow: bool, since: datetime | None,
              channel_filter: str | None, debug: bool) -> int:
    """Tail the file, printing matching lines."""
    try:
        # Start at end if following, else from beginning
        with open(path, "r", encoding="utf-8", errors="replace") as f:
            if follow:
                f.seek(0, os.SEEK_END)
                print(f"{C.BOLD}[astm-log-viewer] Following {path} (Ctrl+C to stop){C.RESET}",
                      file=sys.stderr)
            else:
                print(f"{C.BOLD}[astm-log-viewer] Reading {path} from start{C.RESET}",
                      file=sys.stderr)

            while True:
                line = f.readline()
                if not line:
                    if not follow:
                        break
                    time.sleep(0.2)
                    # Detect rotation
                    try:
                        if os.stat(path).st_size < f.tell():
                            print(f"{C.YELLOW}[astm-log-viewer] Log rotated, reopening{C.RESET}",
                                  file=sys.stderr)
                            f.seek(0)
                        continue
                    except OSError:
                        continue

                # Apply --since filter
                if since:
                    m = LINE_RE.match(line)
                    if m:
                        ts = parse_log_ts(m.group("ts"))
                        if ts and ts < since:
                            continue

                # Apply --debug filter (skip TRACE unless --debug)
                if not debug:
                    m = LINE_RE.match(line)
                    if m and m.group("level").upper() == "TRACE":
                        continue

                # Apply ASTM filter
                if not is_astm_related(line, channel_filter):
                    continue

                print(format_line(line))
    except FileNotFoundError:
        print(f"{C.RED}[astm-log-viewer] ERROR: file not found: {path}{C.RESET}",
              file=sys.stderr)
        return 1
    except KeyboardInterrupt:
        print(f"\n{C.BOLD}[astm-log-viewer] Stopped.{C.RESET}", file=sys.stderr)
        return 0
    return 0


def main() -> int:
    p = argparse.ArgumentParser(description="ASTM log viewer for Mirth Connect")
    p.add_argument("logfile", help="Path to mirth.log (typically /opt/mirth-connect/logs/mirth.log)")
    p.add_argument("--follow", "-f", action="store_true",
                   help="Follow the log file (like tail -f)")
    p.add_argument("--channel",
                   help="Filter by Mirth channel ID (e.g. abc123 — partial match)")
    p.add_argument("--since",
                   help="Only show lines from the last N seconds/minutes/hours "
                        "(e.g. --since 30m, --since 2h, --since 15s)")
    p.add_argument("--debug", action="store_true",
                   help="Show TRACE-level lines (very verbose)")
    args = p.parse_args()

    since = parse_since(args.since) if args.since else None
    if args.since and since is None:
        print(f"{C.RED}Invalid --since value: {args.since}. "
              f"Use format like '30m', '2h', '15s'{C.RESET}", file=sys.stderr)
        return 2

    print(f"{C.BOLD}{'=' * 60}{C.RESET}", file=sys.stderr)
    print(f"{C.BOLD} bitdreamit-astm v3.0.3 — ASTM Log Viewer{C.RESET}", file=sys.stderr)
    print(f"{C.BOLD}{'=' * 60}{C.RESET}", file=sys.stderr)
    if channel := args.channel:
        print(f"{C.GRAY}Filter: channel contains '{channel}'{C.RESET}", file=sys.stderr)
    if since:
        print(f"{C.GRAY}Filter: since {since.strftime('%Y-%m-%d %H:%M:%S')}{C.RESET}",
              file=sys.stderr)
    if args.debug:
        print(f"{C.GRAY}DEBUG mode: showing TRACE lines{C.RESET}", file=sys.stderr)
    print(file=sys.stderr)

    return tail_file(args.logfile, args.follow, since, args.channel, args.debug)


if __name__ == "__main__":
    sys.exit(main())

#!/usr/bin/env python3
"""
Mirth Channel Diagnostic — bitdreamit-astm v3.0.3 troubleshooting tool.

When the Mirth Administrator fails with:
    java.lang.InternalError: processing misc event after document element: 1
    at ...StAXStream2SAX.bridge
    at ...JsonXmlUtil.conversionJsontoXml
    at ...getChannelIdsAndNames

...it usually means one of these:
  1. A channel in the Mirth database has corrupted properties XML
     (typical cause: was saved under the broken v3.0.2 plugin)
  2. A channel name contains characters that break the JSON-to-XML converter
  3. A JDK version bug (some 17.0.x versions have StAXStream2SAX issues)
  4. The plugin's XStream whitelist is not registered, so channel deserialization
     silently fails and the server returns a malformed response

This script bypasses the Mirth Administrator and queries the Mirth REST API
directly. It tells you:
  - Whether the Mirth server is reachable
  - Whether the /channels endpoint returns valid JSON
  - Which channels (if any) have malformed or unparseable properties
  - Which channels use the ASTM connector (and could be the source of trouble)
  - Whether the bitdreamit-astm plugin is installed and registered

USAGE:
    python3 mirth-channel-diagnostic.py --host 127.0.0.1 --port 8443 \\
        --user admin --password admin
    python3 mirth-channel-diagnostic.py --host 127.0.0.1 --port 8443 \\
        --user admin --password admin --insecure

NOTE: Mirth's REST API is at /api/ by default, HTTPS on port 8443
      (or HTTP on port 8080 if you've enabled it).
"""
from __future__ import annotations

import argparse
import json
import re
import ssl
import sys
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET


# ANSI colors
class C:
    R = "\033[31m"; Y = "\033[33m"; G = "\033[32m"; B = "\033[34m"
    GRAY = "\033[90m"; BOLD = "\033[1m"; RESET = "\033[0m"


def log(level, msg):
    colors = {"OK": C.G, "WARN": C.Y, "ERR": C.R, "INFO": C.B, "": C.RESET}
    c = colors.get(level, "")
    prefix = f"[{level}] " if level else ""
    print(f"{c}{prefix}{msg}{C.RESET}")


def http_get(url, auth, insecure, timeout=15):
    """GET a URL, return (status_code, body_str, error_str_or_None)."""
    req = urllib.request.Request(url)
    req.add_header("Authorization", f"Basic {auth}")
    req.add_header("Accept", "application/json")
    ctx = None
    if insecure:
        ctx = ssl.create_default_context()
        ctx.check_hostname = False
        ctx.verify_mode = ssl.CERT_NONE
    try:
        with urllib.request.urlopen(req, timeout=timeout, context=ctx) as resp:
            body = resp.read().decode("utf-8", errors="replace")
            return resp.status, body, None
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", errors="replace") if e.fp else ""
        return e.code, body, f"HTTP {e.code}: {e.reason}"
    except urllib.error.URLError as e:
        return 0, "", f"URLError: {e.reason}"
    except Exception as e:
        return 0, "", f"{type(e).__name__}: {e}"


def check_reachability(host, port, insecure):
    """Step 1: is the Mirth server reachable at all?"""
    log("INFO", f"Step 1: Testing Mirth server reachability at {host}:{port}")
    scheme = "https" if not insecure else "https"
    url = f"{scheme}://{host}:{port}/api/users/_current"
    auth = "admin:admin"  # placeholder, real auth passed in caller
    code, body, err = http_get(url, "", insecure, timeout=5)
    if err and "Connection refused" in err:
        log("ERR", f"  Server not reachable: {err}")
        log("", "       Is Mirth Connect running? Check the service status.")
        return False
    if err and "timed out" in err.lower():
        log("ERR", f"  Server timed out: {err}")
        return False
    # Even if auth fails (401), the server is reachable
    log("OK", f"  Server is reachable (HTTP {code})")
    return True


def check_auth(host, port, user, password, insecure):
    """Step 2: can we authenticate?"""
    log("INFO", f"Step 2: Authenticating as user '{user}'")
    import base64
    auth = base64.b64encode(f"{user}:{password}".encode()).decode()
    url = f"https://{host}:{port}/api/users/_current"
    code, body, err = http_get(url, auth, insecure)
    if code == 200:
        log("OK", f"  Authentication successful (HTTP 200)")
        return auth
    if code == 401:
        log("ERR", f"  Authentication failed (HTTP 401) — wrong username/password?")
        return None
    log("ERR", f"  Unexpected response: HTTP {code} — {err}")
    return None


def check_extensions(host, port, auth, insecure):
    """Step 3: is the bitdreamit-astm plugin installed and loaded?"""
    log("INFO", "Step 3: Checking installed plugins / extensions")
    url = f"https://{host}:{port}/api/extensions"
    code, body, err = http_get(url, auth, insecure)
    if code != 200:
        log("ERR", f"  Cannot list extensions: HTTP {code} — {err}")
        return
    try:
        exts = json.loads(body)
    except json.JSONDecodeError as e:
        log("ERR", f"  Extensions response is not valid JSON: {e}")
        log("", f"  First 200 chars: {body[:200]!r}")
        return
    astm_exts = [e for e in exts if "astm" in str(e).lower()
                 or "bitdreamit" in str(e).lower()]
    if astm_exts:
        log("OK", f"  bitdreamit-astm plugin is installed:")
        for ext in astm_exts:
            name = ext.get("name", "?")
            ver = ext.get("pluginVersion", ext.get("version", "?"))
            enabled = ext.get("enabled", "?")
            log("", f"    - {name} v{ver} (enabled={enabled})")
    else:
        log("WARN", "  bitdreamit-astm plugin NOT found in extensions list!")
        log("", "         The plugin may have failed to load — check mirth.log on the server.")
    return astm_exts


def check_channels_list(host, port, auth, insecure):
    """Step 4: the actual test — can the server return the channel list?"""
    log("INFO", "Step 4: Calling GET /api/channels (this is what fails in Mirth Admin)")
    url = f"https://{host}:{port}/api/channels"
    code, body, err = http_get(url, auth, insecure)
    if code != 200:
        log("ERR", f"  Server returned HTTP {code} — {err}")
        if body:
            log("", f"  Response body (first 500 chars):\n{body[:500]}")
        return None

    # Validate JSON
    try:
        channels = json.loads(body)
    except json.JSONDecodeError as e:
        log("ERR", f"  Server returned invalid JSON: {e}")
        log("", f"  Response body (first 500 chars):\n{body[:500]}")
        return None

    if isinstance(channels, dict) and "list" in channels:
        channels = channels["list"]
    elif isinstance(channels, dict) and not channels:
        channels = []

    if not isinstance(channels, list):
        log("WARN", f"  Unexpected channels response type: {type(channels).__name__}")
        log("", f"  First 500 chars: {body[:500]!r}")
        return None

    log("OK", f"  Server returned {len(channels)} channel(s) as valid JSON")

    # Now check each channel's name for characters that might break JSON→XML conversion
    suspicious = []
    for ch in channels:
        name = ch.get("name", "")
        cid = ch.get("id", "?")
        # Check for characters that could break XML conversion
        # (XML special chars should be escaped by the converter, but some Mirth
        # versions have bugs where they aren't, especially in JDK 17.0.0-17.0.5)
        bad_chars = re.findall(r"[<>&\x00-\x1f]", name)
        if bad_chars:
            suspicious.append((cid, name, bad_chars))

    if suspicious:
        log("ERR", f"  Found {len(suspicious)} channel(s) with suspicious characters in name:")
        for cid, name, chars in suspicious:
            log("", f"    - id={cid}  name={name!r}  bad_chars={set(chars)}")
        log("", "  These characters can break the JSON→XML converter on certain JDK versions.")
        log("", "  FIX: rename the channel via the Mirth REST API or DB to remove these characters.")
    else:
        log("OK", "  All channel names contain only safe characters")

    return channels


def check_channel_properties(host, port, auth, insecure, channels):
    """Step 5: For each ASTM channel, check the properties XML for corruption."""
    if not channels:
        return
    log("INFO", "Step 5: Inspecting each channel's source/destination properties")
    for ch in channels:
        cid = ch.get("id", "?")
        name = ch.get("name", "?")
        source = ch.get("sourceConnector", {})
        sname = source.get("name", "") if source else ""
        sclass = source.get("transformer", {}).get("inboundProtocol", "") if source else ""

        # Look at the source connector's properties class
        sprops = source.get("properties", {}) if source else {}
        sprops_class = sprops.get("@class", sprops.get("class", "")) if isinstance(sprops, dict) else ""

        if "astm" in str(sprops_class).lower() or "astm" in str(sname).lower():
            log("WARN", f"  Channel '{name}' (id={cid}) uses ASTM source connector:")
            log("", f"    Source class: {sprops_class}")

            # Fetch the full channel XML to inspect its properties
            url = f"https://{host}:{port}/api/channels/{urllib.parse.quote(cid)}"
            code, body, err = http_get(url, auth, insecure)
            if code != 200:
                log("ERR", f"    Cannot fetch channel detail: HTTP {code} — {err}")
                continue
            try:
                ch_detail = json.loads(body)
            except json.JSONDecodeError as e:
                log("ERR", f"    Channel detail response is not valid JSON: {e}")
                log("", f"    First 500 chars: {body[:500]!r}")
                continue

            # Check if the channel's properties have all the expected ASTM fields
            sprops = ch_detail.get("sourceConnector", {}).get("properties", {})
            if isinstance(sprops, dict):
                has_tm = "transportMode" in sprops
                has_host = "host" in sprops
                has_port = "port" in sprops
                if not (has_tm and has_host and has_port):
                    log("ERR", f"    Channel properties are MISSING v3.0.2+ fields:")
                    if not has_tm:
                        log("", f"      Missing: transportMode")
                    if not has_host:
                        log("", f"      Missing: host")
                    if not has_port:
                        log("", f"      Missing: port")
                    log("", f"      This is normal for channels created under v2.4.2 — they will be auto-migrated on first load.")
                else:
                    log("OK", f"    transportMode={sprops.get('transportMode')}, "
                          f"host={sprops.get('host')}, port={sprops.get('port')}")
        else:
            # Non-ASTM channel — skip
            pass


def check_jdk_version_bug():
    """Step 6: Check if the JDK has the known StAXStream2SAX bug."""
    log("INFO", "Step 6: Checking JDK version (StAXStream2SAX 'misc event after document element' is a known JDK 17.0.x bug)")
    import subprocess
    try:
        result = subprocess.run(["java", "-version"], capture_output=True, text=True, timeout=5)
        out = (result.stderr or result.stdout).strip()
        log("", f"  JDK reports:\n    {out.replace(chr(10), chr(10) + '    ')}")
        # Parse version
        m = re.search(r'version "(\d+)\.(\d+)\.(\d+)', out)
        if m:
            major, minor, patch = int(m.group(1)), int(m.group(2)), int(m.group(3))
            if major == 17 and minor == 0 and patch < 6:
                log("ERR", f"  JDK 17.0.{patch} has the known StAXStream2SAX bug (fixed in 17.0.6+)")
                log("", "  FIX: Upgrade JDK to 17.0.6 or later, or use JDK 11 LTS.")
            else:
                log("OK", f"  JDK {major}.{minor}.{patch} — no known StAXStream2SAX bug")
    except FileNotFoundError:
        log("WARN", "  'java' command not found on PATH — cannot check JDK version")
    except Exception as e:
        log("WARN", f"  Cannot determine JDK version: {e}")


def main():
    p = argparse.ArgumentParser(description="Mirth channel diagnostic for bitdreamit-astm v3.0.3")
    p.add_argument("--host", default="127.0.0.1", help="Mirth Connect server host")
    p.add_argument("--port", type=int, default=8443, help="Mirth Connect server port (default 8443)")
    p.add_argument("--user", default="admin", help="Mirth Connect admin username")
    p.add_argument("--password", default="admin", help="Mirth Connect admin password")
    p.add_argument("--insecure", action="store_true", help="Skip TLS certificate verification")
    args = p.parse_args()

    print(f"{C.BOLD}{'=' * 70}{C.RESET}")
    print(f"{C.BOLD} Mirth Channel Diagnostic — for bitdreamit-astm v3.0.3 troubleshooting{C.RESET}")
    print(f"{C.BOLD}{'=' * 70}{C.RESET}")
    print(f" Target: https://{args.host}:{args.port}")
    print(f" User:   {args.user}")
    print()

    # Step 1
    if not check_reachability(args.host, args.port, args.insecure):
        return 1

    print()
    # Step 2
    auth = check_auth(args.host, args.port, args.user, args.password, args.insecure)
    if not auth:
        return 1

    print()
    # Step 3
    check_extensions(args.host, args.port, auth, args.insecure)

    print()
    # Step 4 — the actual test that fails in Mirth Admin
    channels = check_channels_list(args.host, args.port, auth, args.insecure)

    print()
    # Step 5
    if channels:
        check_channel_properties(args.host, args.port, auth, args.insecure, channels)

    print()
    # Step 6
    check_jdk_version_bug()

    print()
    print(f"{C.BOLD}{'=' * 70}{C.RESET}")
    print(f"{C.BOLD} Diagnostic complete.{C.RESET}")
    print(f"{C.BOLD}{'=' * 70}{C.RESET}")
    if channels is None:
        print(f"\n{C.R}RESULT: FAIL — the server could not return the channel list.{C.RESET}")
        print(f"{C.R}Look at the server-side mirth.log for the actual cause.{C.RESET}")
        return 1
    else:
        print(f"\n{C.G}RESULT: OK — server returned {len(channels)} channel(s) as valid JSON.{C.RESET}")
        print(f"\n{C.G}If Mirth Administrator STILL fails with the same error after this passes,{C.RESET}")
        print(f"{C.G}the issue is on the client side — try:{C.RESET}")
        print(f"{C.G}  1. Clear the Mirth Administrator's local cache (rm -rf ~/.mirth).{C.RESET}")
        print(f"{C.G}  2. Restart the Mirth Administrator.{C.RESET}")
        print(f"{C.G}  3. Try a different JDK on the client side (use 17.0.6+ or 11 LTS).{C.RESET}")
        return 0


if __name__ == "__main__":
    sys.exit(main())

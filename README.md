# bitdreamit-astm v3.0.3 — Simple Fix

You had v2.4.2 working for 4 years. You upgraded to v3.0.2 and channels silently
stopped working. This package fixes that.

## What was wrong (the simple version)

3 simple bugs in v3.0.2 caused all your trouble:

1. **Channels lost their settings on upgrade.**
   The migration code was empty. Channels that were TCP-Server-on-port-3600
   became TCP-Client-to-127.0.0.1:5004 after the upgrade.

2. **Reader thread crashed silently when a client connected.**
   The code started the reader thread before the socket existed. The thread
   crashed on null and died. Channel showed "Started" but processed nothing.

3. **No status updates reached Mirth.**
   The driver never sent connection status back to Mirth. Dashboard stayed
   blank — the "channel not enable" symptom.

Plus: connection failures were logged at DEBUG (invisible at default log level).

## What this fix does

12 files patched. The 3 critical bugs above are fixed. Plus:

- **Auto-recovery with backoff**: when USB-COM is unplugged or analyzer drops,
  the channel waits 5s, 10s, 20s, 40s, 60s and retries. When the cable is
  plugged back in, the channel automatically returns to normal. NO manual
  restart needed.

- **COM port name change works**: when you edit the channel's COM port in
  Mirth Administrator (e.g., COM3 → COM5) and click Save, the channel picks
  up the new port on the next reconnect cycle (within 5-60s). NO restart needed.

- **Visible logging**: connection failures now log at WARN/ERROR level so you
  can see them in the Mirth log.

## Files in this package

```
bitdreamit-astm-3.0.3-full.zip         <- full source (build this)
bitdreamit-astm-3.0.3-patched.zip       <- patch-only (drop into v3.0.2 source)
bitdreamit-astm-DIAGNOSTIC.md           <- detailed bug-by-bug explanation
bitdreamit-astm-RELEASE-NOTES.md        <- feature list + migration paths
```

## How to install (5 steps)

1. Unzip `bitdreamit-astm-3.0.3-full.zip`
2. Open in IntelliJ IDEA → build the 4 JARs (`AsyncAstm-3.2.jar`, `astm-shared.jar`, `astm-server.jar`, `astm-client.jar`)
3. Copy them into `sign/bitdreamit-astm/` (keep `lib/jSerialComm-2.10.4.jar`)
4. Sign the plugin with your existing keystore (`mykeystore.jks`)
5. Upload to Mirth Connect via Extensions → Install

## How to verify it works

After installing + restarting Mirth:

1. **Open an existing ASTM channel** (one you had under v2.4.2).
   The transport mode and port should now be correct (TCP Server, port 3600)
   instead of the wrong defaults (TCP Client, 127.0.0.1, 5004).
   Click Save to commit the migration.

2. **Start the channel.**
   The dashboard should show "Listening on 0.0.0.0:3600".

3. **Test with the analyzer simulator** (in `tools/`):
   ```bash
   python3 tools/analyzer-simulator.py --host 127.0.0.1 --port 3600
   ```
   You should see ACKs for every frame, and a new message in the Mirth
   Messages view.

## For lab staff (no IT needed)

Once this fix is installed, lab staff just need to know:

- **If the analyzer isn't sending data**: wait 1-2 minutes. The channel
  auto-recovers. If it doesn't, check that:
  - The analyzer is powered on
  - The USB cable is plugged in (firmly)
  - The analyzer's screen shows "Ready" or "Transmit"

- **If the USB cable was moved to a different port**:
  Edit the channel in Mirth Administrator → change the COM port → Save.
  Within 1 minute the channel picks up the new port. NO restart needed.

## What's NOT in this fix (kept simple)

- No watchdog thread (over-engineered for a lab)
- No Python recovery tools (lab staff use Mirth Administrator)
- No custom log viewer (use Mirth's built-in log viewer)
- No idle-timeout property (the standard 5s/10s/20s/40s/60s backoff is enough)

If you need any of those later, ask and I'll add them back.

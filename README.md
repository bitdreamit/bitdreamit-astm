# bitdreamit-astm v3.0.3 — IntelliJ IDEA Project

Open this folder in IntelliJ IDEA. Click Build → Build Artifacts → Build All.
Get 4 JARs in `sign/bitdreamit-astm/`.

## Quick start (5 steps)

1. **Set up `mirth-libs\`** in the parent folder of this project — see `BUILD.md` Step 1.
   - e.g., `D:\Java\mirth-libs\` (shared with your other Mirth plugin projects)
   - e.g., `D:\Java\bitdreamit-astm-3.0.3-full\` (this project, sibling of mirth-libs)
2. **Open this folder in IntelliJ IDEA** (File → Open → select `bitdreamit-astm-3.0.3-full/`).
3. **Configure JDK** — File → Project Structure → Project SDK → set to JDK 1.8 or 17.
4. **Build → Build Artifacts → Build All**.
5. **Sign the plugin** — see `BUILD.md` step 7.

## What you get after build

```
sign/bitdreamit-astm/
├── plugin.xml                  (already there)
├── source.xml                   (already there)
├── destination.xml              (already there)
├── mykeystore.jks               (already there — for signing)
├── astm-shared.jar              <- BUILT
├── astm-server.jar              <- BUILT
├── astm-client.jar              <- BUILT
└── lib/
    ├── jSerialComm-2.10.4.jar   (already there — bundled serial library)
    └── astm-async.jar        <- BUILT
```

## Module structure (4 modules, all in IDEA)

| Module | Source folder | Output JAR | Depends on |
|---|---|---|---|
| `astm-async` | `astm-async/` | `sign/bitdreamit-astm/lib/astm-async.jar` | Mirth Connect, jSerialComm |
| `astm-shared` | `astm-shared/` | `sign/bitdreamit-astm/astm-shared.jar` | Mirth Connect |
| `astm-server` | `astm-server/` | `sign/bitdreamit-astm/astm-server.jar` | astm-async + astm-shared + Mirth Connect + jSerialComm |
| `astm-client` | `astm-client/` | `sign/bitdreamit-astm/astm-client.jar` | astm-shared + Mirth Connect + jSerialComm |

## Documentation

- **`BUILD.md`** — detailed step-by-step build instructions + troubleshooting.
- **`lib/README.md`** — exactly which Mirth Connect JARs to copy into `lib/`.
- **`DIAGNOSTIC.md`** — root-cause analysis of all bugs fixed in v3.0.3.
- **`RELEASE-NOTES.md`** — feature list, migration paths, supported versions.
- **`tools/README.md`** — how to use the analyzer simulator + test channel.

## What was fixed in v3.0.3 (vs v3.0.2)

3 critical bugs caused your "channel not enable, silent failure":

1. **Migration was empty** — v2.4.2 channels lost their settings on upgrade.
2. **Reader thread crashed on connect** — started before socket existed.
3. **No status callback** — Mirth dashboard stayed blank.

Plus: connection failures were invisible (logged at DEBUG).

## Auto-recovery (no manual channel restart)

When USB-COM is unplugged or analyzer drops:
- Channel waits 5s → 10s → 20s → 40s → 60s (exponential backoff)
- Retries automatically
- When cable comes back, channel returns to normal
- Logs every attempt at WARN level (visible in Mirth log)

When you change the COM port in Mirth Administrator:
- Edit channel → change COM port → Save
- Within 1 minute, the channel picks up the new port
- NO restart needed

See `DIAGNOSTIC.md` Bug #15 for details.

## For lab staff (no IT needed)

- If analyzer isn't sending data: wait 1-2 minutes. Channel auto-recovers.
- If USB cable was moved to a different port: edit channel → change COM port → Save.
  Within 1 minute it picks up the new port.

## Files in this package

| Path | Purpose |
|---|---|
| `README.md` | this file |
| `BUILD.md` | how to build the 4 JARs in IntelliJ IDEA |
| `DIAGNOSTIC.md` | bug-by-bug explanation of all fixes |
| `RELEASE-NOTES.md` | feature list, migration, supported versions |
| `.idea/` | IntelliJ IDEA project config (modules, libraries, artifacts) |
| `astm-async/` | Module 1 source + .iml |
| `astm-shared/` | Module 2 source + .iml |
| `astm-server/` | Module 3 source + .iml |
| `astm-client/` | Module 4 source + .iml |
| `lib/` | drop Mirth Connect JARs here (see lib/README.md) |
| `sign/bitdreamit-astm/` | build output (4 JARs go here) |
| `tools/` | analyzer-simulator.py + test channel XML |

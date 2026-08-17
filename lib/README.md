# Mirth Connect Dependency JARs — `../mirth-libs/`

This project uses **two complementary library patterns** to be maximally compatible:

## Pattern 1 — Individual library files (one per JAR)

`.idea/libraries/` contains one XML file per JAR. Each file references a single
JAR by name. This matches the IntelliJ IDEA native pattern (when you add a JAR
via Project Structure → Libraries → + → Java, IDEA generates one of these).

| File | Library name | JAR |
|---|---|---|
| `commons_lang3.xml` | `commons-lang3` | `commons-lang3.jar` |
| `donkey_model.xml` | `donkey-model` | `donkey-model.jar` |
| `donkey_server.xml` | `donkey-server` | `donkey-server.jar` |
| `jSerialComm.xml` | `jSerialComm` | `sign/bitdreamit-astm/lib/jSerialComm-2.10.4.jar` (bundled) |
| `log4j_1_2_api.xml` | `log4j-1.2-api` | `log4j-1.2-api.jar` |
| `log4j_api.xml` | `log4j-api` | `log4j-api.jar` |
| `log4j_core.xml` | `log4j-core` | `log4j-core.jar` |
| `miglayout.xml` | `miglayout` | `miglayout.jar` (or `miglayout-*.jar`) |
| `mirth_client_core.xml` | `mirth-client-core` | `mirth-client-core.jar` |
| `mirth_server.xml` | `mirth-server` | `mirth-server.jar` |
| `mirth_shared.xml` | `mirth-shared` | `mirth-shared.jar` |
| `xstream.xml` | `xstream` | `xstream.jar` (or `xstream-*.jar`) |

Each file points to `../mirth-libs/<jar-name>` (the **parent folder** of this
project).

## Pattern 2 — `mirth-lib` folder catch-all

`.idea/libraries/mirth_lib.xml` is a **jarDirectory** library that picks up
**every** JAR in `../mirth-libs/` automatically. This handles:

- JARs that ship in newer Mirth versions but I didn't list individually
- JARs that have version-suffixed names (e.g., `xstream-1.4.20.jar` instead of `xstream.jar`)
- Any other JAR you drop into `../mirth-libs/`

## Why both patterns?

- **Individual files** give you visibility — when a build fails with
  "Cannot resolve symbol X", you can see exactly which JAR is needed
  (and add it if missing).
- **Folder catch-all** provides resilience — if a JAR name changes between
  Mirth versions, the catch-all still finds it.

Each `.iml` file references BOTH the individual libraries AND the `mirth-lib`
catch-all. IDEA deduplicates classes automatically, so there's no conflict.

## Folder layout

```
<parent-folder>\
├── mirth-libs\                          ← shared Mirth JARs (here!)
│   ├── mirth-server.jar
│   ├── mirth-shared.jar
│   ├── mirth-client-core.jar
│   ├── donkey-server.jar
│   ├── donkey-model.jar
│   ├── log4j-api.jar (or log4j-1.2-api.jar)
│   ├── log4j-core.jar
│   ├── commons-lang3.jar
│   ├── xstream-*.jar
│   └── miglayout-*.jar
└── bitdreamit-astm-3.0.3-full\         ← THIS PROJECT (extracted here)
    ├── .idea\libraries\                  ← references ../mirth-libs
    ├── astm-async\
    ├── astm-shared\
    ├── astm-server\
    └── astm-client\
```

For you specifically (since `mirth-libs` is in `D:\Java\`):

```
D:\Java\
├── mirth-libs\                          ← (already exists from your other project)
├── bitdreamit-astm-3.0.3-full\         ← extract THIS zip here
└── bitdreamit-astm-e1381-transmission\ ← your other project (shares mirth-libs)
```

## How to set up `mirth-libs` (one-time)

### If you already have `D:\Java\mirth-libs\` (from your other project)

**You're done.** Just extract this project as a sibling folder and open in IDEA.

### If you don't have it yet

```powershell
# Windows PowerShell
$MIRTH = "C:\Program Files\Mirth Connect"
$DEST  = "D:\Java\mirth-libs"
New-Item -ItemType Directory -Force -Path $DEST
Copy-Item "$MIRTH\lib\*.jar" $DEST

# Verify
Get-ChildItem $DEST | Measure-Object | Select-Object -ExpandProperty Count
# Should show ~30+ files
```

```bash
# Linux/Mac
mkdir -p /opt/mirth-libs
cp /opt/mirth-connect/lib/*.jar /opt/mirth-libs/
chmod +r /opt/mirth-libs/*.jar
```

## Why PROVIDED scope?

All library references use `scope="PROVIDED"` — the JARs are NOT bundled
into the output (Mirth Connect already has them at runtime). Only the
plugin's own 4 JARs (`astm-client.jar`, `astm-shared.jar`, `astm-server.jar`,
`lib/astm-async.jar`) plus the bundled `lib/jSerialComm-2.10.4.jar` are
shipped in the final plugin package.

## If a JAR has a different name in your Mirth version

Examples:
- Mirth 4.x ships `log4j-api-2.20.0.jar` (version-suffixed)
- Mirth 3.x ships `log4j-1.2-api.jar`

The individual library files use simple names (`log4j-api.jar`). If your
Mirth version uses a different name, you have two options:

**Option A — Use the catch-all `mirth-lib` folder (recommended)**
- The folder catch-all picks up any `*.jar` regardless of name
- Just make sure the JAR is in `../mirth-libs/`
- IDEA will use it automatically (no edits needed)

**Option B — Rename the JAR to match**
```powershell
# Windows PowerShell
cd D:\Java\mirth-libs
Rename-Item log4j-api-2.20.0.jar log4j-api.jar
```

**Option C — Edit the individual library file**
- Open `.idea/libraries/log4j_api.xml`
- Change `<jar name>` to match your actual file

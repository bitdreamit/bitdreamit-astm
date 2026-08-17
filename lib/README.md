# Mirth Connect Dependency JARs

This project uses a single IDEA library called **`mirth-lib`** that points to:

```
$PROJECT_DIR$/../mirth-libs
```

That means: **the `mirth-libs` folder must be in the parent directory of this project.**

## Folder layout (matches your existing setup)

```
D:\Java\
├── mirth-libs\                          ← shared Mirth JARs (already exists from your other project)
│   ├── mirth-server.jar
│   ├── mirth-client-core.jar
│   ├── mirth-shared.jar
│   ├── donkey-server.jar
│   ├── donkey-model.jar
│   ├── log4j-*.jar
│   ├── commons-lang3.jar
│   ├── xstream-*.jar
│   └── miglayout-*.jar
└── bitdreamit-astm-3.0.3-full\          ← THIS PROJECT (extracted here)
    ├── .idea\
    │   └── libraries\mirth_lib.xml       ← references ../mirth-libs (relative)
    ├── astm-async\
    ├── astm-shared\
    ├── astm-server\
    ├── astm-client\
    ├── sign\
    └── BUILD.md
```

So if you have:

```
D:\Java\mirth-libs\
D:\Java\bitdreamit-astm-e1381-transmission\    ← your existing project
```

You can also have:

```
D:\Java\bitdreamit-astm-3.0.3-full\            ← this project
```

Both projects share the SAME `D:\Java\mirth-libs\` folder. The path is
relative (`../mirth-libs`) so both projects work.

## If your mirth-libs is in a different location

Edit this ONE file:

```
.idea/libraries/mirth_lib.xml
```

Change `$PROJECT_DIR$/../mirth-libs` to wherever your Mirth JARs are.
Examples:

```xml
<!-- Same drive, custom folder -->
<root url="file://D:/Java/mirth-libs" />

<!-- Mirth Connect install folder directly -->
<root url="file:///C:/Program Files/Mirth Connect/lib" />

<!-- Linux -->
<root url="file:///opt/mirth-connect/lib" />
```

Use forward slashes `/` even on Windows (IDEA requires this in XML).

## What JARs go in mirth-libs

Copy all `.jar` files from your Mirth Connect install's `lib/` folder:

```powershell
# Windows PowerShell
$MIRTH = "C:\Program Files\Mirth Connect"
$DEST  = "D:\Java\mirth-libs"
New-Item -ItemType Directory -Force -Path $DEST
Copy-Item "$MIRTH\lib\*.jar" $DEST
```

```bash
# Linux
sudo mkdir -p /opt/mirth-libs
sudo cp /opt/mirth-connect/lib/*.jar /opt/mirth-libs/
sudo chmod +r /opt/mirth-libs/*.jar
```

### Minimum required JARs

| JAR | Used by |
|---|---|
| `mirth-server.jar` | astm-server |
| `mirth-client-core.jar` | astm-client |
| `mirth-shared.jar` | astm-shared |
| `donkey-server.jar` | astm-server |
| `donkey-model.jar` | astm-shared |
| `log4j-1.2-api.jar` (or `log4j-api.jar`) | all modules |
| `log4j-core.jar` | all modules |
| `commons-lang3.jar` | astm-server |
| `xstream-[version].jar` | astm-shared |
| `miglayout-[version].jar` | astm-client |

If you copy the entire `lib/` folder, you'll have all of them (plus extras
that don't hurt).

## Why PROVIDED scope?

All `mirth-lib` references use `scope="PROVIDED"` — meaning the JARs are
NOT bundled into the output JARs (Mirth Connect already has them at
runtime). Only the plugin's own 4 JARs (`astm-client.jar`, `astm-shared.jar`,
`astm-server.jar`, `lib/astm-async.jar`) plus the bundled
`lib/jSerialComm-2.10.4.jar` are shipped in the final plugin package.

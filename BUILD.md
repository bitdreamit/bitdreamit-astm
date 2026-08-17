# BUILD.md — How to build bitdreamit-astm v3.0.3

Open the top folder in IntelliJ IDEA → Build → Build Artifacts → Build All.
Get 4 JARs in `sign/bitdreamit-astm/`.

---

## Step 1 — Set up the `mirth-libs` folder (one-time setup)

This project uses an IDEA library called `mirth-lib` pointing to:

```
$PROJECT_DIR$/../mirth-libs
```

That means: **the `mirth-libs` folder must be in the parent directory of this project.**

Example — if you extract this project to `D:\Java\bitdreamit-astm-3.0.3-full\`,
then `mirth-libs` should be at `D:\Java\mirth-libs\` (one level up):

```
D:\Java\
├── mirth-libs\                          ← shared Mirth JARs (here!)
│   ├── mirth-server.jar
│   ├── mirth-shared.jar
│   └── ...
├── bitdreamit-astm-3.0.3-full\         ← THIS PROJECT (here!)
│   ├── .idea\libraries\mirth_lib.xml     ← references ../mirth-libs (relative)
│   └── ...
└── bitdreamit-astm-e1381-transmission\  ← your other project (shares mirth-libs)
```

If you already have `mirth-libs\` set up from your other Mirth plugin project
(`bitdreamit-astm-e1381-transmission`), **just extract this project as a sibling folder** — IDEA will pick up the JARs automatically.

### Create mirth-libs (if you don't have it yet)

```powershell
# Windows PowerShell — one-time setup
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

### If you want to use a different path

Edit ONE file: `.idea/libraries/mirth_lib.xml`. Change `$PROJECT_DIR$/../mirth-libs`
to your preferred path. Use forward slashes even on Windows.

```xml
<!-- Examples: -->
<root url="file://D:/Java/mirth-libs" />
<root url="file:///C:/Program Files/Mirth Connect/lib" />
<root url="file:///opt/mirth-connect/lib" />
```

---

## Step 2 — Extract this project as a sibling of mirth-libs

Make sure the layout looks like this:

```
<parent-folder>\
├── mirth-libs\                          ← (shared with your other projects)
└── bitdreamit-astm-3.0.3-full\         ← this project (unzipped here)
```

Examples of valid parent folders:
- `D:\Java\` (Windows) → `D:\Java\mirth-libs\` + `D:\Java\bitdreamit-astm-3.0.3-full\`
- `C:\Users\YourName\IdeaProjects\` → `...\mirth-libs\` + `...\bitdreamit-astm-3.0.3-full\`
- `/home/user/IdeaProjects/` (Linux)

### Open the project in IntelliJ IDEA

- File → Open → select the `bitdreamit-astm-3.0.3-full/` folder (the TOP folder)
- IDEA detects the `.idea/` folder and loads the 4 modules:
  - `astm-async`
  - `astm-shared`
  - `astm-server`
  - `astm-client`

Wait ~10 seconds for IDEA to index. You should see the 4 modules in the
Project view on the left.

---

## Step 3 — Configure the JDK

- File → Project Structure (Ctrl+Alt+Shift+S)
- Project → SDK → set to **JDK 1.8** (for Mirth Connect 3.x) or **JDK 17**
  (for Mirth Connect 4.x).
- Project language level → **8** (already set in `.idea/misc.xml`).

If IDEA prompts "Configure SDK", click it and point to your JDK install
(e.g., `C:\Program Files\Java\jdk-1.8.0_XXX`).

---

## Step 4 — Verify the mirth-lib library is loaded

- File → Project Structure → Libraries
- You should see **`mirth-lib`** listing all the JARs from `../mirth-libs/`.

If the list is empty or shows red (broken):
- Make sure the `mirth-libs\` folder exists in the **parent** of this project
  (e.g., if project is at `D:\Java\bitdreamit-astm-3.0.3-full\`, then
  `D:\Java\mirth-libs\` must exist)
- Make sure `mirth-libs\` contains `.jar` files (especially `mirth-server.jar`,
  `mirth-shared.jar`, `donkey-server.jar`)
- If the path is wrong, edit `.idea/libraries/mirth_lib.xml` to point to
  wherever your Mirth JARs actually live

You should also see **`jSerialComm`** — this points to
`sign/bitdreamit-astm/lib/jSerialComm-2.10.4.jar` (already bundled in this
project, you don't need to add it).

---

## Step 5 — Build the project (compile all 4 modules)

- **Build → Build Project** (Ctrl+F9)

Wait ~30 seconds. The "Build" tab at the bottom should show:

```
Build completed successfully
```

If you see errors like "Cannot resolve symbol 'com.mirth.connect...'":
- The `mirth-lib` library isn't loading — go back to Step 4.

If you see errors like "Package com.fazecast.jSerialComm does not exist":
- The `jSerialComm` library isn't loading — verify `sign/bitdreamit-astm/lib/jSerialComm-2.10.4.jar` exists.

---

## Step 6 — Build the 4 JAR artifacts

- **Build → Build Artifacts... → All Artifacts → Build**

Wait ~10 seconds. The 4 JARs will appear in `sign/bitdreamit-astm/`:

```
sign/bitdreamit-astm/
├── plugin.xml                  (already there)
├── source.xml                  (already there)
├── destination.xml              (already there)
├── mykeystore.jks               (already there — for signing)
├── astm-shared.jar              ← BUILT
├── astm-server.jar              ← BUILT
├── astm-client.jar              ← BUILT
└── lib/
    ├── jSerialComm-2.10.4.jar   (already there — bundled)
    └── astm-async.jar           ← BUILT
```

---

## Step 7 — Sign the plugin (required by Mirth Connect)

### Find your keystore alias

```bash
cd sign/
keytool -list -keystore bitdreamit-astm/mykeystore.jks
# Enter keystore password → see the alias listed (e.g., "1" or "bitdreamit")
```

### Zip and sign

```bash
cd sign/

# Zip the plugin folder
zip -r bitdreamit-astm.zip bitdreamit-astm/

# Sign with your keystore
jarsigner -keystore bitdreamit-astm/mykeystore.jks bitdreamit-astm.zip <alias>
# Enter keystore password when prompted

# Verify
jarsigner -verify bitdreamit-astm.zip
# Should print: "jar verified."
```

---

## Step 8 — Install in Mirth Connect

- Open Mirth Administrator
- Extensions → Install
- Select the signed `bitdreamit-astm.zip`
- Restart Mirth Connect when prompted

---

## Project structure overview

```
bitdreamit-astm-3.0.3-full/             <- open THIS folder in IDEA
├── .idea/                              <- IDEA project config
│   ├── modules.xml                      <- lists 4 modules
│   ├── misc.xml                          <- JDK 1.8 + project output dir
│   ├── compiler.xml                      <- Java 1.8 bytecode target
│   ├── libraries/
│   │   ├── mirth_lib.xml                 ← references ../mirth-libs (parent folder)
│   │   └── jSerialComm.xml               ← references sign/.../lib/jSerialComm-2.10.4.jar
│   └── artifacts/
│       ├── astm_async_jar.xml            ← builds lib/astm-async.jar
│       ├── astm_shared_jar.xml           ← builds astm-shared.jar
│       ├── astm_server_jar.xml           ← builds astm-server.jar
│       └── astm_client_jar.xml           ← builds astm-client.jar
├── astm-async/                          ← Module 1 (driver + state machine)
│   ├── astm-async.iml                    ← module config
│   └── com/bitdreamit/astm/asyncastm/   ← Java sources
├── astm-shared/                          ← Module 2 (properties)
├── astm-server/                          ← Module 3 (receiver + dispatcher)
├── astm-client/                          ← Module 4 (Mirth Admin UI)
├── lib/                                 ← optional fallback (see lib/README.md)
├── sign/bitdreamit-astm/                 ← BUILD OUTPUT (4 JARs go here)
│   ├── plugin.xml                        ← references lib/astm-async.jar
│   ├── source.xml                        ← references lib/astm-async.jar
│   ├── destination.xml                   ← references lib/astm-async.jar
│   ├── mykeystore.jks                    ← for signing
│   └── lib/
│       ├── jSerialComm-2.10.4.jar        ← bundled (already here)
│       └── (astm-async.jar goes here after build)
├── tools/                              ← analyzer simulator + test channel
├── BUILD.md                            ← this file
├── DIAGNOSTIC.md                       ← bug-by-bug explanation
├── RELEASE-NOTES.md                    ← feature list
└── README.md                           ← top-level overview
```

## Module dependency graph

```
astm-client ──┐
              ├──> astm-shared ──> mirth-lib (PROVIDED)
astm-server ──┤                    jSerialComm (PROVIDED, only astm-async + astm-server + astm-client)
              ├──> astm-async ──> mirth-lib (PROVIDED)
              └──> mirth-lib (PROVIDED)
```

`PROVIDED` means: NOT bundled into the output JARs (Mirth Connect already
has these classes at runtime). Only `jSerialComm-2.10.4.jar` is bundled.

---

## Troubleshooting

### "Cannot resolve symbol 'com.mirth.connect...'"

→ `mirth-lib` library isn't loading. Check:
- The `mirth-libs\` folder exists in the **parent** of this project
  (e.g., `D:\Java\mirth-libs\` if project is at `D:\Java\bitdreamit-astm-3.0.3-full\`)
- It contains `mirth-server.jar`, `mirth-shared.jar`, etc.
- File → Project Structure → Libraries → `mirth-lib` should list JARs (no red X).
- If the path is wrong, edit `.idea/libraries/mirth_lib.xml`.

### "Package com.mirth.connect.donkey.server.event does not exist"

→ `donkey-server.jar` is missing from `mirth-libs\`.
Copy it from your Mirth install's `lib/` folder.

### "Package com.fazecast.jSerialComm does not exist"

→ `sign/bitdreamit-astm/lib/jSerialComm-2.10.4.jar` is missing.
This should be in the zip — check it wasn't accidentally deleted.

### "Package net.migonfocom.swing does not exist" (or `net.miglayout`)

→ `miglayout-*.jar` is missing from `D:\Java\mirth-libs\`.
Copy it from your Mirth install.

### "Package com.thoughtworks.xstream.annotations does not exist"

→ `xstream-*.jar` is missing from `D:\Java\mirth-libs\`.

### "Cannot find symbol method getSendTimeout()"

→ Already fixed in v3.0.3 — make sure you have the latest source files.
The fix casts to `AstmDispatcherProperties` before calling `getSendTimeout()`.

### Build succeeds but plugin won't load in Mirth

- Verify version in `plugin.xml` matches your Mirth Connect version
- Verify the plugin is signed (Step 7)
- Check `mirth.log` for the actual error during plugin load

### Want to rebuild only one JAR?

- Build → Build Artifacts → select the JAR → Build

### Want to clean and rebuild?

- Build → Build Artifacts → select "All" → **Rebuild**

This deletes old JARs and creates new ones from scratch.

### JDK 8 vs JDK 17 — which one?

- **Mirth Connect 3.x** (3.8.0 → 3.12.0): use **JDK 8**
- **Mirth Connect 4.x** (4.0.0+): use **JDK 17**
- **Mirth Connect 26.x**: use **JDK 17** or newer

Set this in File → Project Structure → Project SDK.

# How to Open This Project in IntelliJ IDEA

If modules and libraries are not showing up after you open the project, this
guide explains exactly what to do.

---

## Step 1 — Extract the zip WITHOUT nesting

**Wrong** (causes "module not setup"):
```
D:\Java\
└── bitdreamit-astm-3.0.3-full\              ← extracted folder
    └── bitdreamit-astm-3.0.3-full\          ← nested folder (BAD!)
        ├── .idea\
        └── astm-async\
```

**Right** (single folder):
```
D:\Java\
├── mirth-libs\                              ← (already exists from your other project)
└── bitdreamit-astm-3.0.3-full\              ← THIS is the folder to open in IDEA
    ├── .idea\
    ├── astm-async\
    ├── astm-shared\
    ├── astm-server\
    ├── astm-client\
    └── sign\
```

How to extract correctly:

### Windows (using 7-Zip or built-in extractor)
1. Right-click `bitdreamit-astm-3.0.3-full.zip`
2. "Extract All..." → Extract to: `D:\Java\` (NOT `D:\Java\bitdreamit-astm-3.0.3-full\`)
3. Verify you see `D:\Java\bitdreamit-astm-3.0.3-full\.idea\` (the `.idea` folder is INSIDE the project folder, not one level deeper)

### Windows (PowerShell)
```powershell
cd D:\Java\
Expand-Archive bitdreamit-astm-3.0.3-full.zip -DestinationPath .
# Verify
Test-Path D:\Java\bitdreamit-astm-3.0.3-full\.idea
# Should print: True
```

### Linux
```bash
cd /opt/
unzip bitdreamit-astm-3.0.3-full.zip
# Verify
ls -d /opt/bitdreamit-astm-3.0.3-full/.idea
```

---

## Step 2 — Verify the .idea folder is NOT hidden by Windows

Windows hides folders starting with `.` by default. To verify:

1. Open File Explorer → View → check "Hidden items"
2. Navigate to `D:\Java\bitdreamit-astm-3.0.3-full\`
3. You should see the `.idea` folder (slightly faded)
4. Open `.idea\libraries\` — you should see `mirth_lib.xml` and `jSerialComm.xml`

If `.idea` is missing, the zip didn't extract properly. Re-extract.

---

## Step 3 — Open the project in IntelliJ IDEA

**Important**: Use "Open", NOT "Import Project". Import Project will overwrite
my `.idea` folder.

1. Open IntelliJ IDEA
2. **File → Open...** (NOT "New → Project from Existing Sources")
3. Navigate to `D:\Java\`
4. Click on `bitdreamit-astm-3.0.3-full` folder (just click, don't open it yet)
5. Click "OK"
6. IDEA asks: "Open as project" or "Open as file?" → click **"Open as project"** (the NEW IntelliJ might also offer "Trust project" — click Trust)

---

## Step 4 — Wait for indexing (10-30 seconds)

Look at the bottom-right corner:
- "Indexing..." with a progress bar — wait for it to finish.
- "Loading project..." — wait.
- "Updating indices: 100%" — done.

---

## Step 5 — Verify the 4 modules are loaded

**View → Tool Windows → Project** (if not visible).

The Project panel on the left should show:

```
bitdreamit-astm
├── astm-async           ← module (blue folder icon)
├── astm-client           ← module
├── astm-server           ← module
├── astm-shared           ← module
├── lib                   ← folder (gray)
├── sign                  ← folder (gray)
├── tools                 ← folder (gray)
├── BUILD.md
├── DIAGNOSTIC.md
├── README.md
└── RELEASE-NOTES.md
```

If you see all 4 modules with the blue module icon, modules are loaded. ✅

If you see them as plain folders (gray, no module icon), IDEA didn't load the
modules. Go to Step 6.

---

## Step 6 — Fix "Modules not loaded"

If you see only gray folders (no blue module icons):

1. **File → Close Project**
2. **File → Open...**
3. Navigate to `D:\Java\bitdreamit-astm-3.0.3-full\`
4. **IMPORTANT**: Do NOT select a subfolder. Select the top-level `bitdreamit-astm-3.0.3-full` folder itself.
5. Click "OK" → "Open as project"

If it STILL doesn't load:

1. Open the project as-is.
2. **File → Project Structure** (Ctrl+Alt+Shift+S)
3. Click the **"Modules"** tab on the left.
4. If the list is empty: click the **"+"** button → "Import Module" → select each `.iml` file:
   - `D:\Java\bitdreamit-astm-3.0.3-full\astm-async\astm-async.iml`
   - `D:\Java\bitdreamit-astm-3.0.3-full\astm-shared\astm-shared.iml`
   - `D:\Java\bitdreamit-astm-3.0.3-full\astm-server\astm-server.iml`
   - `D:\Java\bitdreamit-astm-3.0.3-full\astm-client\astm-client.iml`
5. Click "OK" to save.

---

## Step 7 — Set up the JDK (one-time)

If IDEA shows "Project SDK is not defined" or your code has red underlines saying "Cannot resolve symbol":

1. **File → Project Structure** (Ctrl+Alt+Shift+S)
2. **Project** on the left
3. **SDK** dropdown → "Add SDK" → "JDK"
4. Navigate to your JDK install:
   - JDK 1.8: `C:\Program Files\Java\jdk1.8.0_XXX` (recommended for Mirth 3.x)
   - JDK 17: `C:\Program Files\Java\jdk-17` (required for Mirth 4.x)
5. Click "OK"
6. **Language level**: "8 - Lambdas, type annotations etc." (already set in misc.xml)
7. Click "OK" to save.

---

## Step 8 — Verify the libraries are loaded

1. **File → Project Structure** (Ctrl+Alt+Shift+S)
2. Click the **"Libraries"** tab on the left.
3. You should see:
   - **`mirth-lib`** — pointing to `D:\Java\mirth-libs\` (or wherever you extracted mirth-libs)
     - Should list many .jar files (mirth-server.jar, donkey-server.jar, etc.)
     - If it shows "red" or "broken" → the path is wrong → go to Step 9
   - **`jSerialComm`** — pointing to `sign\bitdreamit-astm\lib\jSerialComm-2.10.4.jar`
     - Should show one .jar file
     - Should never be broken (it's bundled in this project)

---

## Step 9 — Fix "mirth-lib library is broken"

If `mirth-lib` shows red/broken:

1. **File → Project Structure → Libraries**
2. Click on `mirth-lib` (it'll show with red X)
3. Click the **"-"** button to remove it
4. Click the **"+"** button → "Java"
5. Navigate to your `mirth-libs` folder:
   - `D:\Java\mirth-libs\` (or wherever you put it)
6. Click "OK" → "OK" to confirm using it in all 4 modules
7. Click "OK" to save

**Alternative**: Edit `.idea/libraries/mirth_lib.xml` directly and change the
path to your actual location.

---

## Step 10 — Build the project

1. **Build → Build Project** (Ctrl+F9)
2. Look at the "Build" tab at the bottom.
3. Should see: `Build completed successfully`
4. Verify the compiled classes appeared in `out/production/<module-name>/`

If you see "Cannot resolve symbol 'com.mirth.connect.server.controllers.ControllerFactory'":
→ The `mirth-lib` library is missing or doesn't contain `mirth-server.jar`.
→ Fix per Step 9.

---

## Step 11 — Build the 4 JAR artifacts

1. **Build → Build Artifacts...**
2. Hover over "All Artifacts" → click "Build"
3. Wait ~10 seconds.
4. Verify the 4 JARs appeared in `sign\bitdreamit-astm\`:
   - `astm-shared.jar`
   - `astm-server.jar`
   - `astm-client.jar`
   - `lib\astm-async.jar`

---

## What was fixed in this release (why it wasn't working before)

Three bugs in the previous IDEA config caused "modules not setup, library not setup":

1. **`misc.xml` had `project-jdk-name="1.8"`** (hardcoded)
   - If your IDEA doesn't have an SDK registered with the exact name `"1.8"`, IDEA silently failed to load the project.
   - **Fix**: Removed `project-jdk-name`. Now `default="true"` tells IDEA to use whatever SDK you configure, with a friendly prompt on first open.

2. **`.idea/.name` was missing**
   - Some IDEA versions require this file to display the project name in the title bar. Without it, IDEA might not initialize the project correctly.
   - **Fix**: Added `.idea/.name` with content `bitdreamit-astm`.

3. **`compiler.xml` had `<resourceExtensions />` (empty tag)**
   - Empty tags can cause some IDEA versions to silently reject the file.
   - **Fix**: Removed the empty tag. Now contains only the necessary configuration.

Plus added:
- `.idea/encodings.xml` — explicit UTF-8 encoding declaration
- `.idea/vcs.xml` — no VCS, prevents IDEA from trying to detect one
- `.gitignore` — prevents IDEA from auto-creating unwanted workspace files

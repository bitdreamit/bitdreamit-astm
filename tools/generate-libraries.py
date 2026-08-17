#!/usr/bin/env python3
"""
Generate IntelliJ IDEA library XML files from a folder of JARs.

Run this script ONCE after extracting the project. It scans your mirth-libs
folder and creates one .xml file per JAR in .idea/libraries/. Then IDEA
can load each JAR individually and there's no chance of the folder catch-all
missing anything.

USAGE:
    # If mirth-libs is at D:\\Java\\mirth-libs (default)
    python3 generate-libraries.py

    # If mirth-libs is elsewhere
    python3 generate-libraries.py --mirth-libs /path/to/mirth-libs

    # If project is in a different folder
    python3 generate-libraries.py --project /path/to/project

REQUIREMENTS:
    - Python 3.6+
    - The mirth-libs folder must exist and contain .jar files
"""
from __future__ import annotations

import argparse
import os
import re
import sys
from pathlib import Path


def jar_to_library_name(jar_filename: str) -> str:
    """Convert 'mirth-server.jar' to library name 'mirth-server'.
    Strips .jar extension. Strips version suffixes like '-1.4.20'."""
    name = jar_filename
    # Strip .jar
    if name.lower().endswith(".jar"):
        name = name[:-4]
    # Strip version suffix like '-1.4.20' or '-2.10.4' or '-1.8.0_351'
    name = re.sub(r"-[\d].*$", "", name)
    return name


def jar_to_filename_stem(jar_filename: str) -> str:
    """Convert 'mirth-server.jar' to XML filename 'mirth_server.xml'.
    IntelliJ uses underscores for non-alphanumeric chars in filenames."""
    name = jar_to_library_name(jar_filename)
    # Replace any non-alphanumeric with underscore
    safe = re.sub(r"[^a-zA-Z0-9]", "_", name)
    return safe


def generate_library_xml(library_name: str, jar_path_relative: str) -> str:
    """Generate the XML content for one library file."""
    return f"""<component name="libraryTable">
  <library name="{library_name}">
    <CLASSES>
      <root url="jar://{jar_path_relative}!/" />
    </CLASSES>
    <JAVADOC />
    <SOURCES />
  </library>
</component>
"""


def main() -> int:
    p = argparse.ArgumentParser(description="Generate IDEA library XML files from a folder of JARs")
    p.add_argument("--mirth-libs", default="D:/Java/mirth-libs",
                   help="Path to mirth-libs folder (default: D:/Java/mirth-libs)")
    p.add_argument("--project", default=None,
                   help="Path to project root (default: auto-detect)")
    p.add_argument("--use-relative", action="store_true",
                   help="Use $PROJECT_DIR$/../mirth-libs/ relative path instead of absolute")
    p.add_argument("--clean", action="store_true",
                   help="Delete existing library XML files first (except mirth_lib.xml and jSerialComm.xml)")
    args = p.parse_args()

    # Auto-detect project root
    script_path = Path(__file__).resolve()
    if args.project:
        project_root = Path(args.project).resolve()
    else:
        # Script is in tools/ folder, project root is parent of tools/
        project_root = script_path.parent.parent

    libraries_dir = project_root / ".idea" / "libraries"

    print(f"Project root:    {project_root}")
    print(f"Libraries dir:   {libraries_dir}")
    print(f"mirth-libs path: {args.mirth_lib}")
    print()

    # Validate mirth-libs folder
    mirth_libs = Path(args.mirth_lib)
    if not mirth_libs.exists():
        print(f"ERROR: mirth-libs folder does not exist: {mirth_libs}")
        print()
        print("Please specify the correct path with --mirth-libs <path>")
        return 1

    if not mirth_libs.is_dir():
        print(f"ERROR: not a directory: {mirth_libs}")
        return 1

    # Find all JAR files
    jars = sorted(mirth_libs.glob("*.jar"))
    if not jars:
        print(f"ERROR: no .jar files found in {mirth_libs}")
        return 1

    print(f"Found {len(jars)} JAR files:")
    for jar in jars:
        print(f"  - {jar.name}")
    print()

    # Create libraries dir if it doesn't exist
    libraries_dir.mkdir(parents=True, exist_ok=True)

    # Optionally clean existing library files
    if args.clean:
        print("Cleaning existing library XML files (except mirth_lib.xml and jSerialComm.xml)...")
        for existing in libraries_dir.glob("*.xml"):
            if existing.stem not in ("mirth_lib", "jSerialComm"):
                print(f"  Deleting: {existing.name}")
                existing.unlink()
        print()

    # Generate library XML for each JAR
    print("Generating library XML files...")
    generated = []
    skipped = []
    for jar in jars:
        library_name = jar_to_library_name(jar.name)
        filename_stem = jar_to_filename_stem(jar.name)
        xml_filename = f"{filename_stem}.xml"
        xml_path = libraries_dir / xml_filename

        if args.use_relative:
            # Use $PROJECT_DIR$/../mirth-libs/<jar>
            jar_path_relative = f"$PROJECT_DIR$/../mirth-libs/{jar.name}"
        else:
            # Use absolute path with forward slashes (IDEA requirement)
            jar_path_abs = str(jar).replace("\\", "/")
            jar_path_relative = jar_path_abs

        xml_content = generate_library_xml(library_name, jar_path_relative)

        if xml_path.exists():
            print(f"  SKIP (already exists): {xml_filename}")
            skipped.append(xml_filename)
            continue

        with open(xml_path, "w", encoding="utf-8") as f:
            f.write(xml_content)
        print(f"  Created: {xml_filename} -> library name: {library_name}")
        generated.append(xml_filename)

    print()
    print(f"=== Summary ===")
    print(f"  Generated: {len(generated)} new library files")
    print(f"  Skipped:   {len(skipped)} existing files")
    print(f"  Total JARs in mirth-libs: {len(jars)}")
    print()
    print(f"Library files in {libraries_dir}:")
    for f in sorted(libraries_dir.glob("*.xml")):
        print(f"  {f.name}")

    print()
    print("=== Next steps ===")
    print("1. (Re)open the project in IntelliJ IDEA")
    print("2. File -> Project Structure -> Libraries")
    print("   - You should see one library per JAR")
    print("   - Each library should show its JAR (no red X)")
    print("3. If any library has a red X:")
    print("   - Click it, click '-', click '+' -> Java -> select the JAR manually")
    print("4. Build -> Build Project (Ctrl+F9)")
    print()
    print("NOTE: The generated library files are NOT yet referenced by the .iml files.")
    print("      The .iml files still reference 'mirth-lib' (folder) and 'jSerialComm'.")
    print("      IDEA will use the folder library. If you want to use individual libraries")
    print("      instead, edit each .iml file and add <orderEntry type=\"library\" name=\"<library-name>\" level=\"project\" />")
    print("      for each JAR you want to reference explicitly.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

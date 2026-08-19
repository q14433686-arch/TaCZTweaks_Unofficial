#!/usr/bin/env python3
"""Cross-file release consistency checks for the Fabric 1.21.11 TaCZ Tweaks port."""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import re
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read_properties(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    return values


def digest(path: Path, algorithm: str) -> str:
    hasher = hashlib.new(algorithm)
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            hasher.update(chunk)
    return hasher.hexdigest()


_ERRORS: list[str] = []


def fail(message: str) -> None:
    _ERRORS.append(message)


def check_manifest() -> None:
    manifest = ROOT / "RESOURCE_IMPORT_MANIFEST.tsv"
    with manifest.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        required = {
            "path", "source_url", "upstream_project", "upstream_version",
            "license", "use", "bundled_in_release_jar",
        }
        missing = required.difference(reader.fieldnames or [])
        if missing:
            fail(f"{manifest.name} missing columns: {sorted(missing)}")
        for row in reader:
            if not row.get("path"):
                continue
            path = ROOT / row["path"]
            if not path.is_file():
                fail(f"Manifest file is missing: {row['path']}")
                continue
            sha256 = (row.get("sha256") or "").strip().lower()
            sha512 = (row.get("sha512") or "").strip().lower()
            if not sha256 and not sha512:
                fail(f"Manifest row for {row['path']} has neither sha256 nor sha512")
            if sha256:
                if not re.fullmatch(r"[0-9a-f]{64}", sha256):
                    fail(f"Manifest row for {row['path']} has malformed sha256")
                else:
                    actual = digest(path, "sha256")
                    if actual != sha256:
                        fail(f"SHA-256 mismatch for {row['path']}: expected {sha256}, got {actual}")
            if sha512:
                if not re.fullmatch(r"[0-9a-f]{128}", sha512):
                    fail(f"Manifest row for {row['path']} has malformed sha512")
                else:
                    actual = digest(path, "sha512")
                    if actual != sha512:
                        fail(f"SHA-512 mismatch for {row['path']}: expected {sha512}, got {actual}")
            for column in required - {"path"}:
                if not row.get(column):
                    fail(f"Manifest row for {row['path']} has empty {column}")


def check_metadata() -> tuple[dict[str, str], dict]:
    props = read_properties(ROOT / "gradle.properties")
    meta = json.loads((ROOT / "src/main/resources/fabric.mod.json").read_text(encoding="utf-8"))

    if meta["version"] != "${version}":
        fail("fabric.mod.json should keep ${version} placeholder for Gradle expansion")
    contact = meta.get("contact") or {}
    for key in ("homepage", "sources", "issues"):
        if "TaCZTweaks_Unofficial" not in contact.get(key, ""):
            fail(f"fabric.mod.json contact.{key} is missing repository URL")
    if not meta.get("contributors"):
        fail("fabric.mod.json contributors is empty")
    if not meta.get("authors"):
        fail("fabric.mod.json authors is empty")

    expected_depends = {
        "minecraft": f"={props['minecraft_version']}",
        "java": ">=21",
        "tacz": "=1.1.8+fabric.1.21.11.R2",
        "yet_another_config_lib_v3": "=3.8.2+1.21.11-fabric",
        "fabricloader": ">=0.19.3 <0.20.0",
        "fabric-api": f">={props['fabric_version']} <0.143.0",
        "fabric-language-kotlin": ">=1.13.13 <1.14.0",
    }
    for key, value in expected_depends.items():
        if meta["depends"].get(key) != value:
            fail(f"fabric.mod.json depends.{key} expected {value}, got {meta['depends'].get(key)}")
    if meta.get("suggests", {}).get("modmenu") != "*":
        fail("fabric.mod.json should suggest modmenu: *")
    if props.get("minecraft_version") != "1.21.11":
        fail(f"gradle.properties minecraft_version drifted: {props.get('minecraft_version')}")
    return props, meta


def check_docs(props: dict[str, str]) -> None:
    version = props["mod_version"]
    required_docs = [
        "CHANGELOG.md", "LICENSES.md", "THIRD_PARTY_NOTICES.md", "docs/CONFIGURATION.md",
        "docs/COMPATIBILITY.md", "docs/KNOWN_ISSUES.md", "docs/data/README.md",
        "docs/data/BULLET_INTERACTIONS.md", "docs/data/BULLET_SOUNDS.md",
        "docs/data/BULLET_PARTICLES.md", "docs/data/MELEE_INTERACTIONS.md",
        "docs/data/SELECTORS.md", "docs/data/MIGRATION.md",
        "CONTRIBUTING.md", "SECURITY.md", "CODE_OF_CONDUCT.md",
    ]
    for rel in required_docs:
        if not (ROOT / rel).is_file():
            fail(f"Required release doc missing: {rel}")
    for rel in ("README.md", "CHANGELOG.md", "BUILD.md"):
        text = (ROOT / rel).read_text(encoding="utf-8")
        if version not in text:
            fail(f"{rel} does not mention current mod_version {version}")

    # Platform project-description copy is intentionally evergreen: it must not embed the
    # exact Minecraft/file version or release stage. File-specific versions belong to the upload
    # metadata and changelog, not the reusable Modrinth/CurseForge project text.
    for rel in ("docs/publish/Modrinth.md", "docs/publish/CurseForge.md"):
        text = (ROOT / rel).read_text(encoding="utf-8")
        for forbidden in (version, "1.21.11", "Beta-1"):
            if forbidden in text:
                fail(f"{rel} should not embed reusable-publication forbidden token {forbidden}")


def check_jar(path: Path, props: dict[str, str]) -> None:
    if not path.exists():
        return
    with zipfile.ZipFile(path) as jar:
        names = set(jar.namelist())
        for entry in [
            "fabric.mod.json", "icon.png", "tacztweaks.mixins.json",
            "META-INF/LICENSE_tacztweaks", "META-INF/THIRD_PARTY_NOTICES_tacztweaks.md",
        ]:
            if entry not in names:
                fail(f"{path} missing {entry}")
        meta = json.loads(jar.read("fabric.mod.json").decode("utf-8"))
        if meta["version"] != props["mod_version"]:
            fail(f"Jar version is {meta['version']}, expected {props['mod_version']}")
        forbidden = [
            n for n in names
            if n.endswith(".log") or n.startswith("fixtures/") or n.startswith("libs/")
            or n == "yacl-fabric.jar" or Path(n).name.startswith("TACZ-Refabricated-")
        ]
        if forbidden:
            fail(f"Jar contains forbidden entries: {forbidden}")


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, help="optional release jar to inspect")
    args = parser.parse_args(argv)

    check_manifest()
    props, _meta = check_metadata()
    if props:
        check_docs(props)
        if args.jar:
            check_jar(args.jar, props)
    if _ERRORS:
        print(f"RELEASE CONSISTENCY: {len(_ERRORS)} error(s)")
        for message in _ERRORS:
            print(f"ERROR: {message}")
        return 1
    print("RELEASE CONSISTENCY: OK")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))

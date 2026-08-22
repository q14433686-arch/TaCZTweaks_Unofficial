#!/usr/bin/env python3
"""Cross-file release consistency checks for TaCZ Tweaks (NeoForge 26.2)."""

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


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def fail(message: str) -> None:
    raise SystemExit(message)


def check_manifest() -> None:
    manifest = ROOT / "RESOURCE_IMPORT_MANIFEST.tsv"
    if not manifest.is_file():
        fail("RESOURCE_IMPORT_MANIFEST.tsv is missing")
    with manifest.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(
            (line for line in handle if line.strip() and not line.startswith("#")),
            delimiter="\t",
        )
        required = {
            "path", "source_url", "upstream_project", "upstream_version", "sha256",
            "license", "use", "bundled_in_release_jar",
        }
        missing = required.difference(reader.fieldnames or [])
        if missing:
            fail(f"{manifest.name} missing columns: {sorted(missing)}")
        rows = list(reader)
        if not rows:
            fail("RESOURCE_IMPORT_MANIFEST.tsv contains no rows")
        for row in rows:
            if not row.get("path"):
                continue
            path = ROOT / row["path"]
            expected = (row.get("sha256") or "").strip().lower()
            if not path.is_file():
                # libs/*.jar are not committed; only a release build (which must run
                # download_dependencies.py first) can verify them.
                print(f"NOTE manifest entry not present locally: {row['path']}")
                continue
            actual = sha256(path)
            if expected in {"", "pending", "tbd"}:
                print(f"NOTE {row['path']}: no digest recorded yet, observed {actual}")
            elif actual != expected:
                fail(f"Checksum mismatch for {row['path']}: expected {expected}, got {actual}")
            for column in required - {"path", "sha256", "retrieved_or_verified_utc"}:
                if not row.get(column):
                    fail(f"Manifest row for {row['path']} has empty {column}")


def check_metadata() -> tuple[dict[str, str], str]:
    props = read_properties(ROOT / "gradle.properties")
    meta = (ROOT / "src/main/templates/META-INF/neoforge.mods.toml").read_text(encoding="utf-8")

    if 'version="${mod_version}"' not in meta:
        fail("neoforge.mods.toml should keep the ${mod_version} placeholder for Gradle expansion")
    if "TaCZTweaks_Unofficial" not in meta:
        fail("neoforge.mods.toml is missing the repository URL")
    expected_properties = {
        "minecraft_version": "26.2",
        "minecraft_version_range": "[26.2]",
        "neo_version": "26.2.0.64",
        "neo_version_range": "[26.2.0.64,)",
        "kotlin_version": "2.4.10",
        "mod_version": "2.14.2+neoforge.26.2.Beta-1",
    }
    for key, expected in expected_properties.items():
        if props.get(key) != expected:
            fail(f"gradle.properties {key} must be {expected}, got {props.get(key)!r}")

    expected_fragments = [
        'modId="neoforge"',
        'modId="minecraft"',
        'versionRange="${minecraft_version_range}"',
        'modId="tacz"',
        'modId="yet_another_config_lib_v3"',
        'versionRange="[3.9.5,3.10.0)"',
        'versionRange="[1.5.1,1.6.0)"',
        'versionRange="[1.3.0,1.4.0)"',
        'versionRange="[3.3.5,3.4.0)"',
    ]
    for fragment in expected_fragments:
        if fragment not in meta:
            fail(f"neoforge.mods.toml is missing {fragment}")
    yacl_block = re.search(
        r'\[\[dependencies\.\$\{mod_id\}\]\]\s+modId="yet_another_config_lib_v3"(?P<body>.*?)(?=\n\[\[|\Z)',
        meta,
        re.S,
    )
    if yacl_block is None or 'side="BOTH"' not in yacl_block.group("body"):
        fail("YACL must be required on BOTH sides because common config types extend YACL")

    gate = (ROOT / "src/main/java/me/muksc/tacztweaks/TaczVersionSupport.java").read_text(encoding="utf-8")
    if 'EXPECTED_FAMILY = "neoforge.26.2"' not in gate or "MIN_REVISION = 1" not in gate:
        fail("TaczVersionSupport no longer targets the neoforge.26.2 R1+ release family")
    return props, meta


def check_docs(props: dict[str, str]) -> None:
    version = props["mod_version"]
    required_docs = [
        "CHANGELOG.md", "LICENSES.md", "THIRD_PARTY_NOTICES.md", "docs/CONFIGURATION.md",
        "docs/COMPATIBILITY.md", "docs/KNOWN_ISSUES.md", "docs/data/README.md",
        "docs/data/BULLET_INTERACTIONS.md", "docs/data/BULLET_SOUNDS.md",
        "docs/data/BULLET_PARTICLES.md", "docs/data/MELEE_INTERACTIONS.md",
        "docs/data/SELECTORS.md", "docs/data/MIGRATION.md",
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
        for forbidden in (version, "26.2", "Beta-1", "Beta-2"):
            if forbidden in text:
                fail(f"{rel} should not embed reusable-publication forbidden token {forbidden}")


def check_jar(path: Path, props: dict[str, str]) -> None:
    if not path.exists():
        return
    with zipfile.ZipFile(path) as jar:
        names = set(jar.namelist())
        for entry in [
            "META-INF/neoforge.mods.toml",
            "META-INF/LICENSE_tacztweaks",
            "META-INF/THIRD_PARTY_NOTICES_tacztweaks.md",
            "icon.png",
            "tacztweaks.mixins.json",
        ]:
            if entry not in names:
                fail(f"{path} missing {entry}")
        meta = jar.read("META-INF/neoforge.mods.toml").decode("utf-8")
        if f'version="{props["mod_version"]}"' not in meta:
            fail(f"Jar metadata does not declare version {props['mod_version']}")
        forbidden = [
            n for n in names
            if n.endswith(".log")
            or n.startswith("fixtures/")
            or n.startswith("libs/")
            or n == "fabric.mod.json"
            or "TACZ-Refabricated" in n
        ]
        if forbidden:
            fail(f"Jar contains forbidden entries: {forbidden}")


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, help="optional release jar to inspect")
    args = parser.parse_args(argv)

    check_manifest()
    props, _meta = check_metadata()
    check_docs(props)
    if args.jar:
        check_jar(args.jar, props)
    print("RELEASE CONSISTENCY: OK")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))

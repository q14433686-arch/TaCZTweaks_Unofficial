#!/usr/bin/env python3
"""Cross-file release consistency checks for TaCZ Tweaks (Refabricated)."""

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

# Mirrors scripts/download_dependencies.py: a manifest row whose SHA-256 is not pinned yet.
PENDING_SHA = "UNVERIFIED_PENDING_CI"


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


def check_manifest(require_deps: bool) -> None:
    manifest = ROOT / "RESOURCE_IMPORT_MANIFEST.tsv"
    with manifest.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        required = {
            "path", "source_url", "upstream_project", "upstream_version", "sha256",
            "license", "use", "bundled_in_release_jar",
        }
        missing = required.difference(reader.fieldnames or [])
        if missing:
            fail(f"{manifest.name} missing columns: {sorted(missing)}")
        for row in reader:
            if not row.get("path"):
                continue
            path = ROOT / row["path"]
            expected = row["sha256"].strip()
            if not path.is_file():
                # libs/*.jar are not committed (see .gitignore); they are rebuilt from this
                # manifest by scripts/download_dependencies.py. Only the jobs that actually
                # restored them (build.yml) demand their presence.
                if require_deps:
                    fail(f"Manifest file is missing: {row['path']}")
                print(f"SKIP (not restored) {row['path']}")
                continue
            actual = sha256(path)
            if expected == PENDING_SHA:
                print(f"PENDING {row['path']} sha256={actual} (pin this in the manifest)")
                continue
            if actual != expected:
                fail(f"Checksum mismatch for {row['path']}: expected {expected}, got {actual}")
            for column in required - {"path", "sha256"}:
                if not row.get(column):
                    fail(f"Manifest row for {row['path']} has empty {column}")


def _semver_key(value: str) -> tuple[int, ...]:
    """Numeric ordering key for a Fabric/SemVer version string.

    Only the numeric release part is compared: "1.14.1+kotlin.2.4.20" -> (1, 14, 1).
    Build metadata after '+' is ignored, which is what Fabric's own comparison does.
    """
    core = value.split("+", 1)[0].split("-", 1)[0]
    parts = []
    for chunk in core.split("."):
        parts.append(int(chunk) if chunk.isdigit() else 0)
    return tuple(parts)


def _satisfies(version: str, predicate: str) -> bool:
    """Evaluate one space-separated Fabric dependency range against a concrete version.

    Supports the operators this project actually uses: '=', '>=', '>', '<=', '<' and '*'.
    """
    predicate = predicate.strip()
    if not predicate or predicate == "*":
        return True
    for op in (">=", "<=", "=", ">", "<"):
        if predicate.startswith(op):
            bound = predicate[len(op):].strip()
            actual, expected = _semver_key(version), _semver_key(bound)
            if op == "=":
                # '=1.2.3' must match exactly, including build metadata.
                return version == bound
            if op == ">=":
                return actual >= expected
            if op == "<=":
                return actual <= expected
            if op == ">":
                return actual > expected
            return actual < expected
    # A bare version behaves like '='.
    return version == predicate


def check_declared_ranges_include_built_versions(props: dict[str, str], meta: dict) -> None:
    """Every version we compile against must satisfy the range we ship to users.

    This exists because of a real incident: gradle.properties built against Fabric
    Language Kotlin 1.13.13 and fabric.mod.json shipped ">=1.13.13 <1.14.0", but the only
    FLK build tagged for Minecraft 26.3 is 1.14.1. The intersection was empty, so the mod
    could not load for ANY user, and nothing in CI noticed: compiling, auditing and jar
    packaging all pass regardless of what the range says. Only the loader enforces it.
    """
    depends = meta.get("depends", {})
    # gradle.properties key -> fabric.mod.json depends key
    pairs = {
        "flk_version": "fabric-language-kotlin",
        "fabric_version": "fabric-api",
        "loader_version": "fabricloader",
    }
    for prop_key, depend_key in pairs.items():
        built = props.get(prop_key)
        declared = depends.get(depend_key)
        if not built or not declared:
            continue
        # A range is space-separated predicates that must ALL hold.
        if not all(_satisfies(built, part) for part in str(declared).split()):
            fail(
                f"fabric.mod.json depends.{depend_key} = '{declared}' excludes the version "
                f"this build actually uses ({prop_key} = {built}). Users would get "
                f"'Incompatible mods found'."
            )


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

    expected_depends = {
        "minecraft": f"={props['minecraft_version']}",
        "java": ">=25",
        "tacz": "=1.1.8+fabric.26.3.R1",
        "yet_another_config_lib_v3": "=3.9.7+26.3-fabric",
    }
    for key, value in expected_depends.items():
        if meta["depends"].get(key) != value:
            fail(f"fabric.mod.json depends.{key} expected {value}, got {meta['depends'].get(key)}")
    if meta.get("suggests", {}).get("modmenu") != "*":
        fail("fabric.mod.json should suggest modmenu: *")
    check_declared_ranges_include_built_versions(props, meta)
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
        for forbidden in (version, props["minecraft_version"], "Beta-1"):
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
        forbidden = [n for n in names if n.endswith(".log") or n.startswith("fixtures/") or n.startswith("libs/")]
        if forbidden:
            fail(f"Jar contains forbidden entries: {forbidden}")


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, help="optional release jar to inspect")
    parser.add_argument(
        "--require-deps",
        action="store_true",
        help="fail when a manifest dependency is absent (use after download_dependencies.py)",
    )
    args = parser.parse_args(argv)

    check_manifest(require_deps=args.require_deps)
    props, _meta = check_metadata()
    check_docs(props)
    if args.jar:
        check_jar(args.jar, props)
    print("RELEASE CONSISTENCY: OK")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))

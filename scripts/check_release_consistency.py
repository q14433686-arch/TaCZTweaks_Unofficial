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
            if not path.is_file():
                fail(f"Manifest file is missing: {row['path']}")
            actual = sha256(path)
            if actual != row["sha256"]:
                fail(f"Checksum mismatch for {row['path']}: expected {row['sha256']}, got {actual}")
            for column in required - {"path", "sha256"}:
                if not row.get(column):
                    fail(f"Manifest row for {row['path']} has empty {column}")


def _semver_key(value: str) -> tuple[int, ...]:
    """Numeric ordering key for a Fabric/SemVer version string.

    Only the numeric release part is compared: "1.13.13+kotlin.2.4.10" -> (1, 13, 13).
    Build metadata after '+' is ignored, which is what Fabric's own comparison does.
    (Synced verbatim from the 26.3 line.)
    """
    core = value.split("+", 1)[0].split("-", 1)[0]
    parts = []
    for chunk in core.split("."):
        parts.append(int(chunk) if chunk.isdigit() else 0)
    return tuple(parts)


def _satisfies(version: str, predicate: str) -> bool:
    """Evaluate one space-separated Fabric dependency range against a concrete version.

    Supports the operators this project actually uses: '=', '>=', '>', '<=', '<' and '*'.
    (Synced verbatim from the 26.3 line.)
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

    Synced from the 26.3 line, where gradle.properties and fabric.mod.json drifted apart
    from what users could actually install and the mod failed with "Incompatible mods
    found". Compiling, auditing and jar packaging all pass regardless of what a range says;
    only Fabric Loader enforces it, at launch.
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


def check_fabric_api_range_is_lower_bound_only(props: dict[str, str], meta: dict) -> None:
    """Keep the Fabric API dependency open-ended above its tested minimum.

    Fabric API releases for one Minecraft line can advance (e.g. 0.155.x -> 0.157.x)
    without changing the supported game version. An arbitrary upper bound rejects such
    compatible updates even though this mod's Minecraft dependency is pinned separately.
    Keep the selected build version as the minimum, with no cap.

    Synced from the 26.3 line (2026-10-15), where ">=0.160.7+26.3 <0.162.0" made Fabric
    Loader reject a user's installed 0.162.0+26.3. This line carried the same shape of cap
    (">=0.155.2+26.1.2 <0.157.0"); the minimum stays this line's own fabric_version.
    """
    declared = meta.get("depends", {}).get("fabric-api", "")
    expected = f">={props['fabric_version']}"
    if declared != expected:
        fail(
            f"fabric.mod.json depends.fabric-api should be the lower-bound-only range "
            f"'{expected}' (no upper cap); got '{declared}'. An upper cap can reject "
            f"newer Fabric API releases for the same Minecraft line."
        )


# build.gradle.kts resolves YACL by Modrinth version id, not by version string, so the version
# this build compiles against is recorded here (same id/version pair as THIRD_PARTY_NOTICES.md).
YACL_BUILD_COORDINATE = "maven.modrinth:1eAoo2KR:svTkvBec"
YACL_BUILT_VERSION = "3.9.6+26.1-fabric"


def check_flk_and_yacl_ranges_are_lower_bound_only(
    props: dict[str, str], meta: dict, build_script: str
) -> None:
    """Keep Fabric Language Kotlin and YACL open-ended above the versions this build uses.

    On the 26.2 line (Issue #33) Fabric Loader also rejected the user's FLK
    1.14.2+kotlin.2.4.21 (">=1.13.13 <1.14.0") and YACL 3.9.7+26.2-fabric
    ("=3.9.6+26.2-fabric"). This line declared the same shapes. Each minimum stays the
    version we compile against: FLK = the release part of flk_version
    ("1.13.13+kotlin.2.4.10" -> ">=1.13.13"), YACL = YACL_BUILT_VERSION, which is tied to the
    Modrinth coordinate above, so swapping the YACL artifact without updating it fails here.

    Fabric ignores build metadata when comparing, so ">=3.9.6+26.1-fabric" means ">=3.9.6";
    a YACL build for another game line is still rejected by YACL's own minecraft dependency.
    """
    if YACL_BUILD_COORDINATE not in build_script:
        fail(
            f"build.gradle.kts no longer resolves YACL {YACL_BUILT_VERSION} "
            f"({YACL_BUILD_COORDINATE}); update YACL_BUILT_VERSION in "
            "scripts/check_release_consistency.py and the fabric.mod.json minimum together."
        )
    flk_version = props.get("flk_version", "")
    if not flk_version:
        fail("gradle.properties is missing flk_version")
    for depend_key, minimum in (
        ("fabric-language-kotlin", flk_version.split("+", 1)[0]),
        ("yet_another_config_lib_v3", YACL_BUILT_VERSION),
    ):
        expected = f">={minimum}"
        declared = meta.get("depends", {}).get(depend_key, "")
        if declared != expected:
            fail(
                f"fabric.mod.json depends.{depend_key} should be the lower-bound-only range "
                f"'{expected}' (no upper cap, no exact pin); got '{declared}'. Caps and exact "
                "pins reject newer compatible releases with 'Incompatible mods found'."
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
        "tacz": "=1.1.8+fabric.26.1.2.R2",
    }
    for key, value in expected_depends.items():
        if meta["depends"].get(key) != value:
            fail(f"fabric.mod.json depends.{key} expected {value}, got {meta['depends'].get(key)}")
    if meta.get("suggests", {}).get("modmenu") != "*":
        fail("fabric.mod.json should suggest modmenu: *")
    check_fabric_api_range_is_lower_bound_only(props, meta)
    build_script = (ROOT / "build.gradle.kts").read_text(encoding="utf-8")
    check_flk_and_yacl_ranges_are_lower_bound_only(props, meta, build_script)
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
        for forbidden in (version, "26.1.2", "26.2", "Beta-1", "Beta-2"):
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

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
            if not path.is_file():
                fail(f"Manifest file is missing: {row['path']}")
            actual = sha256(path)
            if actual != row["sha256"]:
                fail(f"Checksum mismatch for {row['path']}: expected {row['sha256']}, got {actual}")
            for column in required - {"path", "sha256"}:
                if not row.get(column):
                    fail(f"Manifest row for {row['path']} has empty {column}")


def _semver_key(value: str) -> tuple[int, ...]:
    """Compare numeric release components; Fabric ignores build metadata after '+'."""
    core = value.split("+", 1)[0].split("-", 1)[0]
    return tuple(int(part) if part.isdigit() else 0 for part in core.split("."))


def _satisfies(version: str, predicate: str) -> bool:
    """Evaluate the Fabric dependency predicates used by this release line."""
    predicate = predicate.strip()
    if not predicate or predicate == "*":
        return True
    for op in (">=", "<=", "=", ">", "<"):
        if predicate.startswith(op):
            bound = predicate[len(op):].strip()
            if op == "=":
                # Exact pins include build metadata (e.g. the TaCZ release family).
                return version == bound
            actual, expected = _semver_key(version), _semver_key(bound)
            if op == ">=":
                return actual >= expected
            if op == "<=":
                return actual <= expected
            if op == ">":
                return actual > expected
            return actual < expected
    return version == predicate


def check_declared_ranges_include_built_versions(props: dict[str, str], meta: dict) -> None:
    """Fail if the loader would reject an exact version we compile against.

    This catches drift between gradle.properties and fabric.mod.json; the separate
    check_dependency_availability.py catches ranges matching NO published build.
    """
    depends = meta.get("depends", {})
    for prop_key, depend_key in (
        ("flk_version", "fabric-language-kotlin"),
        ("fabric_version", "fabric-api"),
        ("loader_version", "fabricloader"),
    ):
        built = props.get(prop_key)
        declared = depends.get(depend_key)
        if not built or not declared:
            fail(f"missing {prop_key} or fabric.mod.json depends.{depend_key}")
        if not all(_satisfies(built, part) for part in declared.split()):
            fail(
                f"fabric.mod.json depends.{depend_key} = '{declared}' excludes the version "
                f"this build actually uses ({prop_key} = {built}). Users would get "
                "'Incompatible mods found'."
            )


def check_fabric_api_range_is_lower_bound_only(props: dict[str, str], meta: dict) -> None:
    """Don't reject newer Fabric API releases for the same pinned Minecraft line."""
    declared = meta.get("depends", {}).get("fabric-api", "")
    expected = f">={props['fabric_version']}"
    if declared != expected:
        fail(
            f"fabric.mod.json depends.fabric-api should be the lower-bound-only range "
            f"'{expected}' (no upper cap); got '{declared}'."
        )


def check_runtime_version_gate(meta: dict) -> None:
    """Ensure the in-code TaCZ gate admits the exact version declared to Fabric.

    The gate executes at game startup, not during compilation or mixin auditing.
    In 26.2 the declared TaCZ build is R2, not 26.3's R1; copying that floor here
    would either reject supported versions or admit an incompatible hook surface.
    """
    source = (ROOT / "src/main/java/me/muksc/tacztweaks/TaCZTweaks.java").read_text(encoding="utf-8")

    def constant(name: str) -> str | None:
        match = re.search(rf'{name}\s*=\s*"([^"]+)"', source)
        return match.group(1) if match else None

    prefix = constant("SUPPORTED_TACZ_VERSION_PREFIX")
    supported = constant("SUPPORTED_TACZ_VERSION")
    if prefix is None or supported is None:
        fail("could not parse the TaCZ version gate constants from TaCZTweaks.java")

    revision_match = re.search(
        r"MIN_SUPPORTED_TACZ_REVISION\s*=\s*BigInteger\.(?:valueOf\((\d+)\)|(ONE)|(ZERO))", source
    )
    if not revision_match:
        fail("could not parse MIN_SUPPORTED_TACZ_REVISION from TaCZTweaks.java")
    minimum = int(revision_match.group(1)) if revision_match.group(1) else (1 if revision_match.group(2) else 0)

    declared = meta.get("depends", {}).get("tacz", "")
    if not declared.startswith("=") or not declared[1:]:
        fail("fabric.mod.json must declare an exact tacz dependency")
    required = declared[1:]
    if not required.startswith(prefix):
        fail(
            f"runtime gate accepts only '{prefix}<n>' but fabric.mod.json requires "
            f"'{required}'. The mod would refuse to start against its own declared dependency."
        )
    revision_text = required[len(prefix):].split("-", 1)[0]
    if not revision_text.isdigit():
        fail(f"cannot read the R-revision out of the declared tacz version '{required}'")
    if int(revision_text) < minimum:
        fail(
            f"runtime gate demands revision >= R{minimum} but fabric.mod.json requires "
            f"'{required}'. The mod would refuse to start against its own declared dependency."
        )
    if supported != required:
        fail(
            f"TaCZTweaks.SUPPORTED_TACZ_VERSION ('{supported}') disagrees with the declared "
            f"tacz dependency ('{required}')"
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
        "tacz": "=1.1.8+fabric.26.2.R2",
        "yet_another_config_lib_v3": "=3.9.6+26.2-fabric",
    }
    for key, value in expected_depends.items():
        if meta["depends"].get(key) != value:
            fail(f"fabric.mod.json depends.{key} expected {value}, got {meta['depends'].get(key)}")
    if meta.get("suggests", {}).get("modmenu") != "*":
        fail("fabric.mod.json should suggest modmenu: *")
    check_fabric_api_range_is_lower_bound_only(props, meta)
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
        for forbidden in (version, "26.2", "Beta-1"):
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
    props, meta = check_metadata()
    check_runtime_version_gate(meta)
    check_docs(props)
    if args.jar:
        check_jar(args.jar, props)
    print("RELEASE CONSISTENCY: OK")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))

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


def check_manifest(require_deps: bool) -> None:
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
                # libs/*.jar are not committed to Git; scripts/download_dependencies.py
                # rebuilds them from this manifest. Only the jobs that actually restored
                # them (build / audit / release) demand their presence, so the fast
                # offline consistency gate can still validate every text artifact.
                if require_deps:
                    fail(f"Manifest file is missing: {row['path']}")
                else:
                    print(f"SKIP (not restored) {row['path']}")
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


def _semver_key(value: str) -> tuple[int, ...]:
    """Numeric ordering key for a Fabric/SemVer version string.

    Only the numeric release part is compared: "1.13.13+kotlin.2.4.10" -> (1, 13, 13).
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

    Synced from the 26.3 line, where this caught a real incident: gradle.properties built
    against Fabric Language Kotlin 1.13.13 and fabric.mod.json shipped ">=1.13.13 <1.14.0",
    but the only FLK build tagged for Minecraft 26.3 is 1.14.1. The intersection was empty,
    so the mod could not load for ANY user, and nothing in CI noticed: compiling, auditing
    and jar packaging all pass regardless of what the range says. Only the loader enforces
    it, at startup.

    On this 1.21.11 line the FLK range ">=1.13.13 <1.14.0" is CORRECT and must not be
    copied from 26.3 -- verified against api.modrinth.com (2026-09-22, recorded in
    docs/CROSS_BRANCH_SYNC.md §2.4 on the 26.3 branch), FLK 1.13.12 / 1.13.13 / 1.13.14 /
    1.14.0 / 1.14.1 are all tagged for 1.21.11. What is synced is the gate, not the numbers.
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

    Fabric API releases for one Minecraft line can advance from e.g. 0.160.x to 0.162.x
    without changing the supported game version. An arbitrary upper bound rejects such
    compatible updates even though this mod's Minecraft dependency is pinned separately.
    Keep the selected build version as the minimum, with no cap.

    Synced from 26.3 (2026-10-15), where ">=0.160.7+26.3 <0.162.0" made Fabric Loader
    reject a user's installed 0.162.0+26.3. This line had the same shape of cap
    ("<0.143.0"); only the cap is removed here, the 1.21.11 minimum is kept as-is.
    """
    declared = meta.get("depends", {}).get("fabric-api", "")
    expected = f">={props['fabric_version']}"
    if declared != expected:
        fail(
            f"fabric.mod.json depends.fabric-api should be the lower-bound-only range "
            f"'{expected}' (no upper cap); got '{declared}'. An upper cap can reject "
            f"newer Fabric API releases for the same Minecraft line."
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
    if not meta.get("authors"):
        fail("fabric.mod.json authors is empty")

    expected_depends = {
        "minecraft": f"={props['minecraft_version']}",
        "java": ">=21",
        "tacz": "=1.1.8+fabric.1.21.11.R2",
        "yet_another_config_lib_v3": "=3.8.2+1.21.11-fabric",
        "fabricloader": ">=0.19.3 <0.20.0",
        "fabric-language-kotlin": ">=1.13.13 <1.14.0",
    }
    for key, value in expected_depends.items():
        if meta["depends"].get(key) != value:
            fail(f"fabric.mod.json depends.{key} expected {value}, got {meta['depends'].get(key)}")
    if meta.get("suggests", {}).get("modmenu") != "*":
        fail("fabric.mod.json should suggest modmenu: *")
    if props.get("minecraft_version") != "1.21.11":
        fail(f"gradle.properties minecraft_version drifted: {props.get('minecraft_version')}")
    # fabric-api is deliberately NOT in expected_depends: it must stay a lower-bound-only
    # range derived from gradle.properties, which the two checks below enforce.
    check_fabric_api_range_is_lower_bound_only(props, meta)
    check_declared_ranges_include_built_versions(props, meta)
    return props, meta


def check_runtime_version_gate(meta: dict) -> None:
    """The hardcoded runtime TaCZ gate must agree with the declared dependency.

    TaCZTweaks.java refuses to initialise unless the installed TaCZ matches
    SUPPORTED_TACZ_VERSION_PREFIX + a revision >= MIN_SUPPORTED_TACZ_REVISION. That check
    runs only inside the game, so a stale value survives compilation, the mixin audit and
    packaging untouched, and then hard-crashes every user at startup.

    That is exactly what happened on the 26.3 line on 2026-09-22: the branch still gated on
    "1.1.8+fabric.26.2.R2" and threw

        TaCZ Tweaks requires TaCZ 1.1.8+fabric.26.2.R2 or a later R<n> build for
        Minecraft 26.2, found 1.1.8+fabric.26.3.R1

    So: parse the constants out of the Java source and require that the version we declare
    in fabric.mod.json would actually pass the gate.

    Per-branch note (do NOT copy 26.3's constants here): this line's floor is R2, because
    TaCZ Refabricated 1.21.11 R1 predates APIs this mod needs. 26.3 ships from R1. The
    check reads the floor from the source instead of hardcoding it, so both stay correct.
    """
    source = (ROOT / "src/main/java/me/muksc/tacztweaks/TaCZTweaks.java").read_text(encoding="utf-8")

    def constant(name: str) -> str | None:
        match = re.search(rf'{name}\s*=\s*"([^"]+)"', source)
        return match.group(1) if match else None

    prefix = constant("SUPPORTED_TACZ_VERSION_PREFIX")
    supported = constant("SUPPORTED_TACZ_VERSION")
    if prefix is None or supported is None:
        fail("could not parse the TaCZ version gate constants from TaCZTweaks.java")
        return

    revision_match = re.search(
        r"MIN_SUPPORTED_TACZ_REVISION\s*=\s*BigInteger\.(?:valueOf\((\d+)\)|(ONE)|(ZERO))", source
    )
    if not revision_match:
        fail("could not parse MIN_SUPPORTED_TACZ_REVISION from TaCZTweaks.java")
        return
    if revision_match.group(1) is not None:
        minimum = int(revision_match.group(1))
    else:
        minimum = 1 if revision_match.group(2) else 0

    declared = meta.get("depends", {}).get("tacz", "")
    required = declared.lstrip("=").strip()
    if not required:
        fail("fabric.mod.json does not declare a tacz dependency")
        return

    if not required.startswith(prefix):
        fail(
            f"runtime gate accepts only '{prefix}<n>' but fabric.mod.json requires "
            f"'{required}'. The mod would refuse to start against its own declared "
            f"dependency."
        )
        return
    revision_text = required[len(prefix):].split("-", 1)[0]
    if not revision_text.isdigit():
        fail(f"cannot read the R-revision out of the declared tacz version '{required}'")
        return
    if int(revision_text) < minimum:
        fail(
            f"runtime gate demands revision >= R{minimum} but fabric.mod.json requires "
            f"'{required}'. The mod would refuse to start against its own declared "
            f"dependency."
        )
    # The advertised constant should itself be the version we depend on.
    if supported != required:
        fail(
            f"TaCZTweaks.SUPPORTED_TACZ_VERSION ('{supported}') disagrees with the declared "
            f"tacz dependency ('{required}')"
        )


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
    parser.add_argument(
        "--require-deps",
        action="store_true",
        help="fail when a manifest dependency is absent (use after download_dependencies.py)",
    )
    args = parser.parse_args(argv)

    check_manifest(require_deps=args.require_deps)
    props, meta = check_metadata()
    check_runtime_version_gate(meta)
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

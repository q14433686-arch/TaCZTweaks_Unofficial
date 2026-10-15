#!/usr/bin/env python3
"""Regression tests for Fabric dependency-range launch failures and policy.

Two concrete failures established the need for these guards:

* ``fabric-language-kotlin: ">=1.13.13 <1.14.0"`` excluded the only FLK build published
  for Minecraft 26.3, ``1.14.1``.
* ``fabric-api: ">=0.160.7+26.3 <0.162.0"`` excluded the user's installed
  ``0.162.0+26.3`` build by imposing an unnecessary upper cap.

The 26.2 backport extends the lower-bound-only policy to Fabric Loader, FLK and YACL.
The YACL compile version is read from RESOURCE_IMPORT_MANIFEST.tsv rather than duplicated
as a hard-coded constant. Compiling and packaging alone do not enforce any of these ranges;
Fabric Loader evaluates them only at game startup.

These tests cover four distinct checks:

1. ``check_release_consistency._satisfies`` and the manifest comparison ensure every
   dependency version used to compile is admitted by the range shipped to users.
2. Lower-bound-only policy checks prevent arbitrary caps or exact YACL pins from rejecting
   newer versions for the same Minecraft line.
3. ``check_dependency_availability`` asks Modrinth whether each checkable dependency has a
   build for our Minecraft version that matches the shipped range. A network ``SKIP`` is not
   an availability verification.
4. Release-tag tests ensure hotfix version suffixes match the repository's actual tag format.

Run: python3 -m pytest scripts/test_dependency_ranges.py scripts/test_validate_release_tag.py
"""

from __future__ import annotations

import importlib.util
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))


def _load(name: str, filename: str):
    spec = importlib.util.spec_from_file_location(name, ROOT / "scripts" / filename)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


consistency = _load("check_release_consistency", "check_release_consistency.py")
availability = _load("check_dependency_availability", "check_dependency_availability.py")

satisfies = consistency._satisfies

# Representative Fabric builds for Minecraft 26.3. The 0.162.0+26.3 entry is the
# concrete version from the user's crash report; the previous upper cap rejected it.
MODRINTH_26_3 = {
    "fabric-language-kotlin": [{"version_number": "1.14.1+kotlin.2.4.20"}],
    "fabric-api": [
        {"version_number": "0.162.0+26.3"},
        {"version_number": "0.161.0+26.3"},
        {"version_number": "0.160.7+26.3"},
        {"version_number": "0.160.6+26.3"},
        {"version_number": "0.160.5+26.3"},
    ],
    "yacl": [
        {"version_number": "3.9.7+26.3-fabric"},
        {"version_number": "3.9.6+26.3-fabric"},
    ],
}


def _range_matches(version: str, declared: str) -> bool:
    return all(satisfies(version, part) for part in declared.split())


def test_the_shipped_range_excluded_the_only_263_build() -> None:
    """The precise condition that crashed the game."""
    assert not _range_matches("1.14.1+kotlin.2.4.20", ">=1.13.13 <1.14.0")


def test_current_flk_range_admits_the_only_263_build() -> None:
    assert _range_matches("1.14.1+kotlin.2.4.20", ">=1.14.1+kotlin.2.4.20")


def test_range_operators() -> None:
    assert _range_matches("0.160.7+26.3", ">=0.160.7+26.3")
    assert _range_matches("0.162.0+26.3", ">=0.160.7+26.3")
    # This reproduces the reported failure: the old artificial cap rejected 0.162.0.
    assert not _range_matches("0.162.0+26.3", ">=0.160.7+26.3 <0.162.0")
    assert _range_matches("1.1.8+fabric.26.3.R1", "=1.1.8+fabric.26.3.R1")
    # '=' is exact: build metadata must match too, so R2 must not satisfy an R1 pin.
    assert not _range_matches("1.1.8+fabric.26.3.R2", "=1.1.8+fabric.26.3.R1")
    assert _range_matches("9.9.9", "*")


def test_fabric_api_range_is_open_ended_and_accepts_reported_version() -> None:
    props = consistency.read_properties(ROOT / "gradle.properties")
    meta = json.loads((ROOT / "src/main/resources/fabric.mod.json").read_text(encoding="utf-8"))
    declared = meta["depends"]["fabric-api"]

    assert declared == f">={props['fabric_version']}"
    assert _range_matches("0.162.0+26.3", declared)
    consistency.check_fabric_api_range_is_lower_bound_only(props, meta)


def test_fabric_api_policy_rejects_an_upper_cap() -> None:
    props = consistency.read_properties(ROOT / "gradle.properties")
    meta = json.loads((ROOT / "src/main/resources/fabric.mod.json").read_text(encoding="utf-8"))
    meta["depends"]["fabric-api"] += " <0.162.0"

    raised = False
    try:
        consistency.check_fabric_api_range_is_lower_bound_only(props, meta)
    except SystemExit:
        raised = True
    assert raised, "a Fabric API upper cap must be rejected by release consistency"


def test_loader_range_is_open_ended_from_its_build_minimum() -> None:
    props = consistency.read_properties(ROOT / "gradle.properties")
    meta = json.loads((ROOT / "src/main/resources/fabric.mod.json").read_text(encoding="utf-8"))
    declared = meta["depends"]["fabricloader"]

    assert declared == f">={props['loader_version']}"
    assert _range_matches("0.20.0", declared)
    assert not _range_matches("0.19.4", declared)
    consistency.check_fabric_loader_range_is_lower_bound_only(props, meta)


def test_loader_policy_rejects_an_upper_cap() -> None:
    props = consistency.read_properties(ROOT / "gradle.properties")
    meta = json.loads((ROOT / "src/main/resources/fabric.mod.json").read_text(encoding="utf-8"))
    meta["depends"]["fabricloader"] += " <0.20.0"

    raised = False
    try:
        consistency.check_fabric_loader_range_is_lower_bound_only(props, meta)
    except SystemExit:
        raised = True
    assert raised, "a Loader upper cap must be rejected by release consistency"


def test_flk_and_yacl_ranges_are_lower_bound_only() -> None:
    props = consistency.read_properties(ROOT / "gradle.properties")
    meta = json.loads((ROOT / "src/main/resources/fabric.mod.json").read_text(encoding="utf-8"))
    flk_range = meta["depends"]["fabric-language-kotlin"]
    yacl_range = meta["depends"]["yet_another_config_lib_v3"]
    yacl_minimum = consistency.read_yacl_manifest_version()

    assert flk_range == f">={props['flk_version']}"
    assert yacl_range == f">={yacl_minimum}"
    # Synthetic newer versions confirm there is no artificial cap. This does not claim
    # those versions have been game-tested; the CI availability check verifies releases.
    assert _range_matches("2.0.0+kotlin.2.4.21", flk_range)
    assert _range_matches("3.9.8+26.3-fabric", yacl_range)
    assert not _range_matches("3.9.6+26.3-fabric", yacl_range)
    consistency.check_flk_and_yacl_ranges_are_lower_bound_only(props, meta)


def test_flk_policy_rejects_an_upper_cap() -> None:
    props = consistency.read_properties(ROOT / "gradle.properties")
    meta = json.loads((ROOT / "src/main/resources/fabric.mod.json").read_text(encoding="utf-8"))
    meta["depends"]["fabric-language-kotlin"] += " <2.0.0"

    raised = False
    try:
        consistency.check_flk_and_yacl_ranges_are_lower_bound_only(props, meta)
    except SystemExit:
        raised = True
    assert raised, "an FLK upper cap must be rejected by release consistency"


def test_yacl_policy_rejects_an_exact_pin() -> None:
    props = consistency.read_properties(ROOT / "gradle.properties")
    meta = json.loads((ROOT / "src/main/resources/fabric.mod.json").read_text(encoding="utf-8"))
    meta["depends"]["yet_another_config_lib_v3"] = f"={consistency.read_yacl_manifest_version()}"

    raised = False
    try:
        consistency.check_flk_and_yacl_ranges_are_lower_bound_only(props, meta)
    except SystemExit:
        raised = True
    assert raised, "a YACL exact pin must be rejected by release consistency"


def test_shipped_metadata_is_satisfiable_by_known_upstream_builds(monkeypatch) -> None:
    """The end-to-end guard must accept every currently known/reported 26.3 build."""
    monkeypatch.setattr(availability, "fetch_versions", lambda slug, gv: MODRINTH_26_3[slug])
    assert availability.main([]) == 0


def test_availability_check_rejects_the_historical_bug(monkeypatch, tmp_path) -> None:
    """Reintroducing the bad range must fail the check, otherwise the guard is worthless."""
    monkeypatch.setattr(availability, "fetch_versions", lambda slug, gv: MODRINTH_26_3[slug])

    meta_path = ROOT / "src/main/resources/fabric.mod.json"
    original = meta_path.read_text(encoding="utf-8")
    broken = json.loads(original)
    broken["depends"]["fabric-language-kotlin"] = ">=1.13.13 <1.14.0"
    try:
        meta_path.write_text(json.dumps(broken, indent=2), encoding="utf-8")
        assert availability.main([]) == 1
    finally:
        meta_path.write_text(original, encoding="utf-8")


def test_gradle_properties_and_fabric_mod_json_agree() -> None:
    """The versions we compile against must satisfy the ranges we advertise."""
    props = consistency.read_properties(ROOT / "gradle.properties")
    meta = json.loads((ROOT / "src/main/resources/fabric.mod.json").read_text(encoding="utf-8"))
    depends = meta["depends"]
    for prop_key, depend_key in (
        ("flk_version", "fabric-language-kotlin"),
        ("fabric_version", "fabric-api"),
        ("loader_version", "fabricloader"),
    ):
        assert _range_matches(props[prop_key], depends[depend_key]), (
            f"{depend_key} range {depends[depend_key]!r} excludes built {props[prop_key]!r}"
        )
    yacl_version = consistency.read_yacl_manifest_version()
    assert _range_matches(yacl_version, depends["yet_another_config_lib_v3"]), (
        f"YACL range {depends['yet_another_config_lib_v3']!r} excludes compile "
        f"version {yacl_version!r} from RESOURCE_IMPORT_MANIFEST.tsv"
    )
    consistency.check_declared_ranges_include_built_versions(props, meta)


def test_compile_range_guard_rejects_a_yacl_floor_above_manifest_version() -> None:
    """A correct policy string is not enough if its minimum excludes the compile jar."""
    props = consistency.read_properties(ROOT / "gradle.properties")
    meta = json.loads((ROOT / "src/main/resources/fabric.mod.json").read_text(encoding="utf-8"))
    yacl_minimum = consistency.read_yacl_manifest_version()
    meta["depends"]["yet_another_config_lib_v3"] = ">=3.9.8+26.3-fabric"

    raised = False
    try:
        consistency.check_declared_ranges_include_built_versions(props, meta)
    except SystemExit:
        raised = True
    assert raised, (
        f"the consistency check must reject a YACL floor above the compile version "
        f"recorded in the manifest ({yacl_minimum})"
    )


def _java_source() -> str:
    return (ROOT / "src/main/java/me/muksc/tacztweaks/TaCZTweaks.java").read_text(encoding="utf-8")


def test_runtime_gate_matches_declared_dependency() -> None:
    """The repo as it stands must pass the runtime-gate cross-check."""
    meta = json.loads((ROOT / "src/main/resources/fabric.mod.json").read_text(encoding="utf-8"))
    consistency.check_runtime_version_gate(meta)  # must not raise/exit


def test_runtime_gate_catches_a_stale_release_family(tmp_path, monkeypatch) -> None:
    """Reintroducing the 2026-09-22 crash must be caught.

    The gate read 1.1.8+fabric.26.2.R2 on the 26.3 branch, so the mod threw during
    entrypoint init while every CI workflow stayed green.
    """
    java_path = ROOT / "src/main/java/me/muksc/tacztweaks/TaCZTweaks.java"
    original = _java_source()
    stale = (
        original.replace('"1.1.8+fabric.26.3.R1"', '"1.1.8+fabric.26.2.R2"')
        .replace('"1.1.8+fabric.26.3.R"', '"1.1.8+fabric.26.2.R"')
    )
    meta = json.loads((ROOT / "src/main/resources/fabric.mod.json").read_text(encoding="utf-8"))
    try:
        java_path.write_text(stale, encoding="utf-8")
        raised = False
        try:
            consistency.check_runtime_version_gate(meta)
        except SystemExit:
            raised = True
        assert raised, "a stale runtime gate must fail the consistency check"
    finally:
        java_path.write_text(original, encoding="utf-8")


def test_runtime_gate_catches_too_high_a_minimum_revision() -> None:
    """26.3 shipped as R1, so a leftover 'minimum R2' floor must be rejected too."""
    java_path = ROOT / "src/main/java/me/muksc/tacztweaks/TaCZTweaks.java"
    original = _java_source()
    stale = original.replace(
        "MIN_SUPPORTED_TACZ_REVISION = BigInteger.ONE",
        "MIN_SUPPORTED_TACZ_REVISION = BigInteger.valueOf(2)",
    )
    meta = json.loads((ROOT / "src/main/resources/fabric.mod.json").read_text(encoding="utf-8"))
    try:
        java_path.write_text(stale, encoding="utf-8")
        raised = False
        try:
            consistency.check_runtime_version_gate(meta)
        except SystemExit:
            raised = True
        assert raised, "an unreachable minimum revision must fail the consistency check"
    finally:
        java_path.write_text(original, encoding="utf-8")

if __name__ == "__main__":
    raise SystemExit(__import__("pytest").main([__file__, "-q"]))

#!/usr/bin/env python3
"""Regression tests for the 2026-09-22 "Incompatible mods found" incident.

The mod shipped ``fabric-language-kotlin: ">=1.13.13 <1.14.0"`` while the only FLK build
published for Minecraft 26.3 was ``1.14.1``. The intersection was empty, so the game
refused to start for every user. Compiling, the static mixin audit and jar packaging all
passed, because none of them evaluate dependency ranges -- only the Fabric loader does.

Two distinct guards came out of that, and these tests pin down which one catches what:

1. ``check_release_consistency._satisfies`` -- does the version we BUILD against satisfy
   the range we SHIP? Catches the two files drifting apart.
2. ``check_dependency_availability`` -- does upstream actually PUBLISH something in that
   range for our Minecraft version? Catches the case where both files agree with each
   other but disagree with reality, which is what actually happened.

Run: python3 -m pytest scripts/test_dependency_ranges.py
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

# Exactly what api.modrinth.com/v2 returned for game_versions=["26.3"], loaders=["fabric"]
# on 2026-09-22. FLK having a single 26.3 build is the crux of the incident.
MODRINTH_26_3 = {
    "fabric-language-kotlin": [{"version_number": "1.14.1+kotlin.2.4.20"}],
    "fabric-api": [
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


def test_current_range_admits_the_only_263_build() -> None:
    assert _range_matches("1.14.1+kotlin.2.4.20", ">=1.14.1+kotlin.2.4.20 <2.0.0")


def test_range_operators() -> None:
    assert _range_matches("0.160.7+26.3", ">=0.160.7+26.3 <0.162.0")
    assert not _range_matches("0.162.0+26.3", ">=0.160.7+26.3 <0.162.0")
    assert _range_matches("1.1.8+fabric.26.3.R1", "=1.1.8+fabric.26.3.R1")
    # '=' is exact: build metadata must match too, so R2 must not satisfy an R1 pin.
    assert not _range_matches("1.1.8+fabric.26.3.R2", "=1.1.8+fabric.26.3.R1")
    assert _range_matches("9.9.9", "*")


def test_shipped_metadata_is_satisfiable_by_real_upstream_builds(monkeypatch) -> None:
    """The end-to-end guard, against real upstream data: the repo as it stands must pass."""
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

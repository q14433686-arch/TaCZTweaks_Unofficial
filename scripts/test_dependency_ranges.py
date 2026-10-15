#!/usr/bin/env python3
"""Regression tests for Fabric Loader dependency-range launch failures (1.21.11 line).

Synced from the ``26.3`` branch, where two bad Fabric dependency ranges caused
"all four CI workflows green, but the game refuses to start" incidents:

* ``fabric-language-kotlin: ">=1.13.13 <1.14.0"`` excluded the only FLK build published
  for Minecraft 26.3, ``1.14.1``.
* ``fabric-api: ">=0.160.7+26.3 <0.162.0"`` excluded a user's installed ``0.162.0+26.3``
  by imposing an unnecessary upper cap.

Compiling, the static mixin audit and jar packaging all passed in both cases, because none
of them evaluate dependency ranges -- only the Fabric loader does, at startup.

⚠️  WHAT IS AND IS NOT COPIED FROM 26.3
---------------------------------------
The *gates* are copied. The *version numbers* are not, and must not be:

* On this line ``fabric-language-kotlin: ">=1.13.13 <1.14.0"`` is CORRECT. FLK 1.13.12 /
  1.13.13 / 1.13.14 / 1.14.0 / 1.14.1 are all tagged for Minecraft 1.21.11, so the range
  is satisfiable. Copying 26.3's ``>=1.14.1+kotlin.2.4.20`` floor here would exclude the
  1.13.13 build this branch actually compiles against. ``test_copying_263_flk_range_is_rejected``
  pins that down.
* This line's TaCZ floor is R2 (1.21.11 R1 predates APIs this mod needs); 26.3 ships R1.
  The runtime-gate check reads the floor from ``TaCZTweaks.java`` instead of hardcoding it.
* The ``fabric-api`` upper cap (``<0.143.0``) IS removed here, matching 26.3's fix.

⚠️  BRANCH-SPECIFIC TEST HARNESS
--------------------------------
``check_release_consistency.fail()`` on this 1.21.11 branch ACCUMULATES messages into
``_ERRORS`` and lets ``main()`` report them all at once; on ``26.3`` it raises
``SystemExit`` immediately. Negative tests therefore assert on ``_ERRORS`` via
``_expect_failure`` below rather than on ``SystemExit``. Do not "fix" this to match 26.3.

These tests pin down which validation catches what:

1. ``check_release_consistency._satisfies`` -- does the version we BUILD against satisfy
   the range we SHIP? Catches the two files drifting apart.
2. ``check_fabric_api_range_is_lower_bound_only`` -- does Fabric API avoid an arbitrary
   upper cap that blocks later releases for the same game line?
3. ``check_runtime_version_gate`` -- does the hardcoded in-game TaCZ gate accept the
   version ``fabric.mod.json`` declares?
4. ``check_dependency_availability`` -- does upstream actually PUBLISH something in that
   range for our Minecraft version? Catches the case where both files agree with each
   other but disagree with reality, which is what happened in the FLK incident.

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

META_PATH = ROOT / "src/main/resources/fabric.mod.json"
JAVA_PATH = ROOT / "src/main/java/me/muksc/tacztweaks/TaCZTweaks.java"

# Representative Fabric builds for Minecraft 1.21.11.
#
# Provenance: the FLK set is the one verified against api.modrinth.com and recorded in
# docs/CROSS_BRANCH_SYNC.md §2.4 on the 26.3 branch (1.13.12 / 1.13.13 / 1.13.14 /
# 1.14.0 / 1.14.1 are all tagged for 1.21.11). The fabric-api set is this branch's
# previously verified range: gradle.properties builds 0.141.6+1.21.11 and the removed
# upper cap was "<0.143.0", so 0.142.x existed for the same game version. 0.143.0 is
# included as the "next release on the same line" that the old cap would have rejected --
# the exact failure shape reported on 26.3 with 0.162.0+26.3.
MODRINTH_1_21_11 = {
    "fabric-language-kotlin": [
        {"version_number": "1.14.1+kotlin.2.4.20"},
        {"version_number": "1.14.0+kotlin.2.4.10"},
        {"version_number": "1.13.14+kotlin.2.4.10"},
        {"version_number": "1.13.13+kotlin.2.4.10"},
        {"version_number": "1.13.12+kotlin.2.3.21"},
    ],
    "fabric-api": [
        {"version_number": "0.143.0+1.21.11"},
        {"version_number": "0.142.1+1.21.11"},
        {"version_number": "0.141.6+1.21.11"},
        {"version_number": "0.141.5+1.21.11"},
    ],
    "yacl": [
        {"version_number": "3.8.2+1.21.11-fabric"},
    ],
}


def _range_matches(version: str, declared: str) -> bool:
    return all(satisfies(version, part) for part in declared.split())


def _props() -> dict[str, str]:
    return consistency.read_properties(ROOT / "gradle.properties")


def _meta() -> dict:
    return json.loads(META_PATH.read_text(encoding="utf-8"))


def _expect_failure(check, *args) -> list[str]:
    """Run a check and return the errors it recorded.

    This branch's fail() accumulates into _ERRORS instead of raising SystemExit (see the
    module docstring), so "the gate caught it" means "_ERRORS is non-empty afterwards".
    """
    consistency._ERRORS.clear()
    check(*args)
    errors = list(consistency._ERRORS)
    consistency._ERRORS.clear()
    return errors


def _rewrite(path: Path, original: str, mutated: str):
    """Context manager-free helper: write `mutated`, yield, restore `original`."""
    import contextlib

    @contextlib.contextmanager
    def _cm():
        try:
            path.write_text(mutated, encoding="utf-8")
            yield
        finally:
            path.write_text(original, encoding="utf-8")

    return _cm()


# ---------------------------------------------------------------------------
# _satisfies: the operator semantics every other gate is built on
# ---------------------------------------------------------------------------


def test_range_operators() -> None:
    assert _range_matches("0.141.6+1.21.11", ">=0.141.6+1.21.11")
    # The point of removing the cap: a later release on the same game line is admitted.
    assert _range_matches("0.143.0+1.21.11", ">=0.141.6+1.21.11")
    # This reproduces the 26.3 failure shape: an artificial cap rejects it.
    assert not _range_matches("0.143.0+1.21.11", ">=0.141.6+1.21.11 <0.143.0")
    assert _range_matches("1.1.8+fabric.1.21.11.R2", "=1.1.8+fabric.1.21.11.R2")
    # '=' is exact: build metadata must match too, so R3 must not satisfy an R2 pin.
    assert not _range_matches("1.1.8+fabric.1.21.11.R3", "=1.1.8+fabric.1.21.11.R2")
    assert _range_matches("9.9.9", "*")


def test_flk_range_on_this_line_is_satisfiable_and_must_not_be_copied_from_263() -> None:
    """1.21.11 legitimately keeps ">=1.13.13 <1.14.0"; 26.3 could not."""
    published = [v["version_number"] for v in MODRINTH_1_21_11["fabric-language-kotlin"]]

    # Correct here: several published 1.21.11 builds fall inside the range.
    assert _range_matches("1.13.13+kotlin.2.4.10", ">=1.13.13 <1.14.0")
    assert any(_range_matches(v, ">=1.13.13 <1.14.0") for v in published)

    # The same range on 26.3, where only 1.14.1 existed: empty intersection, game won't start.
    assert not _range_matches("1.14.1+kotlin.2.4.20", ">=1.13.13 <1.14.0")


def test_copying_263_flk_range_is_rejected() -> None:
    """§0.3 of the sync guide: the easiest mistake is unifying the FLK range.

    26.3 had to move to ">=1.14.1+kotlin.2.4.20 <2.0.0". Copying that here would exclude
    the 1.13.13 build gradle.properties actually compiles against, which is precisely the
    "Incompatible mods found" class of launch failure.
    """
    props = _props()
    meta = _meta()
    original = meta["depends"]["fabric-language-kotlin"]
    meta["depends"]["fabric-language-kotlin"] = ">=1.14.1+kotlin.2.4.20 <2.0.0"

    errors = _expect_failure(consistency.check_declared_ranges_include_built_versions, props, meta)
    assert errors, "copying 26.3's FLK floor must be rejected on the 1.21.11 line"
    assert any("fabric-language-kotlin" in e for e in errors)

    meta["depends"]["fabric-language-kotlin"] = original
    assert not _expect_failure(
        consistency.check_declared_ranges_include_built_versions, props, meta
    )


# ---------------------------------------------------------------------------
# Fabric API: lower bound only, no artificial cap
# ---------------------------------------------------------------------------


def test_fabric_api_range_is_open_ended() -> None:
    props = _props()
    meta = _meta()
    declared = meta["depends"]["fabric-api"]

    assert declared == f">={props['fabric_version']}"
    assert _range_matches("0.143.0+1.21.11", declared)
    assert not _expect_failure(consistency.check_fabric_api_range_is_lower_bound_only, props, meta)


def test_fabric_api_policy_rejects_an_upper_cap() -> None:
    props = _props()
    meta = _meta()
    meta["depends"]["fabric-api"] += " <0.143.0"

    errors = _expect_failure(consistency.check_fabric_api_range_is_lower_bound_only, props, meta)
    assert errors, "a Fabric API upper cap must be rejected by release consistency"
    assert any("no upper cap" in e for e in errors)


# ---------------------------------------------------------------------------
# Upstream availability: do the declared ranges match anything real?
# ---------------------------------------------------------------------------


def test_shipped_metadata_is_satisfiable_by_known_upstream_builds(monkeypatch) -> None:
    """The end-to-end guard must accept every known 1.21.11 build."""
    monkeypatch.setattr(availability, "fetch_versions", lambda slug, gv: MODRINTH_1_21_11[slug])
    assert availability.main([]) == 0


def test_availability_check_rejects_an_unpublishable_range(monkeypatch) -> None:
    """Reintroducing an unsatisfiable range must fail, otherwise the guard is worthless.

    This is the §2.3 "deliberately break it" acceptance step, automated.
    """
    monkeypatch.setattr(availability, "fetch_versions", lambda slug, gv: MODRINTH_1_21_11[slug])

    original = META_PATH.read_text(encoding="utf-8")
    broken = json.loads(original)
    broken["depends"]["fabric-language-kotlin"] = ">=99.0.0"
    with _rewrite(META_PATH, original, json.dumps(broken, indent=2)):
        assert availability.main([]) == 1


def test_availability_check_rejects_the_capped_fabric_api_range(monkeypatch) -> None:
    """The 26.3 fabric-api incident, replayed against 1.21.11 data."""
    def fetch(slug, game_version):
        if slug == "fabric-api":
            # Only a newer same-line release is installed/published.
            return [{"version_number": "0.143.0+1.21.11"}]
        return MODRINTH_1_21_11[slug]

    monkeypatch.setattr(availability, "fetch_versions", fetch)

    original = META_PATH.read_text(encoding="utf-8")
    broken = json.loads(original)
    broken["depends"]["fabric-api"] += " <0.143.0"
    with _rewrite(META_PATH, original, json.dumps(broken, indent=2)):
        assert availability.main([]) == 1


def test_gradle_properties_and_fabric_mod_json_agree() -> None:
    """The versions we compile against must satisfy the ranges we advertise."""
    props = _props()
    depends = _meta()["depends"]
    for prop_key, depend_key in (
        ("flk_version", "fabric-language-kotlin"),
        ("fabric_version", "fabric-api"),
        ("loader_version", "fabricloader"),
    ):
        assert _range_matches(props[prop_key], depends[depend_key]), (
            f"{depend_key} range {depends[depend_key]!r} excludes built {props[prop_key]!r}"
        )


# ---------------------------------------------------------------------------
# Runtime TaCZ gate: the check that only runs inside the game
# ---------------------------------------------------------------------------


def test_runtime_gate_matches_declared_dependency() -> None:
    """The repo as it stands must pass the runtime-gate cross-check."""
    assert not _expect_failure(consistency.check_runtime_version_gate, _meta())


def test_runtime_gate_catches_a_stale_release_family() -> None:
    """Reintroducing the 2026-09-22 crash must be caught.

    The gate read 1.1.8+fabric.26.2.R2 on the 26.3 branch, so the mod threw during
    entrypoint init while every CI workflow stayed green. The 1.21.11 equivalent is a gate
    left pointing at another release family.
    """
    original = JAVA_PATH.read_text(encoding="utf-8")
    stale = (
        original.replace('"1.1.8+fabric.1.21.11.R2"', '"1.1.8+fabric.26.2.R2"')
        .replace('"1.1.8+fabric.1.21.11.R"', '"1.1.8+fabric.26.2.R"')
    )
    assert stale != original
    with _rewrite(JAVA_PATH, original, stale):
        errors = _expect_failure(consistency.check_runtime_version_gate, _meta())
    assert errors, "a stale runtime gate must fail the consistency check"
    assert any("runtime gate accepts only" in e for e in errors)


def test_runtime_gate_catches_too_high_a_minimum_revision() -> None:
    """A floor above the declared R revision means the mod rejects its own dependency.

    This line declares R2, so a leftover "minimum R3" must be rejected. (26.3 tests the
    mirror image: its floor is R1, so a leftover R2 is the bug there.)
    """
    original = JAVA_PATH.read_text(encoding="utf-8")
    stale = original.replace(
        "MIN_SUPPORTED_TACZ_REVISION = BigInteger.valueOf(2)",
        "MIN_SUPPORTED_TACZ_REVISION = BigInteger.valueOf(3)",
    )
    assert stale != original
    with _rewrite(JAVA_PATH, original, stale):
        errors = _expect_failure(consistency.check_runtime_version_gate, _meta())
    assert errors, "an unreachable minimum revision must fail the consistency check"
    assert any("demands revision >= R3" in e for e in errors)


def test_runtime_gate_catches_a_disagreement_with_fabric_mod_json() -> None:
    """SUPPORTED_TACZ_VERSION must be the version we actually declare."""
    original = JAVA_PATH.read_text(encoding="utf-8")
    stale = original.replace(
        '"1.1.8+fabric.1.21.11.R2"', '"1.1.8+fabric.1.21.11.R9"'
    )
    assert stale != original
    with _rewrite(JAVA_PATH, original, stale):
        errors = _expect_failure(consistency.check_runtime_version_gate, _meta())
    assert errors
    assert any("disagrees with the declared" in e for e in errors)


if __name__ == "__main__":
    raise SystemExit(__import__("pytest").main([__file__, "-q"]))

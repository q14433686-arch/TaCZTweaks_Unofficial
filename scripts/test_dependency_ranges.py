#!/usr/bin/env python3
"""Regression tests for Fabric Loader dependency-range launch failures (1.21.11 line).

Synced from the ``26.3`` branch, where two bad Fabric dependency ranges caused
"all four CI workflows green, but the game refuses to start" incidents:

* ``fabric-language-kotlin: ">=1.13.13 <1.14.0"`` excluded the only FLK build published
  for Minecraft 26.3, ``1.14.1``.
* ``fabric-api: ">=0.160.7+26.3 <0.162.0"`` excluded a user's installed ``0.162.0+26.3``
  by imposing an unnecessary upper cap.
* On 1.21.11 three more of the same shapes -- ``fabricloader: ">=0.19.3 <0.20.0"``,
  ``fabric-language-kotlin: ">=1.13.13 <1.14.0"`` and ``yet_another_config_lib_v3:
  "=3.8.2+1.21.11-fabric"`` -- rejected Fabric Loader 0.20.0 (released 2026-10-15), Fabric
  Language Kotlin 1.14.2, and any later YACL build for this game line. All three are fixed
  here as well (2026-10-16 sync of the 26.1.2 line's fixes).

Compiling, the static mixin audit and jar packaging all passed in both cases, because none
of them evaluate dependency ranges -- only the Fabric loader does, at startup.

⚠️  WHAT IS AND IS NOT COPIED FROM 26.3
---------------------------------------
The *gates* are copied. The *version numbers* are not, and must not be:

* This line's FLK **minimum** stays 1.13.13 -- it is the build this branch compiles against.
  Copying 26.3's ``>=1.14.1+kotlin.2.4.20`` floor here would exclude that build and
  reproduce the 26.3 incident on purpose. ``test_copying_263_flk_range_is_rejected`` pins
  that down. What *is* shared is the policy: no upper cap (FLK 1.14.0 / 1.14.1 / 1.14.2 are
  all tagged for 1.21.11, so ``<1.14.0`` was rejecting published releases here too).
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
2. ``check_fabric_api_range_is_lower_bound_only`` and
   ``check_fabric_loader_range_is_lower_bound_only`` -- do the two loader-provided
   dependencies avoid an arbitrary upper cap that blocks later releases for the same game
   line?
3. ``check_flk_and_yacl_ranges_are_lower_bound_only`` -- are FLK and YACL exactly ``>=``
   what this build compiles against, with YACL's minimum tied to the vendored jar and its
   manifest row?
4. ``check_runtime_version_gate`` -- does the hardcoded in-game TaCZ gate accept the
   version ``fabric.mod.json`` declares?
5. ``check_dependency_availability`` -- does upstream actually PUBLISH something in that
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
        {"version_number": "1.14.2+kotlin.2.4.21"},
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

# Fabric Loader releases at or above this line's minimum (FabricMC/fabric-loader GitHub
# releases, verified 2026-10-16). 0.20.0 was published on 2026-10-15 and is exactly the build
# the old ">=0.19.3 <0.20.0" cap rejected.
PUBLISHED_LOADER = ["0.19.3", "0.19.4", "0.19.5", "0.20.0"]
FUTURE_LOADER = ["0.20.1", "0.21.0", "1.0.0"]
OLD_CAPPED_LOADER = ">=0.19.3 <0.20.0"

# Fabric Language Kotlin builds tagged for Minecraft 1.21.11 (api.modrinth.com, verified
# 2026-10-16). 1.14.2+kotlin.2.4.21 (2026-10-09) is the newest; the removed "<1.14.0" cap
# rejected 1.14.0, 1.14.1 and 1.14.2.
PUBLISHED_FLK = [
    "1.13.13+kotlin.2.4.10", "1.13.14+kotlin.2.4.20", "1.14.0+kotlin.2.4.20",
    "1.14.1+kotlin.2.4.20", "1.14.2+kotlin.2.4.21",
]
FUTURE_FLK = ["1.15.0+kotlin.2.5.0", "2.0.0+kotlin.3.0.0"]
OLD_CAPPED_FLK = ">=1.13.13 <1.14.0"

# YACL Fabric builds published for 1.21.11 (api.modrinth.com, verified 2026-10-16): only 3.8.1
# and 3.8.2 so far, so on this line the exact pin was preventive rather than already broken --
# it would reject the next 3.8.x for this same game line. 3.8.2+1.21.11-fabric is the jar this
# branch vendors; RESOURCE_IMPORT_MANIFEST.tsv records it with sha512 392db7d4...
PUBLISHED_YACL_1_21_11 = ["3.8.1+1.21.11-fabric", "3.8.2+1.21.11-fabric"]
FUTURE_YACL_1_21_11 = ["3.8.3+1.21.11-fabric", "3.9.0+1.21.11-fabric", "4.0.0+1.21.11-fabric"]
OLD_PINNED_YACL = "=3.8.2+1.21.11-fabric"



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


def test_flk_range_keeps_this_lines_minimum_but_drops_the_cap() -> None:
    """The FLK cap was this line's own bug, not a 26.3 import: 1.14.x is published for 1.21.11.

    The minimum must stay 1.13.13 -- it is what this branch compiles against and what the
    removed 26.3-style floor (``>=1.14.1``) would have excluded. But ``<1.14.0`` rejected FLK
    1.14.0, 1.14.1 and 1.14.2, all of which upstream tags for Minecraft 1.21.11: the same
    "Incompatible mods found" shape as Issue #33 on the 26.2 line.
    """
    props = _props()
    declared = _meta()["depends"]["fabric-language-kotlin"]
    assert declared == f">={props['flk_version'].split('+', 1)[0]}", declared
    assert "<" not in declared, declared

    for version in PUBLISHED_FLK + FUTURE_FLK:
        assert _range_matches(version, declared), f"{declared!r} rejects {version}"
    assert not _range_matches("1.13.12+kotlin.2.3.21", declared), "the minimum must still hold"

    # The removed cap rejected every 1.14.x build while admitting the rest.
    assert not _range_matches("1.14.2+kotlin.2.4.21", OLD_CAPPED_FLK)
    assert _range_matches("1.13.13+kotlin.2.4.10", OLD_CAPPED_FLK)


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
# Fabric Loader: lower bound only, no artificial cap
# ---------------------------------------------------------------------------


def test_fabric_loader_range_is_lower_bound_only() -> None:
    props, meta = _props(), _meta()
    declared = meta["depends"]["fabricloader"]
    assert declared == f">={props['loader_version']}", declared
    assert "<" not in declared, f"Fabric Loader range must not carry an upper cap: {declared!r}"
    assert not _expect_failure(consistency.check_fabric_loader_range_is_lower_bound_only, props, meta)


def test_fabric_loader_range_admits_published_and_later_builds() -> None:
    declared = _meta()["depends"]["fabricloader"]
    for version in PUBLISHED_LOADER + FUTURE_LOADER:
        assert _range_matches(version, declared), f"{declared!r} rejects {version}"
    assert not _range_matches("0.19.2", declared), "the minimum must still hold"
    # 0.20.0 (2026-10-15) is the release the old cap rejected; everything below it passed.
    assert not _range_matches("0.20.0", OLD_CAPPED_LOADER)
    assert _range_matches("0.19.5", OLD_CAPPED_LOADER)


def test_fabric_loader_policy_rejects_caps_drift_and_the_263_range() -> None:
    props = _props()
    for declared in (
        OLD_CAPPED_LOADER,
        ">=0.19.3 <1.0.0",
        ">=0.19.5 <0.20.0",  # the 26.3 line's range
        ">=0.19.5",  # 26.3's minimum; it would also switch the Mixin compatibility level
        "*",
    ):
        meta = _meta()
        meta["depends"]["fabricloader"] = declared
        errors = _expect_failure(
            consistency.check_fabric_loader_range_is_lower_bound_only, props, meta
        )
        assert errors, f"fabricloader = {declared!r} must be rejected"


# ---------------------------------------------------------------------------
# Fabric Language Kotlin and YACL: lower bound only, no cap and no exact pin
# ---------------------------------------------------------------------------


def test_flk_and_yacl_ranges_are_lower_bound_only() -> None:
    """The repo as it stands must pass, and YACL must stay tied to the vendored jar."""
    props, meta = _props(), _meta()
    declared = meta["depends"]["yet_another_config_lib_v3"]
    build_script = (ROOT / "build.gradle.kts").read_text(encoding="utf-8")

    assert declared == f">={consistency.YACL_BUILT_VERSION}", declared
    assert _range_matches(consistency.YACL_BUILT_VERSION, declared)
    for version in FUTURE_YACL_1_21_11:
        assert _range_matches(version, declared), f"{declared!r} rejects {version}"
    # 3.8.1 is published for 1.21.11, but the minimum must still hold at what we build against.
    assert PUBLISHED_YACL_1_21_11[0] == "3.8.1+1.21.11-fabric"
    assert not _range_matches(PUBLISHED_YACL_1_21_11[0], declared), "the minimum must still hold"
    # The removed pin admitted exactly one build.
    assert not _range_matches("3.8.3+1.21.11-fabric", OLD_PINNED_YACL)

    assert not _expect_failure(
        consistency.check_flk_and_yacl_ranges_are_lower_bound_only, props, meta, build_script
    )


def test_flk_and_yacl_policy_rejects_caps_pins_and_drift() -> None:
    props = _props()
    build_script = (ROOT / "build.gradle.kts").read_text(encoding="utf-8")
    flk_minimum = props["flk_version"].split("+", 1)[0]
    for depend_key, declared in (
        ("fabric-language-kotlin", OLD_CAPPED_FLK),
        ("fabric-language-kotlin", ">=1.14.1+kotlin.2.4.20 <2.0.0"),  # the 26.3 range
        ("fabric-language-kotlin", ">=1.13.13 <2.0.0"),
        ("fabric-language-kotlin", f"={flk_minimum}"),
        ("yet_another_config_lib_v3", OLD_PINNED_YACL),
        ("yet_another_config_lib_v3", ">=3.8.2"),
        ("yet_another_config_lib_v3", "=3.8.2+1.21.10-fabric"),
    ):
        meta = _meta()
        meta["depends"][depend_key] = declared
        errors = _expect_failure(
            consistency.check_flk_and_yacl_ranges_are_lower_bound_only, props, meta, build_script
        )
        assert errors, f"{depend_key} = {declared!r} must be rejected"


def test_yacl_minimum_is_tied_to_the_vendored_jar_and_manifest() -> None:
    """Swapping the YACL jar or its manifest row without moving the minimum must fail.

    This branch vendors ``libs/yacl-fabric.jar`` instead of resolving YACL from a Maven
    coordinate, so the coupling is to build.gradle.kts's jar path and the manifest row's
    ``upstream_version`` -- the same job 26.1.2 does with its Modrinth coordinate.
    """
    props, meta = _props(), _meta()
    build_script = (ROOT / "build.gradle.kts").read_text(encoding="utf-8")

    errors = _expect_failure(
        consistency.check_flk_and_yacl_ranges_are_lower_bound_only,
        props,
        meta,
        build_script.replace('"libs/yacl-fabric.jar"', '"libs/yacl-other.jar"'),
    )
    assert errors, "build.gradle.kts no longer resolving the vendored YACL jar must be rejected"
    assert any("no longer resolves YACL" in e for e in errors)

    manifest = ROOT / "RESOURCE_IMPORT_MANIFEST.tsv"
    original = manifest.read_text(encoding="utf-8")
    swapped = original.replace(
        "\t3.8.2+1.21.11-fabric\t", "\t3.8.3+1.21.11-fabric\t"
    )
    assert swapped != original, "manifest row for libs/yacl-fabric.jar must record its version"
    with _rewrite(manifest, original, swapped):
        errors = _expect_failure(
            consistency.check_flk_and_yacl_ranges_are_lower_bound_only, props, meta, build_script
        )
    assert errors, "a manifest row disagreeing with the declared YACL minimum must be rejected"
    assert any("RESOURCE_IMPORT_MANIFEST.tsv records YACL" in e for e in errors)


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


# ---------------------------------------------------------------------------
# The aggregate gate: check_metadata() must actually run the new policies
# ---------------------------------------------------------------------------


def test_check_metadata_enforces_every_lower_bound_only_policy() -> None:
    """Each removed shape must be rejected by the real entry point, not only by a helper.

    A helper nobody calls is not a gate -- which is exactly the failure mode these guards
    exist to prevent.
    """
    assert not _expect_failure(consistency.check_metadata)

    original = META_PATH.read_text(encoding="utf-8")
    for depend_key, declared in (
        ("fabricloader", OLD_CAPPED_LOADER),
        ("fabric-language-kotlin", OLD_CAPPED_FLK),
        ("yet_another_config_lib_v3", OLD_PINNED_YACL),
    ):
        broken = json.loads(original)
        broken["depends"][depend_key] = declared
        with _rewrite(META_PATH, original, json.dumps(broken, indent=2)):
            errors = _expect_failure(consistency.check_metadata)
        assert errors, f"check_metadata must reject {depend_key} = {declared!r}"


if __name__ == "__main__":
    raise SystemExit(__import__("pytest").main([__file__, "-q"]))

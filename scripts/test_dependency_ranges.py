#!/usr/bin/env python3
"""Regression tests for the Fabric dependency ranges shipped by the 26.1.2 line.

Fabric API -- synced from the 26.3 line's 2026-10-15 fix. There, ``fabric-api: ">=0.160.7+26.3 <0.162.0"``
made Fabric Loader reject the user's installed ``0.162.0+26.3`` ("Incompatible mods
found"), even though Fabric API releases for one Minecraft line stay compatible and the
``minecraft`` dependency is pinned separately. This line carried the same shape of
artificial cap: ``">=0.155.2+26.1.2 <0.157.0"``.

As of 2026-10-15 the newest Fabric API published for 26.1.2 is ``0.155.3+26.1.2``
(FabricMC/fabric GitHub releases), which the old cap still admitted -- so on this line the
fix is preventive: the first ``0.157.x+26.1.2`` build would otherwise have been rejected.

Fabric Language Kotlin and YACL -- 2026-10-16. On the 26.2 line (Issue #33) Fabric Loader
also rejected FLK ``1.14.2+kotlin.2.4.21`` because of ``">=1.13.13 <1.14.0"`` and YACL
``3.9.7+26.2-fabric`` because of the exact pin ``"=3.9.6+26.2-fabric"``. This line declared the
same shapes, and here the FLK cap already bit: FLK 1.14.0, 1.14.1 and 1.14.2 are published
(FabricMC/fabric-language-kotlin GitHub releases) and the old range rejected all three.

Compiling, the static mixin audit and jar packaging never evaluate dependency ranges --
only Fabric Loader does, at launch -- so these tests pin the policy down offline:

1. ``check_fabric_api_range_is_lower_bound_only`` -- the shipped Fabric API range is exactly
   ``>=<fabric_version>`` (this line's own minimum, no upper cap); re-adding a cap or
   drifting the minimum is rejected.
2. ``check_flk_and_yacl_ranges_are_lower_bound_only`` -- FLK is exactly ``>=`` + the release
   part of ``flk_version`` and YACL exactly ``>=3.9.6+26.1-fabric`` (the artifact
   build.gradle.kts resolves); caps, exact pins, another line's minimum or a swapped YACL
   artifact are rejected.
3. ``check_declared_ranges_include_built_versions`` -- the versions we build against satisfy
   the ranges we ship.
4. ``check_metadata`` really runs these guards, so ``check_release_consistency.py`` fails.

Standard library only, like this branch's other script tests. Run either way::

    python3 scripts/test_dependency_ranges.py
    python3 -m pytest scripts/test_dependency_ranges.py
"""

from __future__ import annotations

import importlib.util
import json
import shutil
import tempfile
import traceback
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
METADATA = ROOT / "src/main/resources/fabric.mod.json"
PROPERTIES = ROOT / "gradle.properties"
BUILD_SCRIPT = ROOT / "build.gradle.kts"


def _load(name: str, filename: str):
    spec = importlib.util.spec_from_file_location(name, ROOT / "scripts" / filename)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


consistency = _load("check_release_consistency", "check_release_consistency.py")
satisfies = consistency._satisfies

# Fabric API builds published for Minecraft 26.1.2 at or above this line's minimum, per the
# FabricMC/fabric GitHub releases on 2026-10-15.
PUBLISHED_26_1_2_FABRIC_API = ["0.155.2+26.1.2", "0.155.3+26.1.2"]
# Hypothetical later builds for the same game line: exactly what an upper cap would reject.
FUTURE_26_1_2_FABRIC_API = ["0.157.0+26.1.2", "0.160.0+26.1.2", "1.0.0+26.1.2"]
OLD_CAPPED_RANGE = ">=0.155.2+26.1.2 <0.157.0"

# Fabric Language Kotlin releases at or above this line's minimum (FabricMC/fabric-language-kotlin
# GitHub releases, 2026-10-16). 1.14.2+kotlin.2.4.21 is the build Issue #33's user had installed.
PUBLISHED_FLK = [
    "1.13.13+kotlin.2.4.10", "1.13.14+kotlin.2.4.20", "1.14.0+kotlin.2.4.20",
    "1.14.1+kotlin.2.4.20", "1.14.2+kotlin.2.4.21",
]
FUTURE_FLK = ["1.15.0+kotlin.2.5.0", "2.0.0+kotlin.3.0.0"]
OLD_CAPPED_FLK = ">=1.13.13 <1.14.0"

# YACL builds for the 26.1 game line (which covers 26.1.2). 3.9.6 is what this build compiles
# against; YACL's 3.9.7 changelog lists a 26.1 Fabric target (isXander/YetAnotherConfigLib).
PUBLISHED_YACL_26_1 = ["3.9.6+26.1-fabric", "3.9.7+26.1-fabric"]
FUTURE_YACL_26_1 = ["3.9.8+26.1-fabric", "3.10.0+26.1-fabric", "4.0.0+26.1-fabric"]
OLD_PINNED_YACL = "=3.9.6+26.1-fabric"


def _range_matches(version: str, declared: str) -> bool:
    return all(satisfies(version, part) for part in declared.split())


def _props() -> dict[str, str]:
    return consistency.read_properties(PROPERTIES)


def _meta() -> dict:
    return json.loads(METADATA.read_text(encoding="utf-8"))


def _build_script() -> str:
    return BUILD_SCRIPT.read_text(encoding="utf-8")


def _rejects(check, *args) -> bool:
    try:
        check(*args)
    except SystemExit:
        return True
    return False


def test_range_operators() -> None:
    assert _range_matches("0.155.2+26.1.2", ">=0.155.2+26.1.2")
    assert _range_matches("0.157.0+26.1.2", ">=0.155.2+26.1.2")
    # The lower bound is still enforced.
    assert not _range_matches("0.155.0+26.1.2", ">=0.155.2+26.1.2")
    # The old artificial cap rejected the first 0.157.x build for the same Minecraft line,
    # while still admitting everything published so far (hence: a preventive fix here).
    assert not _range_matches("0.157.0+26.1.2", OLD_CAPPED_RANGE)
    assert _range_matches("0.155.3+26.1.2", OLD_CAPPED_RANGE)
    assert _range_matches("1.1.8+fabric.26.1.2.R2", "=1.1.8+fabric.26.1.2.R2")
    assert _range_matches("9.9.9", "*")


def test_fabric_api_range_is_lower_bound_only() -> None:
    props, meta = _props(), _meta()
    declared = meta["depends"]["fabric-api"]
    assert declared == f">={props['fabric_version']}", declared
    assert "<" not in declared, f"Fabric API range must not carry an upper cap: {declared!r}"
    consistency.check_fabric_api_range_is_lower_bound_only(props, meta)


def test_fabric_api_range_admits_published_and_later_builds() -> None:
    declared = _meta()["depends"]["fabric-api"]
    for version in PUBLISHED_26_1_2_FABRIC_API + FUTURE_26_1_2_FABRIC_API:
        assert _range_matches(version, declared), f"{declared!r} rejects {version}"


def test_fabric_api_policy_rejects_an_upper_cap() -> None:
    props, meta = _props(), _meta()
    meta["depends"]["fabric-api"] = OLD_CAPPED_RANGE
    assert _rejects(consistency.check_fabric_api_range_is_lower_bound_only, props, meta), (
        "the old '<0.157.0' cap must be rejected by release consistency"
    )


def test_fabric_api_policy_rejects_a_drifted_minimum() -> None:
    """The minimum is this line's own fabric_version -- not 26.3's, and not a guess."""
    props = _props()
    for drifted in (">=0.160.7+26.3", ">=0.150.0+26.1.2", ">=0.155.3+26.1.2", "*"):
        meta = _meta()
        meta["depends"]["fabric-api"] = drifted
        assert _rejects(consistency.check_fabric_api_range_is_lower_bound_only, props, meta), drifted


def test_flk_and_yacl_ranges_are_lower_bound_only() -> None:
    props, meta = _props(), _meta()
    expected = {
        "fabric-language-kotlin": f">={props['flk_version'].split('+', 1)[0]}",
        "yet_another_config_lib_v3": f">={consistency.YACL_BUILT_VERSION}",
    }
    for depend_key, wanted in expected.items():
        declared = meta["depends"][depend_key]
        assert declared == wanted, f"{depend_key}: {declared!r} != {wanted!r}"
        assert "<" not in declared, f"{depend_key} must not carry an upper cap: {declared!r}"
    consistency.check_flk_and_yacl_ranges_are_lower_bound_only(props, meta, _build_script())


def test_flk_range_admits_published_and_later_builds() -> None:
    declared = _meta()["depends"]["fabric-language-kotlin"]
    for version in PUBLISHED_FLK + FUTURE_FLK:
        assert _range_matches(version, declared), f"{declared!r} rejects {version}"
    assert not _range_matches("1.13.12+kotlin.2.4.0", declared), "the minimum must still hold"


def test_yacl_range_admits_published_and_later_builds() -> None:
    declared = _meta()["depends"]["yet_another_config_lib_v3"]
    for version in PUBLISHED_YACL_26_1 + FUTURE_YACL_26_1:
        assert _range_matches(version, declared), f"{declared!r} rejects {version}"
    assert not _range_matches("3.9.5+26.1-fabric", declared), "the minimum must still hold"


def test_old_flk_cap_and_yacl_pin_reproduce_issue_33() -> None:
    """The removed ranges reject exactly the kind of builds Issue #33 reported on 26.2."""
    assert not _range_matches("1.14.2+kotlin.2.4.21", OLD_CAPPED_FLK)
    assert not _range_matches("3.9.7+26.1-fabric", OLD_PINNED_YACL)
    # ...while still admitting what this line was built against, so no build step noticed.
    assert _range_matches("1.13.13+kotlin.2.4.10", OLD_CAPPED_FLK)
    assert _range_matches("3.9.6+26.1-fabric", OLD_PINNED_YACL)


def test_flk_yacl_policy_rejects_caps_pins_and_drift() -> None:
    props, script = _props(), _build_script()
    cases = {
        "fabric-language-kotlin": (
            OLD_CAPPED_FLK,
            ">=1.13.13 <2.0.0",  # the 26.3 line's shape: still a cap
            ">=1.14.1+kotlin.2.4.20",  # 26.3's minimum; CROSS_BRANCH_SYNC 0.3: don't copy it
            ">=1.13.12",
            "*",
        ),
        "yet_another_config_lib_v3": (
            OLD_PINNED_YACL,
            ">=3.9.6+26.1-fabric <3.10.0",
            ">=3.9.6+26.2-fabric",  # another game line's artifact name
            ">=3.9.7+26.3-fabric",
            "*",
        ),
    }
    for depend_key, ranges in cases.items():
        for declared in ranges:
            meta = _meta()
            meta["depends"][depend_key] = declared
            assert _rejects(
                consistency.check_flk_and_yacl_ranges_are_lower_bound_only, props, meta, script
            ), f"{depend_key} = {declared!r} must be rejected"


def test_flk_minimum_follows_the_built_flk_version() -> None:
    """Compiling against a newer FLK must raise the shipped minimum with it."""
    props, meta, script = _props(), _meta(), _build_script()
    props["flk_version"] = "1.14.2+kotlin.2.4.21"
    assert _rejects(consistency.check_flk_and_yacl_ranges_are_lower_bound_only, props, meta, script)
    meta["depends"]["fabric-language-kotlin"] = ">=1.14.2"
    consistency.check_flk_and_yacl_ranges_are_lower_bound_only(props, meta, script)


def test_yacl_minimum_tracks_the_build_coordinate() -> None:
    """Swapping the YACL artifact in build.gradle.kts without updating the minimum must fail."""
    props, meta, script = _props(), _meta(), _build_script()
    assert consistency.YACL_BUILD_COORDINATE in script
    swapped = script.replace(consistency.YACL_BUILD_COORDINATE, "maven.modrinth:1eAoo2KR:newVersionId")
    assert _rejects(consistency.check_flk_and_yacl_ranges_are_lower_bound_only, props, meta, swapped)


def test_gradle_properties_and_fabric_mod_json_agree() -> None:
    props, meta = _props(), _meta()
    consistency.check_declared_ranges_include_built_versions(props, meta)
    for prop_key, depend_key in (
        ("flk_version", "fabric-language-kotlin"),
        ("fabric_version", "fabric-api"),
        ("loader_version", "fabricloader"),
    ):
        assert _range_matches(props[prop_key], meta["depends"][depend_key]), (
            f"{depend_key} range {meta['depends'][depend_key]!r} excludes built {props[prop_key]!r}"
        )


def test_built_version_check_rejects_an_excluding_range() -> None:
    props, meta = _props(), _meta()
    meta["depends"]["fabricloader"] = ">=0.20.0"
    assert _rejects(consistency.check_declared_ranges_include_built_versions, props, meta)


def _check_metadata_rejects(depends: dict[str, str]) -> bool:
    """Run the real ``check_metadata()`` on a scratch copy with ``depends`` overridden.

    Returns True when the check rejected it. The repository files are never modified.
    """
    original_root = consistency.ROOT
    with tempfile.TemporaryDirectory() as tmp:
        scratch = Path(tmp)
        shutil.copy2(PROPERTIES, scratch / "gradle.properties")
        shutil.copy2(BUILD_SCRIPT, scratch / "build.gradle.kts")
        meta = _meta()
        meta["depends"].update(depends)
        target = scratch / "src/main/resources/fabric.mod.json"
        target.parent.mkdir(parents=True)
        target.write_text(json.dumps(meta, indent=2), encoding="utf-8")
        consistency.ROOT = scratch
        try:
            consistency.check_metadata()
        except SystemExit:
            return True
        finally:
            consistency.ROOT = original_root
    return False


def test_check_metadata_enforces_the_policy() -> None:
    consistency.check_metadata()  # the repository as committed must pass
    assert not _check_metadata_rejects({})
    assert not _check_metadata_rejects({"fabric-api": f">={_props()['fabric_version']}"})
    for depends in (
        {"fabric-api": OLD_CAPPED_RANGE},
        {"fabric-language-kotlin": OLD_CAPPED_FLK},
        {"yet_another_config_lib_v3": OLD_PINNED_YACL},
    ):
        assert _check_metadata_rejects(depends), f"check_metadata() must reject {depends}"


def main() -> int:
    tests = [
        (name, fn) for name, fn in sorted(globals().items())
        if name.startswith("test_") and callable(fn)
    ]
    failures = 0
    for name, fn in tests:
        try:
            fn()
        except (Exception, SystemExit):  # report every failure, then exit non-zero
            failures += 1
            print(f"FAIL {name}")
            traceback.print_exc()
        else:
            print(f"PASS {name}")
    print(f"DEPENDENCY RANGE TESTS: {len(tests) - failures} passed, {failures} failed")
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())

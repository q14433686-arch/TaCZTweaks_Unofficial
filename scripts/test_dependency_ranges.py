#!/usr/bin/env python3
"""Regression tests for the Fabric dependency ranges shipped by the 26.1.2 line.

Synced from the 26.3 line's 2026-10-15 fix. There, ``fabric-api: ">=0.160.7+26.3 <0.162.0"``
made Fabric Loader reject the user's installed ``0.162.0+26.3`` ("Incompatible mods
found"), even though Fabric API releases for one Minecraft line stay compatible and the
``minecraft`` dependency is pinned separately. This line carried the same shape of
artificial cap: ``">=0.155.2+26.1.2 <0.157.0"``.

As of 2026-10-15 the newest Fabric API published for 26.1.2 is ``0.155.3+26.1.2``
(FabricMC/fabric GitHub releases), which the old cap still admitted -- so on this line the
fix is preventive: the first ``0.157.x+26.1.2`` build would otherwise have been rejected.

Compiling, the static mixin audit and jar packaging never evaluate dependency ranges --
only Fabric Loader does, at launch -- so these tests pin the policy down offline:

1. ``check_fabric_api_range_is_lower_bound_only`` -- the shipped Fabric API range is exactly
   ``>=<fabric_version>`` (this line's own minimum, no upper cap); re-adding a cap or
   drifting the minimum is rejected.
2. ``check_declared_ranges_include_built_versions`` -- the versions we build against satisfy
   the ranges we ship.
3. ``check_metadata`` really runs both guards, so ``check_release_consistency.py`` fails.

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


def _range_matches(version: str, declared: str) -> bool:
    return all(satisfies(version, part) for part in declared.split())


def _props() -> dict[str, str]:
    return consistency.read_properties(PROPERTIES)


def _meta() -> dict:
    return json.loads(METADATA.read_text(encoding="utf-8"))


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


def _check_metadata_rejects(fabric_api_range: str) -> bool:
    """Run the real ``check_metadata()`` against a scratch copy using ``fabric_api_range``.

    Returns True when the check rejected it. The repository files are never modified.
    """
    original_root = consistency.ROOT
    with tempfile.TemporaryDirectory() as tmp:
        scratch = Path(tmp)
        shutil.copy2(PROPERTIES, scratch / "gradle.properties")
        meta = _meta()
        meta["depends"]["fabric-api"] = fabric_api_range
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
    assert not _check_metadata_rejects(f">={_props()['fabric_version']}")
    assert _check_metadata_rejects(OLD_CAPPED_RANGE), "check_metadata() must reject the old cap"


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

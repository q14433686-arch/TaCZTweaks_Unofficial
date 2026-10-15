"""Regression tests for Fabric 26.2 launch-time dependency and TaCZ gate failures.

The 26.3 line exposed two gaps: a declared dependency range can exclude installed
builds even when the source compiles, and an in-code TaCZ version gate can reject
its own declared dependency. Test both positive cases and deliberately broken inputs.
Run: python3 -m pytest scripts/test_dependency_ranges.py -q
"""

from __future__ import annotations

import json
import sys
import urllib.error
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

import check_dependency_availability as availability  # noqa: E402
import check_release_consistency as consistency  # noqa: E402


def metadata() -> dict:
    return json.loads((ROOT / "src/main/resources/fabric.mod.json").read_text(encoding="utf-8"))


def matches(version: str, predicates: str) -> bool:
    return all(consistency._satisfies(version, part) for part in predicates.split())


# Representative published 26.2 builds: the pinned dependencies and FLK 1.13.13,
# which the 26.3 branch's CROSS_BRANCH_SYNC.md checked against Modrinth.
PUBLISHED_26_2 = {
    "fabric-api": [{"version_number": "0.155.2+26.2"}],
    "fabric-language-kotlin": [{"version_number": "1.13.13+kotlin.2.4.10"}],
    "yacl": [{"version_number": "3.9.6+26.2-fabric"}],
}


def test_shipped_ranges_include_built_versions() -> None:
    props = consistency.read_properties(ROOT / "gradle.properties")
    meta = metadata()
    consistency.check_declared_ranges_include_built_versions(props, meta)
    assert props["minecraft_version"] == "26.2"
    assert meta["depends"]["minecraft"] == "=26.2"
    assert matches("1.13.13+kotlin.2.4.10", meta["depends"]["fabric-language-kotlin"])
    # Do not import 26.3's FLK minimum or TaCZ revision into the 26.2 line.
    assert not matches("1.14.1+kotlin.2.4.20", meta["depends"]["fabric-language-kotlin"])
    assert meta["depends"]["tacz"] == "=1.1.8+fabric.26.2.R2"


def test_build_range_drift_fails() -> None:
    props = consistency.read_properties(ROOT / "gradle.properties")
    meta = metadata()
    meta["depends"]["fabric-language-kotlin"] = ">=99.0.0"
    with pytest.raises(SystemExit, match="excludes the version"):
        consistency.check_declared_ranges_include_built_versions(props, meta)


def test_fabric_api_allows_newer_26_2_versions_without_a_cap() -> None:
    props = consistency.read_properties(ROOT / "gradle.properties")
    meta = metadata()
    declared = meta["depends"]["fabric-api"]
    consistency.check_fabric_api_range_is_lower_bound_only(props, meta)
    assert declared == ">=0.155.2+26.2"
    # Synthetic boundary that the previous artificial cap would have rejected;
    # this test does NOT claim a 0.157.0+26.2 artifact is published.
    assert matches("0.157.0+26.2", declared)
    assert not matches("0.157.0+26.2", ">=0.155.2+26.2 <0.157.0")


def test_fabric_api_upper_cap_fails_consistency() -> None:
    props = consistency.read_properties(ROOT / "gradle.properties")
    meta = metadata()
    meta["depends"]["fabric-api"] += " <0.157.0"
    with pytest.raises(SystemExit, match="no upper cap"):
        consistency.check_fabric_api_range_is_lower_bound_only(props, meta)


def test_runtime_gate_accepts_26_2_r2_dependency() -> None:
    consistency.check_runtime_version_gate(metadata())


def test_runtime_gate_rejects_wrong_family(tmp_path: Path, monkeypatch) -> None:
    source = (ROOT / "src/main/java/me/muksc/tacztweaks/TaCZTweaks.java").read_text(encoding="utf-8")
    broken = source.replace(
        'SUPPORTED_TACZ_VERSION_PREFIX = "1.1.8+fabric.26.2.R"',
        'SUPPORTED_TACZ_VERSION_PREFIX = "1.1.8+fabric.26.3.R"',
    )
    assert source != broken
    java_file = tmp_path / "src/main/java/me/muksc/tacztweaks/TaCZTweaks.java"
    java_file.parent.mkdir(parents=True)
    java_file.write_text(broken, encoding="utf-8")
    monkeypatch.setattr(consistency, "ROOT", tmp_path)
    with pytest.raises(SystemExit, match="runtime gate accepts only"):
        consistency.check_runtime_version_gate(metadata())


def test_runtime_gate_rejects_floor_above_r2(tmp_path: Path, monkeypatch) -> None:
    source = (ROOT / "src/main/java/me/muksc/tacztweaks/TaCZTweaks.java").read_text(encoding="utf-8")
    broken = source.replace(
        "MIN_SUPPORTED_TACZ_REVISION = BigInteger.valueOf(2)",
        "MIN_SUPPORTED_TACZ_REVISION = BigInteger.valueOf(3)",
    )
    assert source != broken
    java_file = tmp_path / "src/main/java/me/muksc/tacztweaks/TaCZTweaks.java"
    java_file.parent.mkdir(parents=True)
    java_file.write_text(broken, encoding="utf-8")
    monkeypatch.setattr(consistency, "ROOT", tmp_path)
    with pytest.raises(SystemExit, match="revision >= R3"):
        consistency.check_runtime_version_gate(metadata())


def test_runtime_gate_rejects_mismatched_advertised_version(tmp_path: Path, monkeypatch) -> None:
    source = (ROOT / "src/main/java/me/muksc/tacztweaks/TaCZTweaks.java").read_text(encoding="utf-8")
    broken = source.replace(
        'SUPPORTED_TACZ_VERSION = "1.1.8+fabric.26.2.R2"',
        'SUPPORTED_TACZ_VERSION = "1.1.8+fabric.26.2.R3"',
    )
    assert source != broken
    java_file = tmp_path / "src/main/java/me/muksc/tacztweaks/TaCZTweaks.java"
    java_file.parent.mkdir(parents=True)
    java_file.write_text(broken, encoding="utf-8")
    monkeypatch.setattr(consistency, "ROOT", tmp_path)
    with pytest.raises(SystemExit, match="disagrees with the declared"):
        consistency.check_runtime_version_gate(metadata())


def test_published_26_2_dependencies_match_declared_ranges(monkeypatch) -> None:
    monkeypatch.setattr(
        availability, "fetch_versions", lambda slug, game_version: PUBLISHED_26_2[slug]
    )
    assert availability.main([]) == 0


def test_availability_rejects_nonexistent_range(tmp_path: Path, monkeypatch, capsys) -> None:
    (tmp_path / "gradle.properties").write_bytes((ROOT / "gradle.properties").read_bytes())
    meta_path = tmp_path / "src/main/resources/fabric.mod.json"
    meta_path.parent.mkdir(parents=True)
    broken = metadata()
    broken["depends"]["fabric-language-kotlin"] = ">=99.0.0"
    meta_path.write_text(json.dumps(broken), encoding="utf-8")
    monkeypatch.setattr(availability, "ROOT", tmp_path)
    monkeypatch.setattr(
        availability, "fetch_versions", lambda slug, game_version: PUBLISHED_26_2[slug]
    )
    assert availability.main([]) == 1
    assert "matches NONE of the builds" in capsys.readouterr().out


def test_network_skip_is_not_reported_as_verified(monkeypatch, capsys) -> None:
    def unavailable(slug: str, game_version: str) -> list[dict]:
        raise urllib.error.URLError("offline")

    monkeypatch.setattr(availability, "fetch_versions", unavailable)
    assert availability.main([]) == 0
    output = capsys.readouterr().out
    assert "SKIP (network unavailable)" in output
    assert "DEPENDENCY AVAILABILITY: OK" not in output

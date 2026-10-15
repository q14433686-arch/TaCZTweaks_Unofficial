#!/usr/bin/env python3
"""Regression tests for 26.3 release-tag normalization."""

from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

from validate_release_tag import expected_release_tag, read_mod_version, validate_release_tag


def test_hotfix_one_matches_the_published_tag_convention() -> None:
    version = "2.14.2+fabric.26.3.Beta-1-hotfix1"
    assert expected_release_tag(version) == "26.3_BETA_1_HOTFIX-1"
    assert validate_release_tag("26.3_BETA_1_HOTFIX-1", version) == "26.3_BETA_1_HOTFIX-1"


def test_current_version_maps_to_hotfix_two_tag() -> None:
    assert expected_release_tag(read_mod_version()) == "26.3_BETA_1_HOTFIX-2"


def test_hotfix_sequence_keeps_the_hyphen_for_multiple_digits() -> None:
    assert (
        expected_release_tag("2.14.2+fabric.26.3.Beta-1-hotfix12")
        == "26.3_BETA_1_HOTFIX-12"
    )


def test_non_263_version_is_rejected() -> None:
    try:
        expected_release_tag("2.14.2+fabric.26.2.Beta-1-hotfix1")
    except ValueError:
        pass
    else:
        raise AssertionError("release tags must only be generated for the 26.3 line")


def test_incorrect_tag_is_rejected() -> None:
    try:
        validate_release_tag(
            "26.3_BETA_1_HOTFIX2", "2.14.2+fabric.26.3.Beta-1-hotfix2"
        )
    except ValueError:
        pass
    else:
        raise AssertionError("the unhyphenated HOTFIX2 spelling must not be accepted")

#!/usr/bin/env python3
"""Validate that a pushed 26.3 release tag matches gradle.properties."""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VERSION_PATTERN = re.compile(
    r"\d+\.\d+\.\d+\+fabric\.26\.3\.(?P<suffix>[A-Za-z0-9]+(?:[.-][A-Za-z0-9]+)*)"
)


def expected_release_tag(version: str) -> str:
    """Return the repository's tag spelling for a Fabric 26.3 mod version.

    Hotfix tags use a hyphen before their sequence number (``HOTFIX-1``), unlike
    the normalized separators in the rest of the suffix (``BETA_1``).
    """
    match = VERSION_PATTERN.fullmatch(version)
    if match is None:
        raise ValueError(f"not a Fabric 26.3 release version: {version!r}")

    suffix = re.sub(r"[.-]", "_", match.group("suffix").upper())
    suffix = re.sub(r"_HOTFIX([0-9]+)$", r"_HOTFIX-\1", suffix)
    return f"26.3_{suffix}"


def read_mod_version() -> str:
    properties = ROOT / "gradle.properties"
    matches = [
        line.partition("=")[2].strip()
        for line in properties.read_text(encoding="utf-8").splitlines()
        if line.strip().startswith("mod_version=")
    ]
    if len(matches) != 1 or not matches[0]:
        raise ValueError("gradle.properties must define exactly one non-empty mod_version")
    return matches[0]


def validate_release_tag(tag: str, version: str | None = None) -> str:
    version = version or read_mod_version()
    expected = expected_release_tag(version)
    if tag != expected:
        raise ValueError(
            f"tag {tag!r} does not match mod_version {version!r}; expected {expected!r}"
        )
    return expected


def main(argv: list[str]) -> int:
    if len(argv) != 1:
        print("usage: python3 scripts/validate_release_tag.py <tag>", file=sys.stderr)
        return 2
    try:
        version = read_mod_version()
        validate_release_tag(argv[0], version)
    except ValueError as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1
    print(f"RELEASE TAG: OK ({argv[0]} matches {version})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))

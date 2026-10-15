#!/usr/bin/env python3
"""Check that a GitHub release tag names this line's current mod_version (26.1.2 line).

Used by ``.github/workflows/release.yml`` before anything is built or published.

The tag must start with ``26.1.2`` followed by ``_`` or ``-``, and the rest must spell the
same release suffix as ``mod_version`` in gradle.properties. Suffixes are compared token
by token (runs of letters / runs of digits, case-insensitive), so the separators people
actually type do not matter, while the release identity still does::

    mod_version  2.14.2+fabric.26.1.2.Beta-1-hotfix2
    accepted     26.1.2_BETA_1_HOTFIX2   26.1.2_BETA_1_HOTFIX-2   26.1.2-beta-1-hotfix_2
    rejected     26.1.2_BETA_1_HOTFIX    (the already published, older release)
                 26.3_BETA_1_HOTFIX2     (another release line)

Why tolerant: the 26.3 workflow compared one exact spelling, and on 2026-10-15 it rejected
the maintainer's ``26.3_BETA_1_HOTFIX-1`` for ``...Beta-1-hotfix1``. This line's existing
tags already mix separators (``26.1.2-BETA_1``, ``26.1.2_BETA_1_HOTFIX``).

Usage::

    python3 scripts/check_release_tag.py 26.1.2_BETA_1_HOTFIX2
    python3 scripts/check_release_tag.py --suggest      # print the conventional tag

Standard library only.
"""

from __future__ import annotations

import argparse
import os
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LINE = "26.1.2"
LINE_MARKER = f"+fabric.{LINE}."


def read_mod_version(properties: Path = ROOT / "gradle.properties") -> str:
    for line in properties.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line.startswith("mod_version="):
            return line.split("=", 1)[1].strip()
    raise ValueError(f"{properties} has no mod_version")


def tokens(text: str) -> list[str]:
    """Release identity of a suffix: letter runs and digit runs, upper-cased."""
    return re.findall(r"[A-Z]+|[0-9]+", text.upper())


def release_suffix(mod_version: str) -> str:
    """'2.14.2+fabric.26.1.2.Beta-1-hotfix2' -> 'Beta-1-hotfix2'."""
    if LINE_MARKER not in mod_version:
        raise ValueError(
            f"mod_version {mod_version!r} does not identify the Fabric {LINE} line "
            f"(expected it to contain {LINE_MARKER!r})"
        )
    suffix = mod_version.split(LINE_MARKER, 1)[1]
    if not tokens(suffix):
        raise ValueError(f"mod_version {mod_version!r} has no release suffix after {LINE_MARKER!r}")
    return suffix


def suggested_tag(mod_version: str) -> str:
    """Conventional spelling, same rule as the 26.3 workflow: upper-case, '.'/'-' -> '_'."""
    return f"{LINE}_" + re.sub(r"[.-]", "_", release_suffix(mod_version).upper())


def tag_matches(tag: str, mod_version: str) -> bool:
    if not (tag.startswith(f"{LINE}_") or tag.startswith(f"{LINE}-")):
        return False
    rest = tag[len(LINE) + 1:]
    return bool(tokens(rest)) and tokens(rest) == tokens(release_suffix(mod_version))


def _error(message: str) -> None:
    prefix = "::error::" if os.environ.get("GITHUB_ACTIONS") == "true" else "ERROR: "
    print(f"{prefix}{message}")


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Validate a 26.1.2 release tag against mod_version.")
    parser.add_argument("tag", nargs="?", help="the pushed tag, e.g. 26.1.2_BETA_1_HOTFIX2")
    parser.add_argument("--suggest", action="store_true", help="print the conventional tag and exit")
    parser.add_argument("--properties", type=Path, default=ROOT / "gradle.properties")
    args = parser.parse_args(argv)

    try:
        mod_version = read_mod_version(args.properties)
        expected = suggested_tag(mod_version)
    except ValueError as error:
        _error(str(error))
        return 1

    if args.suggest:
        print(expected)
        return 0
    if not args.tag:
        parser.error("a tag is required unless --suggest is given")

    if not tag_matches(args.tag, mod_version):
        _error(
            f"Tag {args.tag!r} does not name the current mod_version {mod_version!r}. "
            f"Expected {expected!r} ('_' / '-' separators and letter case may differ). "
            f"Bump mod_version first if this is a new release."
        )
        return 1
    print(f"OK: tag {args.tag} matches mod_version {mod_version}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))

#!/usr/bin/env python3
"""Fail a dedicated-server smoke test from its log, regardless of Gradle's exit code."""

from __future__ import annotations

import argparse
import re
from pathlib import Path

FORBIDDEN = {
    "MixinApplyError": "a mixin failed to apply",
    "InvalidInjectionException": "a mixin injection target failed",
    "Mixin apply for mod tacztweaks failed": "a TaCZ Tweaks mixin failed",
    "Failed to start the minecraft server": "Minecraft reported startup failure",
}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("log", type=Path, help="dedicated-server latest.log or captured stdout")
    args = parser.parse_args()

    if not args.log.is_file():
        print(f"SERVER LOG CHECK: missing log: {args.log}")
        return 1
    text = args.log.read_text(encoding="utf-8", errors="replace")
    text = re.sub(r"\x1b\[[0-9;]*m", "", text)

    errors = [reason for marker, reason in FORBIDDEN.items() if marker in text]
    if not re.search(r"\bDone \([^\r\n]+\)!", text):
        errors.append("dedicated server never reached the Done startup marker")

    if errors:
        print(f"SERVER LOG CHECK: {len(errors)} error(s)")
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    print("SERVER LOG CHECK: PASS (Done marker present; no fatal mixin/startup marker)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""Verify every hard dependency is actually published for our Minecraft version.

Why this exists
---------------
On 2026-09-22 the 26.3 port shipped ``fabric-language-kotlin: ">=1.13.13 <1.14.0"``
while the only FLK build tagged for Minecraft 26.3 was ``1.14.1``. The intersection was
empty, so the mod could not load for *any* user::

    Mod 'TaCZ Tweaks (Refabricated)' requires version 1.13.13 (inclusive) to
    1.14.0 (exclusive) of 'Fabric Language Kotlin', but 1.14.1 is installed!

Nothing caught it. Compiling, the static mixin audit and jar packaging all succeed no
matter what the dependency range says, because only the Fabric loader enforces it, at
runtime. ``check_release_consistency.py`` compares the range against the version we build
with, but in that incident *both* were stale in the same direction, so they agreed with
each other and disagreed with reality.

The only way to catch that class of bug without launching the game is to ask upstream what
it actually publishes. That is what this script does.

It is deliberately a *separate* script: it needs network access, whereas
``check_release_consistency.py`` must stay offline-clean so it can run in restricted
sandboxes and as a pre-commit gate.

Usage::

    python3 scripts/check_dependency_availability.py            # fail on problems
    python3 scripts/check_dependency_availability.py --warn-only
"""

from __future__ import annotations

import argparse
import json
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODRINTH_API = "https://api.modrinth.com/v2"
USER_AGENT = "TaCZTweaks_Unofficial/dependency-availability-check (+https://github.com/q14433686-arch/TaCZTweaks_Unofficial)"

# fabric.mod.json depends key -> Modrinth project slug.
# Only dependencies published on Modrinth are checkable here; `minecraft`, `java` and
# `fabricloader` are not third-party mods, and `tacz` is a GitHub-release dependency.
CHECKABLE = {
    "fabric-language-kotlin": "fabric-language-kotlin",
    "fabric-api": "fabric-api",
    "yet_another_config_lib_v3": "yacl",
}


def read_properties(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            key, value = line.split("=", 1)
            values[key.strip()] = value.strip()
    return values


def fetch_versions(slug: str, game_version: str) -> list[dict]:
    query = urllib.parse.urlencode(
        {"game_versions": json.dumps([game_version]), "loaders": json.dumps(["fabric"])}
    )
    request = urllib.request.Request(
        f"{MODRINTH_API}/project/{slug}/version?{query}", headers={"User-Agent": USER_AGENT}
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.loads(response.read().decode("utf-8"))


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--warn-only",
        action="store_true",
        help="report problems without failing (use when the network may be unavailable)",
    )
    args = parser.parse_args(argv)

    props = read_properties(ROOT / "gradle.properties")
    meta = json.loads((ROOT / "src/main/resources/fabric.mod.json").read_text(encoding="utf-8"))
    game_version = props["minecraft_version"]
    depends = meta.get("depends", {})

    # Imported lazily so this stays usable even if the sibling script changes shape.
    sys.path.insert(0, str(ROOT / "scripts"))
    from check_release_consistency import _satisfies  # noqa: PLC0415

    problems: list[str] = []
    unreachable: list[str] = []

    for depend_key, slug in CHECKABLE.items():
        declared = depends.get(depend_key)
        if not declared:
            continue
        try:
            versions = fetch_versions(slug, game_version)
        except (urllib.error.URLError, TimeoutError, OSError) as error:
            unreachable.append(f"{slug}: {error}")
            continue

        if not versions:
            problems.append(
                f"{depend_key}: upstream publishes NO Fabric build for Minecraft {game_version}"
            )
            continue

        published = [v["version_number"] for v in versions]
        matching = [
            v for v in published if all(_satisfies(v, part) for part in str(declared).split())
        ]
        if matching:
            print(f"OK   {depend_key}: '{declared}' matches {matching}")
        else:
            problems.append(
                f"{depend_key}: declared range '{declared}' matches NONE of the builds "
                f"published for Minecraft {game_version} ({published}). "
                f"Users would get 'Incompatible mods found' at launch."
            )

    for note in unreachable:
        print(f"SKIP (network unavailable) {note}")

    if problems:
        for problem in problems:
            print(f"{'WARN' if args.warn_only else 'ERROR'}: {problem}")
        if not args.warn_only:
            return 1

    if not problems and not unreachable:
        print(f"DEPENDENCY AVAILABILITY: OK (Minecraft {game_version})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))

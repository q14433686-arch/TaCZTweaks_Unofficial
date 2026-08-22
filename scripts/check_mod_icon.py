#!/usr/bin/env python3
"""Validate the distributed mod icon and its license provenance.

This check deliberately uses only the Python standard library so it can run both
standalone and as part of the Gradle verification lifecycle.
"""

from __future__ import annotations

import hashlib
import re
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "src/main/resources"
METADATA = ROOT / "src/main/templates/META-INF/neoforge.mods.toml"
NOTICE = ROOT / "THIRD_PARTY_NOTICES.md"

ICON_PATH = "icon.png"
EXPECTED_SHA256 = "c8591fdd552d0bbad05cd8a60136faf89d5e9fd6d0dab08eb96fa04439c6db9d"
REJECTED_PLACEHOLDER_SHA256 = "5e1272a625af1b0b4d866d0fb468e1cea0a9258411f16a7d06d31a84e8953ac8"
EXPECTED_SIZE = (512, 512)
UPSTREAM_COMMIT = "74ba2412a6149a1d91788c3663497c4c81992983"
MODRINTH_SOURCE = (
    "https://cdn.modrinth.com/data/H8peNuJG/"
    "0c9fcf0f40ec59d591b7cc17452c63a843df122e.png"
)


def validate_icon() -> list[str]:
    """Return human-readable errors for icon, metadata, or provenance drift."""
    errors: list[str] = []

    try:
        metadata = METADATA.read_text(encoding="utf-8")
    except OSError as exc:
        return [f"cannot read {METADATA.relative_to(ROOT)}: {exc}"]

    # NeoForge declares the icon as logoFile in neoforge.mods.toml.
    match = re.search(r'^logoFile\s*=\s*"([^"]+)"', metadata, re.MULTILINE)
    configured_icon = match.group(1) if match else None
    if configured_icon != ICON_PATH:
        errors.append(
            f"neoforge.mods.toml logoFile must be {ICON_PATH!r}, not {configured_icon!r}"
        )

    icon = RESOURCES / ICON_PATH
    try:
        raw = icon.read_bytes()
    except OSError as exc:
        errors.append(f"cannot read {icon.relative_to(ROOT)}: {exc}")
        return errors

    digest = hashlib.sha256(raw).hexdigest()
    if digest == REJECTED_PLACEHOLDER_SHA256:
        errors.append("mod icon is the known dark-grey/orange placeholder")
    elif digest != EXPECTED_SHA256:
        errors.append(
            f"mod icon SHA-256 drifted: expected {EXPECTED_SHA256}, got {digest}"
        )

    if len(raw) < 24 or raw[:8] != b"\x89PNG\r\n\x1a\n" or raw[12:16] != b"IHDR":
        errors.append("mod icon is not a PNG with a valid leading IHDR chunk")
    else:
        dimensions = struct.unpack(">II", raw[16:24])
        if dimensions != EXPECTED_SIZE:
            errors.append(
                f"mod icon must be {EXPECTED_SIZE[0]}x{EXPECTED_SIZE[1]}, "
                f"got {dimensions[0]}x{dimensions[1]}"
            )

    try:
        notice = NOTICE.read_text(encoding="utf-8")
    except OSError as exc:
        errors.append(f"cannot read {NOTICE.relative_to(ROOT)}: {exc}")
        return errors

    required_notice_values = {
        "Modrinth icon source": MODRINTH_SOURCE,
        "immutable upstream revision": UPSTREAM_COMMIT,
        "approved icon checksum": EXPECTED_SHA256,
        "icon license": "GPL-3.0",
    }
    for label, value in required_notice_values.items():
        if value not in notice:
            errors.append(f"THIRD_PARTY_NOTICES.md is missing {label}: {value}")

    return errors


def main() -> int:
    errors = validate_icon()
    if errors:
        print(f"MOD ICON: {len(errors)} error(s)")
        for message in errors:
            print(f"ERROR: {message}")
        return 1
    print(
        f"MOD ICON: OK ({EXPECTED_SIZE[0]}x{EXPECTED_SIZE[1]}, "
        f"SHA-256 {EXPECTED_SHA256})"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

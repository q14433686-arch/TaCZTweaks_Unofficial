#!/usr/bin/env python3
"""Validate the distributed mod icon and its recorded license provenance.

This checker intentionally uses only the Python standard library so it can run
standalone and from Gradle's verification lifecycle.
"""

from __future__ import annotations

import hashlib
import json
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "src/main/resources"
METADATA = RESOURCES / "fabric.mod.json"
NOTICE = ROOT / "THIRD_PARTY_NOTICES.md"

ICON_PATH = "icon.png"
EXPECTED_SHA256 = "c8591fdd552d0bbad05cd8a60136faf89d5e9fd6d0dab08eb96fa04439c6db9d"
REJECTED_PLACEHOLDER_SHA256 = "5e1272a625af1b0b4d866d0fb468e1cea0a9258411f16a7d06d31a84e8953ac8"
EXPECTED_SIZE = (512, 512)
UPSTREAM_COMMIT = "74ba2412a6149a1d91788c3663497c4c81992983"
UPSTREAM_PROJECT = "https://github.com/MUKSC/TaCZTweaks"
UPSTREAM_RESOURCE = (
    f"{UPSTREAM_PROJECT}/blob/{UPSTREAM_COMMIT}/src/main/resources/icon.png"
)
MODRINTH_SOURCE = (
    "https://cdn.modrinth.com/data/H8peNuJG/"
    "0c9fcf0f40ec59d591b7cc17452c63a843df122e.png"
)


def validate_png_ihdr(raw: bytes) -> list[str]:
    """Validate the PNG signature and complete leading IHDR chunk."""
    if len(raw) < 33 or raw[:8] != b"\x89PNG\r\n\x1a\n":
        return ["mod icon is not a PNG with a valid leading IHDR chunk"]

    chunk_length = struct.unpack(">I", raw[8:12])[0]
    chunk_type = raw[12:16]
    if chunk_length != 13 or chunk_type != b"IHDR" or len(raw) < 20 + chunk_length:
        return ["mod icon is not a PNG with a valid leading IHDR chunk"]

    ihdr = raw[16:29]
    stored_crc = struct.unpack(">I", raw[29:33])[0]
    actual_crc = zlib.crc32(chunk_type + ihdr) & 0xFFFFFFFF
    if stored_crc != actual_crc:
        return ["mod icon has an invalid leading IHDR CRC"]

    width, height, bit_depth, color_type, compression, filtering, interlace = struct.unpack(
        ">IIBBBBB", ihdr
    )
    valid_depths = {
        0: {1, 2, 4, 8, 16},
        2: {8, 16},
        3: {1, 2, 4, 8},
        4: {8, 16},
        6: {8, 16},
    }
    if (
        width == 0
        or height == 0
        or bit_depth not in valid_depths.get(color_type, set())
        or compression != 0
        or filtering != 0
        or interlace not in {0, 1}
    ):
        return ["mod icon has invalid IHDR fields"]

    if (width, height) != EXPECTED_SIZE:
        return [
            f"mod icon must be {EXPECTED_SIZE[0]}x{EXPECTED_SIZE[1]}, "
            f"got {width}x{height}"
        ]
    return []


def validate_icon() -> list[str]:
    """Return human-readable errors for metadata, icon, or provenance drift."""
    errors: list[str] = []

    try:
        metadata = json.loads(METADATA.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        errors.append(f"cannot parse {METADATA.relative_to(ROOT)}: {exc}")
    else:
        configured_icon = metadata.get("icon")
        if configured_icon != ICON_PATH:
            errors.append(
                f"fabric.mod.json icon must be {ICON_PATH!r}, not {configured_icon!r}"
            )

    icon = RESOURCES / ICON_PATH
    try:
        raw = icon.read_bytes()
    except OSError as exc:
        errors.append(f"cannot read {icon.relative_to(ROOT)}: {exc}")
    else:
        digest = hashlib.sha256(raw).hexdigest()
        if digest == REJECTED_PLACEHOLDER_SHA256:
            errors.append("mod icon is the known dark-grey/orange placeholder")
        elif digest != EXPECTED_SHA256:
            errors.append(
                f"mod icon SHA-256 drifted: expected {EXPECTED_SHA256}, got {digest}"
            )
        errors.extend(validate_png_ihdr(raw))

    try:
        notice = NOTICE.read_text(encoding="utf-8")
    except OSError as exc:
        errors.append(f"cannot read {NOTICE.relative_to(ROOT)}: {exc}")
    else:
        required_notice_values = {
            "original project": UPSTREAM_PROJECT,
            "immutable upstream revision": UPSTREAM_COMMIT,
            "upstream resource": UPSTREAM_RESOURCE,
            "Modrinth icon source": MODRINTH_SOURCE,
            "icon author": "MUKSC",
            "icon license": "GPL-3.0",
            "approved icon checksum": EXPECTED_SHA256,
            "repository use path": "src/main/resources/icon.png",
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

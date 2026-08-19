#!/usr/bin/env python3
"""Download and verify vendored binary dependencies declared in RESOURCE_IMPORT_MANIFEST.tsv.

This script is intentionally standard-library only so CI and clean developer machines can
reconstruct the `libs/` directory without trusting mutable filenames. Existing files are
always SHA-256 checked; downloads are written to a temporary file and only moved into place
when the expected checksum matches.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import sys
import tempfile
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "RESOURCE_IMPORT_MANIFEST.tsv"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def rows() -> list[dict[str, str]]:
    with MANIFEST.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(
            (line for line in handle if line.strip() and not line.startswith("#")),
            delimiter="\t",
        )
        required = {"path", "source_url", "sha256"}
        missing = required.difference(reader.fieldnames or [])
        if missing:
            raise SystemExit(f"{MANIFEST} is missing columns: {', '.join(sorted(missing))}")
        return list(reader)


def verify_existing(path: Path, expected: str) -> bool:
    if not path.exists():
        return False
    actual = sha256(path)
    if actual != expected:
        raise SystemExit(f"SHA-256 mismatch for {path.relative_to(ROOT)}: expected {expected}, got {actual}")
    return True


def download(row: dict[str, str]) -> None:
    relative = row["path"]
    target = ROOT / relative
    expected = row["sha256"].lower()
    if verify_existing(target, expected):
        print(f"OK existing {relative} {expected}")
        return

    target.parent.mkdir(parents=True, exist_ok=True)
    url = row["source_url"]
    print(f"Downloading {relative} from {url}")
    with tempfile.NamedTemporaryFile(prefix=target.name + ".", suffix=".tmp", dir=target.parent, delete=False) as tmp:
        tmp_path = Path(tmp.name)
        try:
            with urllib.request.urlopen(url, timeout=120) as response:
                while True:
                    chunk = response.read(1024 * 1024)
                    if not chunk:
                        break
                    tmp.write(chunk)
        except Exception:
            tmp_path.unlink(missing_ok=True)
            raise

    actual = sha256(tmp_path)
    if actual != expected:
        tmp_path.unlink(missing_ok=True)
        raise SystemExit(f"Downloaded {relative} with wrong SHA-256: expected {expected}, got {actual}")
    tmp_path.replace(target)
    print(f"OK downloaded {relative} {expected}")


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check-only", action="store_true", help="fail if a dependency is missing; do not download")
    args = parser.parse_args(argv)

    for row in rows():
        target = ROOT / row["path"]
        expected = row["sha256"].lower()
        if args.check_only:
            if not verify_existing(target, expected):
                raise SystemExit(f"Missing dependency: {target.relative_to(ROOT)}")
            print(f"OK {target.relative_to(ROOT)} {expected}")
        else:
            download(row)
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))

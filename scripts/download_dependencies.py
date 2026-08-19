#!/usr/bin/env python3
"""Download and verify vendored binary dependencies declared in RESOURCE_IMPORT_MANIFEST.tsv.

This script is intentionally standard-library only so CI and clean developer machines can
reconstruct the `libs/` directory without trusting mutable filenames. Existing files are
always checksum-checked; downloads are written to a temporary file and only moved into place
when every declared digest matches.

1.21.11 note: GitHub release assets publish SHA-256. The Modrinth version API for the pinned
YACL file currently publishes SHA-1 and SHA-512. The manifest may therefore pin sha256,
sha512, or both. At least one complete digest is required.
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


def digest(path: Path, algorithm: str) -> str:
    hasher = hashlib.new(algorithm)
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            hasher.update(chunk)
    return hasher.hexdigest()


def rows() -> list[dict[str, str]]:
    with MANIFEST.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(
            (line for line in handle if line.strip() and not line.startswith("#")),
            delimiter="\t",
        )
        required = {"path", "source_url"}
        missing = required.difference(reader.fieldnames or [])
        if missing:
            raise SystemExit(f"{MANIFEST} is missing columns: {', '.join(sorted(missing))}")
        return list(reader)


def expected_digests(row: dict[str, str]) -> dict[str, str]:
    values: dict[str, str] = {}
    sha256 = (row.get("sha256") or "").strip().lower()
    sha512 = (row.get("sha512") or "").strip().lower()
    if sha256:
        if len(sha256) != 64 or any(char not in "0123456789abcdef" for char in sha256):
            raise SystemExit(f"{row.get('path')} has malformed sha256: {sha256}")
        values["sha256"] = sha256
    if sha512:
        if len(sha512) != 128 or any(char not in "0123456789abcdef" for char in sha512):
            raise SystemExit(f"{row.get('path')} has malformed sha512: {sha512}")
        values["sha512"] = sha512
    if not values:
        raise SystemExit(f"{row.get('path')} must declare sha256 and/or sha512")
    return values


def verify_existing(path: Path, expected: dict[str, str]) -> bool:
    if not path.exists():
        return False
    for algorithm, digest_value in expected.items():
        actual = digest(path, algorithm)
        if actual != digest_value:
            raise SystemExit(
                f"{algorithm.upper()} mismatch for {path.relative_to(ROOT)}: "
                f"expected {digest_value}, got {actual}"
            )
    return True


def download(row: dict[str, str]) -> None:
    relative = row["path"]
    target = ROOT / relative
    expected = expected_digests(row)
    if verify_existing(target, expected):
        rendered = " ".join(f"{algo}={value}" for algo, value in expected.items())
        print(f"OK existing {relative} {rendered}")
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

    try:
        for algorithm, digest_value in expected.items():
            actual = digest(tmp_path, algorithm)
            if actual != digest_value:
                raise SystemExit(
                    f"Downloaded {relative} with wrong {algorithm.upper()}: "
                    f"expected {digest_value}, got {actual}"
                )
    except Exception:
        tmp_path.unlink(missing_ok=True)
        raise
    tmp_path.replace(target)
    rendered = " ".join(f"{algo}={value}" for algo, value in expected.items())
    print(f"OK downloaded {relative} {rendered}")


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check-only", action="store_true", help="fail if a dependency is missing; do not download")
    args = parser.parse_args(argv)

    for row in rows():
        target = ROOT / row["path"]
        expected = expected_digests(row)
        if args.check_only:
            if not verify_existing(target, expected):
                raise SystemExit(f"Missing dependency: {target.relative_to(ROOT)}")
            rendered = " ".join(f"{algo}={value}" for algo, value in expected.items())
            print(f"OK {target.relative_to(ROOT)} {rendered}")
        else:
            download(row)
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))

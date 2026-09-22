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

# A manifest row may legitimately not know its SHA-256 yet: Modrinth publishes only
# sha1/sha512, and the sandbox that prepares a port cannot always reach the CDN. Such a
# row is downloaded without checksum enforcement and its real digest is printed so a
# human can pin it. It is never silently accepted by --check-only.
PENDING_SHA = "UNVERIFIED_PENDING_CI"


def expected_sha(row: dict[str, str]) -> str:
    """Normalise a manifest sha256 cell.

    Returns PENDING_SHA verbatim for not-yet-pinned rows, otherwise the lowercased digest.
    Must be the only place that interprets this column.
    """
    raw = row["sha256"].strip()
    return raw if raw == PENDING_SHA else raw.lower()


def sha256(path: Path) -> str:
    return _digest(path, "sha256")


def _digest(path: Path, algorithm: str) -> str:
    digest = hashlib.new(algorithm)
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def upstream_sha512(row: dict[str, str]) -> str:
    """The publisher-attested SHA-512 for this row, or "" when none is recorded.

    Modrinth (and the packwiz lockfiles that mirror it) publish sha1/sha512 but never
    sha256, so a row awaiting a pinned sha256 can still be authenticated against this.
    """
    return row.get("sha512_upstream", "").strip().lower()


def check_upstream_sha512(path: Path, row: dict[str, str]) -> None:
    """Fail unless the bytes match the publisher-attested SHA-512, when one is recorded.

    This is what makes PENDING_SHA rows safe: without it, a pending row would accept
    whatever bytes the URL happened to serve.
    """
    expected = upstream_sha512(row)
    if not expected:
        return
    actual = _digest(path, "sha512")
    if actual != expected:
        raise SystemExit(
            f"SHA-512 mismatch for {path.relative_to(ROOT)} against the upstream-attested "
            f"digest:\n  expected {expected}\n  got      {actual}"
        )


def rows() -> list[dict[str, str]]:
    with MANIFEST.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(
            (line for line in handle if line.strip() and not line.startswith("#")),
            delimiter="\t",
        )
        required = {"path", "source_url", "sha256"}  # sha512_upstream is optional
        missing = required.difference(reader.fieldnames or [])
        if missing:
            raise SystemExit(f"{MANIFEST} is missing columns: {', '.join(sorted(missing))}")
        return list(reader)


def verify_existing(path: Path, expected: str, row: dict[str, str]) -> bool:
    if not path.exists():
        return False
    actual = sha256(path)
    if expected == PENDING_SHA:
        check_upstream_sha512(path, row)
        print(f"PENDING {path.relative_to(ROOT)} sha256={actual} (pin this in RESOURCE_IMPORT_MANIFEST.tsv)")
        return True
    if actual != expected:
        raise SystemExit(f"SHA-256 mismatch for {path.relative_to(ROOT)}: expected {expected}, got {actual}")
    return True


def download(row: dict[str, str]) -> None:
    relative = row["path"]
    target = ROOT / relative
    expected = expected_sha(row)
    if verify_existing(target, expected, row):
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
    if expected == PENDING_SHA:
        try:
            check_upstream_sha512(tmp_path, row)
        except SystemExit:
            tmp_path.unlink(missing_ok=True)
            raise
        tmp_path.replace(target)
        print(f"PENDING {relative} sha256={actual} (pin this in RESOURCE_IMPORT_MANIFEST.tsv)")
        return
    if actual != expected:
        tmp_path.unlink(missing_ok=True)
        raise SystemExit(f"Downloaded {relative} with wrong SHA-256: expected {expected}, got {actual}")
    tmp_path.replace(target)
    print(f"OK downloaded {relative} {expected}")


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check-only", action="store_true", help="fail if a dependency is missing; do not download")
    parser.add_argument(
        "--print-sha256",
        action="store_true",
        help="print the SHA-256 of every present dependency (used to pin UNVERIFIED_PENDING_CI rows)",
    )
    parser.add_argument(
        "--require-pinned",
        action="store_true",
        help="fail if any manifest row still carries the UNVERIFIED_PENDING_CI placeholder",
    )
    args = parser.parse_args(argv)

    pending: list[str] = []
    for row in rows():
        target = ROOT / row["path"]
        expected = expected_sha(row)
        if expected == PENDING_SHA:
            pending.append(row["path"])
        if args.check_only:
            if not verify_existing(target, expected, row):
                raise SystemExit(f"Missing dependency: {target.relative_to(ROOT)}")
            print(f"OK {target.relative_to(ROOT)} {expected}")
        else:
            download(row)
        if args.print_sha256 and target.is_file():
            print(f"SHA256 {row['path']} {sha256(target)}")

    if pending:
        message = "manifest rows still pending a pinned SHA-256: " + ", ".join(pending)
        if args.require_pinned:
            raise SystemExit(message)
        print(f"WARNING: {message}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))

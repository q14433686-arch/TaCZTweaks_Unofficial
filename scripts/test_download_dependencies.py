"""Regression tests for scripts/download_dependencies.py manifest checksum handling.

These exist because of a real CI failure (2026-09-22, runs 357024087xx): every workflow
died on "Restore vendored dependencies" because `download()` lowercased the
UNVERIFIED_PENDING_CI sentinel into `unverified_pending_ci`, so the "is this row pending?"
comparison never matched and a pending row was treated as a checksum mismatch.

The invariant under test: the sentinel must survive normalisation, a pinned digest must
still be compared case-insensitively, and a genuinely wrong digest must still fail hard.
"""

from __future__ import annotations

import hashlib
import importlib.util
import subprocess
import sys
from pathlib import Path

import pytest

SCRIPTS = Path(__file__).resolve().parent
SCRIPT = SCRIPTS / "download_dependencies.py"

_spec = importlib.util.spec_from_file_location("download_dependencies", SCRIPT)
_module = importlib.util.module_from_spec(_spec)
assert _spec.loader is not None
_spec.loader.exec_module(_module)

PENDING_SHA = _module.PENDING_SHA
expected_sha = _module.expected_sha

HEADER = (
    "path\tsource_url\tupstream_project\tupstream_version\tsha256\tsha512_upstream\t"
    "license\tuse\tbundled_in_release_jar\tretrieved_or_verified_utc\tnotes\n"
)


def test_pending_sentinel_survives_normalisation() -> None:
    """The bug: .lower() mangled the sentinel so it stopped comparing equal."""
    assert expected_sha({"sha256": PENDING_SHA}) == PENDING_SHA
    assert expected_sha({"sha256": f"  {PENDING_SHA}  "}) == PENDING_SHA


def test_pinned_digest_is_lowercased() -> None:
    digest = "A" * 64
    assert expected_sha({"sha256": digest}) == "a" * 64


def _workspace(
    tmp_path: Path,
    sha_cell: str,
    sha512_cell: str = "",
    payload: bytes = b"not-really-a-jar-but-bytes-are-bytes",
) -> tuple[Path, str]:
    """Build a throwaway repo whose manifest points at a local file:// source."""
    (tmp_path / "scripts").mkdir()
    (tmp_path / "scripts" / "download_dependencies.py").write_bytes(SCRIPT.read_bytes())
    source = tmp_path / "source.jar"
    source.write_bytes(payload)
    digest = hashlib.sha256(payload).hexdigest()
    cell = digest if sha_cell == "__REAL__" else sha_cell
    if sha512_cell == "__REAL__":
        sha512_cell = hashlib.sha512(payload).hexdigest()
    (tmp_path / "RESOURCE_IMPORT_MANIFEST.tsv").write_text(
        HEADER
        + f"libs/dep.jar\tfile://{source}\tproj\t1.0\t{cell}\t{sha512_cell}"
        f"\tMIT\tuse\tno\t2026-09-22\tnote\n",
        encoding="utf-8",
    )
    return tmp_path, digest


def _run(root: Path, *args: str) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [sys.executable, str(root / "scripts" / "download_dependencies.py"), *args],
        capture_output=True,
        text=True,
        cwd=root,
    )


def test_pending_row_downloads_and_reports_digest(tmp_path: Path) -> None:
    root, digest = _workspace(tmp_path, PENDING_SHA)
    result = _run(root, "--print-sha256")
    assert result.returncode == 0, result.stderr
    assert (root / "libs" / "dep.jar").is_file()
    assert digest in result.stdout
    assert "pending a pinned SHA-256" in result.stdout


def test_pending_row_still_blocks_require_pinned(tmp_path: Path) -> None:
    root, _ = _workspace(tmp_path, PENDING_SHA)
    assert _run(root, "--require-pinned").returncode != 0


def test_correct_pinned_digest_passes(tmp_path: Path) -> None:
    root, _ = _workspace(tmp_path, "__REAL__")
    result = _run(root)
    assert result.returncode == 0, result.stderr
    assert (root / "libs" / "dep.jar").is_file()


def test_wrong_pinned_digest_fails_and_discards_download(tmp_path: Path) -> None:
    root, _ = _workspace(tmp_path, "0" * 64)
    result = _run(root)
    assert result.returncode != 0
    assert "wrong SHA-256" in result.stderr
    # A mismatching download must never be left in place for the build to pick up.
    assert not (root / "libs" / "dep.jar").exists()


if __name__ == "__main__":
    raise SystemExit(pytest.main([__file__, "-q"]))


def test_pending_row_is_authenticated_by_upstream_sha512(tmp_path: Path) -> None:
    """A pending sha256 row is still safe when the manifest attests a SHA-512."""
    root, _ = _workspace(tmp_path, PENDING_SHA, sha512_cell="__REAL__")
    result = _run(root)
    assert result.returncode == 0, result.stderr
    assert (root / "libs" / "dep.jar").is_file()


def test_pending_row_rejects_bytes_failing_upstream_sha512(tmp_path: Path) -> None:
    """The point of the sha512 column: wrong bytes must not reach libs/.

    Without this, a row awaiting its sha256 would accept whatever the URL served.
    """
    wrong_sha512 = hashlib.sha512(b"some other file entirely").hexdigest()
    root, _ = _workspace(tmp_path, PENDING_SHA, sha512_cell=wrong_sha512)
    result = _run(root)
    assert result.returncode != 0
    assert "SHA-512 mismatch" in (result.stderr + result.stdout)
    # and nothing is left behind for a later run to trust
    assert not (root / "libs" / "dep.jar").exists()


def test_row_without_sha512_keeps_previous_behaviour(tmp_path: Path) -> None:
    """The new column is optional; rows that omit it behave exactly as before."""
    root, _ = _workspace(tmp_path, PENDING_SHA, sha512_cell="")
    result = _run(root)
    assert result.returncode == 0, result.stderr
    assert (root / "libs" / "dep.jar").is_file()

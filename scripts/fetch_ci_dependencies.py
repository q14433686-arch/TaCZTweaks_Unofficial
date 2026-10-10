#!/usr/bin/env python3
"""Populate libs/ on CI runners from RESOURCE_IMPORT_MANIFEST.tsv.

The binary dependencies are not committed to Git; a developer machine downloads them by
hand or with scripts/download_dependencies.py. GitHub Actions runners cannot do either,
so this script resolves each required manifest row automatically:

  * rows whose ``source_url`` is a direct download (GitHub release assets, Maven Central)
    are fetched from that URL;
  * Modrinth-published rows (YACL, Sound Physics Remastered) are resolved through the
    Modrinth v2 API for game version 26.3 + loader neoforge, and the picked file is saved
    under the manifest path so build.gradle's name-pattern matching keeps working;
  * optional rows without a confirmed 26.3 artifact (First Aid, Pillager's Gun) are
    skipped with a notice — they are not compile requirements.

Standard-library only. Recorded SHA-256 values are verified when present; ``pending``
digests are reported (and printed for backfilling) instead of failing, matching the
transitional rule used by download_dependencies.py.
"""

from __future__ import annotations

import csv
import hashlib
import json
import os
import sys
import tempfile
import time
import traceback
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "RESOURCE_IMPORT_MANIFEST.tsv"
LIBS = ROOT / "libs"

GAME_VERSION = "26.3"
LOADER = "neoforge"

# Manifest path token -> Modrinth project slug. Rows not listed here and not optional are
# downloaded directly from their manifest source_url.
MODRINTH_PROJECTS = {
    "yet_another_config_lib": "yacl",
    "sound-physics-remastered": "sound-physics-remastered",
}

# Manifest path tokens for rows that are optional and may legitimately have no 26.3 build.
OPTIONAL_TOKENS = ("firstaid", "pillagers_gun")

PENDING = {"", "pending", "tbd"}

USER_AGENT = "TaCZTweaks-Unofficial-CI/1.0 (+https://github.com/q14433686-arch/TaCZTweaks_Unofficial)"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


RETRYABLE_HTTP_CODES = (408, 425, 429, 500, 502, 503, 504)


def http_get(url: str, timeout: int = 300, attempts: int = 5) -> bytes:
    """GET with retry/backoff.

    CI runs several workflows per push and they all hit Modrinth/GitHub at once;
    transient 429s and connection resets must not kill the whole job.
    """
    last: Exception | None = None
    for attempt in range(1, attempts + 1):
        try:
            request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
            with urllib.request.urlopen(request, timeout=timeout) as response:
                return response.read()
        except urllib.error.HTTPError as exc:
            if exc.code not in RETRYABLE_HTTP_CODES or attempt == attempts:
                raise
            last = exc
        except (urllib.error.URLError, TimeoutError, ConnectionError, OSError) as exc:
            if attempt == attempts:
                raise
            last = exc
        delay = min(2 ** attempt, 30)
        print(f"  retrying in {delay}s after attempt {attempt} failed ({last})")
        time.sleep(delay)
    raise AssertionError("unreachable")


class _Tee:
    """Fan stdout/stderr out to the Actions console and to a transcript file."""

    def __init__(self, *streams) -> None:
        self.streams = streams

    def write(self, data: str) -> None:
        for stream in self.streams:
            try:
                stream.write(data)
                stream.flush()
            except Exception:
                pass

    def flush(self) -> None:
        for stream in self.streams:
            try:
                stream.flush()
            except Exception:
                pass


def rows() -> list[dict[str, str]]:
    with MANIFEST.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(
            (line for line in handle if line.strip() and not line.startswith("#")),
            delimiter="\t",
        )
        return list(reader)


def verify_or_report(path: Path, expected: str) -> None:
    actual = sha256(path)
    if expected in PENDING:
        print(f"NOTE {path.name}: digest still pending, observed {actual}")
    elif actual != expected.lower():
        raise SystemExit(f"SHA-256 mismatch for {path.name}: expected {expected}, got {actual}")
    else:
        print(f"OK {path.name} matches recorded digest")


def download_to(url: str, target: Path) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    print(f"Downloading {target.name} from {url}")
    with tempfile.NamedTemporaryFile(prefix=target.name + ".", suffix=".tmp",
                                     dir=target.parent, delete=False) as tmp:
        tmp_path = Path(tmp.name)
        try:
            tmp_path.write_bytes(http_get(url))
        except Exception:
            tmp_path.unlink(missing_ok=True)
            raise
    tmp_path.replace(target)


def modrinth_pick_file(slug: str) -> str:
    """Return the download URL of the newest 26.3/neoforge file for a Modrinth project."""
    query = urllib.parse.urlencode({
        "game_versions": json.dumps([GAME_VERSION]),
        "loaders": json.dumps([LOADER]),
    })
    url = f"https://api.modrinth.com/v2/project/{slug}/version?{query}"
    versions = json.loads(http_get(url))
    if not versions:
        raise SystemExit(f"Modrinth project {slug} has no {GAME_VERSION}/{LOADER} versions")
    # The API returns versions newest-first; take the first one with a primary file.
    for version in versions:
        files = version.get("files") or []
        primary = next((f for f in files if f.get("primary")), files[0] if files else None)
        if primary:
            print(f"Modrinth {slug}: {version.get('version_number')} ({version.get('name')})")
            return primary["url"]
    raise SystemExit(f"Modrinth project {slug} returned versions without files")


def fetch_row(row: dict[str, str]) -> None:
    path = ROOT / row["path"]
    name = path.name.lower()
    expected = (row.get("sha256") or "").strip().lower()

    if path.is_file():
        verify_or_report(path, expected)
        return

    if any(token in name for token in OPTIONAL_TOKENS):
        print(f"SKIP optional dependency without a confirmed {GAME_VERSION} artifact: {row['path']}")
        return

    slug = next((project for token, project in MODRINTH_PROJECTS.items() if token in name), None)
    if slug is not None:
        download_to(modrinth_pick_file(slug), path)
    elif row["source_url"].startswith(("https://github.com/", "https://repo1.maven.org/")):
        download_to(row["source_url"], path)
    else:
        raise SystemExit(
            f"No CI resolver for {row['path']}; source_url is not a direct download: {row['source_url']}"
        )
    verify_or_report(path, expected)


def _run() -> int:
    for row in rows():
        fetch_row(row)
    present = sorted(p.name for p in LIBS.glob("*.jar")) if LIBS.is_dir() else []
    print("libs/ now contains:\n  " + ("\n  ".join(present) if present else "(nothing)"))
    return 0


def main() -> int:
    # On CI, mirror the whole transcript into build-reports/compile-java.log. If this
    # fetch step fails, the workflow's compileJava step (which truncates that file
    # before gradle) never runs, so the ci-log push-back uploads this transcript and
    # the failure stays diagnosable without the Actions log UI. On success the
    # workflow truncates the file before writing gradle output, so no noise leaks in.
    if os.environ.get("GITHUB_ACTIONS") != "true":
        return _run()
    log_dir = ROOT / "build-reports"
    log_dir.mkdir(exist_ok=True)
    transcript = (log_dir / "compile-java.log").open("w", encoding="utf-8")
    sys.stdout = _Tee(sys.__stdout__, transcript)
    sys.stderr = sys.stdout
    try:
        return _run()
    except SystemExit as exc:
        print(f"fetch_ci_dependencies: FATAL: {exc}")
        return exc.code if isinstance(exc.code, int) else 1
    except Exception:
        traceback.print_exc()
        return 1
    finally:
        sys.stdout = sys.__stdout__
        sys.stderr = sys.__stderr__
        transcript.close()


if __name__ == "__main__":
    raise SystemExit(main())

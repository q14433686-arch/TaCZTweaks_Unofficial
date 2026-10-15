#!/usr/bin/env python3
"""Publish the useful part of a CI log as a GitHub Actions job annotation.

Why this exists
---------------
Development sandboxes for this repository can reach ``api.github.com`` but not the blob
storage that serves Actions log text, so ``gh run view --log`` fails there. Job annotations
are served by the REST API itself, so a restricted environment can still read why a build
failed::

    gh api repos/<owner>/<repo>/actions/runs/<run_id>/jobs \\
      --jq '.jobs[] | "\\(.id) \\(.name) \\(.conclusion)"'
    gh api repos/<owner>/<repo>/check-runs/<job_id>/annotations --jq '.[].message'

The 26.3 line solves the same problem by committing ``build-reports/compile-java.log`` back
to the branch. On this line annotations are used instead: no CI commits land in the PR (an
unapproved ``github-actions[bot]`` head commit otherwise shows up as failed/pending checks),
and the full build and release jobs can report too, not only compilation.

What is published
-----------------
* failure / cancelled -> ``::error``: Gradle's ``* What went wrong:`` block, compiler and
  test error lines, then the log tail;
* success -> ``::notice``: a short log tail (e.g. ``BUILD SUCCESSFUL``, gate summaries).

Usage::

    python3 scripts/ci_log_annotation.py --title "build (linux)" --status "${{ job.status }}" \\
        build-reports/gradle-build.log

Standard library only; always exits 0 so it can never mask the real step result.
"""

from __future__ import annotations

import argparse
import re
from pathlib import Path

ANSI = re.compile(r"\x1b\[[0-9;?]*[ -/]*[@-~]")
SECTION = re.compile(r"^\* [A-Z]")  # Gradle failure sections: "* What went wrong:", "* Try:", ...
ERROR_LINE = re.compile(
    r"^e: "                      # Kotlin compiler
    r"|: error: "                # javac
    r"|^ERROR:"                  # repository Python gates (audit_port.py, ...)
    r"|FAILED$"                  # Gradle task / JUnit test failures
    r"|^Caused by: "
    r"|^\s+(?:org\.opentest4j\.|java\.lang\.AssertionError|org\.junit\.)"
)
MAX_LINE = 1000


def _escape_data(text: str) -> str:
    return text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def _escape_property(text: str) -> str:
    return _escape_data(text).replace(":", "%3A").replace(",", "%2C")


def _clean(lines: list[str]) -> list[str]:
    cleaned = []
    for line in lines:
        line = ANSI.sub("", line.rstrip("\r\n"))
        if len(line) > MAX_LINE:
            line = line[:MAX_LINE] + " ...[line truncated]"
        cleaned.append(line)
    return cleaned


def what_went_wrong(lines: list[str], limit: int = 60) -> list[str]:
    """Every Gradle '* What went wrong:' block (there is one per failure)."""
    blocks: list[str] = []
    capturing = False
    for line in lines:
        if line.startswith("* What went wrong:"):
            capturing = True
            continue
        if capturing and SECTION.match(line):
            capturing = False
        if capturing:
            blocks.append(line)
            if len(blocks) >= limit:
                break
    while blocks and not blocks[-1].strip():
        blocks.pop()
    return blocks


def error_lines(lines: list[str], limit: int = 80) -> list[str]:
    seen: set[str] = set()
    found: list[str] = []
    for line in lines:
        if ERROR_LINE.search(line) and line not in seen:
            seen.add(line)
            found.append(line)
            if len(found) >= limit:
                break
    return found


def build_message(lines: list[str], *, failed: bool, tail: int, max_chars: int) -> str:
    # [kind, body]; headers are rendered from the current body so they stay accurate.
    sections: list[list] = []
    if failed:
        wrong = what_went_wrong(lines)
        if wrong:
            sections.append(["wrong", wrong])
        errors = error_lines(lines)
        if errors:
            sections.append(["errors", errors])
    sections.append(["tail", lines[-tail:] if tail > 0 else []])

    def header(kind: str, body: list[str]) -> str:
        if kind == "wrong":
            return "== * What went wrong =="
        if kind == "errors":
            return f"== error lines ({len(body)} shown) =="
        return f"== last {len(body)} of {len(lines)} log lines =="

    def render() -> str:
        return "\n".join(header(kind, body) + "\n" + "\n".join(body) for kind, body in sections)

    message = render()
    # Too long: shrink the least important part first -- the tail (keeping its end), then
    # the error lines, then "What went wrong" (keeping their starts).
    for section in reversed(sections):
        while len(message) > max_chars and len(section[1]) > 10:
            cut = max(1, len(section[1]) // 4)
            section[1] = section[1][cut:] if section[0] == "tail" else section[1][:-cut]
            message = render()
    if len(message) > max_chars:
        message = message[: max_chars - 16] + "\n...[truncated]"
    return message


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n", 1)[0])
    parser.add_argument("log", type=Path)
    parser.add_argument("--title", required=True)
    parser.add_argument("--status", default="failure", help="job.status: success, failure or cancelled")
    parser.add_argument("--success-lines", type=int, default=25)
    parser.add_argument("--failure-lines", type=int, default=150)
    parser.add_argument("--max-chars", type=int, default=40000)
    args = parser.parse_args(argv)

    failed = args.status != "success"
    title = f"{args.title}: {args.status}"
    if not args.log.is_file():
        print(f"::warning title={_escape_property(title)}::"
              f"{_escape_data(f'No log at {args.log}; the job stopped before this step wrote one.')}")
        return 0

    raw = args.log.read_text(encoding="utf-8", errors="replace").splitlines()
    lines = _clean(raw)
    message = build_message(
        lines,
        failed=failed,
        tail=args.failure_lines if failed else args.success_lines,
        max_chars=args.max_chars,
    )
    level = "error" if failed else "notice"
    print(f"::{level} title={_escape_property(title)}::{_escape_data(message)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

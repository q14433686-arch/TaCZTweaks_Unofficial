#!/usr/bin/env python3
"""Tests for the CI helper scripts used by .github/workflows/ (26.1.2 line).

* ``check_release_tag.py``  -- release tags must name the current mod_version, tolerating the
  separator styles this repository actually uses (and the one the 26.3 workflow rejected).
* ``ci_log_annotation.py``  -- failing builds must surface "What went wrong" / compiler
  errors through job annotations, which restricted sandboxes can read via api.github.com.

Standard library only. Run either way::

    python3 scripts/test_ci_helpers.py
    python3 -m pytest scripts/test_ci_helpers.py
"""

from __future__ import annotations

import contextlib
import importlib.util
import io
import tempfile
import traceback
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def _load(name: str, filename: str):
    spec = importlib.util.spec_from_file_location(name, ROOT / "scripts" / filename)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


tag = _load("check_release_tag", "check_release_tag.py")
annotation = _load("ci_log_annotation", "ci_log_annotation.py")

HOTFIX2 = "2.14.2+fabric.26.1.2.Beta-1-hotfix2"


def _run(main, argv) -> tuple[int, str]:
    out = io.StringIO()
    with contextlib.redirect_stdout(out):
        code = main(argv)
    return code, out.getvalue()


# --------------------------------------------------------------------------- release tags


def test_current_version_suggests_the_conventional_tag() -> None:
    version = tag.read_mod_version()
    suggested = tag.suggested_tag(version)
    assert suggested.startswith("26.1.2_"), suggested
    assert tag.tag_matches(suggested, version)
    assert tag.suggested_tag(HOTFIX2) == "26.1.2_BETA_1_HOTFIX2"


def test_separator_and_case_variants_are_accepted() -> None:
    for spelling in (
        "26.1.2_BETA_1_HOTFIX2",
        "26.1.2_BETA_1_HOTFIX-2",
        "26.1.2_BETA_1_HOTFIX_2",
        "26.1.2-BETA_1_HOTFIX2",
        "26.1.2_beta-1-hotfix2",
    ):
        assert tag.tag_matches(spelling, HOTFIX2), spelling


def test_historical_tags_match_their_own_versions() -> None:
    # Real tags already published for this line, with their jar versions.
    assert tag.tag_matches("26.1.2-BETA_1", "2.14.2+fabric.26.1.2.Beta-1")
    assert tag.tag_matches("26.1.2_BETA_1_HOTFIX", "2.14.2+fabric.26.1.2.Beta-1-hotfix")


def test_the_spelling_the_263_workflow_rejected_is_accepted() -> None:
    """26.3 failed on '26.3_BETA_1_HOTFIX-1' for '...Beta-1-hotfix1' (2026-10-15)."""
    version = "2.14.2+fabric.26.1.2.Beta-1-hotfix1"
    assert tag.suggested_tag(version) == "26.1.2_BETA_1_HOTFIX1"  # the old exact-match rule
    assert tag.tag_matches("26.1.2_BETA_1_HOTFIX-1", version)


def test_other_releases_and_lines_are_rejected() -> None:
    for spelling in (
        "26.1.2_BETA_1_HOTFIX",        # the published, older release
        "26.1.2_BETA_1_HOTFIX3",
        "26.1.2_BETA_11_HOTFIX2",
        "26.1.2_BETA_1_1_HOTFIX2",
        "26.1.2_B1_HOTFIX2",
        "26.3_BETA_1_HOTFIX2",         # another release line
        "(NEO)26.1.2_BETA_1_HOTFIX2",  # the NeoForge line's tag style
        "26.1.20_BETA_1_HOTFIX2",
        "26.1.2BETA_1_HOTFIX2",
        "26.1.2_",
    ):
        assert not tag.tag_matches(spelling, HOTFIX2), spelling


def test_a_mod_version_for_another_line_is_an_error() -> None:
    try:
        tag.release_suffix("2.14.2+fabric.26.3.Beta-1-hotfix1")
    except ValueError:
        pass
    else:
        raise AssertionError("a 26.3 mod_version must not validate on the 26.1.2 line")
    with tempfile.TemporaryDirectory() as tmp:
        props = Path(tmp) / "gradle.properties"
        props.write_text("mod_version=2.14.2+fabric.26.3.Beta-1-hotfix1\n", encoding="utf-8")
        code, out = _run(tag.main, ["26.3_BETA_1_HOTFIX1", "--properties", str(props)])
        assert code == 1 and "26.1.2" in out, out


def test_cli_against_the_repository() -> None:
    expected = tag.suggested_tag(tag.read_mod_version())
    code, out = _run(tag.main, ["--suggest"])
    assert code == 0 and out.strip() == expected
    code, out = _run(tag.main, [expected])
    assert code == 0 and out.startswith("OK:"), out
    code, out = _run(tag.main, ["26.1.2_NOT_THIS_RELEASE"])
    assert code == 1 and expected in out, out


def test_release_workflow_listens_for_this_lines_tags() -> None:
    workflow = (ROOT / ".github/workflows/release.yml").read_text(encoding="utf-8")
    assert "- '26.1.2_*'" in workflow and "- '26.1.2-*'" in workflow
    assert "scripts/check_release_tag.py" in workflow


# --------------------------------------------------------------------------- annotations

GRADLE_FAILURE = (
    ["> Task :compileJava"]
    + ["/src/Foo.java:12: error: cannot find symbol", "  symbol: class Bar"]
    + [f"progress line {i}" for i in range(400)]
    + [
        "FAILURE: Build failed with an exception.",
        "",
        "* What went wrong:",
        "Execution failed for task ':compileJava'.",
        "> Compilation failed; see the compiler output below.",
        "",
        "* Try:",
        "> Run with --info option to get more log output.",
        "",
        "* Exception is:",
    ]
    + [f"\tat org.gradle.Frame{i}(Frame.java:{i})" for i in range(600)]
    + ["", "BUILD FAILED in 1m 2s"]
)


def test_escaping() -> None:
    assert annotation._escape_data("50% done\r\nnext") == "50%25 done%0D%0Anext"
    assert annotation._escape_property("build (linux): failure, again") == "build (linux)%3A failure%2C again"


def test_failure_keeps_what_went_wrong_and_errors_beyond_the_tail() -> None:
    message = annotation.build_message(GRADLE_FAILURE, failed=True, tail=150, max_chars=40000)
    assert "Execution failed for task ':compileJava'." in message
    assert "/src/Foo.java:12: error: cannot find symbol" in message
    assert "BUILD FAILED in 1m 2s" in message
    assert "* Try:" not in message.split("== error lines")[0]  # block stops at the next section


def test_max_chars_is_respected_without_losing_the_cause() -> None:
    noisy = GRADLE_FAILURE + ["x" * 900 for _ in range(3000)]
    message = annotation.build_message(noisy, failed=True, tail=150, max_chars=8000)
    assert len(message) <= 8000, len(message)
    assert "Execution failed for task ':compileJava'." in message


def _main_output(lines: list[str], status: str, title: str = "build (linux)") -> str:
    with tempfile.TemporaryDirectory() as tmp:
        log = Path(tmp) / "gradle-build.log"
        log.write_text("\n".join(lines) + "\n", encoding="utf-8")
        code, out = _run(annotation.main, [str(log), "--title", title, "--status", status])
    assert code == 0
    return out


def test_failure_is_one_error_annotation_line() -> None:
    out = _main_output(GRADLE_FAILURE, "failure")
    assert out.startswith("::error title=build (linux)%3A failure::"), out[:80]
    assert out.count("\n") == 1, "a workflow command must be a single line"
    assert "cannot find symbol" in out


def test_success_is_a_short_notice() -> None:
    out = _main_output([f"line {i}" for i in range(100)] + ["BUILD SUCCESSFUL in 3m"], "success")
    assert out.startswith("::notice title=build (linux)%3A success::")
    assert "BUILD SUCCESSFUL in 3m" in out and "line 10%0A" not in out


def test_missing_log_is_a_warning_not_a_crash() -> None:
    code, out = _run(annotation.main, ["/nonexistent/gradle-build.log", "--title", "t", "--status", "failure"])
    assert code == 0 and out.startswith("::warning ")


def main() -> int:
    tests = [(n, f) for n, f in sorted(globals().items()) if n.startswith("test_") and callable(f)]
    failures = 0
    for name, fn in tests:
        try:
            fn()
        except (Exception, SystemExit):  # report every failure, then exit non-zero
            failures += 1
            print(f"FAIL {name}")
            traceback.print_exc()
        else:
            print(f"PASS {name}")
    print(f"CI HELPER TESTS: {len(tests) - failures} passed, {failures} failed")
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())

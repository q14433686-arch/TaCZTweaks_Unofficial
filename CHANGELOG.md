# Changelog

All notable changes to this unofficial Fabric 1.21.11 port are tracked here. Release entries
should be user-facing and concise; keep detailed process notes in `docs/maintenance/` or
`PORTING_NOTES.md`.

## 2.14.2+fabric.1.21.11.Beta-1-hotfix2 - Unreleased

Cross-branch sync of the Fabric `26.3` line's runtime-constraint gates. The 26.3 branch hit
three "every CI workflow green, but the game refuses to start" incidents (2026-09-22 twice,
2026-10-15 once); all three were constraints that only the Fabric Loader evaluates, at
startup. Compilation, the static mixin audit and jar packaging never read them. What is
synced here is the **tooling that catches that class of bug**, not 26.3's version numbers.
The work order lives in `docs/CROSS_BRANCH_SYNC.md` on the `26.3` branch.

### Added

- `scripts/check_dependency_availability.py`: asks Modrinth what is *actually* published for
  this Minecraft version and fails when a declared `depends` range matches none of it. Catches
  the case where `gradle.properties` and `fabric.mod.json` agree with each other but both
  disagree with reality — which is exactly how the 26.3 FLK incident slipped through.
  Prints `SKIP (network unavailable)` and exits 0 when `api.modrinth.com` is unreachable, so
  acceptance must check for `SKIP` in the output rather than only the exit code.
- `scripts/test_dependency_ranges.py`: 13 regression tests covering range operators, the
  Fabric API cap removal, upstream availability and the runtime TaCZ gate. Each gate is
  tested in both directions — a gate that cannot fail is worthless.
- `check_release_consistency.check_runtime_version_gate()`: parses `SUPPORTED_TACZ_VERSION`,
  `SUPPORTED_TACZ_VERSION_PREFIX` and `MIN_SUPPORTED_TACZ_REVISION` out of `TaCZTweaks.java`
  and verifies the TaCZ version declared in `fabric.mod.json` would pass that in-game gate.
- `check_release_consistency.check_declared_ranges_include_built_versions()` and its
  `_satisfies` / `_semver_key` helpers: every version this build compiles against must satisfy
  the range shipped to users.
- `check_release_consistency.check_fabric_api_range_is_lower_bound_only()`: rejects any
  artificial upper cap on `fabric-api`.
- `check_release_consistency.py --require-deps`: `libs/*.jar` are not committed, so the fast
  offline consistency gate now prints `SKIP (not restored)` for absent manifest rows and only
  the jobs that actually ran `download_dependencies.py` demand their presence. Without this
  the consistency workflow could never run in a network-restricted sandbox.
- `download_dependencies.py --print-sha256`: emits the SHA-256 of every restored dependency so
  a manifest row can be pinned straight from a CI log.
- Five GitHub Actions workflows under `.github/workflows/`: `consistency.yml`, `audit.yml`,
  `compile-check.yml`, `build.yml`, and `release.yml`. Previously this branch had none.
  `release.yml` publishes a GitHub Release only when a `1.21.11_*` tag is pushed; it re-checks
  that the tag matches `mod_version` and sits on the `1.21.11` branch, then runs the static
  audit, the dependency-range tests, a full build and jar-metadata verification before
  uploading the mod jar and example pack. Ordinary pushes and PRs never publish anything.

### Changed

- `fabric-api` dependency is now lower-bound-only: `>=0.141.6+1.21.11 <0.143.0` →
  `>=0.141.6+1.21.11`. Fabric API advances within one Minecraft line (0.141.x → 0.143.x)
  without changing the supported game version, and `minecraft` is pinned separately, so the cap
  only served to reject compatible updates. On 26.3 the same shape of cap (`<0.162.0`) made the
  Loader refuse a user's installed `0.162.0+26.3`. This line's *minimum* is unchanged.
- `TaCZTweaksVersionTest` now evaluates `TaCZTweaks.SUPPORTED_TACZ_VERSION` instead of retyped
  literals, and its rejection cases are framed as wrong-release-family rather than
  "previous line". A test that pins a stale expectation is worse than no test: that is what kept
  the 26.3 incident invisible while `./gradlew build` stayed green.
- Version renamed `Beta-1-hotfix` → **`Beta-1-hotfix2`**, matching the numbered hotfix scheme
  the 26.3 line adopted (`Beta-1-hotfix1`). The corresponding release tag is
  `1.21.11_BETA_1_HOTFIX2`; `1.21.11_BETA_1_HOTFIX` stays on the previous build.
- All four verification workflows use **JDK 21**, not 26.x's JDK 25 — this is the obfuscated
  Loom-remap port. `compile-check.yml` additionally compiles the test source sets
  (`compileTestJava` / `compileTestKotlin`), so a broken test file surfaces in the fast
  workflow instead of only in `build.yml`.
- `docs/maintenance/ci-workflow.yml` (the original single-file CI *proposal*, written when the
  bot credentials could not push under `.github/workflows/`) is now marked superseded and kept
  for reference only. The five installed workflows are authoritative — and unlike the proposal,
  they do run `audit_port.py --strict` rather than downgrading it.
- Docs updated to match: `CONTRIBUTING.md` gains the runtime-only-constraint rules and the
  "never cap `fabric-api`" / "workflow files need `workflows` permission" expectations;
  `docs/KNOWN_ISSUES.md` gains "green CI does not prove the game launches";
  `docs/README.md` indexes the new scripts and workflows.

### Fixed

- Shield durability mixin argument-count crash (`IncorrectArgumentCountException`) — landed on
  this branch after the `1.21.11_BETA_1_HOTFIX` tag, so it ships here for the first time.

### Deliberately not synced

- **26.3's `fabric-language-kotlin` range.** `>=1.13.13 <1.14.0` is *correct* on this line:
  FLK 1.13.12 / 1.13.13 / 1.13.14 / 1.14.0 / 1.14.1 are all tagged for Minecraft 1.21.11. 26.3
  had to move to `>=1.14.1+kotlin.2.4.20 <2.0.0` because 1.14.1 is the only FLK build tagged
  for 26.3. Copying that floor here would exclude the 1.13.13 build this branch compiles
  against. `test_copying_263_flk_range_is_rejected` pins the lesson down.
- **26.3's TaCZ revision floor.** This line requires **≥R2** (TaCZ Refabricated 1.21.11 R1
  predates APIs this mod needs); 26.3 ships from R1. Both gates are read from source rather
  than hardcoded, so neither line can inherit the other's floor by accident.
- **`scripts/test_audit_optional_targets.py`.** It models the *unobfuscated* 26.x classpath
  (`BlocksAttacks`, the 5-arg vs 6-arg `hurtBlockingItem`) to test a `remap=false` optional
  `@At` target. This branch is an obfuscated Loom-remap port with a different `audit_port.py`
  and different target class names, so there is nothing for it to test here.

### Compatibility

- Minecraft: `=1.21.11`
- Fabric Loader: `>=0.19.3 <0.20.0`
- Fabric API: `>=0.141.6+1.21.11` (no upper cap)
- Java: `>=21`
- TaCZ Refabricated Unofficial runtime gate: `1.1.8+fabric.1.21.11.R<n>` with `n >= 2`
- Fabric Language Kotlin: `>=1.13.13 <1.14.0`
- YetAnotherConfigLib: exactly `3.8.2+1.21.11-fabric`

### Known issues

- Still no in-game launch matrix on this branch. Every incident that motivated this sync was
  "CI green, game broken"; the gates narrow the window but do not replace starting the client,
  an integrated server and a dedicated server once before release.
- `check_dependency_availability.py` has not been run against live `api.modrinth.com` data from
  this branch (the porting sandbox cannot reach it). Its 1.21.11 fixtures come from the
  verified table in `docs/CROSS_BRANCH_SYNC.md` §2.4; the first `consistency.yml` run will
  confirm them against upstream.

---

## 2.14.2+fabric.1.21.11.Beta-1-hotfix - released as tag `1.21.11_BETA_1_HOTFIX`

The shield-durability mixin argument-count fix (`IncorrectArgumentCountException`) landed after
this tag and ships in `Beta-1-hotfix2`.

### Added

- Fabric 1.21.11 port of TaCZ Tweaks v2.14.2 for the `TaCZ_Refabricated_Unofficial` R2 and later revisions in the same release family.
- YACL v3 configuration UI, Mod Menu entrypoint, JSON persistence and server configuration sync.
- Gun movement, reload, unload, manual bolt, underwater-fire, hit-marker and modifier features.
- Data-driven bullet interactions, bullet sounds, bullet particles and melee interactions.
- Optional compatibility paths for Sound Physics Remastered 1.5.1, First Aid New 1.2.5 legacy,
  and Pillager's Gun 3.2.2 on Fabric 1.21.11.
- Example reloadable gun pack artifact.
- Static remapped-port audit, icon/license checksum gate, unit tests and codec smoke tests.
- Release jar content gate for metadata, icon, mixin config, license and third-party notices.
- Vendored dependency manifest and reproducible dependency download/check script.
- Proposed Linux and Windows GitHub Actions CI under `docs/maintenance/ci-workflow.yml`.

### Changed

- Release metadata now includes homepage, source and issue links, plus a separate maintainer
  contributor entry.
- Fabric / YACL / Kotlin dependency predicates are pinned to this branch's 1.21.11 versions
  instead of open `*` ranges.
- `maven-publish` was removed until a real Maven publication target and POM metadata are defined.

### Fixed

- `LICENSE` and `THIRD_PARTY_NOTICES.md` are packaged into the remapped release jar under
  `META-INF/`.
- Local binary dependencies now have pinned checksums, licenses, download URLs and
  release-jar inclusion status.

### Compatibility

- Minecraft: `=1.21.11`
- Fabric Loader: `>=0.19.3 <0.20.0`
- Fabric API: `>=0.141.6+1.21.11 <0.143.0` — as actually shipped by this tag. The upper cap was
  a mistake and is removed in `Beta-1-hotfix2`; anyone on Fabric API `0.143.0+1.21.11` or newer
  will be rejected by the Loader with this build and should upgrade.
- Java: `>=21`
- TaCZ Refabricated Unofficial runtime gate: `1.1.8+fabric.1.21.11.R<n>` with `n >= 2`; the R2 jar remains the compile/test/static-audit baseline.
- Fabric Language Kotlin: `>=1.13.13 <1.14.0`
- YetAnotherConfigLib: exactly `3.8.2+1.21.11-fabric`

### Known issues

- Dedicated-server startup remains a manual pre-release smoke check unless and until automated
  server startup is added to CI.
- Optional mod compatibility is statically present and version-bounded; it has not completed a
  full 1.21.11 in-game matrix.
- `compat.lsoCompat`, `compat.vsCollisionCompat`, `compat.vsExplosionCompat`, and
  `compat.mtsFix` remain in the config schema but have no verified 1.21.11 Fabric target.

### Dependency changes

- Vendored build/test input jar checksums are recorded in `RESOURCE_IMPORT_MANIFEST.tsv`.
- MixinExtras `0.5.4` is the only third-party library intentionally nested into this mod jar.

### Test matrix

- Required before release: `python3 scripts/download_dependencies.py --check-only`,
  `python3 scripts/check_release_consistency.py`, `python3 scripts/check_mod_icon.py`,
  `python3 scripts/audit_port.py --strict` with named/intermediary Minecraft jars and the
  generated refmap, and `./gradlew clean build --stacktrace` on JDK 21.
- Proposed CI runs Linux audit/build and Windows Gradle build.

### Upgrade notes

- `thirdPersonGunRenderingFix` is not present: TaCZ Refabricated 1.21.11 R2 already ships the
  third-person rendering fix.
- Dormant leftover compat keys may still appear in old JSON configs; they do not enable a
  1.21.11 integration.

### Checksums

- Fill in final release artifact SHA-256 values after producing the signed/uploaded jar and
  example pack.

### TaCZ R2+ gate
- The runtime friendly-string check accepts numeric revisions `R<n>` where `n >= 2`, including hotfix/prerelease suffixes, only for `1.1.8+fabric.1.21.11`. R10 uses numeric comparison. Wrong Minecraft/core/family and pre-R2 versions remain rejected. Future builds still require descriptor, client, and server validation.

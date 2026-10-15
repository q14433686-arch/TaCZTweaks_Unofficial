# Changelog

All notable changes to this unofficial Fabric port are tracked here. Release entries should
be user-facing and concise; keep detailed process notes in `docs/maintenance/` or the
handoff/audit documents.

## 2.14.2+fabric.26.1.2.Beta-1-hotfix2 - Unreleased

### Fixed

- Removed the artificial upper bound from the Fabric API dependency: `fabric.mod.json` now
  declares `>=0.155.2+26.1.2` instead of `>=0.155.2+26.1.2 <0.157.0`. Synced from the 26.3
  line, where the same kind of cap made Fabric Loader reject a user's newer Fabric API as
  "incompatible". The minimum stays this line's own `0.155.2+26.1.2`, and Minecraft stays
  pinned to `=26.1.2`. On 26.1.2 this is preventive: the newest Fabric API published for
  26.1.2 when this was changed, `0.155.3+26.1.2`, was still accepted by the old range.
- Removed the artificial upper bound from Fabric Language Kotlin (`>=1.13.13 <1.14.0` ->
  `>=1.13.13`) and the exact YetAnotherConfigLib pin (`=3.9.6+26.1-fabric` ->
  `>=3.9.6+26.1-fabric`). On the 26.2 line (Issue #33) these two shapes made Fabric Loader
  reject FLK `1.14.2+kotlin.2.4.21` and YACL `3.9.7+26.2-fabric`. This line declared the same
  shapes, so the already published FLK 1.14.0, 1.14.1 and 1.14.2 were rejected here too, as
  would be YACL 3.9.7's 26.1 build. The minimums stay the versions this build compiles against.
  FLK 1.14.x itself requires Fabric Loader 0.19.5 or newer.
- Hardened the shield-durability mixin against `BlocksAttacks#hurtBlockingItem` call-site
  drift (port of the 26.2 line's `IncorrectArgumentCountException` fix): both the vanilla
  5-parameter shape and the 6-parameter shape with a trailing `int` are wrapped with
  `require = 0`, each handler passes `original` exactly its own argument count, and a one-time
  warning is logged if shield rules resolved but no durability wrap ran. Merged after
  `Beta-1-hotfix` was published; first shipped in this build.

### Added

- GitHub Actions for this line: version/dependency consistency, the static port audit, a compile
  check, Linux and Windows Gradle builds (the Linux jar is uploaded as an Actions artifact), and
  a tag-triggered GitHub Release workflow that also works for releases created in the web UI.
  These are the first active workflows on this line; the `Beta-1-hotfix` entry below listed CI,
  but only a proposed workflow file existed then.

### Changed

- The artifact version is now `2.14.2+fabric.26.1.2.Beta-1-hotfix2`, because `Beta-1-hotfix`
  was already published as the GitHub release `26.1.2_BETA_1_HOTFIX`.
- `scripts/check_release_consistency.py` now fails unless `depends.fabric-api` is exactly
  `>=<fabric_version>`, `depends.fabric-language-kotlin` is exactly `>=` plus the release part of
  `flk_version`, `depends.yet_another_config_lib_v3` is exactly `>=3.9.6+26.1-fabric` (tied to the
  YACL artifact resolved by `build.gradle.kts`), and the Fabric Loader / Fabric API / Fabric
  Language Kotlin versions used for the build satisfy their declared ranges. The new
  `scripts/test_dependency_ranges.py` covers these checks, including regression cases for the
  removed Fabric API and FLK caps and the removed YACL pin.

### Compatibility

- Fabric API: `>=0.155.2+26.1.2` (no upper bound).
- Fabric Language Kotlin: `>=1.13.13` (no upper bound; FLK 1.14.x needs Fabric Loader `>=0.19.5`).
- YetAnotherConfigLib: `>=3.9.6+26.1-fabric` (no longer an exact pin).
- Unchanged from `Beta-1-hotfix`: Minecraft `26.1.2`; Fabric Loader `>=0.19.3 <0.20.0`;
  Java `>=25`; TaCZ Refabricated Unofficial `1.1.8+fabric.26.1.2.R<n>` with `n >= 2`.

### Test matrix

- Required before release: `python3 scripts/audit_port.py --strict` with the TaCZ R2 jar
  restored, `python3 scripts/check_release_consistency.py`,
  `python3 scripts/test_dependency_ranges.py`, `./gradlew clean build --stacktrace`, and
  client / integrated-server / dedicated-server launches with the newest available 26.1.2
  Fabric API, Fabric Language Kotlin (1.14.2 on Fabric Loader `>=0.19.5`) and YACL 3.9.7 for
  26.1, plus one launch with the minimums (FLK `1.13.13`, YACL `3.9.6+26.1-fabric`).

## 2.14.2+fabric.26.1.2.Beta-1-hotfix - 2026-08-22

### Added

- Fabric 26.1.2 port of TaCZ Tweaks v2.14.2 for `TaCZ_Refabricated_Unofficial` R2.
- YACL v3 configuration UI, Mod Menu entrypoint, JSON persistence and server configuration sync.
- Gun movement, reload, unload, manual bolt, underwater-fire, hit-marker and modifier features.
- Data-driven bullet interactions, bullet sounds, bullet particles and melee interactions.
- Optional compatibility for Sound Physics Remastered (1.5.1+26.1.2), First Aid New (1.2.8) and Pillager's Gun (3.2.2).
- Example reloadable gun pack artifact.
- Static port audit, icon/license checksum gate, unit tests and codec smoke tests.
- Release jar content gate for metadata, icon, mixin config, license and third-party notices.
- Vendored dependency manifest and reproducible dependency download/check script.
- Linux and Windows GitHub Actions CI.

### Changed

- Forge-only or unavailable compatibility toggles were removed instead of being kept as no-op
  options: `thirdPersonGunRenderingFix`, `lsoCompat`, `mtsFix`, `vsCollisionCompat`, and
  `vsExplosionCompat`.
- `maven-publish` was removed until a real Maven publication target and POM metadata are defined.

### Fixed

- The runtime TaCZ version gate no longer compares the exact friendly string against a single
  pinned R2 build. It now accepts `1.1.8+fabric.26.1.2.R<n>` for `n >= 2` (including suffix
  builds such as `R2-hotfix` and later revisions such as `R3`, `R10`), rejecting only the wrong
  Minecraft version, wrong TaCZ core version, wrong Fabric release family, pre-R2 revisions and
  malformed strings. `R10` is compared numerically so it is never mistaken for a version below
  `R2`.
- The self/host artifact version was renamed to `2.14.2+fabric.26.1.2.Beta-1-hotfix` to mark this
  as a hotfix revision of the same 26.1.2 beta line.
- Release metadata now includes homepage, source and issue links, plus a separate maintainer
  contributor entry.
- `LICENSE` and `THIRD_PARTY_NOTICES.md` are packaged into the release jar under `META-INF/`.
- Local binary dependencies now have pinned SHA-256 checks and license/source documentation.

### Compatibility

- Minecraft: `26.1.2`
- Fabric Loader: `>=0.19.3 <0.20.0`
- Fabric API: `>=0.155.2+26.1.2 <0.157.0`
- Java: `>=25`
- TaCZ Refabricated Unofficial: `1.1.8+fabric.26.1.2.R<n>` with `n >= 2` (R2 baseline, hotfix
  builds, and later R<n> revisions in the same release family; the Minecraft version, TaCZ core
  version and Fabric release family remain strict. Future builds must still pass the matching
  descriptor, client and server validation before being claimed as tested.)
- Fabric Language Kotlin: `>=1.13.13 <1.14.0`
- YetAnotherConfigLib: exactly `3.9.6+26.1-fabric`

### Known issues

- Dedicated-server startup remains a manual pre-release smoke check unless and until automated
  server startup is added to CI.
- Optional mod compatibility is statically audited and selectively tested; publish notes must not
  claim unperformed full game-playthrough validation.

### Dependency changes

- Vendored build/test input jar checksums are recorded in `RESOURCE_IMPORT_MANIFEST.tsv`.
- MixinExtras `0.5.4` is the only third-party library intentionally nested into this mod jar.

### Test matrix

- Required before release: `python3 scripts/audit_port.py --strict`,
  `python3 scripts/check_mod_icon.py`, `python3 scripts/download_dependencies.py --check-only`,
  and `./gradlew clean build --stacktrace`.
- CI runs Linux full audit/build and Windows Gradle build.

### Upgrade notes

- Remove stale deleted options from old config files if present; they are intentionally ignored by
  this Fabric port because the corresponding 26.1.2 targets are not available or already fixed.

### Checksums

- Fill in final release artifact SHA-256 values after producing the signed/published jar and zip.

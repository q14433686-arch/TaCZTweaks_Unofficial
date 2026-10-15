# Changelog

All notable changes to the **`26.2(main)` Fabric 26.2** line are tracked here. Other Git
branches keep their own changelogs. Release entries should be user-facing and concise; keep
detailed process notes in `docs/maintenance/` or the handoff/audit documents.

## 2.14.2+fabric.26.2.Beta-1-hotfix2 - Unreleased

### Fixed

- Backported the Fabric 26.3 dependency-range fix to this **26.2** line: Fabric API now
  requires `>=0.155.2+26.2` without an artificial upper cap that could reject newer
  Fabric API releases; Minecraft remains exactly `26.2`.
- Fabric Loader now requires `>=0.19.3` without an arbitrary `<0.20.0` cap. This prevents
  the metadata from preemptively rejecting newer Loader releases; their runtime behavior
  remains unverified until a matching client/server startup test is performed.
- Addressed the other two loader rejections in Issue #33: FLK now requires `>=1.13.13`
  and YACL `>=3.9.6+26.2-fabric`, both without an upper bound. The reported 26.2 versions
  FLK `1.14.2+kotlin.2.4.21` and YACL `3.9.7+26.2-fabric` satisfy these ranges; actual
  client/server behavior with those newer dependencies still needs in-game verification.
- The release-consistency gate now checks that the built Fabric Loader / Fabric API / Fabric
  Language Kotlin versions satisfy the published ranges, and that the TaCZ runtime gate's
  **26.2 R2** family and minimum agree with `fabric.mod.json`. It rejects mismatches before
  a build is published instead of waiting for a launch-time failure.
- Added an upstream dependency-availability check, regression tests for both gates, and
  CI workflows for audit, compilation, full build, and version consistency. The shield
  optional-target audit self-test now runs under pytest without exiting during import.

### Validation

- Dependency availability requires access to Modrinth; offline `SKIP` does not count as a
  successful upstream check. A baseline CI build does not verify runtime behavior with
  newer Loader/FLK/YACL; client and server smoke tests with those versions are still required.

## 2.14.2+fabric.26.2.Beta-1-hotfix - 2026-08-22

### Documentation

- Documented the six maintained release branches (Fabric/NeoForge × 26.2 / 26.1.2 / 1.21.11)
  and aligned issue templates, PR template and publish copy with that layout.

### Added

- Fabric 26.2 port of TaCZ Tweaks v2.14.2 for `TaCZ_Refabricated_Unofficial` R2.
- YACL v3 configuration UI, Mod Menu entrypoint, JSON persistence and server configuration sync.
- Gun movement, reload, unload, manual bolt, underwater-fire, hit-marker and modifier features.
- Data-driven bullet interactions, bullet sounds, bullet particles and melee interactions.
- Optional compatibility for Sound Physics Remastered, First Aid New and Pillager's Gun.
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

- Shield durability/disable overrides no longer crash the game (`MixinExtras
  IncorrectArgumentCountException` on shield block, e.g. when a creeper explodes while
  blocking): `LivingEntityMixin` now wraps both known 26.2 `BlocksAttacks#hurtBlockingItem`
  call-site variants (vanilla 5-argument and the extra-`fixedDamage` variant used by patched
  builds), each handler passes exactly its own call site's argument count, and a future
  unknown variant degrades to a one-time logged warning instead of a hard failure.
- TaCZ `1.1.8+fabric.26.2.R2-hotfix` is accepted alongside the pinned R2 build; the previous
  startup guard no longer rejects the official hotfix release.
- Release metadata now includes homepage, source and issue links, plus a separate maintainer
  contributor entry.
- `LICENSE` and `THIRD_PARTY_NOTICES.md` are packaged into the release jar under `META-INF/`.
- Local binary dependencies now have pinned SHA-256 checks and license/source documentation.

### Compatibility

- Minecraft: `26.2`
- Fabric Loader: `>=0.19.3 <0.20.0`
- Fabric API: `>=0.155.2+26.2 <0.157.0`
- Java: `>=25`
- TaCZ Refabricated Unofficial: `1.1.8+fabric.26.2.R2` and later `R<n>` builds in the same
  release family
- Fabric Language Kotlin: `>=1.13.13 <1.14.0`
- YetAnotherConfigLib: exactly `3.9.6+26.2-fabric`

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
  this Fabric port because the corresponding 26.2 targets are not available or already fixed.

### Checksums

- Fill in final release artifact SHA-256 values after producing the signed/uploaded jar and
  example pack.

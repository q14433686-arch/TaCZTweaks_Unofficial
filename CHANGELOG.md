# Changelog

All notable changes to this unofficial Fabric 1.21.11 port are tracked here. Release entries
should be user-facing and concise; keep detailed process notes in `docs/maintenance/` or
`PORTING_NOTES.md`.

## 2.14.2+fabric.1.21.11.Beta-1-hotfix - Unreleased

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
- Fabric API: `>=0.141.6+1.21.11 <0.143.0`
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

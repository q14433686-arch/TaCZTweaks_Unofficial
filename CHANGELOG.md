# Changelog

All notable changes to this unofficial Fabric port are tracked here. Release entries should
be user-facing and concise; keep detailed process notes in `docs/maintenance/` or the
handoff/audit documents.

## 2.14.2+fabric.26.2.Beta-1 - Unreleased

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
- TaCZ Refabricated Unofficial: exactly `1.1.8+fabric.26.2.R2` or
  `1.1.8+fabric.26.2.R2-hotfix`
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

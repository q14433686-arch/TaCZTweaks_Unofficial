# Changelog

All notable changes to this unofficial Fabric port are tracked here. Release entries should
be user-facing and concise; keep detailed process notes in `docs/maintenance/` or the
handoff/audit documents.

## 2.14.2+fabric.1.21.11.Beta-1 - Unreleased

### Added

- Fabric 1.21.11 port of TaCZ Tweaks v2.14.2 for `TaCZ_Refabricated_Unofficial`
  `1.1.8+fabric.1.21.11.R2`.
- YACL v3 configuration UI, Mod Menu entrypoint, JSON persistence and server configuration sync.
- Gun movement, reload, unload, manual bolt, underwater-fire, hit-marker and modifier features.
- Data-driven bullet interactions, bullet sounds, bullet particles and melee interactions.
- Optional compatibility for Sound Physics Remastered, First Aid New and Pillager's Gun.
- Example reloadable gun pack artifact.
- Static port audit, icon/license checksum gate, unit tests and codec smoke tests.
- Release jar content gate for metadata, icon, mixin config, license and third-party notices.
- Vendored dependency manifest and reproducible dependency download/check script.
- Linux and Windows GitHub Actions CI (proposed workflow in `docs/maintenance/ci-workflow.yml`).

### Changed

- Release metadata now includes homepage, source and issue links, plus a separate maintainer
  contributor entry.
- `LICENSE` and `THIRD_PARTY_NOTICES.md` are packaged into the release jar under `META-INF/`.
- `checkJarContents` targets the Loom-remapped release jar (this branch runs
  `fabric-loom-remap` with Mojang mappings because Minecraft 1.21.11 is obfuscated).
- `maven-publish` was removed until a real Maven publication target and POM metadata are defined.

### Fixed

- Local binary dependencies now have recorded source/license/version entries in
  `RESOURCE_IMPORT_MANIFEST.tsv` and `LICENSES.md` instead of relying on mutable filenames.

### Compatibility

- Minecraft: `1.21.11`
- Fabric Loader: `>=0.19.3`
- Fabric API: `*` (pinned build `0.141.6+1.21.11` in `gradle.properties`)
- Java: `>=21`
- TaCZ Refabricated Unofficial: exactly `1.1.8+fabric.1.21.11.R2`
- Fabric Language Kotlin: `>=1.13.13` (pinned build `1.13.13+kotlin.2.4.10`)
- YetAnotherConfigLib: `*` (pinned vendored build `3.8.2+1.21.11-fabric`)

### Known issues

- Dedicated-server startup remains a manual pre-release smoke check unless and until automated
  server startup is added to CI.
- Optional mod compatibility is statically audited and selectively tested; publish notes must not
  claim unperformed full game-playthrough validation.

### Dependency changes

- Vendored build/test input jar checksums are recorded in `RESOURCE_IMPORT_MANIFEST.tsv`.
  Both rows are currently marked `PENDING` because the sandbox that produced this branch could
  not reach the GitHub release-asset CDN or the Modrinth CDN; pin them with a networked run of
  `python3 scripts/download_dependencies.py` before release.
- MixinExtras `0.5.4` is the only third-party library intentionally nested into this mod jar.

### Test matrix

- Required before release: `python3 scripts/audit_port.py --strict`,
  `python3 scripts/check_mod_icon.py`, `python3 scripts/download_dependencies.py --check-only`,
  and `./gradlew clean build --stacktrace`.
- CI runs Linux full audit/build and Windows Gradle build.

### Upgrade notes

- Remove stale deleted options from old config files if present; this 1.21.11 port keeps the
  legacy compatibility switches that still have valid runtime targets (`lsoCompat`,
  `vsCollisionCompat`, `vsExplosionCompat`, `mtsFix`) and ignores unknown keys.

### Checksums

- Fill in final release artifact SHA-256 values after producing the signed/uploaded jar and
  example pack.

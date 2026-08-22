# Changelog

All notable changes to this unofficial port are tracked here. Detailed evidence belongs in
`docs/records/`; this file does not upgrade unperformed tests into support claims.

## 2.14.2+neoforge.26.2.Beta-1 - Unreleased (source/static port; build and game gates pending)

NeoForge 26.2 loader port of the Fabric `26.2(main)` gameplay line, using the client-load-tested
26.1.2 NeoForge port as its loader skeleton. Targets NeoForge 26.2.0.64 / Java 25 and
TaCZ: Renovated `1.1.8+neoforge.26.2.R1`.

### Changed

- Build target: Minecraft `26.2`, NeoForge `26.2.0.64`, ModDevGradle `2.0.144`, Java 25.
- Release identity: NeoForge 26.2 Beta-1 metadata, README, BUILD guide, compatibility matrix,
  known issues, release scripts and local dependency manifest.
- Dependency family: TaCZ: Renovated 26.2 R1; YACL 3.9.5+26.2 NeoForge; Sound Physics
  1.5.1+26.2 NeoForge; First Aid 1.3.x NeoForge 26.2; Pillager's Gun 3.3.5 NeoForge 26.2.
- Version gate: strict `1.1.8+neoforge.26.2.R<n>`, minimum R1 and numeric revision comparison.
- Gameplay/API delta: applied the Fabric 26.1.2 -> 26.2 business-logic changes without restoring
  Fabric lifecycle, networking, registry or metadata APIs.
- Client isolation: moved bootstrap to a physical-client `@Mod(..., dist = Dist.CLIENT)` entry;
  moved the YACL screen builder to a client package; routed payload client work through a
  reflection bridge matching TaCZ: Renovated 26.2. Common payload handlers no longer reference
  `net.minecraft.client.*` classes.
- YACL metadata is required on BOTH sides because common persistence/sync types extend YACL config
  classes; only the screen builder is client-only.
- Mixin comments/targets and data/selector docs updated for Minecraft 26.2 naming.

### Verified statically

- TaCZ: Renovated 26.2 branch reports `mod_version=1.1.8+neoforge.26.2.R1` and
  `neo_version=26.2.0.64`.
- NeoForge 26.2.x still provides `BreakBlockEvent(Level, BlockPos, BlockState, Player)`.
- NeoForge 26.2.x still patches `LivingEntity#applyItemBlocking` to invoke the six-argument
  `BlocksAttacks#hurtBlockingItem(..., float, int)` overload; the six-argument wrap remains the
  primary path and the five-argument wrap remains an optional fallback.
- First Aid, Pillager's Gun and Sound Physics source surfaces used by optional integrations were
  located for their 26.2 lines. This is not shipped-jar or gameplay verification.

### Known gaps

- `./gradlew test` and `./gradlew build` have not run in this sandbox: JDK 25 is absent and
  required binary endpoints are unreachable.
- Client main-menu startup, dedicated-server `Done (...)!`, and the gun/ADS/reload/unload/config/
  datapack-reload smoke scenarios are pending.
- New dependency SHA-256 values marked `pending` in `RESOURCE_IMPORT_MANIFEST.tsv` must be filled
  from actual downloaded files before release.
- NeoForge block-break protection remains a documented semantic downgrade from Fabric's
  BEFORE/CANCELED/AFTER event chain.

## 2.14.2+fabric.26.1.2.Beta-1-hotfix - Unreleased

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

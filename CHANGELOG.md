# Changelog

All notable changes to this unofficial port are tracked here. Release entries should
be user-facing and concise; keep detailed process notes in `docs/records/`,
`docs/maintenance/` or the handoff/audit documents.

## 2.14.2+neoforge.26.1.2.Beta-1 - Unreleased (source port, not yet built or tested)

Loader port of the Fabric `26.1.2` branch (`2.14.2+fabric.26.1.2.Beta-1-hotfix`) to
NeoForge 26.1.2.97 / Java 25, targeting
[TaCZ: Renovated](https://github.com/q14433686-arch/TaCZ_Renovated) `1.1.8+neoforge.26.1.2.R1`.

### Changed

- Build: Fabric Loom -> ModDevGradle 2.0.144 (`build.gradle`, Groovy), Java 25 toolchain,
  kotlin-stdlib embedded via `jarJar` (NeoForge has no Fabric Language Kotlin).
- Metadata: `fabric.mod.json` -> `src/main/templates/META-INF/neoforge.mods.toml`.
- Entrypoint: `ModInitializer`/`ClientModInitializer` -> `@Mod` constructor plus a
  `Dist.CLIENT`-gated client bootstrap.
- Events: server tick / stop / login / logout / block break / `GunShootEvent` now run on the
  NeoForge event bus; data-pack reload uses `AddServerReloadListenersEvent`.
- Networking: three-stage Fabric registration collapsed into one
  `RegisterPayloadHandlersEvent`; handlers take `IPayloadContext` and use `enqueueWork`.
- Registries: mob effect registration moved to `DeferredRegister`.
- Config screen: Mod Menu entrypoint replaced by the `IConfigScreenFactory` extension point.
- Version gate: new strict `TaczVersionSupport` accepting `1.1.8+neoforge.26.1.2.R<n>` (n >= 1).
- Mixin config: `compatibilityLevel` raised to `JAVA_25`; the mixin plugin now queries the
  NeoForge loading mod list.
- Audit/consistency scripts retargeted at the NeoForge metadata, jar and build script.

### Removed

- Fabric-only sources and gates: `ModMenuApiImpl`, `fabric.mod.json`, Loom build scripts, and
  the unit tests that required a bootstrapped Minecraft (codec / example-pack smoke tests).

### Known gaps

- Not compiled, not smoke-tested on a dedicated server, not tested in game.
- `libs/` dependency digests are still `pending` in `RESOURCE_IMPORT_MANIFEST.tsv`.
- Block-break protection is a semantic downgrade: NeoForge has no
  `PlayerBlockBreakEvents.CANCELED/AFTER` equivalent.
- Optional-mod compatibility (Sound Physics, First Aid, Pillager's Gun, LRTactical) is wired
  but has not been checked against the real NeoForge jars.

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

# Changelog

All notable changes to the **`26.3` Fabric 26.3** line are tracked here. Other Git
branches keep their own changelogs. Release entries should be user-facing and concise; keep
detailed process notes in `docs/maintenance/` or the handoff/audit documents.

## 2.14.2+fabric.26.3.Beta-1 - Unreleased

> Status: dependency coordinates and three source-verified API breaks are ported. **Not yet
> compiled by CI and not tested in-game**; there is no downloadable build for this line.

### Changed

- Retargeted the line to Minecraft 26.3: Fabric Loader 0.19.5, Fabric API 0.160.7+26.3,
  TaCZ Refabricated `1.1.8+fabric.26.3.R1`, YACL `3.9.7+26.3-fabric`, Mod Menu 21.0.0-beta.1.
- `EntityBulletRendererMixin`: 26.3 added a trailing `float partialTicks` to
  `EntityRenderer#shouldRender`, and TaCZ's override follows it; the injector signature now
  carries the extra parameter.
- `GunSoundInstanceMixin`: 26.3 renamed `SoundInstance#resolve` to `getOrResolve`; the
  injection target was renamed accordingly.
- Key bindings moved to the 26.3 `InputConstants` names (`Type.KEYSYM` -> `Type.KEYBOARD`,
  `GLFW.GLFW_KEY_U` -> `InputConstants.KEY_U`); the LWJGL import is gone.
- `libs/*.jar` are no longer committed. They are reconstructed from
  `RESOURCE_IMPORT_MANIFEST.tsv` by `scripts/download_dependencies.py`, which every CI job
  runs first.

### Added

- Four GitHub Actions workflows (`consistency`, `audit`, `compile-check`, `build`). The
  development sandbox can only reach `api.github.com`, so Actions is the only place this
  project can be compiled; `compile-check` writes its log back to `build-reports/` on
  `arena/**` branches so a restricted sandbox can read compile errors.
- `download_dependencies.py --print-sha256` / `--require-pinned`, and
  `check_release_consistency.py --require-deps`, so the manifest can carry a not-yet-pinned
  checksum without either silently passing or blocking the port.

### Known gaps

- The YACL manifest row still carries `UNVERIFIED_PENDING_CI` instead of a SHA-256; the first
  CI run prints the real digest to pin.
- Only breaks provable from the TaCZ 26.3 sources were applied. Anything that needs a
  compiler or the game (mixin descriptors against vanilla 26.3, renderer/Iris behaviour) is
  still unverified.

## 2.14.2+fabric.26.2.Beta-1-hotfix - 26.2 line (historical)

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

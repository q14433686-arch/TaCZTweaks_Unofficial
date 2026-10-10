# Changelog

All notable changes to this unofficial port are tracked here. Detailed evidence belongs in
`docs/records/`; this file does not upgrade unperformed tests into support claims.

## 2.14.2+neoforge.26.3.Beta-1 - Unreleased (compile-level port; NOT build- or gameplay-verified)

NeoForge 26.3 loader port of the 26.2 NeoForge line (itself the client-load-tested skeleton),
re-targeted with the sister Fabric 26.3 line's port guide and the TaCZ_Renovated 26.2→26.3 diff
as authoritative inputs. Targets Minecraft `26.3` / NeoForge `26.3.0.51-beta` / Java 25 and
TaCZ: Renovated `1.1.8+neoforge.26.3.R1`.

**Status red line:** this entry is a *compile-level* port produced in a sandbox without JDK or
outbound build network. `gradlew build`, dedicated-server boot and all gameplay verification are
still open; CI workflows added in this round run the compile gates. Do not treat anything below
as tested behaviour.

### Changed

- Build target: Minecraft `26.3`, NeoForge `26.3.0.51-beta` (26.3 is beta-only; pin follows
  TaCZ_Renovated's 26.3 re-pin — FML 12.0.8 removed `ModConfig.Type.COMMON/SERVER`, so older
  26.3 betas are binary-incompatible), ModDevGradle `2.0.144`, Java 25.
- Release identity: NeoForge 26.3 Beta-1 metadata, README, BUILD guide, compatibility matrix,
  release scripts, version gate and local dependency manifest.
- Dependency family: TaCZ: Renovated 26.3 R1 (`tacz-1.1.8+neoforge.26.3.R1.jar`); YACL 3.9.x for
  26.3 (metadata floor drops 3.9.5 → 3.9.4 to match upstream `port/26.3` `modVersion`);
  Sound Physics `1.5.1+26.3` NeoForge; First Aid / Pillager's Gun remain optional with
  unconfirmed 26.3 artifacts.
- Version gate: strict `1.1.8+neoforge.26.3.R<n>`, minimum R1; the previous
  `1.1.8+neoforge.26.2.R3-hotfix2` build is now rejected (unit-tested).
- `EntityBulletRendererMixin`: handler gains the trailing `float partialTicks` parameter —
  vanilla `EntityRenderer#shouldRender` signature changed in 26.3.
- `GunSoundInstanceMixin`: target renamed `resolve` → `getOrResolve` (vanilla rename adopted by
  TaCZ's `GunSoundInstance` in 26.3).
- Key bindings: `InputConstants.Type.KEYSYM` removed in 26.3 — `UnloadKey`, `ReduceSensitivityKey`
  and `TiltGunKey` now use `Type.KEYBOARD`; `UnloadKey` binds `InputConstants.KEY_U` instead of
  `GLFW.GLFW_KEY_U`.
- CI: drafted `.github/workflows/` (compile-check, build, consistency, release) mirroring the
  TaCZ_Renovated port-repo layout, plus `scripts/fetch_ci_dependencies.py` to populate `libs/`
  from GitHub Releases / Modrinth / Maven Central on runners. The sandbox token lacks the
  `workflows` scope, so the four files ship as ready-to-paste drafts committed under `docs/ci/`
  (deployment steps in `docs/ci/README.md` and each file header).

### Fixed (first CI compile run, 2026-10-10/11)

- `Config.reloadDiscardsMagazineExclusions`: 26.3 removed
  `FriendlyByteBuf#writeCollection`/`readCollection` (same batch as `readMap`/`writeMap`);
  the syncable encoder/decoder now writes a varint length plus per-element `writeUtf` /
  reads them back — wire format unchanged, same approach as TaCZ_Renovated 26.3's
  `BufMapCodec`.
- `ClientMessagePlayerUnload`: NeoForge removed `ItemHandlerHelper.giveItemToPlayer`;
  returned ammo now goes through vanilla `Inventory#placeItemBackInInventory(ItemStack,
  Prediction.SERVER_ONLY)` (26.3's new `Prediction` parameter), matching TaCZ_Renovated
  26.3's own usage.
- `EnderManMixin`: vanilla renamed `EnderMan` → `Enderman` in 26.3 (package unchanged);
  import and `@Mixin` target updated. `hurtServer` and its first `IS_PROJECTILE` check
  re-verified in place; `EntityTypes.ENDERMAN` still exists for the bullet-side mixin.
- CI fetch script: retry/backoff for transient Modrinth/GitHub failures plus a CI
  transcript so a fetch failure no longer re-uploads a stale compile log.

### Verified statically (26.3 decompiled sources + NeoForge 26.3.x patches, 2026-10-10)

- All 7 TaCZ classes changed between 26.2 and 26.3 that this mod targets were diffed:
  `EntityKineticBullet` (body-only change: `invulnerableTime=0` → `DamageCooldownUtil.clear`;
  constructor/targeted methods unchanged), `LocalPlayerDraw#resetData`, `GunSmithTableScreen`
  (`isSuitableForMainHand`, `mouseScrolled` unchanged), `GunSoundInstance` (constructor
  unchanged), `EntityBulletRenderer#shouldRender` (handled above), `RefitKey#onRefitPress`
  (body-only), `CameraSetupEvent#initialCameraRecoil` (targets unchanged). All other targeted
  TaCZ classes are byte-identical between the two branches.
- Vanilla targets re-verified against 26.3 sources: `MouseHandler#handleAccumulatedMovement` /
  `turnPlayer(D)`, `AvatarRenderer#setupRotations(AvatarRenderState,PoseStack,F,F)` incl.
  `isVisuallySwimming` and the single `translate(FFF)` call site, `SoundBufferLibrary`
  (`getCompleteBuffer` / `lambda$getCompleteBuffer$1(Identifier)SoundBuffer` shape / `clear`),
  `LivingEntity#applyItemBlocking(ServerLevel,DamageSource,float)` with the NeoForge
  six-argument `hurtBlockingItem` patch hunk identical on 26.2.x and 26.3.x,
  `EnderMan#hurtServer` (first `DamageSource.is(TagKey)` remains `IS_PROJECTILE`),
  `EnchantmentHelper#getDamageProtection(ServerLevel,LivingEntity,DamageSource)`,
  `ClipContext#collisionContext`, `PoseStack#translate(FFF)`.
- This mod does not use any of the other 26.3 breaking changes listed in the sister port guide
  (`mulPose`, `Player#swing`, `invulnerableTime`, `openUri`, `ItemInHandRenderer` split,
  render-pass ownership, shaders, loot-table schema, recipe sync, `ModConfig.Type.*`).

### Open verification

- `gradlew build` + unit tests on CI (first run pending).
- `scripts/audit_port.py --strict` against the real 26.3 jars (sandbox had no jars; target-class
  checks currently downgrade to WARN without them).
- Gameplay matrix per `docs/records/NEOFORGE_26_3_PORT_RECORD.md` §7 (client, dedicated server,
  optional-compat entries) — not started.

## 2.14.2+neoforge.26.2.Beta-1 - Unreleased (build PASS; client gameplay smoke-tested)

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

### Maintainer validation

- Windows / JDK 25 `gradlew build`: **PASS**, including unit tests and Gradle release gates.
- The built client entered the game; the maintainer reports that most core in-game functions passed
  practical testing. This is non-exhaustive Beta smoke coverage, not a guarantee for every feature,
  configuration combination, datapack, or optional integration.

### Known gaps

- Dedicated-server `Done (...)!` has not been reported.
- The gameplay report did not include a per-scenario transcript, so gun/ADS/reload/unload/config/
  datapack-reload and optional compatibility rows are not individually promoted to PASS.
- Dependency SHA-256 values still marked `pending` in `RESOURCE_IMPORT_MANIFEST.tsv` must be filled
  from actual downloaded files before release-quality provenance is complete.
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

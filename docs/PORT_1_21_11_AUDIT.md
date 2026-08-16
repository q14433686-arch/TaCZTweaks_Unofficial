# Minecraft 1.21.11 / TaCZ R2 port audit

Audit date: 2026-08-16  
Tweaks baseline: `MUKSC/TaCZTweaks` v3 at `2e4b84d`  
Target: Minecraft `1.21.11` Fabric, TaCZ `1.1.8+fabric.1.21.11.R2`  
Target source tag: `q14433686-arch/TaCZ_Refabricated_Unofficial:1.21.11_R2` (`37d2ecc`)

## 1. Scope and evidence

This audit covers every file changed from the v3 baseline, all Mixin entries selected by the
1.21.11 build, build/dependency metadata, common platform abstractions, networking, registries,
resource reloaders, commands, data-driven features, and client input/rendering bridges.

Evidence used:

- the complete TaCZ R2 source tree at the release tag;
- all three maintained TaCZ R2 branches for cross-branch API comparison;
- Minecraft 26.1.2's merged named jar for APIs shared with 1.21.11;
- Fabric API 0.141.6 Javadocs for the Resource Loader v1 transition;
- a real Windows `:1.21.11-fabric:build` result for the pre-audit `.unofficial.1` candidate:
  **BUILD SUCCESSFUL**;
- a post-audit `.unofficial.2` build attempt that reached Java compilation and exposed four
  unconverted `ResourceLocation` parameter lines in two newly multiline method declarations;
- real client startup traces through Fabric Mixin preparation, TaCZ packet registration,
  LRTactical interface transformation, and synced-data initialization.

The four post-audit Java errors are source-fixed by putting a Stonecutter replacement directive on
each affected physical parameter line. All 178 immediate-line replacement directives were then
checked for an ineffective/misplaced directive, with zero remaining. A fresh `.unofficial.2` build
is still required before the current tree can be called build-verified.

Compilation alone is not treated as runtime proof. Every result below says whether it is source-
verified, build-verified, startup-verified, gated, or still requires a gameplay smoke test.

## 2. Build and dependency audit

| Area | Result | Status |
|---|---|---|
| Stonecutter node | Added `1.21.11-fabric`; Java 21; resource format 75 | Build-verified |
| Gradle / Loom | Gradle 9.5.1, Loom remap 1.17.19, legacy Mixin AP + one shared refmap | Build-verified |
| Mappings | Official Mojang mappings only; no nonexistent 1.21.11 Parchment layer | Build-verified |
| TaCZ dependency | CurseMaven project `1627909`, file `8660664` | Resolution/build-verified |
| Forge Config API Port | CurseMaven file `7337130` (21.11.1) | Resolution/build-verified |
| Cloth Config | 21.11.153 supplied explicitly because artifact-only TaCZ metadata is non-transitive | Resolution/build-verified |
| YACL / Mod Menu | 3.8.2 / 17.x API line | Build-verified |
| Legacy Fabric nodes | Missing LGPL SimpleBedrockModel jars restored; optional VS graph made non-transitive | Configuration-verified |
| Artifact identity | `3.0.0-alpha.10.unofficial.2+1.21.11-fabric` | Audit-candidate version; `.1` was client-log verified |

The Gradle deprecation messages and `Cannot remap modifiers` message are warnings. They are not
used to dismiss any Mixin warning or startup failure.

## 3. Minecraft/Fabric API migration audit

| Change | Implementation | Status |
|---|---|---|
| `ResourceLocation` -> `Identifier` | Stonecutter replacements plus an explicit Identifier helper branch | Pre-audit build-verified; post-audit multiline fix source-verified, rebuild pending |
| `critereon` -> `criterion` | Package replacements for predicate codecs | Build-verified |
| Tool tiers | `Tier/Tiers` -> `ToolMaterial` codec for modern versions | Build-verified |
| Resource reload API | Local listener interface + Fabric Resource Loader v1 registration by explicit id | Build-verified; runtime reload pending |
| Registered key categories | `KeyMapping.Category.register(tacztweaks:mod)` | Build-verified |
| Toggle key constructor | Modern Category/BooleanSupplier constructor | Build-verified |
| Permissions | `PermissionSet` + `Permissions.COMMANDS_MODERATOR` | Build-verified |
| Inventory | `getNonEquipmentItems` | Build-verified |
| Loot context keys | `LootContextParam` -> `ContextKey` | Build-verified |
| Avatar rendering | PlayerRenderer path split to AvatarRenderer/render-state signatures | Source/descriptor verified; visual smoke pending |
| Blocking | Modern `BlocksAttacks` path with custom damage/durability/cooldown handling | Source/descriptor verified; gameplay smoke pending |
| Networking | Modern custom payload registry and 1.21.11 Identifier codecs | Build-verified; login/play smoke pending |

## 4. Mixin inventory audit

Current common config:

- 132 common Mixins;
- 31 client Mixins;
- 0 server-only Mixins;
- **163 unique entries total**;
- every listed class exists;
- no duplicates;
- Fabric-specific config contains one existing class;
- both configs use the generated `tacztweaks.refmap.json`;
- strict `required=true` / `defaultRequire=1` remains enabled.

Breakdown:

| Group | Count | 1.21.11 result |
|---|---:|---|
| Core | 143 | Audited below |
| Built-in/legacy LRTactical | 3 | Built-in interface active; 0.3/0.4 legacy handlers explicitly disabled |
| Sound Physics | 5 | Gated by absent optional mod in the reported client |
| Sable | 5 | 1.21.1-only source branch; gated/empty on 1.21.11 |
| Valkyrien Skies | 3 | 1.20.1-only source branch; gated/empty on 1.21.11 |
| FirstAid | 2 | Gated by absent optional mod |
| LSO | 1 | Gated by absent optional mod |
| MTS | 1 | Gated by absent optional mod |

### 4.1 Target subtype audit

All direct TaCZ/LRTactical targets were compared to their R2 declarations. The only behavior Mixin
whose target is an interface is `BuiltinMeleeWeaponMixin -> IMeleeWeapon`; it is now declared as an
interface Mixin. No second class-to-interface mismatch exists. Accessor Mixin interfaces targeting
ordinary classes are valid and were checked separately.

Accessor targets checked against R2 fields/methods:

- `EntityKineticBullet`: `pierce`, `explosion`, `onHitEntity`, `onHitBlock`;
- `InaccuracyType`: `isMove`;
- `LocalPlayerShoot`: `SHOOT_LOCKED_CONDITION`;
- `LocalPlayerDataHolder`: `player`;
- attachment `Modifier`: `addend`, `multiplier`, `function`.

### 4.2 Synthetic lambda audit

**No 1.21.11 core path intentionally targets a javac `lambda$...` method.**

Raw lambda selectors still present in source are confined to:

- pre-1.21.11 Stonecutter alternatives;
- standalone LRTactical 0.3/0.4 alternatives, explicitly disabled for the built-in R2 provider;
- optional Sound Physics compatibility, gated when that mod is absent.

R2 migrations use these named boundaries:

- `shootInternal`, `validateClientShoot`;
- `runShootCycle`, `spawnProjectiles`;
- `reloadWithDisplay`, `reloadWithIndex`;
- `shouldSlide`, `tickAutoBolt`, `startReload`;
- packet `handle` methods.

### 4.3 Internal-call/local-capture hardening

The following fragile paths were removed or rewritten during this audit:

| Former dependency | Replacement |
|---|---|
| packet `hasAttachmentLock` invocation + `@Local player` | packet `handle` HEAD cancellation |
| `ModernKineticGunItem#doPerLivingHurt` invocation and shared locals | one `doMelee` TAIL boundary with explicit cone evaluation |
| `Optional.ofNullable` in reload script dispatch | `startReload` HEAD boundary |
| `ShootKey` assignment expression + local player | `autoShoot` TAIL decision |
| RefitKey internal attachment-lock call + local player | `onRefitPress` HEAD guard |
| reload lambdas and field/invoke interception | `reloadWithDisplay/reloadWithIndex` method wrappers |
| auto-bolt internal `bolt()` interception | complete `tickAutoBolt` transaction guard |
| projectile `addFreshEntity` and cross-method shared locals | thread-scoped `spawnProjectiles` indices + constructor consumption |
| bullet sound hook inside `doBulletSpread` | `EntityKineticBullet#shootFromRotation` TAIL |
| bullet constructor property expressions | constructor TAIL field transforms |
| SoundEngine local `Sound` capture | direct `getCompleteBuffer(id)` operation wrapper |
| recoil state shared across three Mixins | explicit `RecoilState` snapshot in each spline wrapper |
| crawl cooldown expression | method-entry temporary state and RETURN restoration |
| dynamic attachment-slot locals | explicit Minecraft player/gun lookup |

Actual non-argument local captures used only for tooltip presentation on 1.21.11 were removed or gated;
core modifier cache behavior remains active. Remaining core `@Local` uses on the target are method-argument
captures, not compiler local-variable layout dependencies.

### 4.4 Remaining verified internal call sites

Some behavior fundamentally modifies an operation inside a method. These sites remain because no
public result can represent the same semantics. Each was checked against R2 source:

- manual-bolt suppression of `IClientPlayerGunOperator#bolt` inside `validateClientShoot`;
- player-specific damage/headshot reads inside `EntityKineticBullet#onHitEntity`;
- `GunRecoil#getSplineFunction`'s argument multiplier (argument capture only);
- `GunRecoil#genPitchSplineFunction/genYawSplineFunction` calls in `initialCameraRecoil`;
- `AttachmentType.values()` in `GunRefitScreen#addAttachmentTypeButtons`;
- Sound buffer lookup in `SoundEngine#play`;
- TaCZ MouseHandler and LocalPlayer handler names used by MixinSquared.

These are source/descriptor verified, but their visual/gameplay outcome remains part of smoke testing.

## 5. Runtime checkpoint audit

Observed successful checkpoints before the latest hardening patch:

- Fabric loaded the expected 1.21.11 dependency set;
- `tacztweaks.refmap.json` was read (there is no Tweaks missing-refmap warning);
- MixinExtras initialized;
- the LRTactical interface Mixin passed PREPARE after its subtype correction;
- TaCZ C2S packet classes transformed and registered after packet guards moved to `handle`;
- all listed TaCZ synced data keys registered;
- startup reached `ModItems.init()` and exposed the old modern-melee call-site injection, which is now
  replaced together with the other `ModernKineticGunItem` internal hook.

The Iris/Carry On/YACL missing-refmap messages and AMD RenderSystem information warning originate from
those mods/early renderer initialization. They are not being counted as Tweaks validation failures.

## 6. Non-Mixin code audit

- Registry wrappers now use non-null Kotlin generic bounds and explicit holder casts.
- Dynamic datapack registries use a typed registration helper rather than star-projection inference.
- Resource reloaders use explicit ids under Resource Loader v1.
- Broadcast-sound fanout uses the public server player list and same-level/range checks.
- Particle emission uses the modern two-boolean per-player overload.
- Block updates use the modern neighbor-update method.
- Commands use modern permissions and non-equipment inventory access.
- Unloading no longer modifies TaCZ's attachment-refit `dropAllAmmo` internals; it uses a dedicated,
  bounded implementation for magazine/chamber, dummy, fuel and creative cases.

## 7. Validation gates before release

The port must remain draft until all of the following are observed with the post-audit jar:

1. `:1.21.11-fabric:build` succeeds with no Tweaks Mixin target warnings.
2. Client reaches the title screen with the reported Iris/Sodium/REI/Carry On/PAL set.
3. Dedicated server reaches `Done` without loading client-only classes.
4. A world can be joined and `/reload` completes.
5. Shoot/reload/bolt/melee and packet refit/unload paths execute without injection errors.
6. Config sync, attributes, bullet interactions, particles/sounds and custom shield handling are smoked.
7. Built-in LRTactical melee and at least one knife pack are exercised.

Until these gates pass, the PR remains draft and no claim of release readiness is made.

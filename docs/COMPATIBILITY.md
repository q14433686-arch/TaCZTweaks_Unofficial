# Compatibility matrix

This matrix separates implementation state from evidence. Do not promote an integration from
static/build verification to game or server validation until the matching test has actually been
run for the stated versions.

| Project | Type | Supported range | Auto enabled | Current evidence | Known boundary |
|---|---|---|---:|---|---|
| Minecraft | hard platform | `1.21.11` | yes | compile/test/build | No other Minecraft versions are declared. |
| Fabric Loader | hard dependency | `>=0.19.3` | yes | compile/test/build | Loader API drift outside this range is not claimed. |
| Fabric API | hard dependency | `*` (pinned build `0.141.6+1.21.11`) | yes | compile/test/build | Keep release metadata and Gradle property aligned. |
| Fabric Language Kotlin | hard dependency | `>=1.13.13` (pinned build `1.13.13+kotlin.2.4.10`) | yes | compile/test/build | Runtime Kotlin stdlib comes through FLK. |
| TaCZ Refabricated Unofficial | hard dependency | exactly `1.1.8+fabric.1.21.11.R2` | required | compile/test/static descriptor audit | Mixin targets are pinned to R2 descriptors. |
| YetAnotherConfigLib | hard dependency | `*` (vendored build `3.8.2+1.21.11-fabric`) | required | compile/build/checksum | Runtime mod is not nested; users must install it. |
| Mod Menu | optional UI entry | `*` suggested; compiled against `17.0.0` | when present | compile-only | Absence must not break startup. |
| Sound Physics Remastered | optional compat | `>=1.5.1 <1.6.0` (1.21.11 Fabric: `fabric-1.21.11-1.5.1`) | when present | source/static/build; airspace codec fixture | Airspace behavior depends on its runtime ray data. |
| First Aid New | optional compat | `>=1.2.5 <1.3.0` (1.21.11 Fabric: `firstaid-1.2.5+fabric1.21.11-legacy.jar`) | config controlled | source/static/build | This branch does not redistribute First Aid shader copies. |
| Pillager's Gun | optional compat | `>=3.2.2 <3.3.0` (1.21.11 Fabric line) | when present | source/static/build | Friendly-fire behavior follows that mod's config. |
| Legendary Survival Overhaul | optional compat | 1.21.11 target when present (`compat.lsoCompat`) | config controlled | source/static/build | Not a hard dependency; switch is a no-op without the mod. |
| Valkyrien Skies | optional compat | 1.21.11 target when present (`compat.vsCollisionCompat` / `vsExplosionCompat`) | config controlled | source/static/build | Not a hard dependency; switches are no-ops without the mod. |
| MTS / Immersive Vehicles | optional compat | 1.21.11 target when present (`compat.mtsFix`) | config controlled | source/static/build | Not a hard dependency; switch is a no-op without the mod. |

## Evidence categories

- **Source/static**: mixin descriptors, target classes, config uses, or package names were checked.
- **Compile/test/build**: `./gradlew build` and unit/codec tests pass against pinned dependencies.
- **Game validation**: a client or integrated server scenario was launched and manually exercised.
- **Dedicated-server validation**: a headless server was launched and checked with
  `scripts/check_server_log.py`.

Release notes must state which categories were completed for that release. On this 1.21.11
branch, the real-game and dedicated-server matrices are still pending manual validation.

# Compatibility matrix

This matrix is for the Fabric **1.21.11** branch. It separates implementation state from
evidence. Do not promote an integration from static/build verification to game or server
validation until the matching test has actually been run for the stated versions.

| Project | Type | Supported range | Auto enabled | Current evidence | Known boundary |
|---|---|---|---:|---|---|
| Minecraft | hard platform | `=1.21.11` | yes | compile/test/build intended; obfuscated + Loom remap | No other Minecraft versions are declared. |
| Fabric Loader | hard dependency | `>=0.19.3 <0.20.0` | yes | compile/test/build intended | Loader API drift outside this range is not claimed. |
| Fabric API | hard dependency | `>=0.141.6+1.21.11 <0.143.0` | yes | compile/test/build intended | Keep release metadata and Gradle property aligned. |
| Fabric Language Kotlin | hard dependency | `>=1.13.13 <1.14.0` | yes | compile/test/build intended | Runtime Kotlin stdlib comes through FLK. |
| TaCZ Refabricated Unofficial | hard dependency | exactly `1.1.8+fabric.1.21.11.R2` | required | compile/test/static descriptor audit | Mixin targets are pinned to 1.21.11 R2 descriptors. |
| YetAnotherConfigLib | hard dependency | exactly `3.8.2+1.21.11-fabric` | required | compile/build/checksum | Runtime mod is not nested; users must install it. |
| Mod Menu | optional UI entry | `*` suggested; compiled against `17.0.0` | when present | compile-only | Absence must not break startup. |
| Sound Physics Remastered | optional compat | `>=1.5.1 <1.6.0`; verified line `fabric-1.21.11-1.5.1` | when present | source/static; airspace codec fixture | Airspace behavior depends on its runtime ray data. Not a completed playthrough. |
| First Aid New | optional compat | `>=1.2.5 <1.3.0` Fabric 1.21.11 legacy | config controlled | source/static | This is the 1.2.5 legacy jar, not the 26.2 1.3.x shader-override path. |
| Pillager's Gun | optional compat | `>=3.2.2 <3.3.0`; verified line `pillagers_gun-3.2.2 fabric 1.21.11.jar` | when present | source/static | `3.3.x` belongs to later 26.x lines, not this branch. |
| Legendary Survival Overhaul | no current target | unsupported | no | documented absence / dormant config key | `compat.lsoCompat` is unwired. |
| Valkyrien Skies | no current target | unsupported | no | documented absence / dormant config keys | `compat.vsCollisionCompat` and `compat.vsExplosionCompat` are unwired. |
| MTS / Immersive Vehicles | no current target | unsupported | no | documented absence / dormant config key | `compat.mtsFix` is unwired. |

## Evidence categories

- **Source/static**: mixin descriptors, target classes, config uses, or package names were checked.
- **Compile/test/build**: `./gradlew build` and unit/codec tests pass against pinned dependencies.
- **Game validation**: a client or integrated server scenario was launched and manually exercised.
- **Dedicated-server validation**: a headless server was launched and checked with
  `scripts/check_server_log.py`.

Release notes must state which categories were completed for that release. This branch has
static/code coverage for the optional mods above; dedicated-server and full optional-mod
playthroughs remain incomplete unless a later changelog says otherwise.

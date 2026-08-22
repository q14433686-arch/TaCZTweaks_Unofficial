# Compatibility matrix

This matrix separates implementation state from evidence. Do not promote an integration from
static/build verification to game or server validation until the matching test has actually been
run for the stated versions.

| Project | Type | Supported range | Auto enabled | Current evidence | Known boundary |
|---|---|---|---:|---|---|
| Minecraft | hard platform | `26.1.2` | yes | compile/test/build | No other Minecraft versions are declared. |
| Fabric Loader | hard dependency | `>=0.19.3 <0.20.0` | yes | compile/test/build | Loader API drift outside this range is not claimed. |
| Fabric API | hard dependency | `>=0.155.2+26.1.2 <0.157.0` | yes | compile/test/build | Keep release metadata and Gradle property aligned. |
| Fabric Language Kotlin | hard dependency | `>=1.13.13 <1.14.0` | yes | compile/test/build | Runtime Kotlin stdlib comes through FLK. |
| TaCZ Refabricated Unofficial | hard dependency | `1.1.8+fabric.26.1.2.R<n>` with `n >= 2`; R2 baseline | required | compile/test/static descriptor audit + version gate test | Mixin targets are pinned to R2 descriptors; the runtime gate accepts later R<n> builds in the same release family. Future builds must still pass the matching descriptor, client and server validation. |
| YetAnotherConfigLib | hard dependency | exactly `3.9.6+26.1-fabric` | required | compile/build/checksum | Runtime mod is not nested; users must install it. |
| Mod Menu | optional UI entry | `*` suggested; compiled against `18.0.0-alpha.8` | when present | compile-only | Absence must not break startup. |
| Sound Physics Remastered | optional compat | `>=1.5.1 <1.6.0` | when present | source/static/build; airspace codec fixture | Airspace behavior depends on its runtime ray data. |
| First Aid New | optional compat | `>=1.2.8 <1.3.0` Fabric 26.1.2 | config controlled | source/static/build | Friendly-fire/damage-tag handling audited. |
| Pillager's Gun | optional compat | `>=3.2.2 <3.3.0` Fabric 26.1.2 | when present | source/static/build | Friendly-fire behavior follows that mod's config. |
| Legendary Survival Overhaul | no current target | unsupported | no | documented absence | No supported Fabric 26.1.2 target. |
| Valkyrien Skies | no current target | unsupported | no | documented absence | Public Fabric target does not match 26.1.2. |
| MTS / Immersive Vehicles | no current target | unsupported | no | documented absence | No supported Fabric 26.1.2 target found. |

## Evidence categories

- **Source/static**: mixin descriptors, target classes, config uses, or package names were checked.
- **Compile/test/build**: `./gradlew build` and unit/codec tests pass against pinned dependencies.
- **Game validation**: a client or integrated server scenario was launched and manually exercised.
- **Dedicated-server validation**: a headless server was launched and checked with
  `scripts/check_server_log.py`.

Release notes must state which categories were completed for that release.

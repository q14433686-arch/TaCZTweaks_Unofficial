# Compatibility matrix

This matrix separates implementation state from evidence. Do not promote an integration from
static/build verification to game or server validation until the matching test has actually been
run for the stated versions.

| Project | Type | Supported range | Auto enabled | Current evidence | Known boundary |
|---|---|---|---:|---|---|
| Minecraft | hard platform | `26.1.2` | yes | compile/test/build | No other Minecraft versions are declared. |
| Fabric Loader | hard dependency | `>=0.19.3` (lower bound only) | yes | compile/test/build against `0.19.3` | No upper cap: the old `<0.20.0` rejected Fabric Loader 0.20.0 (released 2026-10-15). The minimum must equal `loader_version`, enforced by `check_release_consistency.py`. Fabric derives the Mixin compatibility level from this minimum, so this mod's mixins keep the 0.19.3 level (`COMPATIBILITY_0_17_1`) on newer loaders. Later loaders are accepted but not individually tested. |
| Fabric API | hard dependency | `>=0.155.2+26.1.2` (lower bound only) | yes | compile/test/build against `0.155.2+26.1.2` | No artificial upper cap (synced from the 26.3 line); the minimum must equal `fabric_version`, enforced by `check_release_consistency.py`. Later 26.1.2 Fabric API builds are accepted but not individually tested. |
| Fabric Language Kotlin | hard dependency | `>=1.13.13` (lower bound only) | yes | compile/test/build against `1.13.13+kotlin.2.4.10` | No upper cap (the old `<1.14.0` rejected the published FLK 1.14.0–1.14.2; Issue #33 pattern). The minimum must equal the release part of `flk_version`, enforced by `check_release_consistency.py`. FLK 1.14.x itself requires Fabric Loader `>=0.19.5`. Later FLK builds are accepted but not individually tested. Runtime Kotlin stdlib comes through FLK. |
| TaCZ Refabricated Unofficial | hard dependency | `1.1.8+fabric.26.1.2.R<n>` with `n >= 2`; R2 baseline | required | compile/test/static descriptor audit + version gate test | Mixin targets are pinned to R2 descriptors; the runtime gate accepts later R<n> builds in the same release family. Future builds must still pass the matching descriptor, client and server validation. |
| YetAnotherConfigLib | hard dependency | `>=3.9.6+26.1-fabric` (lower bound only) | required | compile/build/checksum against `3.9.6+26.1-fabric` | No exact pin (Issue #33 pattern). The minimum must match the Modrinth artifact `svTkvBec` that `build.gradle.kts` resolves, enforced by `check_release_consistency.py`. Each YACL build declares its own Minecraft range, so builds for other game lines are still rejected. Later builds such as 3.9.7 for 26.1 are accepted but not individually tested. Runtime mod is not nested; users must install it. |
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

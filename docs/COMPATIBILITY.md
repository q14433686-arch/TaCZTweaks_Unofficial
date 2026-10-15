# Compatibility matrix

This matrix is for the **`26.3` Fabric 26.3** line only. NeoForge and other Minecraft
lines have their own matrices on those branches ([branch map](BRANCHES.md)). The pending
`Beta-1-hotfix2` dependency-policy update keeps the exact Minecraft/TaCZ constraints while
removing unnecessary upper caps from Loader, Fabric API, FLK and YACL.

This matrix separates implementation state from evidence. The open-ended ranges below mean
"this line's tested minimum or newer"; they do **not** claim every future dependency release
has been game-tested. Record game/server validation separately before making broader claims.

| Project | Type | Supported range | Auto enabled | Current evidence | Known boundary |
|---|---|---|---:|---|---|
| Minecraft | hard platform | exactly `26.3` | yes | CI compile/test/build; no separate game-test matrix recorded | No other Minecraft versions are declared. |
| Fabric Loader | hard dependency | `>=0.19.5` | yes | compile/test/build | Minimum follows this line's build; newer Loader releases are not individually game-tested. |
| Fabric API | hard dependency | `>=0.160.7+26.3` | yes | compile/test/build | Minimum follows this line's build; Minecraft remains exact. |
| Fabric Language Kotlin | hard dependency | `>=1.14.1+kotlin.2.4.20` | yes | compile/test/build | Runtime Kotlin stdlib comes through FLK; later releases are not individually game-tested. |
| TaCZ Refabricated Unofficial | hard dependency | declared `=1.1.8+fabric.26.3.R1`; runtime accepts R1 and later `R<n>` builds in that release family | required | CI compile/test/static descriptor audit; no separate game-test matrix recorded | Minecraft/core version and `fabric.26.3` family remain strict; other families (including `fabric.26.2`) are rejected at startup. |
| YetAnotherConfigLib | hard dependency | `>=3.9.7+26.3-fabric` | required | compile/build/checksum | Runtime mod is not nested; users must install it. Newer YACL releases are not individually game-tested. |
| Mod Menu | optional UI entry | `*` suggested; compiled against `21.0.0-beta.1` | when present | compile-only | Absence must not break startup. |
| Sound Physics Remastered | optional compat | `>=1.5.1 <1.6.0` | when present | source/static/build; airspace codec fixture | Airspace behavior depends on its runtime ray data. |
| First Aid New | optional compat | `>=1.3.0 <1.4.0` Fabric 26.3 | config controlled | source/static/build; shader resource override audited | Shader override is limited to this range. |
| Pillager's Gun | optional compat | `>=3.3.5 <3.4.0` Fabric 26.3 | when present | source/static/build | Friendly-fire behavior follows that mod's config. |
| Legendary Survival Overhaul | no current target | unsupported | no | documented absence | No supported Fabric 26.3 target. |
| Valkyrien Skies | no current target | unsupported | no | documented absence | Public Fabric target does not match 26.3. |
| MTS / Immersive Vehicles | no current target | unsupported | no | documented absence | No supported Fabric 26.3 target found. |

## Evidence categories

- **Source/static**: mixin descriptors, target classes, config uses, or package names were checked.
- **Compile/test/build**: `./gradlew build` and unit/codec tests pass against the current dependency baseline.
- **Game validation**: a client or integrated server scenario was launched and manually exercised.
- **Dedicated-server validation**: a headless server was launched and checked with
  `scripts/check_server_log.py`.

Release notes must state which categories were completed for that release. The new open-ended
dependency ranges are metadata policy, not a substitute for testing individual future versions.

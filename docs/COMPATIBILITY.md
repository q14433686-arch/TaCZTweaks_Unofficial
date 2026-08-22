# Compatibility matrix

This matrix separates implementation state from evidence. Do not promote an integration from
static/build verification to game or server validation until the matching test has actually been
run for the stated versions.

> Branch status: this is the **NeoForge 26.1.2** line. Nothing below has been compiled or played
> yet — every row is at most "source/static". See `docs/records/NEOFORGE_26_1_2_PORT_PLAN.md`.

| Project | Type | Supported range | Auto enabled | Current evidence | Known boundary |
|---|---|---|---:|---|---|
| Minecraft | hard platform | `26.1.2` | yes | source/static | No other Minecraft versions are declared. |
| NeoForge | hard dependency | `[26.1.2.97,)` | yes | source/static | Built against the same NeoForge build as TaCZ: Renovated 26.1.2. |
| TaCZ: Renovated | hard dependency | `1.1.8+neoforge.26.1.2.R<n>` with `n >= 1` | required | source/static (all 75 mixin target classes and 56 TaCZ-owned `@At` targets located in the R1 source tree) | Descriptor-level verification against the shipped jar is still outstanding. |
| YetAnotherConfigLib | hard dependency (client) | `3.9.6 for neoforge 26.1` | required | source/static | Runtime mod is not nested; users must install it. |
| Kotlin stdlib | embedded | `2.4.10` via `jarJar` | yes | source/static | NeoForge has no Fabric Language Kotlin; conflicts with other embedders are untested. |
| Sound Physics Remastered | optional compat | `>=1.5.1 <1.6.0` NeoForge 26.1.2 | when present | source/static | Class names were not re-checked against the NeoForge build. |
| First Aid New | optional compat | `>=1.2.8 <1.3.0` NeoForge 26.1 | config controlled | source/static | Reflection target `ichttt.mods.firstaid.common.EventHandler#recordProjectileHit` not verified on the NeoForge jar. |
| Pillager's Gun (Unofficial Port) | optional compat | `>=3.2.2 <3.3.0` NeoForge 26.1.2 | when present | source/static | Friendly-fire config surface not verified on the NeoForge jar. |
| LRTactical | bundled inside TaCZ: Renovated | n/a | when present | source/static | `IMeleeWeapon#performAttack` hook not exercised. |
| Legendary Survival Overhaul / Valkyrien Skies / MTS | not investigated on this loader | unsupported | no | none | The Fabric branch documented these as absent for Fabric; the NeoForge situation was **not** re-checked, so no switches are provided. |

## Evidence categories

- **Source/static**: mixin descriptors, target classes, config uses, or package names were checked.
- **Compile/test/build**: `./gradlew build` and unit tests pass against pinned dependencies.
- **Game validation**: a client or integrated server scenario was launched and manually exercised.
- **Dedicated-server validation**: a headless server was launched and checked with
  `scripts/check_server_log.py`.

Release notes must state which categories were completed for that release.

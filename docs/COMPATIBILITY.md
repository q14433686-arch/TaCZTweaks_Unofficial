# Compatibility matrix

This matrix separates source evidence from tests that actually ran. The target is the NeoForge 26.3
line. **This is a compile-level port: no 26.3 build has run yet (CI first-run pending), and no 26.3
client/server has launched.** Results from the 26.2 NeoForge line are useful porting evidence but do
not count as 26.3 runtime validation.

| Project | Type | Declared range | Current evidence | Boundary |
|---|---|---|---|---|
| Minecraft | hard platform | exactly `26.3` | static target re-check against 26.3 sources only | No build/run yet; no other Minecraft version is declared. |
| NeoForge | hard dependency | `[26.3.0.51-beta,)` | 26.3.x patches checked (blocking hunk identical to 26.2.x) | Beta channel only; build and dedicated-server startup pending. |
| TaCZ: Renovated | hard dependency | `1.1.8+neoforge.26.3.R<n>`, n >= 1 | R1 release located; all 7 changed target classes diffed, others byte-identical to 26.2 | Jar not yet compiled against; wrong core/loader/MC family remains rejected at startup. |
| YetAnotherConfigLib | hard dependency (both physical sides) | `[3.9.4,3.10.0)`; upstream `port/26.3` declares `modVersion=3.9.4` | upstream branch existence only | Exact published 26.3 filename and API surface unconfirmed until jar is fetched. |
| Kotlin stdlib | embedded | `2.4.10` via `jarJar` | unchanged from 26.2 | jar-content gate runs on first CI build. |
| Sound Physics Remastered | optional | `[1.5.1,1.6.0)` NeoForge 26.3 | upstream master pins 26.3 / `mod_version=1.5.1+26.3`; target class unchanged vs 26.2 source check | Shipped jar and airspace behavior untested. |
| First Aid New | optional | `[1.3.0,1.4.0)` NeoForge | no 26.3 artifact confirmed | 26.2 source surface was checked previously; 26.3 status open. |
| Pillager’s Gun (Unofficial Port) | optional | `[3.3.5,3.4.0)` NeoForge | no 26.3 artifact confirmed | 26.2 source surface was checked previously; 26.3 status open. |
| LRTactical | bundled in TaCZ: Renovated | target dependency's built-in version | `IMeleeWeapon#performAttack` unchanged in Renovated 26.3 source | Melee scenario untested. |
| Legendary Survival Overhaul / Valkyrien Skies / MTS | unsupported in this port | none | not re-verified for NeoForge 26.3 | No dormant/no-op switches are shipped. |

## Evidence categories

- **Source/static**: a source file, patch, declaration or method/call site was inspected.
- **Compile/test/build**: `./gradlew test` and `./gradlew build` completed against the pinned jars.
- **Client validation**: a NeoForge 26.3 client reached the main menu without mixin/injection failure.
- **Dedicated-server validation**: `scripts/check_server_log.py` accepted a log containing `Done (...)!`.
- **Gameplay validation**: gun, ADS, reload, unload, config screen and datapack reload were exercised.

All rows are currently at source/static level for the 26.3 line. Compile/test/build evidence begins
with the first green CI run; client/server/gameplay evidence requires the maintainer checklist in
`records/NEOFORGE_26_3_PORT_RECORD.md` §7.

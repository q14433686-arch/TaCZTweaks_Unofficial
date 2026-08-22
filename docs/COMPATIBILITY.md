# Compatibility matrix

This matrix separates source evidence from tests that actually ran. The target is the NeoForge 26.2
line; results from the 26.1.2 NeoForge skeleton are useful porting evidence but do not count as 26.2
runtime validation.

| Project | Type | Declared range | Current evidence | Boundary |
|---|---|---|---|---|
| Minecraft | hard platform | exactly `26.2` | source/static | No other Minecraft version is declared. |
| NeoForge | hard dependency | `[26.2.0.64,)` | source/static against 26.2.x sources/patches | Build, client and server startup are pending. |
| TaCZ: Renovated | hard dependency | `1.1.8+neoforge.26.2.R<n>`, n >= 1 | 26.2 source tree checked; release API version/digest located | Shipped jar descriptor audit and gameplay are pending. Wrong core/loader/MC family is rejected at startup. |
| YetAnotherConfigLib | hard dependency (both physical sides) | `[3.9.5,3.10.0)`; NeoForge 26.2 artifact required | release listing located | Core config types extend YACL classes, so dedicated servers also need the jar; config screen has not been opened on 26.2. |
| Kotlin stdlib | embedded | `2.4.10` via `jarJar` | build configuration only | Release jar nesting and coexistence with other Kotlin embedders are untested on 26.2. |
| Sound Physics Remastered | optional | `[1.5.1,1.6.0)` NeoForge 26.2 | 1.5.1+26.2 source call sites checked | Shipped jar and airspace behavior untested. |
| First Aid New | optional | `[1.3.0,1.4.0)` NeoForge 26.2 | `EventHandler#handleCustomPlayerDamage(Player, DamageSource, float)` and `#recordProjectileHit(Player, Entity, Vec3)` located in the NeoForge 26.2 source module | 1.3.0-patched release jar and gameplay untested; 1.2.8 exists but is outside the declared range because the 1.3.x shader override was not verified against it. |
| Pillager’s Gun (Unofficial Port) | optional | `[3.3.5,3.4.0)` NeoForge 26.2 | 3.3.5 release located; `PillagersGunConfig#values()` / `Values#friendlyFire()` located in source | Shipped 3.3.5 jar and friendly-fire behavior untested. |
| LRTactical | bundled in TaCZ: Renovated | target dependency's built-in version | target classes/method names located in Renovated 26.2 source | Melee scenario untested. |
| Legendary Survival Overhaul / Valkyrien Skies / MTS | unsupported in this port | none | not re-verified for NeoForge 26.2 | No dormant/no-op switches are shipped. |

## Evidence categories

- **Source/static**: a source file, patch, declaration or method/call site was inspected.
- **Compile/test/build**: `./gradlew test` and `./gradlew build` completed against the pinned jars.
- **Client validation**: a NeoForge 26.2 client reached the main menu without mixin/injection failure.
- **Dedicated-server validation**: `scripts/check_server_log.py` accepted a log containing `Done (...)!`.
- **Gameplay validation**: gun, ADS, reload, unload, config screen and datapack reload were exercised.

At present, no row has been promoted beyond source/static for this 26.2 port.

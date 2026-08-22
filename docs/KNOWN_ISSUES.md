# Known issues and validation gaps

## Build passed; runtime coverage is intentionally non-exhaustive

A maintainer completed a Windows / JDK 25 `gradlew build` successfully and reports that the built
client entered the game with most core functions passing practical tests. That is meaningful Beta
smoke coverage, but it is not a guarantee for every feature, config combination, datapack, hardware
setup, or optional-mod combination.

Still outstanding or not individually evidenced:

- dedicated-server startup and `scripts/check_server_log.py` showing `Done (...)!`;
- a per-scenario record for gun / ADS / reload / unload / config screen / datapack reload;
- runtime matrices for Sound Physics, First Aid, Pillager's Gun and protection mods.

## Block-break protection is weaker than on Fabric

Fabric exposes `PlayerBlockBreakEvents.BEFORE / CANCELED / AFTER`. NeoForge 26.2.x exposes the
cancellable `net.neoforged.neoforge.event.level.block.BreakBlockEvent`, fired for the break attempt,
but no equivalent three-phase chain.

Bullet/melee block destruction therefore checks `Level#mayInteract` and posts a `BreakBlockEvent`.
Claim/protection mods that rely on Fabric's canceled/after notifications may not receive an equivalent
callback. Internal break-progress cleanup listens at `EventPriority.LOWEST` and ignores canceled
events, which only approximates “the block is about to be removed.” Specific protection mods are
untested. This is a documented semantic downgrade, not a completed compatibility fix.

## Shield durability depends on a NeoForge-patched call site

NeoForge 26.2.x patches `LivingEntity#applyItemBlocking` to invoke the six-argument overload:

```text
BlocksAttacks#hurtBlockingItem(Level, ItemStack, LivingEntity, InteractionHand, float, int)
```

The six-argument wrap is the primary path. A five-argument wrap remains with `require = 0` as a
vanilla/unpatched fallback. If neither target matches, the mod logs a one-time warning when a shield
rule resolves. Build and general client startup no longer report an injection failure, but the
maintainer did not provide a targeted shield-rule transcript.

## Optional integrations are source-checked, not runtime-verified

Sound Physics Remastered, First Aid New, Pillager's Gun and bundled LRTactical have gated code paths.
Their 26.2 source/API surfaces were located, but the published jars and behavior have not been tested
with this port. Stay inside `docs/COMPATIBILITY.md` ranges and attach full logs to reports.

## Pending dependency digests block release

`RESOURCE_IMPORT_MANIFEST.tsv` still uses `pending` for binaries not covered by the available build
log. TaCZ and Sound Physics hashes were taken from the maintainer's actual build inputs; YACL and
optional jars still need exact SHA-256 values before release-quality provenance is complete.

## YACL is required on dedicated servers too

Although only clients render the config screen, shared config persistence/sync classes extend YACL
config types. `neoforge.mods.toml` therefore marks YACL required on BOTH physical sides. Omitting it
from a dedicated server is unsupported.

## Kotlin runtime is embedded

NeoForge has no Fabric Language Kotlin equivalent here. The release jar uses `jarJar` for
`kotlin-stdlib 2.4.10`; Gradle jar-content checks and general client startup passed. Coexistence with
arbitrary other mods embedding different Kotlin versions remains outside the Beta smoke coverage.

## Removed legacy switches

The following options are not shipped:

- `thirdPersonGunRenderingFix`
- `lsoCompat`
- `mtsFix`
- `vsCollisionCompat`
- `vsExplosionCompat`

LSO, MTS and Valkyrien Skies targets were not re-verified for this NeoForge 26.2 port, so they are
listed as unsupported rather than represented by empty/no-op switches. Remove stale keys from old
JSON configuration files.

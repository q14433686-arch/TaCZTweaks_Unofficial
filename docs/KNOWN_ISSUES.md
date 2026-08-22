# Known issues and validation gaps

## NeoForge 26.2 has not passed build or runtime gates yet

The code and dependency surfaces have been translated and checked statically, but this sandbox has no
JDK 25 and cannot reach Maven/Modrinth/GitHub release-asset binary endpoints. A maintainer's first
Windows/JDK 25 build reached Kotlin compilation and exposed two now-corrected issues; a successful
rerun has not yet been reported. The following remain **not passed** for 26.2:

- `./gradlew test`
- `./gradlew build`
- client main-menu startup
- dedicated-server startup and `scripts/check_server_log.py`
- gun / ADS / reload / unload / config screen / datapack reload smoke scenarios

The client-load PASS of the 26.1.2 NeoForge skeleton is not a 26.2 PASS.

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
rule resolves. Build and client mixin application still need to prove the 26.2 compiled descriptor.

## Optional integrations are source-checked, not runtime-verified

Sound Physics Remastered, First Aid New, Pillager's Gun and bundled LRTactical have gated code paths.
Their 26.2 source/API surfaces were located, but the published jars and behavior have not been tested
with this port. Stay inside `docs/COMPATIBILITY.md` ranges and attach full logs to reports.

## Pending dependency digests block release

`RESOURCE_IMPORT_MANIFEST.tsv` uses `pending` for binaries that this sandbox could not download.
Before a release, replace every pending digest with the SHA-256 of the exact artifact and rerun both
dependency and release-consistency checks. The TaCZ digest is copied from GitHub's release asset API,
but should still be re-hashed on the release machine.

## YACL is required on dedicated servers too

Although only clients render the config screen, shared config persistence/sync classes extend YACL
config types. `neoforge.mods.toml` therefore marks YACL required on BOTH physical sides. Omitting it
from a dedicated server is unsupported.

## Kotlin runtime is embedded

NeoForge has no Fabric Language Kotlin equivalent here. The release jar uses `jarJar` for
`kotlin-stdlib 2.4.10`; release-jar inspection and coexistence with other mods embedding Kotlin have
not yet been tested on NeoForge 26.2.

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

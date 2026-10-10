# Known issues and validation gaps

## Compile-level port; nothing has run on 26.3 yet

The 26.3 line is a compile-level port produced in a sandbox without JDK or outbound build network.
No `gradlew build`, no client launch, no dedicated-server start and no gameplay scenario has been
executed for this line. The 26.2 line's build/smoke results do not carry over. Everything below is
a gap to close, not a regression that was observed.

Outstanding (in dependency order):

- first green CI `build` (`.github/workflows/build.yml`) including unit tests and release gates;
- `scripts/audit_port.py --strict` against the real 26.3 jars (currently WARN-downgraded without jars);
- dedicated-server startup and `scripts/check_server_log.py` showing `Done (...)!`;
- a per-scenario record for gun / ADS / reload / unload / config screen / datapack reload;
- runtime matrices for Sound Physics, First Aid, Pillager's Gun and protection mods.

## Block-break protection is weaker than on Fabric

Fabric exposes `PlayerBlockBreakEvents.BEFORE / CANCELED / AFTER`. NeoForge 26.3.x exposes the
cancellable `net.neoforged.neoforge.event.level.block.BreakBlockEvent`, fired for the break attempt,
but no equivalent three-phase chain.

Bullet/melee block destruction therefore checks `Level#mayInteract` and posts a `BreakBlockEvent`.
Claim/protection mods that rely on Fabric's canceled/after notifications may not receive an equivalent
callback. Internal break-progress cleanup listens at `EventPriority.LOWEST` and ignores canceled
events, which only approximates “the block is about to be removed.” Specific protection mods are
untested. This is a documented semantic downgrade, not a completed compatibility fix.

## Shield durability depends on a NeoForge-patched call site

NeoForge 26.3.x patches `LivingEntity#applyItemBlocking` to invoke the six-argument overload:

```text
BlocksAttacks#hurtBlockingItem(Level, ItemStack, LivingEntity, InteractionHand, float, int)
```

The six-argument wrap is the primary path. A five-argument wrap remains with `require = 0` as a
vanilla/unpatched fallback. If neither target matches, the mod logs a one-time warning when a shield
rule resolves. The 26.3.x blocking patch hunk is byte-identical to 26.2.x, but no 26.3 run has
confirmed injection yet, and there is no targeted shield-rule transcript on any line.

## Optional integrations are source-checked, not runtime-verified

Sound Physics Remastered, First Aid New, Pillager's Gun and bundled LRTactical have gated code paths.
Their 26.2 source/API surfaces were located previously and the unchanged ones were re-checked against
the Renovated 26.3 sources, but no 26.3 published jar or behavior has been tested. First Aid and
Pillager's Gun have no confirmed 26.3 artifacts at all. Stay inside `docs/COMPATIBILITY.md` ranges
and attach full logs to reports.

## Pending dependency digests block release

`RESOURCE_IMPORT_MANIFEST.tsv` uses `pending` for every 26.3 binary: the porting sandbox could not
download release assets or Modrinth files, so no digest has been computed for this line. The first
machine that fetches each jar must record its SHA-256 before release-quality provenance is complete.

## YACL is required on dedicated servers too

Although only clients render the config screen, shared config persistence/sync classes extend YACL
config types. `neoforge.mods.toml` therefore marks YACL required on BOTH physical sides. Omitting it
from a dedicated server is unsupported.

## Kotlin runtime is embedded

NeoForge has no Fabric Language Kotlin equivalent here. The release jar uses `jarJar` for
`kotlin-stdlib 2.4.10`; the Gradle jar-content gate will run on the first CI build. Coexistence with
arbitrary other mods embedding different Kotlin versions is untested on this line.

## Removed legacy switches

The following options are not shipped:

- `thirdPersonGunRenderingFix`
- `lsoCompat`
- `mtsFix`
- `vsCollisionCompat`
- `vsExplosionCompat`

LSO, MTS and Valkyrien Skies targets were not re-verified for this NeoForge 26.3 port, so they are
listed as unsupported rather than represented by empty/no-op switches. Remove stale keys from old
JSON configuration files.

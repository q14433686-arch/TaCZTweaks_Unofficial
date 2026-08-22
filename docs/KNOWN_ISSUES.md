# Known issues

This page is for user-facing limitations. Porting debates and obsolete investigation notes belong
in `AUDIT.md`, `PORTING_NOTES.md`, or handoff files.

## This branch has never been built or played

The NeoForge 26.1.2 port is a source-level translation done in an environment without a JDK and
without network access to Maven/CurseForge. `./gradlew build`, `./gradlew test`, dedicated-server
smoke tests and in-game testing are all still outstanding.

## Block-break protection is weaker than on Fabric

Fabric exposes `PlayerBlockBreakEvents.BEFORE / CANCELED / AFTER`; NeoForge only has the
cancellable `BlockEvent.BreakEvent`. Player-owned bullet/melee block destruction therefore runs
`Level#mayInteract` plus that event, but claim/protection mods relying on a "canceled" or "after"
notification will not receive one. Behaviour with specific protection mods is untested.

## Dedicated-server startup is not yet automated in CI

The repository has a server-log checker, but GitHub Actions currently runs static audit, checksum
checks and Gradle builds only. Dedicated-server startup remains a manual pre-release gate until a
server smoke workflow is added.

## Optional compatibility claims are version-bounded

Sound Physics Remastered, First Aid New and Pillager's Gun support is limited to the ranges in
`neoforge.mods.toml` and `docs/COMPATIBILITY.md`. Reports outside those ranges should first reproduce
inside the declared range.

## Removed legacy compatibility switches

The following old options are not present in this port:

- `thirdPersonGunRenderingFix` — removed on the Fabric line because the target port fixes it
  natively; whether TaCZ: Renovated needs it has **not** been re-checked.
- `lsoCompat`, `mtsFix`, `vsCollisionCompat`, `vsExplosionCompat` — the availability of those mods
  on NeoForge 26.1.2 was **not** investigated, so no switches are shipped.

If these keys remain in an old JSON config, remove them to avoid confusion.

## Data-pack errors fail by reload diagnostics

The data-driven systems are strict enough to reject malformed codecs. Keep a minimal test pack and
attach the relevant reload log when filing issues.

# Known issues

This page is for user-facing limitations. Porting debates and obsolete investigation notes belong
in `AUDIT.md`, `PORTING_NOTES.md`, or handoff files.

## Dedicated-server startup is not yet automated in CI

The repository has a server-log checker, but GitHub Actions currently runs static audit, checksum
checks and Gradle builds only. Dedicated-server startup remains a manual pre-release gate until a
server smoke workflow is added.

## Optional compatibility claims are version-bounded

Sound Physics Remastered, First Aid New and Pillager's Gun support is limited to the ranges in
`fabric.mod.json` and `docs/COMPATIBILITY.md`. Reports outside those ranges should first reproduce
inside the declared range.

## Removed legacy compatibility switches

The following old options are intentionally not supported in this Fabric 26.2 port:

- `thirdPersonGunRenderingFix` — TaCZ Refabricated R2 already fixes the target behavior.
- `lsoCompat` — no supported Legendary Survival Overhaul Fabric 26.2 target.
- `mtsFix` — no supported MTS / Immersive Vehicles Fabric 26.2 target.
- `vsCollisionCompat` and `vsExplosionCompat` — no supported Valkyrien Skies Fabric 26.2 target.

If these keys remain in an old JSON config, remove them to avoid confusion.

## Data-pack errors fail by reload diagnostics

The data-driven systems are strict enough to reject malformed codecs. Keep a minimal test pack and
attach the relevant reload log when filing issues.

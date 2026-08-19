# Known issues

This page is for user-facing 1.21.11 limitations. Porting debates and obsolete investigation
notes belong in `PORTING_NOTES.md`.

## Dedicated-server startup is not yet automated in CI

The repository has a server-log checker, but the proposed GitHub Actions workflow currently
runs static audit, checksum checks and Gradle builds only. Dedicated-server startup remains a
manual pre-release gate until a server smoke workflow is added.

## Optional compatibility claims are version-bounded

Sound Physics Remastered, First Aid New and Pillager's Gun support is limited to the ranges in
`fabric.mod.json` and `docs/COMPATIBILITY.md`. Reports outside those ranges should first
reproduce inside the declared 1.21.11 range.

Those integrations are present in source and statically audited. They have not completed a
full 1.21.11 in-game matrix.

## Dormant leftover compatibility switches

The following keys still exist in `config/tacztweaks.json` and the YACL compatibility group,
but they have no verified Fabric 1.21.11 target:

- `compat.lsoCompat`
- `compat.mtsFix`
- `compat.vsCollisionCompat`
- `compat.vsExplosionCompat`

`thirdPersonGunRenderingFix` is not a current option. TaCZ Refabricated 1.21.11 R2 already
fixes the third-person gun rendering target.

## Remap / refmap branch

Minecraft 1.21.11 is obfuscated in this port. The published artifact is the Loom `remapJar`
output, and mixins use `tacztweaks.refmap.json`. Mixin reports that mention intermediary
names need the generated refmap and both named and intermediary Minecraft jars for a strict
audit.

## Data-pack errors fail by reload diagnostics

The data-driven systems are strict enough to reject malformed codecs. Keep a minimal test pack
and attach the relevant reload log when filing issues.

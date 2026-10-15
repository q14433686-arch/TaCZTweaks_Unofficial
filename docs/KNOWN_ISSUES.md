# Known issues

This page is for user-facing 1.21.11 limitations. Porting debates and obsolete investigation
notes belong in `PORTING_NOTES.md`.

## Dedicated-server startup is not yet automated in CI

The repository has a server-log checker, and five GitHub Actions workflows are now installed
(`consistency`, `audit`, `compile-check`, `build`, and a tag-triggered `release`). They run the
static audit, checksum checks, dependency-range gates and full Gradle builds — but none of them
starts a dedicated server. Dedicated-server startup remains a manual pre-release gate until a
server smoke workflow is added.

## Green CI does not prove the game launches

Every gate in this repository is static: compilation, the mixin audit, jar packaging and the
dependency-range checks all run without Minecraft. The Fabric Loader's `depends` resolution and
the TaCZ version gate in `TaCZTweaks.java` only execute when the game starts. The `26.3` line
shipped three times with all workflows green and the game refusing to launch — twice from an
unsatisfiable dependency range, once from an artificial upper bound on `fabric-api` that
rejected a user's installed build.

The synced gates (`check_runtime_version_gate`, `check_declared_ranges_include_built_versions`,
`check_fabric_api_range_is_lower_bound_only`, `scripts/check_dependency_availability.py`) narrow
that window considerably, but they still cannot replace one real launch of the client, an
integrated server and a dedicated server before a release is published.

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

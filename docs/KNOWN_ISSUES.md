# Known issues

This page is for user-facing limitations. Porting debates and obsolete investigation notes belong
in `PORTING_NOTES.md` or handoff files.

## Dedicated-server startup is not yet automated in CI

The repository has a server-log checker (`scripts/check_server_log.py`), but CI currently runs
static audit, checksum checks and Gradle builds only. Dedicated-server startup remains a manual
pre-release gate until a server smoke workflow is added.

## Optional compatibility claims are version-bounded

Sound Physics Remastered, First Aid New and Pillager's Gun support is limited to the ranges in
`fabric.mod.json` and `docs/COMPATIBILITY.md`. Reports outside those ranges should first reproduce
inside the declared range.

## Legacy compatibility switches are no-ops without their mods

`compat.lsoCompat`, `compat.vsCollisionCompat`, `compat.vsExplosionCompat`, and `compat.mtsFix`
exist on this 1.21.11 branch because their runtime targets are still supported there. They do
nothing when the corresponding mod is not installed; install the exact 1.21.11-compatible mod
before enabling them.

## Third-person gun rendering fix is not exposed as a config option

TaCZ Refabricated Unofficial `1.1.8+fabric.1.21.11.R2` already includes the third-person gun
rendering fix, so this port does not expose the old `thirdPersonGunRenderingFix` toggle. Remove
that key from old config files if present.

## Data-pack errors fail by reload diagnostics

The data-driven systems are strict enough to reject malformed codecs. Keep a minimal test pack and
attach the relevant reload log when filing issues.

## Vendored dependency checksums are pending networked pinning

The `sha256` column of `RESOURCE_IMPORT_MANIFEST.tsv` is `PENDING` for both vendored jars because
the sandbox that produced this branch could not reach the GitHub release-asset CDN or the Modrinth
CDN. Every checksum gate fails with an explanatory message until a networked run of
`python3 scripts/download_dependencies.py` pins the values. Do not release until they are pinned.

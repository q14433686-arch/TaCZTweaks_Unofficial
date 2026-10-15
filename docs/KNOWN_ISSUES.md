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

## 50 untriaged audit warnings against the real TaCZ R2 jar

This branch had no CI at all until the workflows were synced from the `26.3` line, so
`scripts/audit_port.py` had never been run against the real TaCZ jar. Its first CI run
reports:

```
AUDIT: 0 error(s), 132 warning(s)
```

Full output is committed to `build-reports/audit.log` on `arena/**` branches. The 132
break down as:

| Count | Category | Status |
|---:|---|---|
| 11 | `target class not checked because its jar was not supplied` | Expected. `audit.yml` supplies only the TaCZ jar; vanilla `net.minecraft.*` and optional-compat classes are absent by design. |
| 20 | `@At owner class not checked because its jar was not supplied` | Expected, same reason. |
| 46 | `@At method reference absent` | **Untriaged** — see below. |
| 46 | `target bytecode does not contain @At reference` | **Untriaged** — the bytecode-side echo of the same 12 declarations. |
| 4 | `target method absent` | **Untriaged** — see below. |
| 4 | `dormant config option Compat.*` | Documented: `lsoCompat`, `mtsFix`, `vsCollisionCompat`, `vsExplosionCompat` are unwired. |
| 1 | `synthetic target exception in use` | Documented narrow exception on `SoundBufferLibraryMixin`. |

The 92 `@At` warnings come from **12 distinct declarations**, and every one of those 12 has a
named `net/minecraft/*` type in its descriptor (`ItemStack`, `Vec3`, `Identifier`, `Entity`,
`Projectile`, `LivingEntity`, `SoundEvent`). The mixin sources are written against **named**
Minecraft types, while the TaCZ Refabricated jar for this obfuscated line is compiled against
**intermediary** ones (`class_1799`, `class_1247`, `class_2960`, …), which Loom's remap +
`tacztweaks.refmap.json` reconcile at build time. `audit_port.py` has no descriptor-translation
step: `--minecraft-named-jar` / `--minecraft-intermediary-jar` only choose *which jar* a class is
looked up in (`choose_jar`), and the refmap is consulted solely for vanilla `net.minecraft.*`
mixin target *classes*. So this mismatch is plausibly structural to auditing pre-remap source,
not evidence of broken mixins.

That reading is **not yet verified**, and the same ambiguity is visible in the compile log: the
mixin annotation processor emitted 6 `Cannot find target method` / `Unable to determine
descriptor` warnings for `EntityKineticBulletMixin` and `SoundPhysicsMixin`.

The 4 `target method absent` cases are different and deserve priority, because they are
*name-only* lookups that a descriptor mismatch cannot explain:

- `com.tacz.guns.client.gui.GunSmithTableScreen#mouseScrolled`
- `com.tacz.guns.client.sound.GunSoundInstance#<init>(…)` (named descriptor, so possibly the same cause)
- `com.tacz.guns.client.sound.GunSoundInstance#resolve`
- `com.tacz.guns.util.block.ProjectileExplosion#explode`

Each is either an inherited method the audit does not walk superclasses for (false positive) or a
genuine rename/removal in R2 (a mixin that would fail or silently no-op at runtime). Triage needs
the R2 jar, which the porting sandbox cannot download.

Under `--strict` all 96 of these become errors, so `--strict` is **not currently achievable** on
this branch even with the named/intermediary jars and a generated refmap supplied. Until they are
triaged, treat the non-strict result as the CI signal and do not read "0 errors" as "every mixin
target verified".

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

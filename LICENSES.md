# Licenses and redistributed materials

This file summarizes the license posture for source code, resources, embedded libraries,
and vendored binary dependencies on the **Fabric 1.21.11** branch. Checksums and download
URLs for local binary inputs are pinned in
[`RESOURCE_IMPORT_MANIFEST.tsv`](RESOURCE_IMPORT_MANIFEST.tsv).

## Project code

- Project: TaCZ Tweaks (Refabricated), this unofficial Fabric 1.21.11 port.
- License: GPL-3.0, inherited from the original MUKSC/TaCZTweaks project.
- Full text: [`LICENSE`](LICENSE).

## Original TaCZ Tweaks material reused here

- Upstream project: <https://github.com/MUKSC/TaCZTweaks>
- Upstream version/revision: v2.14.2 (`74ba2412a6149a1d91788c3663497c4c81992983`).
- License: GPL-3.0.
- Use in this repository: Java/Kotlin porting basis, `src/main/resources/icon.png`, and
  the example-pack audio fixtures under `tacz-tweaks-example-pack/assets/`.
- Notices: [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).

## Libraries embedded into the released mod jar

| Component | Version | License | Source | Purpose |
|---|---:|---|---|---|
| MixinExtras Fabric | 0.5.4 | MIT | <https://github.com/LlamaLad7/MixinExtras> | Runtime support for selected mixin injection helpers. |

The build includes `META-INF/LICENSE_tacztweaks` and
`META-INF/THIRD_PARTY_NOTICES_tacztweaks.md` in the remapped release jar.

This branch does **not** embed TaCZ, YACL, Fabric API, Fabric Language Kotlin, or optional
compatibility mods in the published jar.

## Vendored binary inputs tracked in `libs/`

| Path | Upstream | Version | License | Build use | In this mod jar? |
|---|---|---:|---|---|---|
| `libs/TACZ-Refabricated-1.21.11-1.1.8+fabric.1.21.11.R2.jar` | `q14433686-arch/TaCZ_Refabricated_Unofficial` | `1.1.8+fabric.1.21.11.R2` | `GPL3 / CC BY-NC-ND 4.0` as declared by its `fabric.mod.json` | `modCompileOnly`, `testRuntimeOnly` target API/mixin surface | No |
| `libs/yacl-fabric.jar` | `isXander/YetAnotherConfigLib` | `3.8.2+1.21.11-fabric` | `LGPL-3.0-or-later` | `modImplementation` runtime dependency expected to be installed separately | No |

These jars make this checkout buildable and testable against the exact 1.21.11 targets used
by the port. They are not nested into the TaCZ Tweaks release jar; players must install the
corresponding runtime mods themselves.

Reconstruct or verify with:

```bash
python3 scripts/download_dependencies.py
python3 scripts/download_dependencies.py --check-only
```

## Modified compatibility resources

This 1.21.11 branch does **not** redistribute modified First Aid shaders or other rewritten
third-party shader/resource copies. First Aid New support here is the `1.2.5` Fabric 1.21.11
legacy line, not the 26.2 `1.3.x` shader-override path.

See [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) for detailed attribution.

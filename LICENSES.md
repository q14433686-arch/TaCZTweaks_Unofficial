# Licenses and redistributed materials

This file summarizes the license posture for source code, resources, embedded libraries,
and vendored binary dependencies. Checksums and download URLs for local binary inputs are
pinned in [`RESOURCE_IMPORT_MANIFEST.tsv`](RESOURCE_IMPORT_MANIFEST.tsv).

## Project code

- Project: TaCZ Tweaks unofficial ports (Fabric and NeoForge lines in this repository).
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
`META-INF/THIRD_PARTY_NOTICES_tacztweaks.md` in the release jar.

## Vendored binary inputs tracked in `libs/`

| Path | Upstream | Version | License | Build use | In this mod jar? |
|---|---|---:|---|---|---|
| `libs/TACZ-Refabricated-26.2-1.1.8+fabric.26.2.R2.jar` | `q14433686-arch/TaCZ_Refabricated_Unofficial` | `1.1.8+fabric.26.2.R2` | `GPL3 / CC BY-NC-ND 4.0` as declared by its `fabric.mod.json` | `compileOnly`, `testRuntimeOnly` target API/mixin surface | No |
| `libs/yacl-fabric.jar` | `isXander/YetAnotherConfigLib` | `3.9.6+26.2-fabric` | `LGPL-3.0-or-later` as declared by its `fabric.mod.json` | `implementation` runtime dependency expected to be installed separately | No |

These jars are kept only to make this checkout buildable and testable against the exact
26.2 targets used by the port. They are not nested into the TaCZ Tweaks release jar;
players must install the corresponding runtime mods themselves.

## Modified compatibility resources

| Component | License | Use |
|---|---|---|
| First Aid New shader compatibility copies | GPL-3.0 | Shader files under `src/main/resources/assets/firstaid/shaders/post/` with one unsupported DynamicTransforms import removed. |

See [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) for detailed attribution.

# Licenses and redistributed materials

This file summarizes source, resource, embedded-library and local build-input licensing for the
unofficial NeoForge 26.2 port. Exact local paths, source URLs and checksums are tracked in
[`RESOURCE_IMPORT_MANIFEST.tsv`](RESOURCE_IMPORT_MANIFEST.tsv).

## Project code and reused TaCZ Tweaks material

- Project code: GPL-3.0, inherited from [`MUKSC/TaCZTweaks`](https://github.com/MUKSC/TaCZTweaks).
- Porting basis: upstream v2.14.2, commit `74ba2412a6149a1d91788c3663497c4c81992983`.
- Reused resources: `src/main/resources/icon.png` and example-pack audio under
  `tacz-tweaks-example-pack/assets/`, also GPL-3.0.
- Full license text: [`LICENSE`](LICENSE).

## Library embedded in the released mod jar

| Component | Version | License | Source | Purpose |
|---|---:|---|---|---|
| Kotlin standard library | 2.4.10 | Apache-2.0 | <https://github.com/JetBrains/kotlin> | NeoForge has no Fabric Language Kotlin equivalent; embedded with ModDevGradle `jarJar`. |

MixinExtras is provided by the NeoForge runtime for this target and is not intentionally nested by
this build.

## Local binary build inputs (`libs/*.jar`)

These jars are ignored by Git and are **not** nested into the TaCZ Tweaks release jar. Runtime mods
must be installed separately where required.

| Local artifact | Upstream/version | License | Build use | In this mod jar? |
|---|---|---|---|---|
| `tacz-1.1.8+neoforge.26.2.R1.jar` | `q14433686-arch/TaCZ_Renovated`, R1 | GPL-3.0 | compile API/mixin target and local runtime | No |
| `yet_another_config_lib_v3-3.9.6+26.2-neoforge.jar` | isXander/YACL 3.9.6 | LGPL-3.0 | compile config API and local runtime on both physical sides | No |
| `sound-physics-remastered-neoforge-1.5.1+26.2.jar` | henkelmax/SPR 1.5.1 | GPL-3.0 | optional-integration target validation | No |
| `firstaid-1.3.0-patched+neoforge26.2.jar` | maoruiQa/First-Aid-New 1.3.0-patched | GPL-3.0 | optional source/jar compatibility audit | No |
| `pillagers_gun-3.3.5-neoforge-26.2.jar` | SmartStreamLabs/Pillager's Gun port 3.3.5 | GPL-3.0 | optional source/jar compatibility audit | No |
| `commons-math3-3.6.1.jar` | Apache Commons Math 3.6.1 | Apache-2.0 | compile-only signature type; TaCZ supplies runtime copy | No |

## Modified First Aid compatibility resources

| Component | Source snapshot | License | Use |
|---|---|---|---|
| First Aid New 26.2 shader copies | `maoruiQa/FIrst-Aid-New` commit `8fc4dd579c02ba3d3b29b96b2d22a2e12c48a5c5` | GPL-3.0 | `assets/firstaid/shaders/post/` copies remove an unsupported, unused DynamicTransforms import while preserving the shader behavior. |

See [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) for detailed attribution and the approved
mod-icon checksum.

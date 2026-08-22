# Third-party notices

## Original TaCZ Tweaks mod icon

- Project: <https://github.com/MUKSC/TaCZTweaks>
- Immutable upstream revision: `74ba2412a6149a1d91788c3663497c4c81992983`
- Upstream resource: <https://github.com/MUKSC/TaCZTweaks/blob/74ba2412a6149a1d91788c3663497c4c81992983/src/main/resources/icon.png>
- Modrinth icon source: <https://cdn.modrinth.com/data/H8peNuJG/0c9fcf0f40ec59d591b7cc17452c63a843df122e.png>
- Author: MUKSC
- Icon license: GPL-3.0
- Approved icon checksum (SHA-256): `c8591fdd552d0bbad05cd8a60136faf89d5e9fd6d0dab08eb96fa04439c6db9d`
- Use: `src/main/resources/icon.png`, distributed in this mod jar.

The repository includes the complete GPL-3.0 text in [`LICENSE`](LICENSE).

## Original TaCZ Tweaks example-pack audio

- Project: <https://github.com/MUKSC/TaCZTweaks>
- Version/revision: v2.14.2 (`74ba2412`)
- Author: MUKSC
- License: GPL-3.0
- Use: metal-impact and whizz `.ogg` files under `tacz-tweaks-example-pack/assets/`.

## Embedded Kotlin standard library

- Project: <https://github.com/JetBrains/kotlin>
- Version: 2.4.10
- License: Apache License 2.0
- Use: embedded with ModDevGradle `jarJar` because this NeoForge target has no Fabric Language
  Kotlin runtime provider.

The Kotlin standard library license and notices remain part of the nested artifact. No local
`libs/*.jar` dependency is intentionally nested into this mod jar.

## Local NeoForge 26.2 build/runtime inputs

Exact file paths, download URLs, checksums, licenses and inclusion flags are maintained in
[`RESOURCE_IMPORT_MANIFEST.tsv`](RESOURCE_IMPORT_MANIFEST.tsv). The inputs are:

- TaCZ: Renovated `1.1.8+neoforge.26.2.R1` — GPL-3.0;
- YetAnotherConfigLib `3.9.6+26.2-neoforge` — LGPL-3.0;
- Sound Physics Remastered `1.5.1+26.2` NeoForge — GPL-3.0;
- First Aid New `1.3.0-patched+neoforge26.2` (the separate 1.2.8 artifact is not in this port's declared range) — GPL-3.0;
- Pillager’s Gun (Unofficial Port) `3.3.5` NeoForge 26.2 — GPL-3.0;
- Apache Commons Math `3.6.1` — Apache-2.0.

These jars are ignored by Git and not redistributed inside TaCZ Tweaks. Users install required
runtime mods separately.

## First Aid New shader compatibility copies

- Project: <https://github.com/maoruiQa/FIrst-Aid-New>
- Source snapshot inspected: `8fc4dd579c02ba3d3b29b96b2d22a2e12c48a5c5`
- License: GPL-3.0
- Modified resources:
  - `assets/firstaid/shaders/post/pain_pulse_blur.fsh`
  - `assets/firstaid/shaders/post/saturation_boost.fsh`
- Modification inherited from the Fabric 26.2 semantic line: remove the unused
  `minecraft:dynamictransforms.glsl` import whose uniform block is absent from the matching First
  Aid post chains; preserve all visual calculations.

These copies apply only within the declared First Aid `[1.3.0,1.4.0)` compatibility range and must
be re-evaluated when upstream changes the shaders.

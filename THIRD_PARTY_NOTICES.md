# Third-party notices

Third-party items retain the authorship and license recorded in their individual entries.
This repository's GPL-3.0 code license does not automatically relicense every linked,
compatible, or separately installed third-party asset or project.

## Original TaCZ Tweaks mod icon

- Project: <https://github.com/MUKSC/TaCZTweaks>
- Source revision: v2.14.2 (`74ba2412a6149a1d91788c3663497c4c81992983`)
- Upstream resource: <https://github.com/MUKSC/TaCZTweaks/blob/74ba2412a6149a1d91788c3663497c4c81992983/src/main/resources/icon.png>
- Modrinth icon source: <https://cdn.modrinth.com/data/H8peNuJG/0c9fcf0f40ec59d591b7cc17452c63a843df122e.png>
- Author: MUKSC
- License: GPL-3.0
- SHA-256: `c8591fdd552d0bbad05cd8a60136faf89d5e9fd6d0dab08eb96fa04439c6db9d`
- Use here: `src/main/resources/icon.png` (the mod icon distributed in the jar).

The icon is an unmodified resource from the GPL-3.0-licensed original project. The full
GPL-3.0 text is included as [`LICENSE`](LICENSE). This entry applies specifically to the
icon and does not make a blanket licensing claim about other third-party content.

## Original TaCZ Tweaks example-pack audio

- Project: <https://github.com/MUKSC/TaCZTweaks>
- Version: v2.14.2 (`74ba2412`)
- Author: MUKSC
- License: GPL-3.0
- Use here: metal-impact and whizz `.ogg` fixtures under `tacz-tweaks-example-pack/assets/`.

The full GPL-3.0 text is included as [`LICENSE`](LICENSE). These files are packaged in the
example-pack zip, not inside the TaCZ Tweaks mod jar.

## Vendored compile/runtime input jars

The following local jars are expected under `libs/` to make this checkout build and test
against fixed Fabric 1.21.11 targets. Their source URLs, versions, checksums and release-jar
inclusion status are pinned in [`RESOURCE_IMPORT_MANIFEST.tsv`](RESOURCE_IMPORT_MANIFEST.tsv).
They are **not** nested into this project's published jar.

### TaCZ Refabricated Unofficial 1.1.8+fabric.1.21.11.R2

- File: `libs/TACZ-Refabricated-1.21.11-1.1.8+fabric.1.21.11.R2.jar`
- Project: <https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial>
- Release: <https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial/releases/tag/1.21.11_R2>
- License declared by its `fabric.mod.json`: `GPL3 / CC BY-NC-ND 4.0`
- SHA-256: `34d117f316a2ab6b3ca4d9bfbf61a87fefc416c2919874e057688054d75e788d`
- Use here: compile-only and test-runtime API/mixin target dependency. It is not nested into
  this project's published jar; users must install the TaCZ Refabricated mod separately.

### YetAnotherConfigLib 3.8.2+1.21.11-fabric

- File: `libs/yacl-fabric.jar`
- Project: <https://github.com/isXander/YetAnotherConfigLib>
- Modrinth file: <https://cdn.modrinth.com/data/1eAoo2KR/versions/pHWDw3Vc/yet_another_config_lib_v3-3.8.2%2B1.21.11-fabric.jar>
- License: `LGPL-3.0-or-later` as declared by the upstream project
- SHA-512: `392db7d471030cca27483ecf58c626a14cd73d71a18afe6d4173c6b030948b8a925b36e708d4cc2c897dfa3f20a7f23b999fc18aa6d36c156da29037601153ac`
- SHA-1 (Modrinth): `f99cda70903f16dd6927a276150b2762b4d5ab66`
- Use here: runtime configuration library expected as a separate installed mod. It is not
  nested into this project's published jar.

## MixinExtras 0.5.4

- Project: <https://github.com/LlamaLad7/MixinExtras>
- Version/tag commit: `a6a2a42611341b1976b8b33b9a927ff5c84424ec`
- Author: LlamaLad7
- License: MIT
- Use here: `mixinextras-fabric` is embedded in the published remapped mod jar.

Copyright (c) 2022-present LlamaLad7

Permission is hereby granted, free of charge, to any person obtaining a copy of this software
and associated documentation files (the "Software"), to deal in the Software without
restriction, including without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.

## Modified third-party shaders / resources

This 1.21.11 branch does not ship modified First Aid post-process shaders or other rewritten
third-party resource copies. Optional First Aid New compatibility is limited to the
`>=1.2.5 <1.3.0` Fabric 1.21.11 legacy line documented in `fabric.mod.json`.

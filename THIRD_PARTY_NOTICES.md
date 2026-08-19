# Third-party notices

Third-party items retain the authorship and license recorded in their individual entries.
This repository's GPL-3.0 code license does not automatically relicense every linked,
compatible, or separately installed third-party asset or project.

## Original TaCZ Tweaks mod icon

- Project: <https://github.com/MUKSC/TaCZTweaks>
- Fixed upstream commit: `74ba2412a6149a1d91788c3663497c4c81992983`
- Upstream resource: <https://github.com/MUKSC/TaCZTweaks/blob/74ba2412a6149a1d91788c3663497c4c81992983/src/main/resources/icon.png>
- Modrinth CDN source: <https://cdn.modrinth.com/data/H8peNuJG/0c9fcf0f40ec59d591b7cc17452c63a843df122e.png>
- Author: MUKSC
- License: GPL-3.0
- Approved SHA-256: `c8591fdd552d0bbad05cd8a60136faf89d5e9fd6d0dab08eb96fa04439c6db9d`
- Use in this repository: `src/main/resources/icon.png` (the mod icon distributed in the jar)

The icon is an unmodified resource from the GPL-3.0-licensed original project. The license
text is included as [`LICENSE`](LICENSE). This entry applies specifically to the icon and
does not make a blanket licensing claim about other third-party content.

## Original TaCZ Tweaks example-pack audio

- Project: <https://github.com/MUKSC/TaCZTweaks>
- Version: v2.14.2 (`74ba2412a6149a1d91788c3663497c4c81992983`)
- Author: MUKSC
- License: GPL-3.0
- Use here: the metal-impact and whizz `.ogg` fixtures under
  `tacz-tweaks-example-pack/assets/tacztweaks/sounds/`, byte-identical to the upstream files
  (sizes verified against the upstream tree at the fixed commit). They are distributed in the
  `tacz-tweaks-example-pack-*.zip` build artifact, not in the mod jar.
- SHA-256 per file:

| Path | SHA-256 |
|---|---|
| `tacz-tweaks-example-pack/assets/tacztweaks/sounds/hit/metal1.ogg` | `74a5c2d36b8a9ccb53f9bf4d1723dcfd7df54ef5cfa39604784805138b767329` |
| `tacz-tweaks-example-pack/assets/tacztweaks/sounds/hit/metal2.ogg` | `9ce520f30a7564313b6f5e8c4d36a0cf668ed46f260242c7c8d48c3d8f7e9faa` |
| `tacz-tweaks-example-pack/assets/tacztweaks/sounds/hit/metal3.ogg` | `edcbbf7d663736b829acfbbf34b0d9d513342cdb8433825d54e2852607750fc4` |
| `tacz-tweaks-example-pack/assets/tacztweaks/sounds/hit/metal4.ogg` | `6eb19dc5741b9b405a7e8dc6c1dc2d450e332cc19af862a8ee153aef3590ff47` |
| `tacz-tweaks-example-pack/assets/tacztweaks/sounds/whizz/far1.ogg` | `309e8ec70420a6617a37ffb3a0c613c7f325927a4c581a4def145e573e6388e9` |
| `tacz-tweaks-example-pack/assets/tacztweaks/sounds/whizz/far2.ogg` | `7851657dbcaa89e38cbc749cc66fccfd3a3346624127863ba7d6ed766eabb8f3` |
| `tacz-tweaks-example-pack/assets/tacztweaks/sounds/whizz/far3.ogg` | `c335e12dcd7b228903f98aa123f8a2024b83e89e4e2a807720cec6f0e06a9967` |
| `tacz-tweaks-example-pack/assets/tacztweaks/sounds/whizz/mid1.ogg` | `562d3a9457df0cabf5623f03182f7b730fda7b2313919ef12d453bac3063ea2b` |
| `tacz-tweaks-example-pack/assets/tacztweaks/sounds/whizz/mid2.ogg` | `8f953b68b682b36bc409c25531711c4115020b00a39adc5eb9a0238b7901221d` |
| `tacz-tweaks-example-pack/assets/tacztweaks/sounds/whizz/mid3.ogg` | `8ba51e350b06f0cae9c0cd2a3107b8e75995025c089909239306d953cfe6509f` |
| `tacz-tweaks-example-pack/assets/tacztweaks/sounds/whizz/near1.ogg` | `4c0acac387d80f00599e03c725df346a139068004dfd22270745c35a6bfa1151` |
| `tacz-tweaks-example-pack/assets/tacztweaks/sounds/whizz/near2.ogg` | `6d10d904758c2593cda850abcdcefe15c43431a0cfde61d53dab8ac08dea02a4` |
| `tacz-tweaks-example-pack/assets/tacztweaks/sounds/whizz/near3.ogg` | `bf9899655a141bef5b92d3cb807af83cc697741461053bc4ef97af3626946da3` |

The full GPL-3.0 license is included as [`LICENSE`](LICENSE).

## Original TaCZ Tweaks runtime trigger sound

- Project: <https://github.com/MUKSC/TaCZTweaks>
- Version: v2.14.2 (`74ba2412a6149a1d91788c3663497c4c81992983`)
- Author: MUKSC
- License: GPL-3.0
- SHA-256: `da73a2cce7ffd25ce11cddb29feb0c67da9ac27b43069d7e8a049cd89e0350ff`
- Use here: `src/main/resources/assets/tacztweaks/sounds/sound_physics_trigger.ogg` (bundled
  in the mod jar for the Sound Physics Remastered airspace trigger).

## Bundled in the built mod jar

- **MixinExtras** — `io.github.llamalad7:mixinextras-fabric` `0.5.4`
  - Upstream: <https://github.com/LlamaLad7/MixinExtras>
  - Author: LlamaLad7
  - License: MIT
  - Purpose: advanced mixin injectors used by this port
  - This is the only third-party library intentionally nested into the release jar.

## Compile / runtime linked dependencies (Maven-resolved, not vendored)

- Fabric Loader
- Fabric API
- Fabric Language Kotlin
- Mod Menu (compile-only optional)
- Sound Physics Remastered (optional compatibility)
- First Aid New (optional compatibility)
- Pillager's Gun (optional compatibility)

These are not redistributed by this repository's source tree unless supplied separately by
the builder/runtime.

## Modified resources

This 1.21.11 branch redistributes no modified third-party shader or resource files. (The
First Aid shader compatibility copies present on the 26.2 branch do not exist here.)

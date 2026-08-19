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

The icon is an unmodified resource from the GPL-3.0-licensed original project. The
license text is included as [`LICENSE`](LICENSE). This entry applies specifically to
the icon and does not make a blanket licensing claim about other third-party content.

## Bundled in the built jar

- **MixinExtras** — `io.github.llamalad7:mixinextras-fabric`
  - Upstream: https://github.com/LlamaLad7/MixinExtras
  - Purpose: advanced mixin injectors used by this port
  - License: see upstream project

## Compile / runtime linked dependencies

- Fabric Loader
- Fabric API
- Fabric Language Kotlin
- YetAnotherConfigLib v3
- TaCZ Refabricated Unofficial (`tacz`)

These are not redistributed by this repository's source tree unless supplied separately by the builder/runtime.

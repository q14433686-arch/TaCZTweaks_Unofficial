# Licenses and redistributed materials

This file summarizes the license posture for source code, resources, embedded libraries,
and vendored binary dependencies. Checksums and download URLs for local binary inputs are
pinned in [`RESOURCE_IMPORT_MANIFEST.tsv`](RESOURCE_IMPORT_MANIFEST.tsv).

## Project code

- Project: TaCZ Tweaks (Refabricated), this unofficial Fabric port.
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
| `libs/TACZ-Refabricated-26.1.2-1.1.8+fabric.26.1.2.R2.jar` | `q14433686-arch/TaCZ_Refabricated_Unofficial` | `1.1.8+fabric.26.1.2.R2` | `GPL3 / CC BY-NC-ND 4.0` as declared by its `fabric.mod.json` | `compileOnly`, `testRuntimeOnly` target API/mixin surface | No |

This jar is kept to make this checkout buildable and testable against the exact 26.1.2 targets
used by the port. It is not nested into the TaCZ Tweaks release jar; players must install the
corresponding runtime mod separately.

## Configuration library

| Component | Version | License | Source | Build use | In this mod jar? |
|---|---:|---|---|---|---|
| YetAnotherConfigLib | `3.9.6+26.1-fabric` | LGPL-3.0-or-later | Modrinth `svTkvBec` via Maven | `implementation` runtime dependency | No |

See [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) for detailed attribution.

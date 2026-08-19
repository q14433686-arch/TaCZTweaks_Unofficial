# Licenses and redistributed materials

This file summarizes the license posture for source code, resources, embedded libraries,
and Maven-resolved dependencies.

## Project code

- Project: TaCZ Tweaks (Refabricated), this unofficial Fabric port.
- License: GPL-3.0, inherited from the original MUKSC/TaCZTweaks project.
- Full text: [`LICENSE`](LICENSE).

## Original TaCZ Tweaks material reused here

- Upstream project: <https://github.com/MUKSC/TaCZTweaks>
- Upstream version/revision: v2.14.2 (`74ba2412a6149a1d91788c3663497c4c81992983`).
- License: GPL-3.0.
- Use in this repository: Java/Kotlin porting basis, `src/main/resources/icon.png`, the
  `sound_physics_trigger.ogg` runtime sound under `src/main/resources/assets/tacztweaks/sounds/`,
  and the example-pack audio fixtures under `tacz-tweaks-example-pack/assets/`.
- Notices: [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).

## Libraries embedded into the released mod jar

| Component | Version | License | Source | Purpose |
|---|---:|---|---|---|
| MixinExtras Fabric | 0.5.4 | MIT | <https://github.com/LlamaLad7/MixinExtras> | Runtime support for selected mixin injection helpers. |

The build includes `META-INF/LICENSE_tacztweaks` and
`META-INF/THIRD_PARTY_NOTICES_tacztweaks.md` in the release jar.

## Maven-resolved dependencies (not vendored, not embedded)

TaCZ Refabricated Unofficial (`curse.maven:unofficial-tacz-refabricated-1627909:8660664`,
project <https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial>,
license `GPL3 / CC BY-NC-ND 4.0` per its `fabric.mod.json`) and
YetAnotherConfigLib (`maven.modrinth:yacl:3.8.2+1.21.11-fabric`,
project <https://github.com/isXander/YetAnotherConfigLib>,
license `LGPL-3.0-or-later` per its `fabric.mod.json`) are resolved from Maven at build
time. Gradle's built-in dependency verification validates their integrity. Users must install
the corresponding runtime mods themselves; these jars are not nested into the TaCZ Tweaks
release jar.

## Modified compatibility resources

This 1.21.11 branch does not redistribute modified shader resources. (The First Aid shader
compatibility copies that exist on the 26.2 branch are not present here.)

See [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) for detailed attribution.
# TaCZ Tweaks

> **Unofficial port status:** this repository adds a Fabric build for Minecraft **1.21.11**
> targeting **[UNOFFICIAL] TaCZ Refabricated 1.1.8+fabric.1.21.11.R2**
> ([source](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)).
> The port is based on TaCZ Tweaks v3 (`3.0.0-alpha.10`) and uses an
> `unofficial` version suffix to avoid being mistaken for an upstream MUKSC release.

[![Modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg)](https://modrinth.com/mod/tacz-tweaks)
[![CurseForge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/curseforge_vector.svg)](https://www.curseforge.com/minecraft/mc-mods/tacz-tweaks)
[![GitHub](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/github_vector.svg)](https://github.com/MUKSC/TaCZTweaks)

TaCZ Tweaks is an addon for the [Timeless and Classics Zero](https://modrinth.com/mod/timeless-and-classics-zero) mod.  
This mod adds various features and customization options to expand and enhance TaCZ.  
Access the in-game config screen through the mods list for options.  
**Requires the dependencies listed in the section below.**

If you have any questions, you can reach me on the [TaCZ Official Discord](https://discord.gg/uX6TdWUVpA) in the [#community-showcase > TaCZ Tweaks](https://discord.com/channels/1243278348399022252/1313570204000980992) channel.  
Alternatively, you can use the [GitHub Discussions](https://github.com/MUKSC/TaCZTweaks/discussions) page or the [issues](https://github.com/MUKSC/TaCZTweaks/issues) page.

## Dependencies
Forge:
- [TaCZ](https://modrinth.com/mod/timeless-and-classics-zero)
- [Kotlin for Forge](https://modrinth.com/mod/kotlin-for-forge)
- [YACL](https://modrinth.com/mod/yacl)

NeoForge:
- [TaCZ 1.21.1 NeoForge Port](https://modrinth.com/mod/tacz-1.21.1)
- [Kotlin for Forge](https://modrinth.com/mod/kotlin-for-forge)
- [YACL](https://modrinth.com/mod/yacl)

Fabric 1.20.1 / 1.21.1:
- [TaCZ: Refabricated](https://modrinth.com/mod/tacz-refabricated)
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin)
- [YACL](https://modrinth.com/mod/yacl)
- [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port)

Fabric 1.21.11 (unofficial build):
- [[UNOFFICIAL] TaCZ Refabricated, CurseForge file 8660664](https://www.curseforge.com/minecraft/mc-mods/unofficial-tacz-refabricated/files/8660664)
- Fabric API `0.141.6+1.21.11`
- Fabric Language Kotlin `1.13.8+kotlin.2.3.0` or newer
- YACL `3.8.2+1.21.11-fabric`
- Forge Config API Port `21.11.1`
- Cloth Config `21.11.153` when using TaCZ's Mod Menu configuration screen

## Building the 1.21.11 port

```bash
./gradlew :1.21.11-fabric:build
```

The build resolves the target TaCZ jar from CurseMaven using
`curse.maven:unofficial-tacz-refabricated-1627909:8660664`; the large dependency is not vendored in this repository.

## Features
TaCZ Tweaks offers various features for both regular players, and technical players such as modpack makers and server owners.  
This is not an opinionated mod; it doesn't offer you a defined gameplay style, but instead it provides you with a bunch of customization options.

- Data-driven Bullet Interaction System
  - Customize piercing, destroy blocks, play sounds, summon particles, etc.
- Attributes and Commands
  - Attributes to modify damage, disable guns, etc.
  - Commands to refill ammo, etc.
- Stats Balancing
  - Global modifiers for damage, RPM, magazine capacity, inaccuracy, recoil, etc.
- Compatibility and Bug Fixes
  - Added compatibility with Valkyrien Skies, Sable/Create Aeronautics, FirstAid, etc.
  - Fixed TaCZ issues with crawl cooldown, third-person gun rendering, etc.
- Gameplay Enhancements
  - New features including unloading guns, shoot or reload while sprinting, realistic magazine-style reloading, audible reload sounds in multiplayer, etc.
  - Various gameplay tweaks such as configurable crawl pitch limit, Projectile Protection enchantment works against bullets, Endermen evade bullets, etc.
- Quality of Life
  - QoL changes like always filter Gun Smith Table recipes by item in hand, cancel inspection animation, etc.
  - Preference options such as suppress kill sounds, stop ADS while reloading or bolting, hide hit markers, etc.

## License
Copyright (C) 2024-2026  MUKSC  
Licensed under [GPLv3](https://github.com/MUKSC/TaCZTweaks/blob/main/LICENSE)
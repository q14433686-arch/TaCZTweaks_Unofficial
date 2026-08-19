# CurseForge 发布文案

## Project name

```text
TaCZ Tweaks (Refabricated)
```

名称只保留稳定项目名，不加入 Minecraft 版本、文件版本或发布阶段。

## Summary

```text
An unofficial Fabric port providing configurable gunplay, movement, balance, interactions, sound, and data-driven customization for TaCZ.
```

## Description

```markdown
# Unofficial community port

**TaCZ Tweaks (Refabricated) is an unofficial Fabric port of TaCZ Tweaks by MUKSC.**
It is designed for TaCZ Refabricated Unofficial and is not affiliated with, reviewed by,
or endorsed by MUKSC, the TaCZ development team, or the maintainers of the underlying
Fabric port.

## Overview

The mod adds configurable gun handling, movement, global balance modifiers, projectile
and melee interactions, sound and particle rules, server configuration synchronization,
and quality-of-life controls to TaCZ.

It is intended for players who want more control over gun behavior and for server owners
or pack authors who need data-driven interaction rules without modifying TaCZ itself.

## Features

### Gunplay and movement

- configurable unloading, chambering, manual cycling, and reload behavior;
- sprinting while shooting or reloading;
- shooting-sequence reload and fire-mode controls;
- underwater firing restrictions;
- gun tilt, sensitivity, prone movement, hit-marker, and hit-sound settings.

### Balance and gameplay

- global damage, player-damage, headshot, and armor-penetration modifiers;
- projectile speed, gravity, friction, aim time, spread, fire-rate, and recoil modifiers;
- projectile-protection enchantment support;
- shield interaction with remaining damage, durability loss, and disable duration;
- endless ammunition, Enderman projectile evasion, attachment restrictions, and workbench filtering.

### Data-driven systems

Data and resource packs can define:

- block, entity, and shield bullet interactions;
- destruction progress, replacement, penetration, and damage;
- impact, fly-by, constant, and environment-dependent sounds;
- block and entity particles with absolute, relative, or local coordinates;
- continuous particle emitters;
- gun melee and compatible melee-weapon interactions;
- selectors based on guns, categories, ammunition, predicates, damage, speed, silencer state,
  bursts, pellets, random chance, and logical combinations.

### Configuration

The port provides JSON persistence, server-to-client configuration synchronization, and an
in-game YACL configuration screen. Mod Menu integration is optional.

## How this port differs from the original

This is not an unchanged copy of the original mod. The target environment requires substantial
port-specific work, including:

- replacing loader-specific events, networking, permissions, tags, and reload APIs;
- adapting mixins to the methods exposed by the Fabric TaCZ port;
- redesigning mutable sound-request state and cache lifecycles;
- integrating current shield, enchantment, prone-rendering, and block-break behavior;
- restoring data-driven managers and legacy-schema conversion;
- synchronizing server-controlled settings;
- adding static audits, behavior and codec tests, server-log checks, and asset-provenance gates.

## Requirements

Install the file matching your Minecraft and Fabric environment. Required dependencies include
Fabric API, Fabric Language Kotlin, TaCZ Refabricated Unofficial, and YetAnotherConfigLib.

Exact game, loader, Java, and dependency requirements are listed on each uploaded file. Do not
infer compatibility from the project name or from an old screenshot.

Optional integration is available for Mod Menu, Sound Physics Remastered, First Aid New, and
Pillager's Gun when a compatible release is installed. These integrations do not grant permission
to redistribute third-party projects or their assets.

## Support and issue reports

Please report only problems specific to this port. Before submitting:

1. search open and closed issues;
2. test with only this mod and its required dependencies;
3. verify whether the problem still occurs after removing this port;
4. collect the complete log or crash report;
5. record exact versions, minimal reproduction steps, the affected side, configuration changes,
   and all third-party content involved.

Screenshots and isolated error lines cannot replace a complete log. Problems introduced by this
port should not be reported to the original TaCZ Tweaks, TaCZ, or compatibility-mod authors.

## Credits and license

Original TaCZ Tweaks was created by MUKSC. This derivative port is distributed under the GNU
General Public License v3.0. The project icon and bundled third-party resources retain their
documented authorship and licenses. See the source repository's third-party notices for details.

## Project links

- Original project: https://github.com/MUKSC/TaCZTweaks
- Port source: https://github.com/q14433686-arch/TaCZTweaks_Unofficial
- Documentation: https://github.com/q14433686-arch/TaCZTweaks_Unofficial/tree/HEAD/docs
- Issue tracker: https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues

The links above provide source code, attribution, documentation, and support. Mod files should be
downloaded from this CurseForge project page rather than from an external file link.

## AI-assisted page disclosure

This project-page description was drafted with generative AI assistance and reviewed by the
project maintainer. The licensed upstream project icon is not AI-generated or AI-modified.
```

## CurseForge fields and moderation checklist

- **Project type:** Mods
- **Mod loader:** Fabric
- **License:** GNU General Public License v3.0
- **Required relations:** Fabric API, Fabric Language Kotlin, TaCZ Refabricated Unofficial, YetAnotherConfigLib
- **Optional relations:** only integrations applicable to the uploaded file
- **Source and Issues:** use the dedicated project fields as well as the links at the bottom
- **Files:** tag every upload with its exact game version, loader and release status
- **Avatar:** use the licensed square icon from this repository
- **Description:** do not add external jar download links

若 CurseForge 无法为某个依赖建立 Relation，不应把它伪装成可选依赖；应在文件说明和正文中明确其必需性，并等待平台审核或依赖项目上线。

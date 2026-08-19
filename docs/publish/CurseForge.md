# CurseForge 发布文案

## Project name

```text
TaCZ Tweaks (Refabricated)
```

项目名只保留稳定名称，不加入游戏版本、文件版本或发布阶段。

## Summary

```text
An unofficial Fabric port providing configurable gunplay, movement, balance, interactions, sound, and data-driven customization for TaCZ.
```

## Description

```markdown
# Unofficial community port

**TaCZ Tweaks (Refabricated) is an unofficial community Fabric port of TaCZ Tweaks by
MUKSC.** It is designed for TaCZ Refabricated Unofficial and is not affiliated with,
reviewed by, or endorsed by MUKSC, the TaCZ team, or the maintainers of the underlying
Fabric port.

## Overview

The mod adds configurable gun handling and movement, global balance modifiers, projectile
and melee interactions, sound and particle rules, data-driven customization, server
configuration synchronization, and quality-of-life controls to TaCZ.

It is intended for Fabric players who want more control over gun behavior and for server
owners or pack authors who need reloadable interaction rules without modifying TaCZ itself.

## Features

### Gunplay, movement, and balance

- configurable unloading, chambering, cycling, reload, sprint, firing, sensitivity, and gun-tilt behavior;
- global damage, player-damage, headshot, armor penetration, projectile, spread, fire-rate, and recoil modifiers;
- projectile-protection enchantment and shield interactions;
- configurable hit-marker, sound, particle, attachment, ammunition, and workbench behavior.

### Data-driven systems

Data and resource packs can define supported block/entity bullet interactions, shield behavior,
impact and fly-by sounds, particles, and gun or compatible melee-weapon interactions. Selectors
can use guns, ammunition, categories, predicates, damage, speed, silencer state, bursts, pellets,
chance, and logical combinations. A reloadable example pack is included in the source repository.

### Configuration

The port provides JSON persistence, server-to-client configuration synchronization, and an
in-game YACL configuration screen. Mod Menu integration is optional.

## How this port differs from the original

This is a maintained derivative port, **not an unchanged reupload of the original files**. The
target environment requires substantial port-specific work, including:

- replacing loader-specific events, networking, permissions, tags, and resource-reload APIs;
- adapting mixins to the methods and class descriptors exposed by the Fabric TaCZ port;
- implementing Fabric payload registration and server/client configuration synchronization;
- adapting protected block breaking, shield, enchantment, prone-rendering, sound, and particle paths;
- restoring data-driven managers and conversion for supported legacy interaction data;
- gating optional integrations so they remain optional;
- adding remap-aware static audits, tests, dedicated-server log checks, and asset-provenance gates.

## Before downloading

Choose a file whose game version, loader, environment, release channel, and dependency metadata
match your instance. Do not infer compatibility from the stable project name or an old screenshot.
Some real-game client, multiplayer, dedicated-server, and optional-integration matrices may still
be incomplete; read the selected file's changelog and current source documentation before using
it in an important world. Back up worlds and configurations before changing mods.

## Requirements and optional compatibility

A compatible Minecraft and Java runtime, Fabric Loader, Fabric API, Fabric Language Kotlin,
TaCZ Refabricated Unofficial, and YetAnotherConfigLib are required. Exact requirements belong in
each uploaded file's game/loader metadata and **Relations**; this description does not replace them.

Mod Menu is optional. The source also contains optional integration paths for Sound Physics
Remastered, First Aid New, and Pillager's Gun. Only releases permitted by the uploaded file's
metadata and current documentation should be used. No guarantee is made that every third-party
gun pack, data pack, resource pack, or optional mod will be compatible.

Compatibility does not imply ownership, maintenance, endorsement, relicensing, or permission to
redistribute third-party projects or assets.

## Support and issue reports

Before submitting a port-specific report:

1. search open and closed issues;
2. reproduce from a new world or test server with only this mod and its hard dependencies;
3. repeat the same test after removing this port;
4. collect the complete log or crash report;
5. record exact versions, minimal steps, frequency, the affected client/server environment,
   configuration changes, and all third-party content involved.

Screenshots and isolated error lines cannot replace a complete log. Problems introduced by this
port should not be reported to the original TaCZ Tweaks, TaCZ, or compatibility-mod authors.
Use the support documentation and issue forms linked in **Project links** below.

## Credits and license

Original TaCZ Tweaks was created by MUKSC. This derivative project's code is distributed under
the GNU General Public License v3.0. The licensed original icon and other third-party resources
retain their documented authorship and licenses. Compatibility with external content does not
relicense that content.

## AI-assisted page disclosure

This project-page text was drafted with generative AI assistance and reviewed by the project
maintainer against the current source. The licensed original TaCZ Tweaks icon is not AI-generated
or AI-modified.

## Project links

- Original project: https://github.com/MUKSC/TaCZTweaks
- Underlying Fabric dependency: https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial
- Port source: https://github.com/q14433686-arch/TaCZTweaks_Unofficial
- Documentation: https://github.com/q14433686-arch/TaCZTweaks_Unofficial/tree/HEAD/docs
- Support: https://github.com/q14433686-arch/TaCZTweaks_Unofficial/blob/HEAD/docs/SUPPORT.md
- Issue tracker: https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues
- Third-party notices: https://github.com/q14433686-arch/TaCZTweaks_Unofficial/blob/HEAD/THIRD_PARTY_NOTICES.md

Download mod files from this CurseForge project page; no external jar download link is provided.
```

## CurseForge fields and moderation checklist

- **Project type:** Mods
- **Mod loader:** Fabric
- **License:** GNU General Public License v3.0
- **Required Relations:** Fabric API, Fabric Language Kotlin, TaCZ Refabricated Unofficial, YetAnotherConfigLib, and any other hard dependency declared by the uploaded file
- **Optional Relations:** Mod Menu and only integrations applicable to the uploaded file
- **Files:** set exact game version, loader, file version, dependencies, and release type on every upload
- **Description order:** keep the English description before any translated description
- **Fork disclosure:** retain the port-specific changes section; do not copy the original description as a substitute
- **External links:** source, docs, Issues, and notices stay at the bottom of the description
- **Downloads:** do not add external jar download links
- **Avatar:** use `src/main/resources/icon.png`, the square original icon with source and license recorded in `THIRD_PARTY_NOTICES.md`
- **Media:** do not use AI-generated or AI-modified icons, banners, or Gallery images

若 CurseForge 暂时无法为硬依赖建立 Relation，不得把该依赖伪装成可选项；应在文件说明中明确其必需性，并等待平台支持或审核。

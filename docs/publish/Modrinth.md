# Modrinth 发布文案

## Project title

```text
TaCZ Tweaks (Refabricated)
```

Title 只保留稳定项目名，不加入游戏版本、文件版本或发布阶段。

## Summary

```text
A community Fabric port adding configurable gunplay, movement, balance, interactions, sound, and data-driven customization to TaCZ.
```

Summary 是单行纯文本，不使用 Markdown；它不能代替文件 metadata。

## Description

```markdown
# Unofficial community port

> **This is an unofficial community Fabric port of TaCZ Tweaks. It is not affiliated
> with, reviewed by, or endorsed by MUKSC, the TaCZ team, or the maintainers of the
> underlying TaCZ Refabricated Unofficial port.**

## What this project does

TaCZ Tweaks (Refabricated) adapts MUKSC's TaCZ Tweaks for use with TaCZ Refabricated
Unofficial on Fabric. It adds configurable gun handling and movement, global balance
modifiers, projectile and melee interactions, sound and particle rules, data-driven
customization, server configuration synchronization, and quality-of-life controls.

## Why use it

The original project targets a different mod-loader environment. This port is for Fabric
players, server owners, and pack authors who want TaCZ Tweaks-style controls and reloadable
interaction data while using the unofficial Fabric TaCZ port.

Implemented areas include:

- configurable unloading, chambering, cycling, reload, sprint, firing, sensitivity, and gun-tilt behavior;
- global damage, player-damage, headshot, armor penetration, projectile, spread, fire-rate, and recoil modifiers;
- projectile-protection enchantment and shield interaction support;
- block/entity bullet interactions and gun or compatible melee-weapon interactions;
- configurable impact, fly-by, constant, and environment-dependent sounds and particles;
- selectors for guns, ammunition, categories, predicates, damage, speed, silencer state, bursts, pellets, chance, and logical combinations;
- JSON configuration persistence and server-to-client synchronization;
- in-game YACL configuration, with optional Mod Menu integration.

## What changed from the original

This is a maintained derivative port, **not an unchanged reupload of the original files**.
Substantial port-specific work includes:

- replacing loader-specific events, networking, permissions, tags, and resource-reload APIs;
- adapting mixins to the actual methods and class descriptors exposed by the Fabric TaCZ port;
- implementing Fabric payload registration and server/client configuration synchronization;
- adapting protected block breaking, shield, enchantment, prone-rendering, sound, and particle paths;
- restoring data-driven managers and conversion for supported legacy interaction data;
- gating optional integrations so absent optional mods do not become hard dependencies;
- adding remap-aware static audits, tests, dedicated-server log checks, and icon provenance gates.

## Before downloading

Choose a file whose game version, loader, environment, release channel, and Dependencies
metadata match your instance. Do not infer compatibility from the stable project title or an
old screenshot. Some real-game client, multiplayer, dedicated-server, and optional-integration
matrices may still be incomplete; read the selected file's changelog and the current source
documentation before deploying it to an important world.

Back up worlds and configurations before changing mods. Server and relevant clients must use
mutually compatible files and configuration.

## Requirements and optional compatibility

A compatible Minecraft and Java runtime, Fabric Loader, Fabric API, Fabric Language Kotlin,
TaCZ Refabricated Unofficial, and YetAnotherConfigLib are required. **Every uploaded file must
also declare its required projects in Modrinth's Dependencies field; this description does not
replace metadata.**

Mod Menu is optional. The source also contains optional integration paths for Sound Physics
Remastered, First Aid New, and Pillager's Gun. Only use releases allowed by the selected file's
metadata and current documentation. No guarantee is made that every third-party gun pack,
data pack, resource pack, or optional mod will be compatible.

Compatibility does not imply ownership, maintenance, endorsement, relicensing, or permission
to redistribute third-party projects or assets.

## Support and bug reports

Before reporting a port-specific problem, search open and closed issues, reproduce from a new
world or test server with only hard dependencies, and repeat the same test after removing this
port. Provide exact versions, the complete log or crash report, minimal steps, frequency,
client/server environment, relevant configuration, and every third-party content item involved.
Screenshots and isolated error lines cannot replace a complete log.

- [Support and troubleshooting](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/blob/HEAD/docs/SUPPORT.md)
- [Bug report](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues/new?template=bug_report.yml)
- [Compatibility report](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues/new?template=compat_report.yml)

Please do not ask the original TaCZ Tweaks, TaCZ, or compatibility-mod authors to support
problems introduced by this port.

## Credits, source, documentation, and license

- Original project and author: [MUKSC/TaCZTweaks](https://github.com/MUKSC/TaCZTweaks)
- Underlying Fabric dependency: [TaCZ Refabricated Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)
- Port source: [TaCZTweaks Unofficial](https://github.com/q14433686-arch/TaCZTweaks_Unofficial)
- Documentation: [docs](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/tree/HEAD/docs)
- Issues: [issue tracker](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues)
- License: GNU General Public License v3.0
- Third-party notices: [THIRD_PARTY_NOTICES.md](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/blob/HEAD/THIRD_PARTY_NOTICES.md)

This derivative project's code is distributed under GPL-3.0. The licensed original icon and
other third-party resources retain the authorship and license recorded in the notices.
Compatibility with external content does not relicense that content.

## AI disclosure

This project-page text was drafted with generative AI assistance and reviewed by the project
maintainer against the current source. The licensed original TaCZ Tweaks icon is not AI-generated
or AI-modified.
```

## Modrinth fields and media checklist

- **License:** GPL-3.0
- **Environment:** client and server, consistent with the uploaded file
- **Required Dependencies:** Fabric API, Fabric Language Kotlin, TaCZ Refabricated Unofficial, YetAnotherConfigLib, and any other hard dependency declared by that file
- **Optional Dependencies:** Mod Menu and only integrations applicable to that file
- **Versions and channel:** set per file in game version, loader, version number, and release channel metadata
- **Links:** Source and Issues point to this repository; License and environment fields agree with the page
- **AI disclosure:** enable the applicable disclosure because the project-page text was AI-assisted
- **Icon/banner/Gallery:** no AI-generated or AI-modified media
- **Gallery:** every image must be real, relevant to implemented behavior, and have a descriptive title

正文中的依赖列表不能代替每个上传文件的结构化 Dependencies 字段。

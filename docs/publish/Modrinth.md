# Modrinth 发布文案

## Project title

```text
TaCZ Tweaks (Refabricated)
```

项目标题保持为名称本身，不加入 Minecraft 版本、文件版本或发布阶段。

## Summary

```text
A community Fabric port that adds configurable gunplay, movement, balance, interactions, sound, and data-driven customization to TaCZ.
```

摘要为纯文本，不使用 Markdown，也不重复完整项目标题。

## Description

```markdown
# Unofficial community port

> **This is an unofficial Fabric port of TaCZ Tweaks. It is not affiliated with,
> reviewed by, or endorsed by MUKSC, the TaCZ development team, or the maintainers
> of TaCZ Refabricated Unofficial.**

## What this project does

TaCZ Tweaks (Refabricated) brings the configurable gameplay and data-driven systems
of MUKSC's TaCZ Tweaks to Fabric for use with TaCZ Refabricated Unofficial.

It expands TaCZ with gun-handling options, movement controls, global balance
modifiers, projectile and melee interactions, sound and particle rules, server
configuration synchronization, and quality-of-life settings.

## Why use it

The original project targets a different mod-loader environment. This port lets
Fabric players and server owners use the same broad set of tweaks while providing
implementations adapted to the current Fabric and TaCZ APIs.

Highlights include:

- configurable unloading, chambering, cycling, reloading, sprinting, and underwater firing;
- gun tilt, sensitivity, prone movement, hit-marker, and hit-sound controls;
- global damage, headshot, armor penetration, projectile, spread, fire-rate, and recoil modifiers;
- projectile-protection enchantment and shield interaction support;
- gun melee and compatible melee-weapon block interactions;
- configurable impact, fly-by, constant, and environment-dependent sounds;
- data-driven bullet interactions, particles, sounds, and melee rules;
- JSON configuration persistence and server-to-client configuration synchronization;
- in-game configuration through YACL, with optional Mod Menu integration.

## What changed from the original

This is a maintained derivative port, not an unchanged reupload. Port-specific work includes:

- replacing loader-specific events, networking, permissions, tags, and resource reload APIs;
- adapting mixins to the actual methods and descriptors exposed by the Fabric TaCZ port;
- redesigning sound conversion so mutable request state is not attached to shared resource identifiers;
- integrating current shield, projectile protection, prone-rendering, and block-break event paths;
- restoring data-driven managers and legacy-schema conversion;
- synchronizing server-controlled settings and cleaning lifecycle-sensitive caches;
- adding static audits, codec and behavior tests, dedicated-server log checks, and icon provenance checks.

## Requirements and installation

Install the file for your Minecraft and Fabric environment together with all dependencies
listed in that file's **Dependencies** section. Required projects include Fabric API,
Fabric Language Kotlin, TaCZ Refabricated Unofficial, and YetAnotherConfigLib.

Mod Menu is optional. Compatibility integrations are also optional and only activate when
the corresponding project is installed.

For multiplayer, the server and relevant clients must use mutually compatible files and
configuration. Do not infer compatibility from the project title; always read the metadata
attached to the file you download.

## Optional compatibility

The port contains optional integration for Sound Physics Remastered, First Aid New, and
Pillager's Gun. Support is limited to releases explicitly identified by each uploaded file's
metadata and project documentation. Compatibility with a third-party project does not imply
ownership, endorsement, or permission to redistribute that project's assets.

## Support and bug reports

Report port-specific problems through this project's issue templates:

- [Support and troubleshooting](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/blob/HEAD/docs/SUPPORT.md)
- [Bug report](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues/new?template=bug_report.yml)
- [Compatibility report](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues/new?template=compat_report.yml)

A useful report includes exact versions, complete logs, minimal reproduction steps, the
client/server side, relevant configuration, and every required third-party content pack.
Please do not ask the original authors to support problems introduced by this port.

## Credits, source, and license

- Original project and author: [MUKSC/TaCZTweaks](https://github.com/MUKSC/TaCZTweaks)
- Fabric TaCZ dependency: [TaCZ Refabricated Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)
- Port source: [TaCZTweaks Unofficial](https://github.com/q14433686-arch/TaCZTweaks_Unofficial)
- License: GNU General Public License v3.0
- Third-party notices: [THIRD_PARTY_NOTICES.md](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/blob/HEAD/THIRD_PARTY_NOTICES.md)

The icon and bundled third-party resources retain their documented authorship and licenses.
Compatibility with external content does not relicense that content.

## AI disclosure

This project-page description was drafted with generative AI assistance and reviewed by the
project maintainer. The licensed upstream project icon is not AI-generated or AI-modified.
```

## Modrinth fields

- **License:** GPL-3.0
- **Environment:** client and server
- **Required dependencies:** Fabric API, Fabric Language Kotlin, TaCZ Refabricated Unofficial, YetAnotherConfigLib
- **Optional dependencies:** Mod Menu and only the integrations applicable to the uploaded file
- **Links:** Source and Issues point to this repository
- **AI disclosure:** enable the applicable disclosure because the page text was AI-assisted
- **Gallery:** use only real, relevant screenshots and provide a title for every image

所有依赖仍必须填写在每个上传文件的 Dependencies 字段中；正文列表不能代替结构化 metadata。

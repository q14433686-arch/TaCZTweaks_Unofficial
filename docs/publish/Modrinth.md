# Modrinth 发布文案（TaCZ Tweaks 非官方移植）

> 项目级正文。结构与 CurseForge 文案统一，但 Modrinth 允许在正文直接给出源码、下载与 Issues
> 链接。单次版本变化写进版本 Changelog，不累积到项目介绍。
> 具体支持的 Minecraft 版本、加载器与实测状态由每个上传文件的字段和
> [`RELEASE.md`](RELEASE.md) 承载，正文保持长期稳定。

## Project Title

```text
[UNOFFICIAL]TaCZ Tweaks
```

标题只放名称本身，不加 Minecraft 版本、文件版本或发布阶段。

## Summary

```text
An unofficial community port of TaCZ Tweaks, adding configurable gunplay, movement, balance modifiers, data-driven interactions and sounds to unofficial TaCZ ports.
```

摘要为单行纯文本，不使用 Markdown，也不重复完整标题。

## Description

```markdown
# Unofficial community port

> **TaCZ Tweaks (this project) is an unofficial community port of
> [MUKSC/TaCZTweaks](https://github.com/MUKSC/TaCZTweaks). It is not an official
> release, and it has not been reviewed or endorsed by MUKSC, the TACZ Dev Team,
> the maintainers of the underlying TaCZ ports, or the authors of any optional
> compatibility mod.**

This project follows the public GPL source lineage of the original TaCZ Tweaks.
The goal is porting and maintenance for newer Minecraft releases, not designing
new gameplay systems.

## What it adds

TaCZ Tweaks extends TaCZ with options that the base mod does not expose:

- **Gun handling** — unload (creative/survival, magazine and optional chambered
  round), underwater fire lockout, manual bolt, bolt-before-reload, cancel a
  repeated inspect, drop the magazine on reload.
- **Movement** — run-and-gun, sprint while reloading, reload or switch fire mode
  while shooting, gun tilt, a hold-to-reduce-sensitivity key.
- **Global balance modifiers** — damage, player damage, headshot, armor pierce,
  projectile speed, gravity, friction, aim time, per-stance spread, rate of fire
  and recoil. Explosive rounds also route player damage through the player-damage
  modifier, and the gunsmith table property diagram uses the modified baseline.
- **Data-driven systems** — `bullet_interactions`, `bullet_sounds`,
  `bullet_particles` and `melee_interactions`, with selectors for gun, category,
  ammo, regex, predicate, damage, speed, silenced, burst, pellet, random and
  logical combinations. A reloadable example pack ships with the source.
- **Quality of life** — endless-ammo status effect, endermen dodge bullets,
  no refit in adventure mode, RPS display, held-item filtering in the gunsmith
  table, hit-marker and hit-sound control, prone pitch limits and smoothing.

Everything is configured through an in-game screen (Yet Another Config Lib),
persisted as JSON, and server-authoritative options are synchronised to clients.

## Loaders and dependencies

| Item | Notes |
|---|---|
| Loader | Fabric and NeoForge builds are published from the same source tree |
| Required | The matching unofficial TaCZ port for your loader and Minecraft release |
| Required | Yet Another Config Lib |
| Required (Fabric only) | Fabric API and Fabric Language Kotlin |
| Kotlin runtime (NeoForge) | Embedded in the mod file; no extra download |
| Optional | Sound Physics Remastered, First Aid New, Pillager's Gun (Unofficial Port) |

Every file states its exact Minecraft version, loader and dependency versions in
its upload metadata. **Files are not interchangeable between Minecraft releases or
between loaders**, and the TaCZ port version is checked at startup: a mismatched
TaCZ build refuses to load instead of failing later in confusing ways.

## Compatibility boundaries

- Optional-mod integrations are gated at runtime and limited to the version ranges
  declared by the file. Reports outside those ranges should first be reproduced
  inside them.
- The NeoForge builds cannot reproduce the Fabric block-protection event chain
  one-for-one: bullet and melee block breaking runs the vanilla interaction check
  plus the cancellable break event, but protection mods that rely on additional
  "cancelled"/"after" callbacks may not be notified. Verify with your claim mod.
- A feature being listed here does not mean it has been play-tested for every
  Minecraft release. Per-file test status is stated in each release's notes.
- Pre-release files aim to load cleanly with the main paths working. They do not
  promise that every single option has been exercised; the exact verification
  scope for a build is always written in that file's release notes.
- Third-party gun packs and content packs must be verified against the exact
  Minecraft release you run.

## Reporting problems

Reproduce on a minimal environment (Minecraft + loader + the matching TaCZ port +
this mod + Yet Another Config Lib) and attach the complete `latest.log` or crash
report. Multiplayer reports need both server and client logs. Do not report this
port's issues to MUKSC, the TACZ Dev Team, or optional compatibility mod authors.

## Links and credits

- [Original project — MUKSC/TaCZTweaks](https://github.com/MUKSC/TaCZTweaks)
- [Source of this port](https://github.com/q14433686-arch/TaCZTweaks_Unofficial)
- [Downloads and release notes](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/releases)
- [Issue tracker](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues)
- [Unofficial TaCZ port for Fabric — TaCZ Refabricated Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)
- [Unofficial TaCZ port for NeoForge — TaCZ: Renovated](https://github.com/q14433686-arch/TaCZ_Renovated)

Code is GPL-3.0, inherited from the original project by MUKSC. Bundled and
referenced third-party assets keep their own licenses; code licenses do not
automatically cover models, textures, animations or sounds. See
[LICENSE](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/blob/HEAD/LICENSE),
[LICENSES.md](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/blob/HEAD/LICENSES.md)
and
[THIRD_PARTY_NOTICES.md](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/blob/HEAD/THIRD_PARTY_NOTICES.md).

Provided as-is, without warranty. Parts of this project page were drafted with
generative AI assistance and reviewed by the maintainer.
```

## 项目字段

| 字段 | 值 |
|---|---|
| License | `GPL-3.0-only` |
| Environment | Client and server |
| Loaders | Fabric、NeoForge（按实际已发布文件勾选） |
| Game versions | 按每个上传文件实际填写，不写进项目描述 |
| Required dependencies | 对应加载器的非官方 TaCZ 移植、Yet Another Config Lib；Fabric 线另加 Fabric API、Fabric Language Kotlin |
| Optional dependencies | Sound Physics Remastered、First Aid New、Pillager's Gun (Unofficial Port) |
| Source | `https://github.com/q14433686-arch/TaCZTweaks_Unofficial` |
| Issues | `https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues` |
| AI content disclosure | 开启（项目页文案有 AI 辅助；图标与截图非 AI 生成） |

## 单个版本 Changelog 模板

```markdown
## [[完整版本号]]

**Minecraft [[版本]] · [[Fabric/NeoForge]] [[版本]] · Java [[版本]] · TaCZ [[要求的移植版本]]**

### Changes
- [[从该分支 CHANGELOG / GitHub Release 摘取本次变化]]

### Verified in this build
- [[只列本次实际跑过的：构建 / 单测 / 客户端启动 / 专服冒烟 / 游戏内功能]]

### Known boundaries
- [[本版本未实测或不支持的项]]

Files for other Minecraft releases or other loaders are not interchangeable.
Back up worlds and configs before upgrading.
```

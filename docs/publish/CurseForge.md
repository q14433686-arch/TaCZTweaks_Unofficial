# CurseForge 发布文案（TaCZ Tweaks 非官方移植）

> 项目级正文。与 Modrinth 文案同源，仅按 CurseForge 的审核口径调整：不放外部 jar 直链，
> 依赖通过 Relations 声明，版本信息放在文件字段与 Changelog。
> 支持范围与实测状态见 [`RELEASE.md`](RELEASE.md)。

## Project Name

```text
[UNOFFICIAL]TaCZ Tweaks
```

名称里不写 Minecraft 版本、文件版本或发布阶段。

## Summary

```text
An unofficial community port of TaCZ Tweaks, adding configurable gunplay, movement, balance modifiers, data-driven interactions and sounds to unofficial TaCZ ports.
```

## Description

```markdown
# Unofficial community port

> **This project is an unofficial community port of
> [MUKSC/TaCZTweaks](https://github.com/MUKSC/TaCZTweaks). It is not an official
> release and has not been reviewed or endorsed by MUKSC, the TACZ Dev Team, the
> maintainers of the underlying TaCZ ports, or the authors of any optional
> compatibility mod. It is not a re-upload: the loader layer, registration,
> events, networking, data loading and mixin targets were rewritten for the
> current Minecraft releases.**

## What it adds

TaCZ Tweaks extends TaCZ with options the base mod does not expose.

**Gun handling** — unload (creative/survival, magazine and optional chambered
round), underwater fire lockout, manual bolt, bolt-before-reload, cancel a
repeated inspect, drop the magazine on reload.

**Movement** — run-and-gun, sprint while reloading, reload or switch fire mode
while shooting, gun tilt, a hold-to-reduce-sensitivity key.

**Global balance modifiers** — damage, player damage, headshot, armor pierce,
projectile speed, gravity, friction, aim time, per-stance spread, rate of fire and
recoil. Explosive rounds route player damage through the player-damage modifier,
and the gunsmith table property diagram uses the modified baseline.

**Data-driven systems** — `bullet_interactions`, `bullet_sounds`,
`bullet_particles` and `melee_interactions`, with selectors for gun, category,
ammo, regex, predicate, damage, speed, silenced, burst, pellet, random and logical
combinations. A reloadable example pack ships with the source.

**Quality of life** — endless-ammo status effect, endermen dodge bullets, no refit
in adventure mode, RPS display, held-item filtering in the gunsmith table,
hit-marker and hit-sound control, prone pitch limits and smoothing.

Everything is configured in-game through Yet Another Config Lib, persisted as
JSON, and server-authoritative options are synchronised to clients.

## Loaders and dependencies

| Item | Notes |
|---|---|
| Loader | Fabric and NeoForge builds come from the same source tree |
| Required | The matching unofficial TaCZ port for your loader and Minecraft release |
| Required | Yet Another Config Lib |
| Required (Fabric only) | Fabric API and Fabric Language Kotlin |
| Kotlin runtime (NeoForge) | Embedded in the mod file |
| Optional | Sound Physics Remastered, First Aid New, Pillager's Gun (Unofficial Port) |

Each uploaded file declares its exact Minecraft version, loader and dependency
versions. **Files are not interchangeable between Minecraft releases or between
loaders.** The required TaCZ port version is checked when the game starts, so a
mismatched TaCZ build is rejected up front rather than failing later.

## Compatibility boundaries

- Optional-mod integrations are gated at runtime and bounded by the version ranges
  the file declares.
- The NeoForge builds cannot reproduce the Fabric block-protection event chain
  exactly: bullet and melee block breaking runs the vanilla interaction check plus
  the cancellable break event, but protection mods relying on extra
  "cancelled"/"after" callbacks may not be notified. Verify with your claim mod.
- Listing a feature here does not mean it has been play-tested on every Minecraft
  release; per-file test status is stated in that file's Changelog.
- Third-party gun packs and content packs must be verified against the exact
  Minecraft release you run.

## Reporting problems

Reproduce on a minimal environment (Minecraft + loader + the matching TaCZ port +
this mod + Yet Another Config Lib), then attach the complete `latest.log` or crash
report. Multiplayer reports need server and client logs. Do not send this port's
issues to MUKSC, the TACZ Dev Team, or optional compatibility mod authors.

## Links and credits

- Original project: MUKSC/TaCZTweaks on GitHub
- Source, downloads and issue tracker: q14433686-arch/TaCZTweaks_Unofficial on GitHub
- Underlying unofficial TaCZ ports: TaCZ Refabricated Unofficial (Fabric) and
  TaCZ: Renovated (NeoForge)

Code is GPL-3.0, inherited from the original project by MUKSC. Third-party assets
keep their own licenses; code licenses do not automatically cover models, textures,
animations or sounds. See LICENSE, LICENSES.md and THIRD_PARTY_NOTICES.md in the
source repository.

Provided as-is, without warranty. Parts of this project page were drafted with
generative AI assistance and reviewed by the maintainer.
```

## 项目字段与 Relations

| 字段 | 值 |
|---|---|
| Category | Mods → Armor, Tools, and Weapons（可再加 Adventure and RPG） |
| License | `GPL-3.0-only` |
| Environment | Client and server |
| Source | 指向本仓库 |
| Issues | 指向本仓库 Issues |
| Relations · Required | 对应加载器的非官方 TaCZ 移植、Yet Another Config Lib；Fabric 文件另加 Fabric API、Fabric Language Kotlin |
| Relations · Optional | Sound Physics Remastered、First Aid New、Pillager's Gun (Unofficial Port) |

Relations 必须逐文件设置：Fabric 文件不要挂 NeoForge 依赖，反之亦然。

## 上传与审核检查表

1. 文件名保留完整版本标识，游戏版本与加载器由平台字段选择，不塞进项目名；
2. Release type 与该构建的实测程度一致（未做专服与游戏内实测的不要标 Release）；
3. 描述中未出现"官方""授权""与原作者合作"等措辞；
4. 未上传第三方枪包、模型、贴图或音频；
5. 未在项目页放外部 jar 直链；
6. 图标为仓库内有来源与许可记录的图标，截图反映真实游戏画面；
7. 已按提交页当时显示的字段如实完成 AI 披露；
8. 该文件的 Changelog 使用下方模板，且"已核验"一栏只写实际跑过的项。

## 单个文件 Changelog 模板

```markdown
## [[完整版本号]]

**Minecraft [[版本]] · [[Fabric/NeoForge]] [[版本]] · Java [[版本]] · TaCZ [[要求的移植版本]]**

### Changes
- [[本次变化]]

### Verified in this build
- [[构建 / 单测 / 客户端启动 / 专服冒烟 / 游戏内功能，只写真跑过的]]

### Known boundaries
- [[本版本未实测或不支持的项]]

Files for other Minecraft releases or other loaders are not interchangeable.
Back up worlds and configs before upgrading.
```

# TaCZ Tweaks (Refabricated) — 1.21.11

## English summary

**TaCZ Tweaks (Refabricated)** is an **unofficial community Fabric port** of
[MUKSC/TaCZTweaks](https://github.com/MUKSC/TaCZTweaks) (Forge 1.20.1 / TaCZ 1.1.8), built for
the unofficial Fabric port **[TaCZ_Refabricated_Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)**
on **Minecraft 1.21.11** (Java 21). It adds configurable gun handling and movement, global balance
modifiers, projectile and melee interactions, sound and particle rules, data-driven customization,
server configuration synchronization, and quality-of-life controls to Timeless & Classics: Zero.

> **This is NOT an official release and is not affiliated with, reviewed by, or endorsed by
> MUKSC, the TaCZ team, or the TaCZ_Refabricated_Unofficial maintainers.** Before reporting a
> problem, reproduce it with only this mod and its hard dependencies, then repeat the same test
> after removing this mod. Do not ask the original TaCZ Tweaks, TaCZ, or compatibility-mod
> authors to support issues caused by this port.

**Current test build:** Beta-1 (`2.14.2+fabric.1.21.11.Beta-1`)

### Required dependencies

| Dependency | Version |
|---|---|
| Minecraft | `1.21.11` |
| Java | `>=21` |
| Fabric Loader | `>=0.19.3` |
| Fabric API | `0.141.6+1.21.11` (build pinned in `gradle.properties`; metadata uses `*`) |
| [UNOFFICIAL] TaCZ Refabricated | exactly `1.1.8+fabric.1.21.11.R2` |
| Fabric Language Kotlin | `1.13.13+kotlin.2.4.10` |
| YetAnotherConfigLib (YACL) | `3.8.2+1.21.11-fabric` (vendored build; metadata uses `*`) |

Optional compatibility: Mod Menu, Sound Physics Remastered `>=1.5.1 <1.6.0`, First Aid New
`>=1.2.5 <1.3.0`, Pillager's Gun `>=3.2.2 <3.3.0` (see `fabric.mod.json` and
[docs/COMPATIBILITY.md](docs/COMPATIBILITY.md)).

### Documentation and support

- [Documentation index](docs/README.md) · [Installation & build](BUILD.md) · [Configuration](docs/CONFIGURATION.md) · [Compatibility matrix](docs/COMPATIBILITY.md) · [Known issues](docs/KNOWN_ISSUES.md)
- [Support & troubleshooting](docs/SUPPORT.md) · [Bug reports](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues/new?template=bug_report.yml) · [Compatibility reports](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues/new?template=compat_report.yml)
- [Issues](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues) · [Source](https://github.com/q14433686-arch/TaCZTweaks_Unofficial) · License: GPL-3.0 · [Third-party notices](THIRD_PARTY_NOTICES.md)

---

# TaCZ Tweaks (Refabricated) — 1.21.11（中文）

**Fabric 移植版 TaCZ Tweaks**，适配非官方 Fabric 移植
[`TaCZ_Refabricated_Unofficial`](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial) 的
Minecraft **1.21.11** / Java **21** / **混淆 + Loom remap + refmap** 分支。

原项目：**[MUKSC/TaCZTweaks](https://github.com/MUKSC/TaCZTweaks)**（Forge 1.20.1 / TaCZ 1.1.8），
代码遵循 **GPL-3.0** 发布。

当前测试版本：**Beta-1**（`2.14.2+fabric.1.21.11.Beta-1`）。

[文档索引](docs/README.md) · [安装与构建](BUILD.md) · [问题排查与 Bug 提交](docs/SUPPORT.md) · [Issues](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues) · [发布文案](docs/publish/README.md)

> **这是非官方社区移植。** 提交问题前必须完成最小环境测试，并移除本模组做相同条件的对照测试。本移植产生的问题不要提交给原版 TaCZ Tweaks、TaCZ 或兼容模组作者。

---

## 当前状态

这是 **仍在追赶 26.2 已验证实现的 1.21.11 移植分支**。当前已具备：

- 配置系统（YACL v3）、JSON 持久化、客户端/服务端同步、ModMenu 入口；
- 基础枪械/移动 tweak、基础卸弹、基础滑铲同步、共享枪声；
- 全局 modifier、bullet interaction / sound / particle / melee data 与行为；
- old v2 bullet interaction converter、burst/pellet selector、shield 交互、projectile explosion `playerDamage`；
- crawl 第一人称 pitch controller 与 AvatarRenderer 第三人称平滑过渡代码；
- First Aid / Pillager’s Gun / Sound Physics Remastered 的 1.21.11 可选兼容代码路径；
- client-only modifier diagrams、gunsmith safety 守卫；
- 示例资源包 `tacz-tweaks-example-pack/`；
- 面向 1.21.11 混淆环境的 mixin/refmap 工程骨架与静态审计脚本。

本轮修复已补上若干**高优先级安全、功能与发布缺口**：

- MixinExtras 升级到 **0.5.4**，与 mixin JSON 的最低版本声明一致；
- `crawl.LocalPlayerCrawlMixin` 改回 `client` 分组；
- 服务端共享枪声加入开关、存活/持枪、命名空间、距离、浮点合法性与限流校验；
- 滑铲 C2S 改为服务端 executor + 短 lease/逐 tick 复验；
- 配置同步 payload 增加 **1 MiB** 上限、绝对拷贝与失败回滚；
- 卸弹逻辑改为 physical / dummy / FUEL / inventory / closed-bolt 分支；
- `SafeMath.blockBreakingDelta`、`ValueRange`、粒子维度/格式/上限与 whizz 去重补强；
- 恢复 old v2 bullet interaction converter、burst/pellet selector、shield 数据结构、entity pierce prepare/finish、projectile explosion `playerDamage`；
- 恢复 gun melee / LRTactical melee block interaction 与 protected block breaking helper；
- 恢复 client-only modifier diagram mixins 与 gunsmith safety 守卫；
- 确认第三人称枪械渲染修复已由 TaCZ 1.21.11 R2 自带 `ItemInHandLayerMixin` 原生提供，因此本模组不再单独暴露对应开关；
- 恢复 `betterMonoConversion`：把 TaCZ 的 mono 标记重新接回 `GunSoundInstance` → `SoundBufferLibrary` PCM downmix 路径，并按 1.21.11 源码验证 `SoundBuffer` 构造点；
- 恢复 First Aid / Pillager’s Gun / Sound Physics Remastered 可选兼容代码路径；
- 增加 dedicated-server 日志门禁脚本、混淆端口审计脚本、JUnit 基础测试脚手架与 ASCII `GRADLE_USER_HOME` test staging；
- 版本号与说明文档统一对齐到 **Beta-1** 命名，并修正文档中遗留的版本/依赖描述错误。

---

## 仍未完成的差距

以下内容 **不能视为已完成**：

- First Aid / Sound Physics / Pillager’s Gun 的 **1.21.11 实机运行矩阵**；
- `betterMonoConversion` 的 **实机运行验证**（源码与审计例外已补，仍需用户侧带真实 1.21.11 jars/游戏进程做最终确认）；
- 多人/专服/客户端完整实机矩阵；
- 依赖真实 TaCZ / Minecraft jars 的最终 strict audit 与整仓 `./gradlew clean build` 验证。

这表示“**尚未完成实测验收**”，不是“Fabric/1.21.11 做不到”。

---

## 依赖

| 类别 | 依赖 | 版本 / 说明 |
|---|---|---|
| 必需 | Minecraft | `1.21.11` |
| 必需 | Fabric Loader | `>=0.19.3` |
| 必需 | Fabric API | `0.141.6+1.21.11` |
| 必需 | [UNOFFICIAL] TaCZ Refabricated | **`1.1.8+fabric.1.21.11.R2`**（运行时精确验证 friendly string） |
| 必需 | Fabric Language Kotlin | `1.13.13+kotlin.2.4.10` |
| 必需 | YetAnotherConfigLib (YACL) | **`3.8.2+1.21.11-fabric`** |
| 必需 | Java | `>=21` |
| 可选兼容 | Sound Physics Remastered | `>=1.5.1 <1.6.0`；1.21.11 Fabric 已核实对应 `fabric-1.21.11-1.5.1` |
| 可选兼容 | First Aid New | `>=1.2.5 <1.3.0`；1.21.11 Fabric 已核实对应 `firstaid-1.2.5+fabric1.21.11-legacy.jar` |
| 可选兼容 | Pillager’s Gun (Unofficial Port) | `>=3.2.2 <3.3.0`；1.21.11 Fabric 已核实对应 `pillagers_gun-3.2.2 fabric 1.21.11.jar` |

### 可选兼容版本说明（已按 1.21.11 真实发布线核实）

- **Sound Physics Remastered**：1.21.11 Fabric 对应 **`1.5.1`**，不是 1.21.1 / 26.x 的别的发布线；
- **First Aid New**：1.21.11 Fabric 要看 **legacy** 线，即 **`1.2.5+fabric1.21.11-legacy.jar`**；
- **Pillager’s Gun (Unofficial Port)**：1.21.11 Fabric 对应线为 3.2.x；`3.3.x` 是后续 26.x/更高线，**不属于本分支的 1.21.11 目标版本**。

这些范围已与 `fabric.mod.json` 的 `suggests` / `breaks` 同步。

---

## 构建

```bash
# 需要 JDK 21
./gradlew build
# 产物：build/libs/tacztweaks-2.14.2+fabric.1.21.11.Beta-1.jar
# 示例包：build/distributions/tacz-tweaks-example-pack-2.14.2+fabric.1.21.11.Beta-1.zip
```

发布产物 jar 内包含 `META-INF/LICENSE_tacztweaks` 与 `META-INF/THIRD_PARTY_NOTICES_tacztweaks.md`。
本分支运行 Loom remap 模式（`fabric-loom-remap` + Mojang mappings），`checkJarContents` 门禁
检查的是 **remapped release jar**（`remapJar` 任务产物）；在没有 `remapJar` 的环境中会自动
回退检查普通 `jar` 输出。

可用的发布前门禁：

```bash
python3 scripts/check_release_consistency.py
python3 scripts/check_mod_icon.py
python3 scripts/audit_port.py --strict \
  --tacz-jar <path-to-tacz-jar> \
  --minecraft-named-jar <1.21.11 named jar> \
  --minecraft-intermediary-jar <1.21.11 intermediary jar> \
  --refmap build/resources/main/tacztweaks.refmap.json
python3 scripts/check_server_log.py <dedicated-server-latest.log>
```

> TaCZ 和 YACL 通过 CurseMaven / Modrinth Maven 自动解析，不存放在 `libs/` 下。
> 依赖记录在 [`LICENSES.md`](LICENSES.md) 与 [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)。

图标门禁同时进入静态审计和 Gradle `check` / `build` 生命周期，用于锁定 `fabric.mod.json`
路径、有效 IHDR、512×512 尺寸、批准的 SHA-256 与第三方许可记录。`checkJarContents` 也接入 `check` / `build`。

## 许可

- 代码：GPL-3.0（继承原项目 MUKSC/TaCZTweaks）
- 原作者：MUKSC
- 原版图标、嵌入组件与其他第三方来源/许可：[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)

## 问题反馈

提交前请阅读[支持范围与问题反馈](docs/SUPPORT.md)，搜索 open 和 closed issues，并使用对应表单：

- [Bug 报告](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues/new?template=bug_report.yml)
- [内容包 / 兼容性问题](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues/new?template=compat_report.yml)

报告必须包含完整版本、完整日志、从新建存档或测试服务器开始的最小复现步骤、出现环境、
相关配置，以及枪包、可选兼容模组和其他第三方内容的准确名称与完整版本。“最新版”、
截图或单行报错不能代替这些资料。

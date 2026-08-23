# TaCZ Tweaks (Refabricated)
[![CurseForge Downloads](https://cf.way2muchnoise.eu/full_1659175_downloads.svg)](https://www.curseforge.com/minecraft/mc-mods/tacz-tweaks-refabricated)
[![CurseForge Versions](https://cf.way2muchnoise.eu/versions/1659175.svg)](https://www.curseforge.com/minecraft/mc-mods/tacz-tweaks-refabricated/files)
[![GitHub Downloads](https://img.shields.io/github/downloads/q14433686-arch/TaCZTweaks_Unofficial/total?logo=github&label=GitHub%20Downloads)](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/releases)

**English:** unofficial community ports of
[`MUKSC/TaCZTweaks`](https://github.com/MUKSC/TaCZTweaks) v2.14.2.
This default branch (`26.2(main)`) is the **Fabric 26.2** line for
[`TaCZ_Refabricated_Unofficial`](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial).
The same repository also maintains Fabric/NeoForge lines for 26.2, 26.1.2 and 1.21.11 — see
[branch map](docs/BRANCHES.md). Not affiliated with or endorsed by MUKSC, the TACZ Dev Team, or
optional compatibility mod authors. Install the exact dependencies for **your** branch before
filing issues.

**中文：TaCZ Tweaks 非官方社区移植。** 默认分支 `26.2(main)` 是 **Fabric 26.2** 线，适配
[`TaCZ_Refabricated_Unofficial`](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)。
仓库同时维护 NeoForge 与其它 Minecraft 版本线，见[分支对照](docs/BRANCHES.md)。

原项目：[MUKSC/TaCZTweaks](https://github.com/MUKSC/TaCZTweaks) v2.14.2
（Forge 1.20.1 / TaCZ 1.1.8）。代码按 GPL-3.0 发布；本项目与 MUKSC、TACZ Dev Team
无从属或背书关系。

本线当前测试版本：**Beta-1-hotfix**（`2.14.2+fabric.26.2.Beta-1-hotfix`）。

[分支对照](docs/BRANCHES.md) · [文档索引](docs/README.md) · [安装与构建](BUILD.md) · [配置项](docs/CONFIGURATION.md) · [兼容矩阵](docs/COMPATIBILITY.md) · [问题排查与 Bug 提交](docs/SUPPORT.md) · [Issues](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues) · [发布文案](docs/publish/README.md)

> 本项目是非官方社区移植。请先按支持文档完成最小环境对照测试；本移植产生的问题不要提交给原版 TaCZ Tweaks、TaCZ 或兼容模组作者。

## 功能状态

### 枪械、移动与配置

- YACL v3 配置、JSON 持久化、服务端配置同步、ModMenu 入口；
- 卸弹（创造/生存、弹匣及可选枪膛弹）、水下禁射、手动拉栓、换弹前拉栓；
- 跑打、换弹奔跑、射击中换弹/切换开火模式、再次检视取消、换弹丢弃弹匣；
- 枪械倾斜、降低灵敏度键、禁用子弹剔除、隐藏命中标记及命中音控制；
- 动态/静态匍匐俯仰限制和 26.2 `AvatarRenderer` 匍匐过渡平滑；
- 完整全局修饰器：伤害、玩家伤害、爆头、穿甲、弹速、重力、摩擦、举枪时间、
  各姿态扩散、射速和后坐力；爆炸弹对玩家伤害也走 `playerDamage`，改装界面属性图同步使用修饰后基线；
- 无尽弹药状态效果、末影人躲子弹、冒险模式禁改装、RPS 显示、工作台手持筛选。

### 早先误判为“不能做”、现已落地

| 功能 | 26.2 实现 |
|---|---|
| `betterMonoConversion` | 不向共享 `Identifier` 挂每次播放的可变状态；按具体音效请求区分 mono/stereo 缓存，在 `SoundBuffer` 构造前混合左右声道 |
| `bulletProtection` | 在 `EnchantmentHelper#getDamageProtection` 按每件护甲恢复原版弹射物保护公式，并排除虚空弹 |
| 匍匐 `visualTweak` | 精确挂到 `AvatarRenderer.setupRotations(AvatarRenderState, …)`，不使用脆弱局部变量序号 |
| melee 方块交互 | 枪托/刺刀及目标端内置 LRTactical `IMeleeWeapon#performAttack` 均接入 |
| shield 交互 | 接入 26.2 `BlocksAttacks` 组件，支持剩余伤害、耐久和禁用时长 |
| airspace 混响条件音 | 可选兼容 Sound Physics Remastered 1.5.1+26.2 Fabric，使用真实 ray-count 缩放 |
| predicate / tier | 使用 26.2 新包名的 advancement predicate 与 `ToolMaterial` 错误挖掘标签 |
| burst/pellet 选择器 | 挂到目标端 R2 的 `spawnProjectiles` / `runShootCycle` 稳定 hook |
| First Aid | 兼容 First Aid New 1.3.x Fabric 26.2：记录命中位置、按投射物分配部位伤害，并覆盖其会导致 pain post chain 重载失败的多余 DynamicTransforms import |
| Pillager’s Gun | 兼容 3.3.5 Fabric 26.2 的袭击者友伤规则，并读取其 `friendlyFire` 配置 |

`thirdPersonGunRenderingFix` 没有重复实现：TaCZ Refabricated R2 已在
`ItemInHandLayerMixin` 原生修复左右利手、`isSelf` 和副手提交问题。原空开关已删除。

### 数据驱动系统

- `bullet_interactions`：方块/实体/盾牌规则、破坏进度、替换、穿透、伤害；兼容 v2 旧格式并在加载时转换；
- `bullet_sounds`：方块/实体命中、constant、whizz、airspace 条件音；
- `bullet_particles`：方块/实体粒子、绝对/相对/局部坐标、持续发射；
- `melee_interactions`：枪械近战与 LRTactical 近战方块破坏；
- 支持 gun/category/ammo/regex/predicate/damage/speed/silenced/burst/pellet/random 及逻辑组合；
- 示例包：`tacz-tweaks-example-pack/`。

玩家拥有的方块破坏会先检查 `Level.mayInteract`，并调用 Fabric `PlayerBlockBreakEvents` 的 BEFORE/CANCELED/AFTER 完整链；具体领地模组仍需在发布矩阵中逐个验证。粒子只发送到产生它的维度。

## 当前确实没有目标的兼容项

截至 **2026-08-18**，以下项目没有 Fabric 26.2 目标，因此不保留无效配置开关：

- Legendary Survival Overhaul：公开新版为 NeoForge，26.2 仍是计划/实验状态；
- Valkyrien Skies：公开 Fabric 版仍停在 1.20.1；
- MTS / Immersive Vehicles：未找到 Fabric 26.2 发行版。

这表示“当前没有可注入的模组”，不是 Minecraft/Fabric API 无法实现。First Aid、Sound Physics 和
Pillager’s Gun 过去也被误写为不存在，现已根据 26.2 实际发行版更正。

完整证据与逐项结论见 [`AUDIT.md`](AUDIT.md)，移植细节见
[`PORTING_NOTES.md`](PORTING_NOTES.md)。

## 依赖

| 类型 | 依赖 | 版本 |
|---|---|---|
| 必需 | Minecraft | 26.2 |
| 必需 | Fabric Loader | >=0.19.3、<0.20.0 |
| 必需 | Fabric API | >=0.155.2+26.2、<0.157.0 |
| 必需 | [UNOFFICIAL] TaCZ Refabricated | **1.1.8+fabric.26.2.R2 及之后的 R<n> 版本**（运行时按当前 26.2 发布系列校验） |
| 必需 | Fabric Language Kotlin | >=1.13.13、<1.14.0 |
| 必需 | YetAnotherConfigLib | 3.9.6+26.2-fabric |
| 可选 | Sound Physics Remastered | 1.5.1+26.2 Fabric |
| 可选 | First Aid New | >=1.3.0、<1.4.0 Fabric 26.2（shader 覆盖仅验证此范围） |
| 可选 | Pillager’s Gun (Unofficial Port) | >=3.3.5、<3.4.0 Fabric 26.2 |
| 运行环境 | Java | >=25 |

## 构建与审计

```bash
# 需要 JDK 25
python3 scripts/download_dependencies.py --check-only
python3 scripts/audit_port.py --strict
python3 scripts/check_release_consistency.py
./gradlew build
# build/libs/tacztweaks-2.14.2+fabric.26.2.Beta-1-hotfix.jar
```

`scripts/audit_port.py` 会检查 mixin 注册/目标方法、无行为配置项、语言键一致性、
MixinExtras 最低版本，以及模组图标的元数据路径、尺寸、SHA-256 与许可声明。`./gradlew build`
也会通过 `checkModIcon` 执行图标回归门禁。可再传 `--minecraft-jar <loom生成的26.2.jar>` 做原版
类方法审计，或传 `--upstream-root <TaCZTweaks-v2.14.2>` 输出上游差异清单。

## 许可

- 代码：GPL-3.0（继承原项目 MUKSC/TaCZTweaks）
- 原作者：MUKSC
- 许可证总览：[`LICENSES.md`](LICENSES.md)
- 原版图标、嵌入依赖、本地二进制输入和修改后的 First Aid shader 来源/许可证：[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)
- 本地二进制依赖来源和 SHA-256：[`RESOURCE_IMPORT_MANIFEST.tsv`](RESOURCE_IMPORT_MANIFEST.tsv)

## 问题反馈

提交前请阅读[支持范围与问题反馈](docs/SUPPORT.md)，搜索 open/closed issues，并使用对应表单：

- [Bug 报告](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues/new?template=bug_report.yml)
- [内容包 / 兼容性问题](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues/new?template=compat_report.yml)

报告必须包含完整版本、完整日志、最小复现步骤、出现环境、相关配置和第三方内容。仅写“最新版”、仅贴截图或单行报错无法用于定位。

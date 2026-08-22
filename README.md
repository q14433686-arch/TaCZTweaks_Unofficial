# TaCZ Tweaks (Renovated)

**English:** an unofficial NeoForge 26.1.2 community port of
[`MUKSC/TaCZTweaks`](https://github.com/MUKSC/TaCZTweaks) v2.14.2 for
[`TaCZ_Renovated`](https://github.com/q14433686-arch/TaCZ_Renovated).
It is not affiliated with or endorsed by MUKSC, the TACZ Dev Team, or optional compatibility mod
authors. Install the exact dependencies listed below before filing issues. Start with
[documentation](docs/README.md), [configuration](docs/CONFIGURATION.md),
[compatibility](docs/COMPATIBILITY.md), [known issues](docs/KNOWN_ISSUES.md), and
[support requirements](docs/SUPPORT.md).

**中文：TaCZ Tweaks 的 NeoForge 26.1.2 社区移植版**，适配
[`TaCZ_Renovated`](https://github.com/q14433686-arch/TaCZ_Renovated)（modId 仍为 `tacz`）。

原项目：[MUKSC/TaCZTweaks](https://github.com/MUKSC/TaCZTweaks) v2.14.2
（Forge 1.20.1 / TaCZ 1.1.8）。代码按 GPL-3.0 发布；本项目与 MUKSC、TACZ Dev Team
无从属或背书关系。游戏语义来源是本仓库的 Fabric `26.1.2` 分支。

当前版本：**Beta-1**（`2.14.2+neoforge.26.1.2.Beta-1`）。

> **验证状态（重要）**：本分支完成的是**源码级移植**——构建骨架、入口、事件、网络、
> 数据重载、配置屏、compat 门控均已改写为 NeoForge 形态，75 个 mixin 的目标类已对
> TaCZ: Renovated 26.1.2 源码做过静态核对。**但尚未执行过 `./gradlew build`、专服冒烟或
> 客户端实机测试**（移植环境没有 JDK，也无法下载依赖 jar）。在这些验证完成前，
> 请把本分支视为未经测试的工程分支。逐项状态见
> [`docs/records/NEOFORGE_26_1_2_PORT_PLAN.md`](docs/records/NEOFORGE_26_1_2_PORT_PLAN.md)
> 和 [`docs/records/NEOFORGE_26_1_2_PORT_RECORD.md`](docs/records/NEOFORGE_26_1_2_PORT_RECORD.md)。

[文档索引](docs/README.md) · [安装与构建](BUILD.md) · [配置项](docs/CONFIGURATION.md) · [兼容矩阵](docs/COMPATIBILITY.md) · [已知问题](docs/KNOWN_ISSUES.md) · [问题排查与 Bug 提交](docs/SUPPORT.md) · [Issues](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues)

> 本项目是非官方社区移植。请先按支持文档完成最小环境对照测试；本移植产生的问题不要提交给原版 TaCZ Tweaks、TaCZ 或兼容模组作者。

## 功能状态

功能集与 Fabric `26.1.2` 分支一致（同一套 Kotlin/Java 逻辑，只替换加载器表面）：

### 枪械、移动与配置

- YACL v3 配置、JSON 持久化、服务端配置同步、NeoForge 模组列表内的配置屏入口；
- 卸弹（创造/生存、弹匣及可选枪膛弹）、水下禁射、手动拉栓、换弹前拉栓；
- 跑打、换弹奔跑、射击中换弹/切换开火模式、再次检视取消、换弹丢弃弹匣；
- 枪械倾斜、降低灵敏度键、禁用子弹剔除、隐藏命中标记及命中音控制；
- 动态/静态匍匐俯仰限制和 26.1.2 `AvatarRenderer` 匍匐过渡平滑；
- 完整全局修饰器：伤害、玩家伤害、爆头、穿甲、弹速、重力、摩擦、举枪时间、
  各姿态扩散、射速和后坐力；爆炸弹对玩家伤害也走 `playerDamage`，改装界面属性图同步使用修饰后基线；
- 无尽弹药状态效果、末影人躲子弹、冒险模式禁改装、RPS 显示、工作台手持筛选。

### 数据驱动系统

- `bullet_interactions`：方块/实体/盾牌规则、破坏进度、替换、穿透、伤害；兼容 v2 旧格式并在加载时转换；
- `bullet_sounds`：方块/实体命中、constant、whizz、airspace 条件音；
- `bullet_particles`：方块/实体粒子、绝对/相对/局部坐标、持续发射；
- `melee_interactions`：枪械近战与 LRTactical 近战方块破坏；
- 支持 gun/category/ammo/regex/predicate/damage/speed/silenced/burst/pellet/random 及逻辑组合；
- 示例包：`tacz-tweaks-example-pack/`。

### 与 Fabric 版的行为差异（必须知道）

| 项目 | 差异 |
|---|---|
| 方块破坏保护链 | NeoForge 只有可取消的 `BlockEvent.BreakEvent`，没有 Fabric 的 `BEFORE / CANCELED / AFTER` 三段链。玩家造成的破坏仍会走 `Level#mayInteract` + `BreakEvent`，但依赖 “canceled/after” 通知的领地类模组可能收不到回调（语义降级，未实测） |
| 配置屏入口 | 没有 ModMenu；改由 NeoForge 模组列表的 `IConfigScreenFactory` 扩展点打开 |
| Kotlin 运行时 | 没有 Fabric Language Kotlin；kotlin-stdlib 由本模组 `jarJar` 内嵌 |
| 单元测试 | 依赖 Minecraft 引导的 codec / 示例包测试未随本分支保留，只保留纯 JDK 测试 |
| `thirdPersonGunRenderingFix` | Fabric 目标端已原生修复因而删除该开关；NeoForge 目标端（TaCZ: Renovated）**尚未逐项核对**，暂不重新引入开关 |

## 依赖

| 类型 | 依赖 | 版本 |
|---|---|---|
| 必需 | Minecraft | 26.1.2 |
| 必需 | NeoForge | 26.1.2.97 或更高的 26.1.2.x |
| 必需 | [UNOFFICIAL] TaCZ: Renovated | `1.1.8+neoforge.26.1.2.R<n>`，n >= 1（接受 R1 及同发布系列的后续 revision；Minecraft 版本、TaCZ 核心版本和 neoforge release family 严格匹配） |
| 必需（客户端） | YetAnotherConfigLib | 3.9.6 for neoforge 26.1 |
| 可选 | Sound Physics Remastered | 1.5.1+26.1.2 NeoForge |
| 可选 | First Aid New | >=1.2.8、<1.3.0 NeoForge 26.1 |
| 可选 | Pillager’s Gun (Unofficial Port) | >=3.2.2、<3.3.0 NeoForge 26.1.2 |
| 运行环境 | Java | >=25 |

> TaCZ 版本门禁由 `TaczVersionSupport` 在模组构造期执行：接受
> `1.1.8+neoforge.26.1.2.R<n>`（`n >= 1`，revision 数值比较，允许 `-hotfix.1` 之类后缀），
> 拒绝 Fabric 版本串、1.21.11 / 26.2 家族、错误核心版本与畸形 revision。
> 注意与 Fabric 分支（`>= R2`）和 NeoForge 1.21.11 分支（`>= r0`）的阈值不同：
> NeoForge 26.1.2 线的首个发行版就是 R1。

## 可选兼容项状态

| 模组 | NeoForge 26.1.2 发行物 | 本移植状态 |
|---|---|---|
| Sound Physics Remastered | 有（1.5.1+26.1.2） | 已接线，**未实测** |
| First Aid New | 有（1.2.8+neoforge26.1） | 已接线（反射 + mixin 门控），**未对真 jar 核类名，未实测** |
| Pillager’s Gun (Unofficial Port) | 有（3.2.2 neoforge 26.1.2） | 已接线，**未对真 jar 核类名，未实测** |
| LRTactical | 由 TaCZ: Renovated 内置 | 近战接口已接线，**未实测** |
| Legendary Survival Overhaul / Valkyrien Skies / MTS | **未核对** NeoForge 26.1.2 发行情况 | 无配置开关 |

以上"已接线"仅表示代码路径存在且有门控，**不等于兼容性已验证**。

## 构建与审计

```bash
# 需要 JDK 25
python3 scripts/download_dependencies.py --check-only
python3 scripts/audit_port.py --strict
python3 scripts/check_release_consistency.py
./gradlew build
# build/libs/tacztweaks-2.14.2+neoforge.26.1.2.Beta-1.jar
```

详细步骤见 [`BUILD.md`](BUILD.md)。

## 许可

- 代码：GPL-3.0（继承原项目 MUKSC/TaCZTweaks）
- 原作者：MUKSC
- 许可证总览：[`LICENSES.md`](LICENSES.md)
- 图标、依赖与示例包音频来源/许可证：[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)
- 本地二进制依赖来源和 SHA-256：[`RESOURCE_IMPORT_MANIFEST.tsv`](RESOURCE_IMPORT_MANIFEST.tsv)

## 问题反馈

提交前请阅读[支持范围与问题反馈](docs/SUPPORT.md)，搜索 open/closed issues，并使用对应表单：

- [Bug 报告](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues/new?template=bug_report.yml)
- [内容包 / 兼容性问题](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues/new?template=compat_report.yml)

报告必须包含完整版本、完整日志、最小复现步骤、出现环境、相关配置和第三方内容。

# TaCZ Tweaks (Renovated)
[![CurseForge Downloads](https://cf.way2muchnoise.eu/full_1659175_downloads.svg)](https://www.curseforge.com/minecraft/mc-mods/tacz-tweaks-refabricated)
[![CurseForge Versions](https://cf.way2muchnoise.eu/versions/1659175.svg)](https://www.curseforge.com/minecraft/mc-mods/tacz-tweaks-refabricated/files)
[![GitHub Downloads](https://img.shields.io/github/downloads/q14433686-arch/TaCZTweaks_Unofficial/total?logo=github&label=GitHub%20Downloads)](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/releases)

TaCZ Tweaks v2.14.2 的**非官方 NeoForge 26.3 社区移植**，目标依赖为
[`q14433686-arch/TaCZ_Renovated`](https://github.com/q14433686-arch/TaCZ_Renovated)
26.3 分支（modId 仍为 `tacz`）。原项目：
[`MUKSC/TaCZTweaks`](https://github.com/MUKSC/TaCZTweaks)（Forge 1.20.1）。
本项目与 MUKSC、TACZ Dev Team 及可选兼容模组作者无从属或背书关系。

当前产物版本：**`2.14.2+neoforge.26.3.Beta-1`**。
游戏语义来自本仓库 Fabric `26.3` 姊妹线；加载器实现继承已通过 26.2 实机验证的
NeoForge 骨架，再针对 26.3 API、依赖和 mixin 目标更新。

> **26.3 验证状态：编译级移植（未实测）。** 本轮按姊妹线《26.3 移植指南》与
> TaCZ_Renovated 26.2→26.3 实际 diff 逐项适配，mixin 目标经 26.3 反编译源码与
> NeoForge 26.3.x patches 静态核对；移植沙箱无 JDK、无出网构建，`gradlew build`
> 与游戏内验证由 CI 与维护者补做。26.2 线的构建/实机证据不适用于 26.3。
> 执行记录见 [`docs/records/NEOFORGE_26_3_PORT_RECORD.md`](docs/records/NEOFORGE_26_3_PORT_RECORD.md)。

[文档索引](docs/README.md) · [安装与构建](BUILD.md) ·
[配置项](docs/CONFIGURATION.md) · [兼容矩阵](docs/COMPATIBILITY.md) ·
[已知问题](docs/KNOWN_ISSUES.md) · [问题反馈要求](docs/SUPPORT.md)· [CurseForge](https://www.curseforge.com/minecraft/mc-mods/tacz-tweaks-refabricated)

## 功能范围

### 枪械、移动与配置

- YACL v3 配置屏、JSON 持久化、服务端配置同步；
- 卸弹、手动拉栓、换弹前拉栓、水下禁射、跑打、换弹奔跑、换弹取消与丢弃弹匣；
- 枪械倾斜、灵敏度按键、动态/静态匍匐俯仰、命中标记与命中音控制；
- 伤害、玩家伤害、爆头、穿甲、弹速、重力、摩擦、举枪、扩散、射速和后坐力修饰；
- 无尽弹药效果、末影人躲避子弹、冒险模式禁改装、RPS 与工作台筛选。

### 数据驱动系统

- `bullet_interactions`：方块、实体和盾牌规则，含破坏进度、替换、穿透、伤害；
- `bullet_sounds`：命中、constant、whizz、airspace 条件音；
- `bullet_particles`：方块/实体粒子、绝对/相对/局部坐标与持续发射；
- `melee_interactions`：枪械近战与 LRTactical 近战方块破坏；
- 可重载示例包：`tacz-tweaks-example-pack/`。

## 与 Fabric 26.3 的明确差异

| 项目 | NeoForge 26.3 实现/边界 |
|---|---|
| 方块破坏保护链 | 只能使用 `Level#mayInteract` + 可取消的 `net.neoforged.neoforge.event.level.block.BreakBlockEvent`；没有 Fabric `BEFORE / CANCELED / AFTER` 三段链。依赖 canceled/after 通知的保护模组可能收不到等价回调，属于已知语义降级，未实测。 |
| 配置入口 | 没有 Mod Menu；通过 NeoForge 模组列表的 `IConfigScreenFactory` 打开 YACL。 |
| Kotlin 运行时 | NeoForge 没有 Fabric Language Kotlin；`kotlin-stdlib 2.4.10` 由本模组 `jarJar` 内嵌。 |
| 客户端隔离 | 客户端初始化使用独立 `@Mod(..., dist = Dist.CLIENT)`；YACL 屏幕构造在 client 包；payload 客户端工作经反射 bridge 进入，公共入口/handler 不解析 `net.minecraft.client.*`。YACL 配置基类仍供双端持久化/同步使用，因此 YACL 是双端硬依赖。 |
| 测试源集 | ModDevGradle 不处理测试源集；仅保留版本门、数学和拆栈等纯 JDK 测试。 |

## 依赖

| 类型 | 依赖 | 目标版本 |
|---|---|---|
| 必需 | Minecraft | `26.3` |
| 必需 | NeoForge | `26.3.0.51-beta`（26.3 仅 beta 通道；与 TaCZ_Renovated 26.3 钉选一致，转 release 后重钉） |
| 必需 | TaCZ: Renovated | `1.1.8+neoforge.26.3.R<n>`，`n >= 1` |
| 必需（双端） | YetAnotherConfigLib | 26.3 的 3.9.x（上游 port/26.3 线 modVersion=3.9.4），低于 3.10 |
| 可选 | Sound Physics Remastered | `1.5.1+26.3` NeoForge |
| 可选 | First Aid New | `>=1.3.0、<1.4.0` 的 NeoForge 26.3 构建（26.3 发行物未核实） |
| 可选 | Pillager’s Gun (Unofficial Port) | `>=3.3.5、<3.4.0` 的 NeoForge 26.3 构建（26.3 发行物未核实） |
| 运行环境 | Java | 25 |

TaCZ 版本门由 `TaczVersionSupport` 严格检查：仅接受 `1.1.8+neoforge.26.3.R<n>`
（R1 起，revision 数值比较，可带规范后缀），拒绝 Fabric、26.2、26.1.2、1.21.11、r0、错误核心版本和
畸形字符串。Renovated 26.3 首发是 `26.3的mod文件_R1`（产物 `tacz-1.1.8+neoforge.26.3.R1.jar`）。

## 可选兼容状态

| 模组 | 26.3 发行物 | 当前证据 |
|---|---|---|
| Sound Physics Remastered | 上游 master 已钉 26.3（`mod_version=1.5.1+26.3`，2026-10-10 实查）；发布渠道文件名待取件确认 | 26.2 源码中的 `SoundPhysics#evaluateEnvironment(...)` 调用点已静态核对；26.3 侧目标签名未变（类未改动者按 26.2 结论承继），未对发布 jar/实机验证。 |
| First Aid New | 未核实 | 26.2 源码中的 `EventHandler#handleCustomPlayerDamage(...)` 已核对；26.3 发行物与实机均未验证。 |
| Pillager’s Gun (Unofficial Port) | 未核实 | 源码中的 `PillagersGunConfig#values()` / `Values#friendlyFire()` 已按 26.2 核对；26.3 发行物与实机均未验证。 |
| LRTactical | 由 TaCZ: Renovated 内置 | 目标类在 Renovated 26.3 源码中存在（`IMeleeWeapon#performAttack` 未变）；未实测。 |
| Legendary Survival Overhaul / Valkyrien Skies / MTS | 本轮未确认可用目标 | 暂不支持，不提供空开关。 |

“已静态核对”只表示类/方法和调用点存在，不等于兼容性 PASS。

## 构建与门禁

```bash
# JDK 25；先按 BUILD.md 放入 libs/*.jar
python3 scripts/download_dependencies.py --check-only
python3 scripts/check_release_consistency.py
python3 scripts/audit_port.py --strict
./gradlew test
./gradlew build
```

26.3 线当前只到「静态适配完成」一级：CI 四条流程稿（`.github/workflows/`，与 TaCZ 移植仓同款
compile-check / build / consistency / release）已就绪并提交在 `docs/ci/`，待经网页上线（沙箱无 `workflows` 权限），
上线后负责编译与发布门禁；游戏内验证由维护者按
`docs/records/NEOFORGE_26_3_PORT_RECORD.md` §7 清单补做。详细步骤与产物位置见 [`BUILD.md`](BUILD.md)。

## 许可与反馈

- 代码：GPL-3.0；原作者：MUKSC。
- 许可证总览：[`LICENSES.md`](LICENSES.md)。
- 图标、依赖、示例音频来源：[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)。
- 本地依赖来源和摘要：[`RESOURCE_IMPORT_MANIFEST.tsv`](RESOURCE_IMPORT_MANIFEST.tsv)。
- 提交问题前请阅读 [`docs/SUPPORT.md`](docs/SUPPORT.md)，附完整版本、日志和最小复现。

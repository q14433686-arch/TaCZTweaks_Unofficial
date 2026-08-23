# TaCZ Tweaks (Renovated)
[![CurseForge Downloads](https://cf.way2muchnoise.eu/full_1659175_downloads.svg)](https://www.curseforge.com/minecraft/mc-mods/tacz-tweaks-refabricated)
[![CurseForge Versions](https://cf.way2muchnoise.eu/versions/1659175.svg)](https://www.curseforge.com/minecraft/mc-mods/tacz-tweaks-refabricated/files)
[![GitHub Downloads](https://img.shields.io/github/downloads/q14433686-arch/TaCZTweaks_Unofficial/total?logo=github&label=GitHub%20Downloads)](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/releases)

TaCZ Tweaks v2.14.2 的**非官方 NeoForge 26.2 社区移植**，目标依赖为
[`q14433686-arch/TaCZ_Renovated`](https://github.com/q14433686-arch/TaCZ_Renovated)
26.2 分支（modId 仍为 `tacz`）。原项目：
[`MUKSC/TaCZTweaks`](https://github.com/MUKSC/TaCZTweaks)（Forge 1.20.1）。
本项目与 MUKSC、TACZ Dev Team 及可选兼容模组作者无从属或背书关系。

当前产物版本：**`2.14.2+neoforge.26.2.Beta-1`**。
游戏语义来自本仓库 Fabric `26.2(main)` 分支；加载器实现继承已经通过 26.1.2 客户端加载验证的
NeoForge 骨架，再针对 26.2 API、依赖和 mixin 目标更新。

> **26.2 验证状态：Windows / JDK 25 的 `gradlew build` 已 PASS（包含单测与发布门禁）；
> 维护者已进入游戏并报告大多数核心功能通过实际测试。** 这是 Beta 阶段的冒烟/回归证据，
> 不是对每个配置组合、数据包、可选兼容模组或边界条件的保证。专服 `Done (...)!` 尚未报告，
> 各游戏内场景也没有逐项测试记录。详细证据与边界见
> [`docs/records/NEOFORGE_26_2_PORT_RECORD.md`](docs/records/NEOFORGE_26_2_PORT_RECORD.md)。

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

## 与 Fabric 26.2 的明确差异

| 项目 | NeoForge 26.2 实现/边界 |
|---|---|
| 方块破坏保护链 | 只能使用 `Level#mayInteract` + 可取消的 `net.neoforged.neoforge.event.level.block.BreakBlockEvent`；没有 Fabric `BEFORE / CANCELED / AFTER` 三段链。依赖 canceled/after 通知的保护模组可能收不到等价回调，属于已知语义降级，未实测。 |
| 配置入口 | 没有 Mod Menu；通过 NeoForge 模组列表的 `IConfigScreenFactory` 打开 YACL。 |
| Kotlin 运行时 | NeoForge 没有 Fabric Language Kotlin；`kotlin-stdlib 2.4.10` 由本模组 `jarJar` 内嵌。 |
| 客户端隔离 | 客户端初始化使用独立 `@Mod(..., dist = Dist.CLIENT)`；YACL 屏幕构造在 client 包；payload 客户端工作经反射 bridge 进入，公共入口/handler 不解析 `net.minecraft.client.*`。YACL 配置基类仍供双端持久化/同步使用，因此 YACL 是双端硬依赖。 |
| 测试源集 | ModDevGradle 不处理测试源集；仅保留版本门、数学和拆栈等纯 JDK 测试。 |

## 依赖

| 类型 | 依赖 | 目标版本 |
|---|---|---|
| 必需 | Minecraft | `26.2` |
| 必需 | NeoForge | `26.2.0.64` 或同 Minecraft 线的后续版本 |
| 必需 | TaCZ: Renovated | `1.1.8+neoforge.26.2.R<n>`，`n >= 1` |
| 必需（双端） | YetAnotherConfigLib | `3.9.5+26.2-neoforge` 起，低于 3.10 |
| 可选 | Sound Physics Remastered | `1.5.1+26.2` NeoForge |
| 可选 | First Aid New | `>=1.3.0、<1.4.0` 的 NeoForge 26.2 构建 |
| 可选 | Pillager’s Gun (Unofficial Port) | `>=3.3.5、<3.4.0` 的 NeoForge 26.2 构建 |
| 运行环境 | Java | 25 |

TaCZ 版本门由 `TaczVersionSupport` 严格检查：仅接受 `1.1.8+neoforge.26.2.R<n>`
（R1 起，revision 数值比较，可带规范后缀），拒绝 Fabric、26.1.2、1.21.11、r0、错误核心版本和
畸形字符串。不要照抄 Fabric 26.2 的 R2 门槛；Renovated 26.2 首发就是 R1。

## 可选兼容状态

| 模组 | 26.2 发行物 | 当前证据 |
|---|---|---|
| Sound Physics Remastered | 有：1.5.1+26.2 NeoForge | 26.2 源码中的 `SoundPhysics#evaluateEnvironment(...)` 调用点已静态核对；未对发布 jar/实机验证。 |
| First Aid New | 有：1.2.8 与 1.3.0-patched NeoForge 26.2（本移植仅声明 1.3.x） | 26.2 源码中的 `EventHandler#handleCustomPlayerDamage(...)`、`#recordProjectileHit(Player, Entity, Vec3)` 已核对；未实测。 |
| Pillager’s Gun (Unofficial Port) | 有：3.3.5 NeoForge 26.2 | 源码中的 `PillagersGunConfig#values()` / `Values#friendlyFire()` 已核对；发布 jar 与实机未验证。 |
| LRTactical | 由 TaCZ: Renovated 内置 | 目标类在 Renovated 26.2 源码中存在；未实测。 |
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

已报告 `build` PASS 和客户端核心功能冒烟；尚未报告专服 `Done (...)!`，也没有对拿枪、开镜、
换弹、卸弹、配置屏、数据包重载及可选兼容项逐项留证。Beta 只代表当前测试覆盖，不构成全面保证。
详细步骤与产物位置见 [`BUILD.md`](BUILD.md)。

## 许可与反馈

- 代码：GPL-3.0；原作者：MUKSC。
- 许可证总览：[`LICENSES.md`](LICENSES.md)。
- 图标、依赖、示例音频来源：[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)。
- 本地依赖来源和摘要：[`RESOURCE_IMPORT_MANIFEST.tsv`](RESOURCE_IMPORT_MANIFEST.tsv)。
- 提交问题前请阅读 [`docs/SUPPORT.md`](docs/SUPPORT.md)，附完整版本、日志和最小复现。

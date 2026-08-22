# 审计工单：WP-TWEAKS-0 —— TaCZTweaks_Unofficial → NeoForge 1.21.11 可行性审计

> **文档性质**：移植立项前的可行性审计工单（对应决策备忘
> `DECISION_TACZTWEAKS_PORT.md`，发起人已选定「先审计」路径）。
> 执行 AGENT 开工前先读本线 [`CHARTER.md`](CHARTER.md) / [`AGENTS.md`](AGENTS.md)
> 与 [`docs/PORT_1_21_11_BRIEF.md`](docs/PORT_1_21_11_BRIEF.md)（其红线全部继续有效）。
> 审计结论 + 证据写入 `docs/records/`，**不得**在审计未完成时声称可移植性。

---

## 0. 一句话目标

对 [TaCZTweaks_Unofficial](https://github.com/q14433686-arch/TaCZTweaks_Unofficial)
**`1.21.11` 分支（冻结基线 commit `9d7d7b0`，2026-08-22）** 做可移植性审计：
逐项核实「76 个 mixin 的目标对账 / 12 处 Fabric 依赖的 NeoForge 等价映射 /
非 mixin 文件改写清单 / 兼容模组可用性 / 数据资源面」，
输出证据文档 + 工作包切分草案（WP-TWEAKS-1..n），供发起人做 GO/NO-GO 决策。

**产出即决策依据，不是移植本身。** 审计不通过的项目逐条记录，不硬修。

## 1. 基线与环境事实（2026-08-22 已联网核实，可直接引用）

| 事实 | 值 |
|---|---|
| 源仓库 | q14433686-arch/TaCZTweaks_Unofficial，`1.21.11` 分支头 `9d7d7b0`，GPL-3.0 |
| 体量 | 88 java 文件；**76 个 mixin/accessor/mixininterface**；纯 Java（test 含 3 个 .kt） |
| mixin 目标 | 主要为 tacz/LR 内部类（`remap=false`）＋少量原版类（`remap=true`），无 `targets=` 字符串 |
| Fabric 耦合 | **12 处 fabric import，集中 4 文件**：`TaCZTweaks.java`（入口/网络/事件）、`TaCZTweaksClient.java`（客户端入口/YACL 屏）、`compat/ModMenuApiImpl.java`、`client/input/`（ReduceSensitivity / TiltGun / Unload 三键） |
| 依赖 | `tacz`（pin `=1.1.8+fabric.1.21.11.R2`）、fabric-api `>=0.141.6`、**YACL `3.8.2`**、MixinExtras `0.5.4`、FLK（仅测试） |
| 原始上游 | MUKSC/TaCZTweaks（Forge 1.20.1 / Kotlin / v2.14.2）——**仅语义出处**，非平移来源 |
| 姊妹侧成熟度 | Beta-1：First Aid / SPR / Pillager's Gun 实机矩阵、betterMonoConversion 实机、多人/专服矩阵、strict audit 未完成 |
| 本地资产 | 姊妹 1.21.11 源码 tarball 已暂存（`/tmp/tweaks-src`，88 文件全量）；本线 release 源码 zip 是完整真相源 |

## 2. 权威与参考边界

| 源 | 角色 |
|---|---|
| 姊妹 tweaks `1.21.11` 分支（冻结 `9d7d7b0`） | **语义唯一权威**。禁止照抄其 Fabric API 表面 |
| MUKSC/TaCZTweaks（Forge 1.20.1/Kotlin） | 语义出处与功能核对参考（如 YACL 屏设计、配置项语义），不做代码平移 |
| 本线 tacz（TaCZ-Renovated 1.21.11） | **代码出发点 + NeoForge 习语权威**（WP-11211 的全部模式：MDG 骨架 / PayloadRegistrar / 事件面 / libs escape hatch / 审计脚本） |
| CurseForge `tacz-port` jar | 洁净室红线原文有效 |

## 3. 审计清单（全部有证据才勾选，证据入 records/WP_TWEAKS_0_AUDIT.md）

### A. mixin 目标全量对账（76 个，核心工作量）

解析 `/tmp/tweaks-src` 全部 `@Mixin(value=...)` 目标，分三类核实：

1. **tacz/LR 内部类目标**：与本线 release 源码 zip 逐类比对——
   类存在性（源级 grep 已粗验主要目标全在）+ **方法/字段级签名比对**
   （对每个 `method = "..."` 注点，在本线对应源文件里核对同名方法及形参）。
   已知差异高危面要专项核对：本线重写过的渲染体系（scope 包、IrisCompat 不同形态）、
   网络层（PayloadRegistrar vs Fabric payload）、LR 层（我们与姊妹 LR 同为 me.xjqsh 包，
   但接线细节可能不同）。
2. **原版类目标**：对 1.21.11 named jar（NeoForm rename 产物，缺失则重建）javap 逐条核。
   Fabric 与 NeoForge 同为官方映射，预期源级一致，但仍须逐条过（本线 GameRendererMixin
   教训：签名漂移只会在运行期爆炸）。
3. **第三方目标**（First Aid / SPR / Pillager's Gun / LR）：核查 NeoForge 侧对应类存在性
   （有 NeoForge 构建的拉 jar javap；没有的记录"保留代码+运行期不触发"结论）。

### B. 12 处 Fabric 依赖 → NeoForge 等价映射（逐条）

对 `/tmp/tweaks-src` 里每个 fabric import 给出映射（预期命中本线已验证习语）：

- 事件（Fabric 的注册/事件 → NeoForge 事件总线/ClientTickEvent 等）；
- 按键注册（`KeyBindingHelper` → NeoForge KeyMapping + 注册面）；
- 网络（payload → `PayloadRegistrar` + `ServerMessageXxx` 模式，注意 config 同步 1 MiB 上限等姊妹补强逻辑要一并平移）；
- 配置（YACL 3.8.2 NeoForge API + 配置屏注册；`ModMenuApiImpl` → NeoForge 替代：评估用 YACL 自身 Forge API 或本线 tacz 的配置屏惯例，**ModMenu 不可用，入口需替换方案**）；
- registry / particle / tag 等残余面逐项查。

### C. 非 mixin 文件改写清单（逐文件）

`TaCZTweaks` / `TaCZTweaksClient` / `CrawlPitchController` / 三按键 /
`FirstAidCompat` / `PillagersGunCompat` / `ModMenuApiImpl` + `compat-stubs-src`
（soundphysics/firstaid 编译期 stub 的 NeoForge 侧处理）——每文件给出：
现状、改写要点、预计行数级规模。

### D. 依赖钉版核查（网络实证）

| 依赖 | 目标 |
|---|---|
| YACL | 已证 `3.8.2+1.21.11-neoforge` 存在（Modrinth）。确认 maven 坐标/本地 jar 方案 |
| MixinExtras | 0.5.4 jar 依赖方案（本线 tacz 已在用其 WrapOperation） |
| First Aid | 查 NeoForge 1.21.11 构建（有则兼容路径可实测，无则记录） |
| Sound Physics Remastered | 同上 |
| Pillager's Gun | 大概率 Fabric-only → 记录口径 |
| tacz 依赖声明 | 改 `tacz >= 1.1.8+neoforge...` 谓词（注意 modId 同为 `tacz`） |

### E. 数据/资源面

tweaks 的 data-driven 系统（配置 schema、lang、示例资源包 tacz-tweaks-example-pack）
是否纯资源平移；`data/` 目录里的 json 是否引用了 Fabric 侧路径。

### F. 风险与边界结论

- 姊妹 Beta-1 未验证项清单在 NeoForge 侧的继承方式（跟随验收 vs 声明不覆盖）；
- tacz 内部类耦合点清单（未来我们改 tacz 时哪些 tweaks mixin 会断）;
- 结论评级：全绿 / 有条件可移植（列条件）/ 不可移植（列阻塞项）。

## 4. 审计方法与环境

- `/tmp/tweaks-src` 已就绪（88 文件 tarball 解包，`grep`/`python` 可离线解析）；
- 本线真相源：`release/tacz-renovated-1.21.11-r0-source.zip`（本环境工作树会被修剪，
  **一切比对以 zip 为准**）；
- 1.21.11 named jar：`~/.gradle/caches/neoformruntime/.../rename_*_output.jar`（缺失则
  `JAVA_TOOL_OPTIONS="-Xmx320m ..." ./gradlew compileJava --no-parallel` 重建）；
- 复用 `tacz-port-11211/tools/audit_mixins.py`（改路径后直接跑 tweaks 源码）。

## 5. 纪律复述

1. 洁净室：`tacz-port` jar 红线照旧；MUKSC 原版 Kotlin 代码不照抄、只做语义核对。
2. 证据：每条结论指认 `类#方法(签名)` + 来源层级，入 records。
3. 不得声称未实现：审计输出的"可移植"仅指代码面；运行面未验证项一律标注。
4. 冻结基线：姊妹 `9d7d7b0` 之后的新提交**不追**（同步另排期）。

## 6. 产出

- `docs/records/WP_TWEAKS_0_AUDIT.md`：A–F 全表 + 证据 + 评级结论；
- WP-TWEAKS-1..n 工作包切分草案（骨架→编译收敛→实机矩阵→发布）；
- GO/NO-GO 建议交发起人拍板。

---

*调研快照：2026-08-22。链接与版本以执行当日实况为准。*

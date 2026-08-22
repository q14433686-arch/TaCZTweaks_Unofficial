# 发布与更新文档规范：TaCZ Tweaks 非官方移植

> 本文件是发布文案的内部维护说明，也是**发布状态的唯一事实来源**。项目级介绍保持稳定；
> 版本级变化来自对应分支的 `CHANGELOG.md`、兼容矩阵与 `docs/records/` 的测试记录。
> 禁止把多个历史版本的修复列表持续堆进平台首页。

## 1. 当前项目范围（截至 2026-08-23）

### Fabric 线（依赖 TaCZ Refabricated Unofficial）

| Minecraft | 分支 | 版本 | 发布页 |
|---|---|---|---|
| 26.2 | [`26.2(main)`](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/tree/26.2(main)) | `2.14.2+fabric.26.2.Beta-1-hotfix` | [26.2_BETA_1_HOTFIX](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/releases/tag/26.2_BETA_1_HOTFIX) |
| 26.1.2 | [`26.1.2`](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/tree/26.1.2) | `2.14.2+fabric.26.1.2.Beta-1-hotfix` | [26.1.2_BETA_1_HOTFIX](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/releases/tag/26.1.2_BETA_1_HOTFIX) |
| 1.21.11 | [`1.21.11`](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/tree/1.21.11) | `2.14.2+fabric.1.21.11.Beta-1-hotfix` | [1.21.11_BETA_1_HOTFIX](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/releases/tag/1.21.11_BETA_1_HOTFIX) |

### NeoForge 线（依赖 TaCZ: Renovated）

| Minecraft | 分支 | 版本 | 状态 |
|---|---|---|---|
| 26.2 | `arena/01a02ad6-…`（待并入 `26.2-neoforge`） | `2.14.2+neoforge.26.2.Beta-1` | 移植完成，编译与单机实测通过，尚未发布 |
| 26.1.2 | `arena/01a02a8c-…`（待并入 `26.1.2-neoforge`） | `2.14.2+neoforge.26.1.2.Beta-1` | 移植完成，尚未发布 |
| 1.21.11 | [`1.21.11-neoforge`](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/tree/1.21.11-neoforge) | `2.14.2+neoforge.1.21.11.Beta-1-hotfix` | 已合入分支，未单独建发布页 |

- Fabric 与 NeoForge 的文件**不可互换**，不同 Minecraft 版本的文件同样不可互换。
- 三条 Minecraft 线共用项目级边界，但**测试结论不跨版本、不跨加载器继承**。
- 必需前置：对应加载器的 TaCZ 移植 + Yet Another Config Lib；Fabric 线另需 Fabric API 与
  Fabric Language Kotlin。

## 2. 实测状态（只写实际跑过的）

| 构建 | 编译 | 单元测试 | 客户端启动 | 专服冒烟 | 游戏内功能 |
|---|---|---|---|---|---|
| Fabric 26.2 / 26.1.2 / 1.21.11 | 见各分支 `CHANGELOG.md` 与 `AUDIT.md` | 同左 | 同左 | 同左 | 同左 |
| NeoForge 1.21.11 | PASS（Beta-1 基线） | 版本门单测已编写 | PASS（用户实测） | PASS | 未完整覆盖 |
| NeoForge 26.1.2 | **PASS**（维护者本机 JDK 25） | 未跑 | **PASS**（2026-08-23，客户端加载至主界面无 mixin 错误） | **未做** | **未做** |
| NeoForge 26.2 | **PASS** | 未跑 | **PASS** | **未做** | **部分**（2026-08-23 单机实测，主要功能生效；未逐项全覆盖） |

NeoForge 26.2 的实测口径（维护者 2026-08-23 反馈原话归档）：**编译与单机（客户端 + 集成服）
通过，游戏内功能已实测且大多数生效，不保证全部生效**。因此对外只能写「主要功能已实测可用」，
不得写成「全部功能已验证」。专用服务器冒烟未执行。

### 2.1 预发布构建的验收口径

预发布（Beta 阶段）文件的验收目标是：**能加载、不崩溃、主要路径可用**。它不承诺逐功能矩阵
全覆盖，也不承诺与任意第三方枪包/模组组合都成立。写文案时按下面三档区分，不许升级：

| 档位 | 含义 | 允许的措辞 |
|---|---|---|
| 已验证 | 本次构建实际跑过并通过 | "verified in this build" / "本次已核验" |
| 已实测但未全覆盖 | 跑过、主要路径可用，存在未逐项检查的功能 | "mostly working, not exhaustively tested" / "主要功能已实测可用，未逐项全覆盖" |
| 未实测 | 没跑过 | "not tested" / "未实测" |

任何未跑过的项目一律进 Release 正文的「本次未验证」，不得省略；未完成项在对外文案里写
「未实测」，不得写成「已支持 / 已修复」。

### 2.2 各线未完成事项

NeoForge 26.2 线：

- 专服冒烟（`scripts/check_server_log.py`）未执行；
- 游戏内功能矩阵未逐项覆盖（维护者口径：大多数生效）；
- 依赖摘要、可选兼容（Sound Physics / First Aid / Pillager's Gun / LRTactical）的真 jar 类名核对
  与实测状态以该分支的 `docs/records/` 为准。

NeoForge 26.1.2 线：

- 专服冒烟（`scripts/check_server_log.py`）与游戏内功能矩阵（拿枪、开镜、换弹、卸弹、配置屏、
  数据包重载）未执行；
- `RESOURCE_IMPORT_MANIFEST.tsv` 中 YACL 与两个可选兼容 jar 的 SHA-256 仍为 `pending`；
- 四个可选兼容（Sound Physics、First Aid、Pillager's Gun、LRTactical）代码路径已接线，
  **未对第三方真 jar 逐一核对类名，也未实测**；
- NeoForge 侧方块破坏保护为语义降级，见 `docs/KNOWN_ISSUES.md`。

## 3. 文案分层

| 层级 | 文件 / 平台 | 应写内容 | 不应写内容 |
|---|---|---|---|
| 项目级 | `Modrinth.md`、`CurseForge.md`、`MCMOD.md` | 项目定位、加载器与依赖关系、安装、长期边界、来源与许可 | 具体 Minecraft/文件版本、某次提交的修复、过期实测结论 |
| 版本级 | 平台文件 Changelog、GitHub Release、本文件 §1–§2 | 该构建的准确环境、本次变化、本次核验项、升级注意 | 其他分支未经验证的结论 |
| 仓库活文档 | 各分支 `README.md`、`CHANGELOG.md`、`docs/COMPATIBILITY.md`、`docs/KNOWN_ISSUES.md` | 当前实现与证据索引 | 平台营销措辞 |
| 审计记录 | `docs/records/` | 当时的 API 证据与测试记录 | 事后覆盖改写 |

更新顺序固定为：**代码/配置 → 分支活文档 → GitHub Release → 平台文件 Changelog →（必要时）项目级介绍**。

## 4. GitHub Release 正文模板

```markdown
# TaCZ Tweaks — [[Minecraft 版本]] / [[Fabric 或 NeoForge]]（非官方移植）

> **非官方社区移植，不是 MUKSC 的官方发布，也未获 TACZ Dev Team 审核或背书。
> 本移植的问题请提交到本仓库，不要打扰原作者或可选兼容模组作者。**

## 环境

- Minecraft：**[[版本]]**
- 加载器：**[[Fabric/NeoForge 的准确版本或范围]]**
- Java：**[[版本]]+**
- Mod：**`[[gradle.properties 的完整 mod_version]]`**
- 必需前置：**[[TaCZ 移植的要求版本]]、Yet Another Config Lib[[、Fabric API、Fabric Language Kotlin]]**
- 可选：Sound Physics Remastered、First Aid New、Pillager's Gun（非官方移植）

## 本次变化

- [[从该分支 CHANGELOG 摘取]]

## 本次已核验

- [[逐项列出真实跑过的：gradlew build / gradlew test / 客户端启动 / 专服 Done(...)! / 游戏内功能]]

## 本次未验证

- [[明确列出未做的项，不要留空]]

## 升级注意

不同 Minecraft 版本与不同加载器的文件不可互换。升级前备份存档与配置
（`config/tacztweaks.json`）。
```

## 5. 平台文件 Changelog 模板

见 [`Modrinth.md`](Modrinth.md) 与 [`CurseForge.md`](CurseForge.md) 末尾，两站共用同一结构：
`Changes` / `Verified in this build` / `Known boundaries`。

## 6. 发布前门禁

```bash
python3 scripts/download_dependencies.py --check-only
python3 scripts/audit_port.py --strict
python3 scripts/check_release_consistency.py
./gradlew build
```

以及人工确认：

- [ ] 改过 `mod_version` → README、BUILD、CHANGELOG 三处已同步；
- [ ] 本文件 §1、§2 已按本次结果更新；
- [ ] 平台项目页未出现 Minecraft 版本、文件版本或发布阶段编号；
- [ ] Release type（Alpha/Beta/Release）与 §2 的实测程度一致；
- [ ] 依赖字段与该分支的 mod 元数据一致；
- [ ] 未实测项已在 Release 正文的「本次未验证」中列出。

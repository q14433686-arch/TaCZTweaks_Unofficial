# 发布文案与平台检查表（TaCZ Tweaks 非官方移植）

> 本目录只放**项目级**长期文案与平台检查表。项目页保持稳定：Minecraft、加载器、依赖和发布
> 阶段由每个上传文件的平台字段承载，不写进项目标题、摘要或长期描述。
> 版本级内容（当前支持范围、每次构建的变化与实测结论）见 [`RELEASE.md`](RELEASE.md)。

## 文件

| 文件 | 用途 | 语言 |
|---|---|---|
| [`Modrinth.md`](Modrinth.md) | 标题、摘要、描述、项目字段、单版本 Changelog 模板 | 英文正文 |
| [`CurseForge.md`](CurseForge.md) | 名称、摘要、描述、审核与字段检查表 | 英文正文 |
| [`MCMOD.md`](MCMOD.md) | MC 百科词条正文（BBCode）与维护清单 | 中文正文 |
| [`RELEASE.md`](RELEASE.md) | 发布状态、文案分层规则、GitHub Release 与平台 Changelog 模板 | 中文 |

## 文案分层（写之前先确认自己在写哪一层）

| 层级 | 落点 | 应写 | 不应写 |
|---|---|---|---|
| 项目级 | `Modrinth.md`、`CurseForge.md`、`MCMOD.md` | 项目定位、加载器与依赖关系、安装方式、长期边界、来源与许可 | 具体 Minecraft/文件版本、单次修复列表、过期实测结论 |
| 版本级 | 平台文件 Changelog、GitHub Release、`RELEASE.md` | 该构建的准确环境、这次改了什么、这次核验了什么 | 其他分支未验证的结论 |
| 仓库活文档 | 各分支 `README.md`、`CHANGELOG.md`、`docs/COMPATIBILITY.md` | 当前实现与证据索引 | 平台营销措辞 |
| 审计记录 | `docs/records/` | 当时的 API 证据与测试记录 | 事后覆盖改写 |

更新顺序固定为：**代码 → 分支活文档 → GitHub Release → 平台文件 Changelog →（必要时）项目级介绍**。

## 两个平台都必须做到

1. 明确写出这是非官方社区移植，未获 MUKSC、TACZ Dev Team、底层 TaCZ 移植维护者或任何可选兼容
   模组作者的审阅与背书；
2. 说明它不是原文件重传，并说明相较原项目实际做了哪些移植工作；
3. License 选 GPL-3.0，保留原作者、上游项目与第三方资产署名；
4. 每个上传文件的 metadata / Dependencies 必须准确填写游戏版本、加载器、必需依赖与可选依赖；
5. 不承诺所有第三方枪包、内容包或可选模组都兼容；
6. 支持链接指向本仓库 Issue 模板，不把本移植的问题转交给上游作者；
7. 不提供未经许可的第三方枪包、贴图、模型、动画或音频；
8. 图标使用仓库内有来源与许可记录的图标（`scripts/check_mod_icon.py` / Gradle `checkModIcon` 会校验）。

## 发布前门禁

```bash
python3 scripts/download_dependencies.py --check-only
python3 scripts/audit_port.py --strict
python3 scripts/check_release_consistency.py
./gradlew build
```

`audit_port.py` 会顺带检查本目录：**项目级文案不得包含当前 Minecraft 版本、当前 `mod_version`
或带编号的发布阶段字样**。这是有意的——项目页要能跨版本复用。

然后逐条确认：

- 项目名称里没有游戏版本、文件版本或发布阶段；
- 摘要是单行纯文本；
- 没有把可选兼容写成硬依赖；
- 上传文件的依赖字段与该分支的 mod 元数据（Fabric 线 `fabric.mod.json` / NeoForge 线
  `neoforge.mods.toml`）一致；
- Source、Issues、License 与环境 metadata 均已填写；
- 项目页没有外部 jar 下载直链；
- 该文件的实测状态与 `RELEASE.md` 一致，未实测的不写成已支持。

## AI 披露

这些发布文案使用了生成式 AI 辅助并由维护者复核。Modrinth 规则要求对使用生成式 AI 辅助的项目页
作相应披露；发布者应启用平台的 AI content disclosure 并保留文案中的说明。**不得**使用 AI 生成或
AI 修改的项目图标、横幅或 Gallery 图片。CurseForge 发布者按其提交页当时显示的披露字段如实勾选。
任何展示图片必须反映真实游戏内容。

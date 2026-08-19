# 发布文案与平台检查表

本目录提供 Modrinth 与 CurseForge 的独立长期文案。项目页保持稳定；Minecraft、加载器、Java、依赖、文件版本和发布渠道由每个上传文件的平台字段承载，不写入项目标题、摘要或长期描述。

## 文件

- [`Modrinth.md`](Modrinth.md)：稳定标题、单行摘要、英文主体文案与字段检查表
- [`CurseForge.md`](CurseForge.md)：稳定项目名、英文主体文案与审核检查表

## 两个平台都必须做到

1. 明确说明这是非官方社区移植，未获 MUKSC、TaCZ 团队或底层 Fabric 移植维护者审核、背书；
2. 说明它不是原文件重传，并列出相较原版的实质性移植工作；
3. 只宣传当前源码确实实现的功能；尚未完成的实机矩阵不得包装为兼容保证；
4. 正确选择 GPL-3.0，并保留原作者、上游项目、原版图标和其他第三方资产的独立署名与许可边界；
5. 在每个上传文件的 metadata/Dependencies 中填写准确的游戏版本、加载器、必需依赖、可选依赖和 release channel；
6. 不承诺所有第三方枪包、数据包、资源包或可选模组都兼容；
7. 支持链接指向本仓库文档和 Issue 表单，不把本移植产生的问题转交给原作者；
8. 不提供无权分发的第三方枪包、贴图、模型、动画、音频或外部 jar 下载；
9. 图标只使用仓库内已记录不可变来源、许可和校验和的方形原版图标。

## 发布前检查

```bash
python3 scripts/download_dependencies.py --check-only
python3 scripts/check_release_consistency.py
python3 scripts/check_mod_icon.py
python3 scripts/audit_port.py --strict \
  --tacz-jar <matching-tacz-jar> \
  --minecraft-named-jar <matching-named-jar> \
  --minecraft-intermediary-jar <matching-intermediary-jar> \
  --refmap <generated-refmap>
./gradlew clean build
```

然后确认：

- 项目名称没有游戏版本、文件版本或发布阶段；
- 摘要是单行纯文本；
- `docs/publish/` 没有当前游戏/模组版本或带编号的发布阶段；
- 上传文件的 game version、loader、environment、release channel 与 Dependencies 字段和 `fabric.mod.json` 一致；
- 硬依赖与可选兼容没有混淆；
- Source、Issues、License 与文档链接指向正确仓库；
- 图片真实、相关、拥有使用权且未由生成式 AI 生成或修改；
- 项目页没有外部 jar 下载链接。

## AI 披露

这些项目页文案使用了生成式 AI 辅助，并由维护者按当前源码复核。发布者必须启用平台要求的 AI content disclosure，并遵守提交时显示的最新规则。

经许可采用的原版 TaCZ Tweaks 图标不是 AI 生成或 AI 修改。图标、横幅和 Gallery 图片不得使用生成式 AI 生成或修改；所有展示图片还必须真实反映实际游戏内容。

# TaCZ Tweaks (Refabricated) 文档

文档分为玩家支持、扩展包说明、发布资料和维护审计。README 只提供项目概览；遇到问题时应从本页进入对应的事实来源。

## 玩家与服务器管理员

- [安装、依赖与构建](../BUILD.md)
- [配置项参考](CONFIGURATION.md)
- [兼容矩阵](COMPATIBILITY.md)
- [已知问题](KNOWN_ISSUES.md)
- [问题排查与 Bug 提交流程](SUPPORT.md)
- [示例扩展包](../tacz-tweaks-example-pack/)

下载文件时，以发布平台为该文件标注的 Minecraft、加载器和依赖版本为准，不要从项目标题或旧截图推断兼容范围。

## 数据驱动内容作者

仓库中的 `tacz-tweaks-example-pack/` 是可重载示例，包含以下系统的实际目录和 schema 示例：

- [选择器通用格式](data/SELECTORS.md)
- [bullet_interactions](data/BULLET_INTERACTIONS.md)
- [bullet_sounds](data/BULLET_SOUNDS.md)
- [bullet_particles](data/BULLET_PARTICLES.md)
- [melee_interactions](data/MELEE_INTERACTIONS.md)
- [迁移说明](data/MIGRATION.md)

修改或分发示例中的第三方音频前，请同时阅读 [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md)。兼容某个第三方枪包、资源包或模组，不代表本项目拥有、转授或改变该内容的许可。

## 发布维护

- [发布文案与平台检查表](publish/README.md)
- [Modrinth 文案](publish/Modrinth.md)
- [CurseForge 文案](publish/CurseForge.md)
- [项目许可](../LICENSE)
- [第三方来源与许可](../THIRD_PARTY_NOTICES.md)

## 移植与审计

- [当前审计结论](../AUDIT.md)
- [移植笔记](../PORTING_NOTES.md)
- [历史交接记录](../AGENT_HANDOFF_26_1_2.md)
- [维护规则](../AGENTS.md)

过程笔记可能记录已经被后续工作推翻的旧结论。判断当前行为时，优先使用当前源码、`AUDIT.md` 和实际依赖的 class descriptor。

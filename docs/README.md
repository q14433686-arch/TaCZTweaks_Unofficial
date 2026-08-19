# TaCZ Tweaks (Refabricated) 文档

这里是安装、支持、数据驱动内容、发布资料与维护审计的统一入口。根目录 README 提供项目概览；处理问题或发布文件时，应从本页进入对应的当前事实来源。

## 玩家与服务器管理员

- [安装、真实依赖与构建](../BUILD.md)
- [配置参考（全部选项、默认值与同步范围）](CONFIGURATION.md)
- [兼容性矩阵（支持范围与验证证据）](COMPATIBILITY.md)
- [已知问题](KNOWN_ISSUES.md)
- [支持范围、问题排查与 Bug 提交流程](SUPPORT.md)
- [项目 Issues](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues)

下载文件时，以发布平台为该文件标注的 Minecraft、加载器、依赖、环境和发布渠道字段为准，不要从项目标题、旧截图或旧过程笔记推断兼容范围。

## 数据驱动内容

- [数据格式总览](data/README.md)
- [选择器（Selectors）](data/SELECTORS.md)
- [弹丸交互（Bullet interactions）](data/BULLET_INTERACTIONS.md)
- [弹丸音效（Bullet sounds）](data/BULLET_SOUNDS.md)
- [弹丸粒子（Bullet particles）](data/BULLET_PARTICLES.md)
- [近战交互（Melee interactions）](data/MELEE_INTERACTIONS.md)
- [迁移说明（MIGRATION）](data/MIGRATION.md)

仓库中的 [`tacz-tweaks-example-pack/`](../tacz-tweaks-example-pack/) 是可重载的示例包。当前分支实际提供：

- `bullet_interactions` 示例；
- `bullet_sounds` 与配套音频示例；
- `bullet_particles` 示例；
- `tags/block` 方块标签与 schema smoke fixtures。

修改或分发示例内的第三方音频前，请阅读 [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md)。本项目兼容某个枪包、资源包、数据包或模组，不代表本项目拥有、维护、转授或改变该内容的许可。

## 发布文案

- [发布文案与平台检查表](publish/README.md)
- [Modrinth 文案](publish/Modrinth.md)
- [CurseForge 文案](publish/CurseForge.md)

长期项目页不保存当前文件版本；具体版本和依赖必须记录在每个上传文件的平台 metadata 中。

## 许可与第三方来源

- [项目代码许可](../LICENSE)
- [图标、嵌入组件与其他第三方来源](../THIRD_PARTY_NOTICES.md)

仓库代码使用 GPL-3.0，并不自动重新许可所有链接、兼容或由用户另行安装的第三方内容；应以各 notice 与上游许可为准。

## 当前审计结论与移植笔记

- [README 当前状态、已知差距与依赖](../README.md)
- [当前静态审计实现](../scripts/audit_port.py)
- [专用服务器日志门禁](../scripts/check_server_log.py)
- [发布一致性检查](../scripts/check_release_consistency.py)
- [提议的 CI workflow（需维护者移动到 `.github/workflows/ci.yml`）](maintenance/ci-workflow.yml)
- [移植笔记](../PORTING_NOTES.md)

当前分支保留了静态审计、测试夹具与服务器日志门禁，但 README 中列出的实机矩阵仍未全部完成。审计命令及其外部 jar/refmap 前置条件以 [BUILD.md](../BUILD.md) 为准。

**历史过程笔记可能已被后续结论推翻。判断当前事实时，应优先查看当前源码、当前审计结论、`fabric.mod.json` 和实际依赖的 class descriptor，而不是较早的移植设想。**

## 维护规则

1. 依赖范围以当前 `src/main/resources/fabric.mod.json` 与 `gradle.properties` 为事实来源；
2. 更改图标、支持文件或发布文案后必须运行严格审计和图标检查；
3. `docs/publish/` 不写当前游戏、模组、依赖版本或带编号的发布阶段；
4. 第三方资源必须记录不可变来源、作者、许可、校验和与仓库使用路径；
5. `libs/` 下每个 jar 必须记录在 `RESOURCE_IMPORT_MANIFEST.tsv` 中并固定 SHA-256，禁止无记录的可变二进制；
6. 发布前运行 `scripts/check_release_consistency.py`、`scripts/download_dependencies.py --check-only`、`scripts/check_mod_icon.py` 与 `./gradlew clean build`；
7. 功能声明必须能由当前源码、测试或明确标注的实机验证支持，不能从其他分支直接推断。

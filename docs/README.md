# TaCZ Tweaks (Refabricated) 文档

这里是安装、支持、数据驱动内容、发布资料与维护审计的统一入口。根目录 README 提供项目概览；处理问题或发布文件时，应从本页进入对应的当前事实来源。

本页描述的是 **Fabric 1.21.11** 分支，不要把它和 26.x 未混淆分支的依赖或文档混用。

## 玩家与服务器管理员

- [安装、真实依赖与构建](../BUILD.md)
- [配置项参考](CONFIGURATION.md)
- [兼容矩阵](COMPATIBILITY.md)
- [已知问题](KNOWN_ISSUES.md)
- [支持范围、问题排查与 Bug 提交流程](SUPPORT.md)
- [项目 Issues](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues)
- [示例扩展包](../tacz-tweaks-example-pack/)

下载文件时，以发布平台为该文件标注的 Minecraft、加载器、依赖、环境和发布渠道字段为准，不要从项目标题、旧截图或旧过程笔记推断兼容范围。

## 数据驱动内容作者

仓库中的 [`tacz-tweaks-example-pack/`](../tacz-tweaks-example-pack/) 是可重载的示例包。当前分支实际提供：

- [选择器通用格式](data/SELECTORS.md)
- [bullet_interactions](data/BULLET_INTERACTIONS.md)
- [bullet_sounds](data/BULLET_SOUNDS.md)
- [bullet_particles](data/BULLET_PARTICLES.md)
- [melee_interactions](data/MELEE_INTERACTIONS.md)
- [迁移说明](data/MIGRATION.md)

当前示例包含 `bullet_interactions`、`bullet_sounds`、`bullet_particles` 与 `tags/block`，没有 melee 示例文件。修改或分发示例内的第三方音频前，请阅读 [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md)。本项目兼容某个枪包、资源包、数据包或模组，不代表本项目拥有、维护、转授或改变该内容的许可。

## 发布文案

- [发布文案与平台检查表](publish/README.md)
- [Modrinth 文案](publish/Modrinth.md)
- [CurseForge 文案](publish/CurseForge.md)

长期项目页不保存当前文件版本；具体版本和依赖必须记录在每个上传文件的平台 metadata 中。

## 许可与第三方来源

- [项目代码许可](../LICENSE)
- [许可证总览](../LICENSES.md)
- [图标、嵌入组件、本地 jar 与其他第三方来源](../THIRD_PARTY_NOTICES.md)
- [本地二进制依赖清单](../RESOURCE_IMPORT_MANIFEST.tsv)

仓库代码使用 GPL-3.0，并不自动重新许可所有链接、兼容或由用户另行安装的第三方内容；应以各 notice 与上游许可为准。

## 当前审计结论与移植笔记

- [README 当前状态、已知差距与依赖](../README.md)
- [当前静态审计实现](../scripts/audit_port.py)
- [发布一致性检查（含运行时 TaCZ 闸门交叉校验）](../scripts/check_release_consistency.py)
- [依赖可用性门禁（查上游实际发布版本）](../scripts/check_dependency_availability.py)
- [依赖区间回归测试](../scripts/test_dependency_ranges.py)
- [专用服务器日志门禁](../scripts/check_server_log.py)
- [移植笔记](../PORTING_NOTES.md)
- 已安装的 GitHub Actions 流程：[`consistency`](../.github/workflows/consistency.yml) ·
  [`audit`](../.github/workflows/audit.yml) ·
  [`compile-check`](../.github/workflows/compile-check.yml) ·
  [`build`](../.github/workflows/build.yml) ·
  [`release`](../.github/workflows/release.yml)（仅 `1.21.11_*` tag 触发）
- [早期提案版 CI workflow（已被上面五条取代，仅存档）](maintenance/ci-workflow.yml)

当前分支保留了静态审计、测试夹具与服务器日志门禁，但 README 中列出的实机矩阵仍未全部完成。审计命令及其外部 jar/refmap 前置条件以 [BUILD.md](../BUILD.md) 为准。

`fabric.mod.json` 的 `depends` 区间与 `TaCZTweaks.java` 里硬编码的版本闸门属于**只在运行时生效的约束**：编译、mixin 静态审计与打包都不读它们，只有 Fabric Loader 在游戏启动时执行。`26.3` 线因此出过三次「CI 全绿、游戏起不来」，跨线同步的完整工单在该分支的 `docs/CROSS_BRANCH_SYNC.md`。

**历史过程笔记可能已被后续结论推翻。判断当前事实时，应优先查看当前源码、当前审计结论、`fabric.mod.json` 和实际依赖的 class descriptor，而不是较早的移植设想。**

## 维护规则

1. 依赖范围以当前 `src/main/resources/fabric.mod.json` 与 `gradle.properties` 为事实来源；
2. 更改图标、支持文件或发布文案后必须运行严格审计和图标检查；
3. `docs/publish/` 不写当前游戏、模组、依赖版本或带编号的发布阶段；
4. 第三方资源必须记录不可变来源、作者、许可、校验和与仓库使用路径；
5. 功能声明必须能由当前源码、测试或明确标注的实机验证支持，不能从其他分支直接推断。

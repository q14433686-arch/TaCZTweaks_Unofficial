# 支持范围与问题反馈

## 先确定问题归属

本仓库处理 **TaCZ Tweaks (Renovated)** 的 NeoForge 移植、配置、数据系统和兼容接线问题。
提交前请做对照：

1. 备份存档和配置；
2. 移除 TaCZ Tweaks，只保留 TaCZ: Renovated、YACL 和 NeoForge；
3. 用相同枪械、内容包和操作重测；
4. 若问题仍存在，通常属于底层 TaCZ: Renovated、NeoForge 或第三方内容。

本移植产生的问题不要提交给 MUKSC、TACZ Dev Team 或兼容模组作者。

## 最小复现环境

推荐新建独立实例，仅安装：

- NeoForge（使用本文件对应发布件声明的版本）；
- TaCZ: Renovated；
- YetAnotherConfigLib（双端必需）；
- TaCZ Tweaks (Renovated)。

先用默认配置与默认内容复现，再一次只加入一个可选模组、枪包、数据包、资源包或 shader pack。
专服问题还应分别提供服务端与客户端日志。

## 有效报告必须包含

1. Minecraft 与 NeoForge 完整版本；
2. 本模组、TaCZ: Renovated、YACL 和所有可选模组完整版本；
3. 单人、局域网、多人客户端或专用服务器环境；
4. 实际与预期行为；
5. 从新建世界/测试服开始的最小复现步骤；
6. 完整 `latest.log`，崩溃时附 crash report；
7. 移除 TaCZ Tweaks 后的对照结果；
8. 相关枪包、数据包、shader 和配置。

“最新版”、截图、启动器摘要或单行堆栈不能替代完整版本与日志。公开日志前先删除私人地址、令牌等
敏感信息。

## Mixin / 启动问题

请保留包含以下关键词前后文的完整日志：

- `MixinApplyError`
- `InvalidInjectionException`
- `Critical injection failure`
- `Scanned 0 target(s)`
- `TaCZ Tweaks requires TaCZ`

专服报告请额外运行 `python3 scripts/check_server_log.py <latest.log>` 并附输出；该脚本通过也不能替代
完整日志。

## 第三方内容与兼容

只在特定枪包、数据包、资源包或可选模组出现的问题，请使用兼容性模板并提供：准确名称/版本、
官方来源、依赖、移除后的对照结果及最小文件集合。不要重新分发无许可内容。

## 提交入口

- [Bug 报告](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues/new?template=bug_report.yml)
- [内容包 / 兼容性问题](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues/new?template=compat_report.yml)
- [搜索已有 Issues](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues?q=is%3Aissue)

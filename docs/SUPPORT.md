# 支持范围与问题反馈

## 先确定问题属于哪个项目

本仓库只处理本移植各发行线的配置、数据驱动功能和兼容接线问题。
先对照 [分支表](BRANCHES.md) 确认 Minecraft 版本与 Fabric / NeoForge，再按**那条线**的依赖做最小复现。
本页其余步骤以默认线 **Fabric 26.2（`26.2(main)`）** 为例；其它线把「Fabric API / TaCZ Refabricated」换成该分支 README 列出的加载器与 TaCZ 移植。

提交到本仓库前，先进行一次对照测试：

1. 备份存档和配置；
2. 移除 TaCZ Tweaks (Refabricated)，保留 TaCZ Refabricated Unofficial 及其依赖；
3. 使用相同枪械、内容包和操作重新测试；
4. 如果问题仍然发生，它通常属于底层 TaCZ 移植或第三方内容，而不是本项目。

请勿要求原版 TaCZ Tweaks、TaCZ 或兼容模组作者为本移植产生的问题提供支持。

## 提交前的最小复现

推荐建立独立测试实例，仅安装：

- TaCZ Tweaks (Refabricated)
- Fabric API
- Fabric Language Kotlin
- TaCZ Refabricated Unofficial
- YetAnotherConfigLib

先使用默认配置和默认内容复现。随后一次只加入一个可选模组、枪包、数据包、资源包或 shader pack。这样可以区分核心回归与第三方兼容问题。

无法在最小环境复现并不妨碍报告，但必须准确列出触发问题所需的额外内容。

## 有效 Bug 报告必须包含

1. Minecraft、加载器（Fabric 或 NeoForge）和 **Git 发行分支** 的完整名称；
2. 本模组及全部必需依赖的完整版本；
3. 单人、局域网、多人客户端或专用服务器环境；
4. 实际表现和预期表现；
5. 从新建存档或测试服务器开始的最小复现步骤；
6. 完整 `latest.log`，崩溃时还需 crash report；
7. 是否能在最小环境、默认配置下复现；
8. 所有相关枪包、可选兼容模组、shader pack 和配置项。

“最新版”、一张报错截图、启动器摘要或单独一行堆栈都不能替代完整版本和完整日志。日志可上传到 [mclo.gs](https://mclo.gs/) 或 GitHub Gist；公开前请检查其中是否含有不希望披露的信息。

## 第三方内容与兼容问题

若问题只在特定枪包、数据包、资源包或可选模组存在时发生，请使用 **内容包 / 兼容性问题** 模板，并补充：

- 内容的准确名称、完整版本和官方来源链接；
- 作者声明的前置依赖与安装方式；
- 移除该内容后的对照结果；
- 能复现问题的最小文件集合。

不要仅凭紫黑贴图、缺失模型或报错类名推断问题归属。目录层级、资源路径、依赖谓词、内容损坏和许可限制都可能产生相似现象。

## 安全提交附件

- 不要上传包含访问令牌、服务器密钥、私人地址或玩家隐私信息的配置；
- 不要重新分发没有许可的枪包或资源。优先提供官方来源链接和最小自制复现包；
- 存档上传前保留本地备份；
- 截图和视频用于说明视觉结果，不能代替日志。

## 提交入口

- [Bug 报告](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues/new?template=bug_report.yml)
- [内容包 / 兼容性问题](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues/new?template=compat_report.yml)
- [搜索已有 Issues](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues?q=is%3Aissue)

# 支持范围与问题反馈

## 先确定问题属于哪个项目

本仓库只处理 **TaCZ Tweaks (Refabricated)** 的移植、配置、数据驱动功能和可选兼容接线问题。请按以下对照判断归属：

- **本移植问题**：安装本模组后出现，移除本模组并保持其余测试条件不变后消失；
- **底层 TaCZ 移植问题**：移除本模组后，在 TaCZ Refabricated Unofficial 及其硬依赖环境中仍可复现；
- **第三方内容问题**：只在特定枪包、数据包、资源包或可选兼容模组存在时出现，移除该内容后消失。

相似报错不等于相同归属。资源路径、内容损坏、错误依赖、配置和 mixin 冲突都可能产生相似表现。请勿要求原版 TaCZ Tweaks、TaCZ 或兼容模组作者为本移植产生的问题提供支持。

## 如何移除本模组做对照

1. 备份存档、服务器与配置；
2. 建立独立测试实例，不要直接在唯一存档上排查；
3. 移除 TaCZ Tweaks (Refabricated) 的 jar，并暂时移除仅供本模组使用的数据/资源包；
4. 保留底层 TaCZ 及其依赖，使用相同枪械、内容包、世界条件和操作重新测试；
5. 保存有本模组和无本模组两次测试的完整日志；
6. 若无本模组时仍复现，再逐项移除第三方内容以确定真正归属。

## 当前分支的最小环境

最小环境必须按当前 [`fabric.mod.json`](../src/main/resources/fabric.mod.json) 的真实 `depends` 生成，而不是照抄其他 Minecraft 分支。当前硬依赖集合为：

- 与该文件 metadata 匹配的 Minecraft 与 Java；
- Fabric Loader；
- Fabric API；
- Fabric Language Kotlin；
- TaCZ Refabricated Unofficial（mod id `tacz`）；
- YetAnotherConfigLib v3（mod id `yet_another_config_lib_v3`）；
- TaCZ Tweaks (Refabricated) 本身。

具体版本应从当前文件的依赖谓词和下载文件的平台 metadata 读取。Mod Menu、Sound Physics Remastered、First Aid New、Pillager’s Gun、枪包、shader、额外数据包与资源包都不是最小环境的一部分。先使用新建存档、默认配置和默认内容复现，再一次只加入一个可选项目。

无法在最小环境复现并不妨碍报告，但必须准确列出触发问题所需的所有额外内容。

## 有效 Bug 报告必须包含

1. Minecraft、本模组、Fabric Loader、Java 与全部硬依赖的完整版本；
2. 单人、LAN、多人客户端或专用服务器环境；
3. 实际行为和预期行为；
4. 从新建存档或测试服务器开始的最小复现步骤与复现概率；
5. 是否能在上述最小环境和默认配置下复现；
6. 有本模组/移除本模组后的对照结果；
7. 完整 `latest.log`；崩溃时还需完整 crash report；
8. 相关配置，以及所有枪包、可选兼容模组和其他模组的准确名称与完整版本。

“最新版”、一张报错截图、启动器摘要或单独一行堆栈都不能替代完整版本与完整日志。日志可上传到 [mclo.gs](https://mclo.gs/) 或 GitHub Gist。

## 第三方内容与兼容问题

若问题只在特定枪包、数据包、资源包或可选模组存在时发生，请使用兼容性模板，并额外提供：

- 第三方内容的准确名称、完整版本和官方发布页或源码链接；
- 作者声明的前置依赖与安装方式；
- 移除该内容后用相同步骤得到的对照结果；
- 能复现问题的最小文件集合及全部其他已安装模组；
- 有第三方内容与无第三方内容两次测试的完整日志。

兼容不代表本项目拥有、维护或重新许可第三方内容，也不保证其所有版本都兼容。

## 日志、附件、隐私、安全与许可

- 公开前检查日志、配置和存档，移除访问令牌、服务器密钥、私人地址及玩家隐私信息；
- 不要修改日志以隐藏与问题相关的模组列表或堆栈；如需脱敏，应说明脱敏范围；
- 不要上传无权再分发的枪包、模型、贴图、动画、音频或整合包；优先提供官方来源链接；
- 如需附件，优先制作由报告者拥有或明确许可的最小复现包；
- 存档上传前保留本地备份；不要在不可信文件上复现；
- 截图与视频可说明视觉结果，但不能代替完整日志；
- 提交附件即表示你有权为问题排查提供该材料；附件不会因此被本项目重新许可。

## 提交入口

- [Bug 报告](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues/new?template=bug_report.yml)
- [内容包 / 兼容性问题](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues/new?template=compat_report.yml)
- [搜索 open 和 closed Issues](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/issues?q=is%3Aissue)

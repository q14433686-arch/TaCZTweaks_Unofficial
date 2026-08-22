# MC 百科发布文案（TaCZ Tweaks 非官方移植）

> 供 MC 百科词条编辑使用。正文采用 MC 百科 BBCode。版本范围与实测状态以
> [`RELEASE.md`](RELEASE.md) 和 GitHub Releases 为准；更新词条时按文末维护清单核对，
> 不要把每个文件的更新日志堆进词条正文。

## 词条名称

- 中文名：`永恒枪械工坊：零 · 调校（非官方移植）`
- 英文名：`TaCZ Tweaks (Unofficial Port)`
- 原项目：`MUKSC/TaCZTweaks`

## 简介

```text
《TaCZ Tweaks》是为《永恒枪械工坊：零》（Timeless & Classics Guns: Zero，简称 TaCZ）
提供可配置枪械手感、移动行为、全局平衡修饰器与数据驱动交互的扩展模组。
本词条描述的是它的非官方社区移植版本，面向新版 Minecraft 的 Fabric 与 NeoForge 环境，
需要配合对应加载器的非官方 TaCZ 移植使用。
```

## 正文（复制到 MC 百科）

```bbcode
[h1=非官方移植说明]

本模组是 [url=https://github.com/MUKSC/TaCZTweaks]MUKSC / TaCZTweaks[/url] 的非官方社区移植，沿用其公开的 GPL-3.0 源码。它不是原作者的官方发布，也未经 MUKSC、TACZ Dev Team、底层 TaCZ 移植维护者或任何可选兼容模组作者审阅与背书。移植工作集中在加载器适配（注册、事件、网络、数据重载、mixin 注入点）与新版 Minecraft 适配，不新增枪械内容。

本移植的问题请提交到本项目的 Issue，不要打扰上游作者。

[h1=它做什么]

TaCZ Tweaks 在 TaCZ 之上补充了原版没有暴露的可配置项：

[list]
[*][b]枪械操作[/b]：卸弹（创造/生存、弹匣与可选枪膛弹）、水下禁射、手动拉栓、换弹前拉栓、再次检视取消、换弹丢弃弹匣。
[*][b]移动行为[/b]：跑打、换弹奔跑、射击中换弹或切换开火模式、枪械倾斜、按住降低灵敏度。
[*][b]全局平衡修饰器[/b]：伤害、玩家伤害、爆头、穿甲、弹速、重力、摩擦、举枪时间、各姿态扩散、射速、后坐力；爆炸弹对玩家的伤害同样走玩家伤害修饰器，改装台属性图使用修饰后的基线。
[*][b]数据驱动系统[/b]：bullet_interactions、bullet_sounds、bullet_particles、melee_interactions，支持按枪械、类别、弹药、正则、predicate、伤害、速度、消音、点射、弹丸、随机以及逻辑组合选择，源码内附可重载示例包。
[*][b]体验改进[/b]：无尽弹药状态效果、末影人躲子弹、冒险模式禁改装、RPS 显示、改装台手持筛选、命中标记与命中音控制、匍匐俯仰限制与过渡平滑。
[/list]

全部选项通过游戏内配置界面（Yet Another Config Lib）调整，以 JSON 持久化，服务端权威项会同步给客户端。

[h1=运行环境与前置]

[list]
[*]加载器：Fabric 与 NeoForge 由同一套源码分别构建，[b]两者文件不可混用[/b]。
[*]必需前置：对应加载器的非官方 TaCZ 移植（Fabric 用 TaCZ Refabricated Unofficial，NeoForge 用 TaCZ: Renovated）、Yet Another Config Lib。
[*]Fabric 线另需 Fabric API 与 Fabric Language Kotlin；NeoForge 线的 Kotlin 运行时已内嵌在模组文件内。
[*]可选兼容：Sound Physics Remastered、First Aid New、Pillager's Gun（非官方移植）。
[*]启动时会校验 TaCZ 移植的版本系列，装错版本会直接拒绝加载，而不是运行中出现难以定位的异常。
[/list]

各文件支持的具体 Minecraft 版本、加载器版本与依赖版本，以发布页对应文件的字段与更新日志为准。

[h1=已知边界]

[list]
[*]可选模组的兼容都在运行期做门控，并且只在文件声明的版本区间内成立。
[*]NeoForge 线无法完全复刻 Fabric 的方块保护事件链：子弹与近战破坏方块会走原版交互检查加可取消的破坏事件，但依赖额外"已取消/破坏后"回调的领地类模组可能收不到通知，需自行验证。
[*]功能被列出不等于在每个 Minecraft 版本都做过游戏内实测，逐版本的实测程度写在该文件的更新日志里。
[*]第三方枪包与内容包需要按你实际运行的 Minecraft 版本自行验证。
[/list]

[h1=反馈方式]

请先在最小环境复现：Minecraft + 加载器 + 对应的 TaCZ 移植 + 本模组 + Yet Another Config Lib，然后附上完整的 latest.log 或崩溃报告；联机问题需要同时提供服务端与客户端日志。

[h1=来源与许可]

[list]
[*]原项目：[url=https://github.com/MUKSC/TaCZTweaks]MUKSC / TaCZTweaks[/url]
[*]本移植源码：[url=https://github.com/q14433686-arch/TaCZTweaks_Unofficial]q14433686-arch / TaCZTweaks_Unofficial[/url]
[*]Fabric 侧 TaCZ 移植：[url=https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial]TaCZ Refabricated Unofficial[/url]
[*]NeoForge 侧 TaCZ 移植：[url=https://github.com/q14433686-arch/TaCZ_Renovated]TaCZ: Renovated[/url]
[/list]

代码继承原项目的 GPL-3.0。第三方资源保留各自许可，代码许可不自动覆盖模型、贴图、动画与音频，详见仓库内的 LICENSE、LICENSES.md 与 THIRD_PARTY_NOTICES.md。本模组按现状提供，不附带任何担保。

词条正文部分内容由生成式 AI 辅助撰写并经维护者复核。
```

## 词条维护清单

1. 新增或下架某个 Minecraft 版本时，只改「运行环境与前置」的表述与发布页链接，不在正文堆版本号；
2. 实测状态变化（例如某分支完成专服冒烟）只更新 `RELEASE.md` 与该文件的更新日志，词条里维持
   「以该文件更新日志为准」的口径；
3. 不在词条内提供任何第三方枪包、模型、贴图或音频的下载；
4. 不使用「官方」「授权」「与原作者合作」等措辞；
5. 图片必须是真实游戏截图，且不使用 AI 生成或 AI 修改的图标与横幅。

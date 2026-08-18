# TaCZ Tweaks (Refabricated) — 1.21.11

**Fabric 移植版 TaCZ Tweaks**，适配非官方 Fabric 移植
[`TaCZ_Refabricated_Unofficial`](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial) 的
Minecraft **1.21.11** / Java **21** / **混淆 + Loom remap + refmap** 分支。

原项目：**[MUKSC/TaCZTweaks](https://github.com/MUKSC/TaCZTweaks)**（Forge 1.20.1 / TaCZ 1.1.8），
代码遵循 **GPL-3.0** 发布。

> 本仓库源码已使用 **R4** 版本号：`2.14.2+fabric.1.21.11.R4`

---

## 当前状态

这是 **仍在追赶 26.2 已验证实现的 1.21.11 移植分支**。当前已具备：

- 配置系统（YACL v3）、JSON 持久化、客户端/服务端同步、ModMenu 入口；
- 基础枪械/移动 tweak、基础卸弹、基础滑铲同步、共享枪声；
- 全局 modifier、基础 bullet interaction / sound / particle data 与行为；
- 示例资源包 `tacz-tweaks-example-pack/`；
- 面向 1.21.11 混淆环境的 mixin/refmap 工程骨架。

本轮修复已补上若干**高优先级安全与发布缺口**：

- MixinExtras 升级到 **0.5.4**，与 mixin JSON 的最低版本声明一致；
- `crawl.LocalPlayerCrawlMixin` 改回 `client` 分组；
- 服务端共享枪声加入开关、存活/持枪、命名空间、距离、浮点合法性与限流校验；
- 滑铲 C2S 改为服务端 executor + 短 lease/逐 tick 复验；
- 配置同步 payload 增加 **1 MiB** 上限、绝对拷贝与失败回滚；
- 卸弹逻辑改为 physical / dummy / FUEL / inventory / closed-bolt 分支；
- `SafeMath.blockBreakingDelta`、`ValueRange`、粒子维度/格式/上限与 whizz 去重补强；
- 增加 dedicated-server 日志门禁脚本、混淆端口审计脚本、JUnit 基础测试脚手架与 ASCII `GRADLE_USER_HOME` test staging；
- 版本号统一提升到 **R4**，并修正文档中遗留的 R2/R3 与 YACL 版本错误。

---

## 仍未完成的差距

以下内容 **不能视为已完成**：

- 26.2 parity 的 crawl 第一/三人称完整视觉过渡；
- melee / shield / bulletProtection / betterMonoConversion 的完整 1.21.11 版实现；
- old v2 bullet interaction converter、burst/pellet allocator、airspace payload 全链路；
- 多人/专服/客户端完整实机矩阵；
- 依赖真实 TaCZ / Minecraft jars 的最终 strict audit 与整仓 `./gradlew clean build` 验证。

这表示“**尚未移植完成或尚未实测**”，不是“Fabric/1.21.11 做不到”。

---

## 依赖

| 依赖 | 版本 |
|---|---|
| Minecraft | 1.21.11 |
| Fabric Loader | >=0.19.3 |
| Fabric API | `0.141.6+1.21.11` |
| [UNOFFICIAL] TaCZ Refabricated | **1.1.8+fabric.1.21.11.R2**（运行时精确验证 friendly string） |
| Fabric Language Kotlin | `1.13.13+kotlin.2.4.10` |
| YetAnotherConfigLib (YACL) | **3.8.2+1.21.11-fabric** |
| Java | >=21 |

---

## 构建

```bash
# 需要 JDK 21
./gradlew build
# 产物：build/libs/tacztweaks-2.14.2+fabric.1.21.11.R4.jar
# 示例包：build/distributions/tacz-tweaks-example-pack-2.14.2+fabric.1.21.11.R4.zip
```

可用的发布前门禁：

```bash
python3 scripts/audit_port.py --strict \
  --tacz-jar libs/TACZ-Refabricated-1.21.11-1.1.8+fabric.1.21.11.R2.jar \
  --minecraft-named-jar <1.21.11 named jar> \
  --minecraft-intermediary-jar <1.21.11 intermediary jar>
python3 scripts/check_server_log.py <dedicated-server-latest.log>
```

> `libs/` 下的 `TACZ-Refabricated-1.21.11-1.1.8+fabric.1.21.11.R2.jar`（compileOnly / testRuntimeOnly）
> 与 `yacl-fabric.jar` 需要按 BUILD.md 手动下载。

## 许可

- 代码：GPL-3.0（继承原项目 MUKSC/TaCZTweaks）
- 原作者：MUKSC
- Bundled notices：见 `THIRD_PARTY_NOTICES.md`

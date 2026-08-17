# TaCZ Tweaks → TaCZ_Refabricated_Unofficial 适配计划

> 目标：把本仓库的 **TaCZ Tweaks v3（3.0.0-alpha.10）** 移植为适配
> [q14433686-arch/TaCZ_Refabricated_Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial) 的版本。
> 本文件是"先看看"阶段的分析结论，用于确定改什么、做哪个版本、怎么分步。

---

## 1. 现状（我们现在有什么）

| 项 | 值 |
|---|---|
| 项目 | TaCZ Tweaks（addon mod） |
| 版本 | `3.0.0-alpha.10`，`version.target = 1.1.8`（TaCZ 基线） |
| 语言 | Kotlin 主体 + Java mixin |
| 代码规模 | 约 343 个文件：~100 个 Kotlin、~120 个 mixin、~10 个 accessor |
| 构建 | Stonecutter 多加载器：`1.20.1-forge/fabric`、`1.21.1-neoforge/fabric` |
| 配置 GUI | YACL |
| 加载器抽象 | 已有 `platform/` 接口层 + 各加载器实现（`kotlin-fabric/` 等） |
| 依赖的 TaCZ API | `com.tacz.guns.*`（99 类）+ `cn.sh1rocu.tacz.*`（3 类） |

## 2. 目标（我们要适配到哪）

| 项 | 值 |
|---|---|
| 上游 | [TaCZ_Refabricated_Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)，Fabric 移植，TaCZ 1.1.8 |
| 分支 | `26.2(main)`（默认）/ `26.1.2` / `1.21.11` |
| Minecraft / Java / 混淆 | 26.2 → Java 25 / **未混淆（mojmap 直编）**；1.21.11 → Java 21 / **混淆（Loom remap）** |
| 构建 | 单 Fabric + Loom 1.17，Fabric API `0.155.2+26.2`，loader `0.19.3`，**无需 mappings** |
| 运行时依赖 | MAE、commons-math3、luaj、bcel、Forge Config API Port、Cloth Config、ModMenu |

## 3. API 兼容性核查（最关键的一步，结果乐观）

把本仓库所有对 TaCZ 的 import 逐一在目标仓库源码里核对：

- `com.tacz.*` 引用的 **99 个类全部存在**于目标仓库。
- `cn.sh1rocu.*` 引用的 3 个类（`ClientPlayerNetworkEvent`、`InputEvent`、`ItemHandlerHelper`）全部存在。
- `com.tacz.guns.entity.EntityKineticBullet.EntityResult` 内嵌类仍存在。

**结论：TaCZ 自身 API 在 26.2 端口里被完整保留（TaCZ 1.1.8 基线不变），无需改 TaCZ 侧 API。**

### 3.1 成员级复核（回答"你确认 TACZ 的 API 没变？"）
不只查类是否存在，还逐一把 mixin 里用到的 `(类, 成员)` 描述符核对目标源码：
- 抽取全部 62 个 `Lcom/tacz/...;member` 引用，59 个直接命中目标源码；其余 4 个均为
  继承/生成/误报：`isCanceled()` 来自 `ICancellableEvent` 接口默认方法、
  `AttachmentType.values()` 是枚举编译器生成、`InaccuracyType#F` 是描述符类型记号。
- 因此 **TaCZ 侧的类、方法、字段在 26.2 端口里全部保留**。
- 注意：26.2 是 Minecraft 本体把 `ResourceLocation` 改名成 `Identifier` 等，属于**vanilla 迁移**，
  不是 TaCZ 变更（详见 §4.3），这才是主要成本。

真正的移植工作集中在以下"外围"。

## 4. 需要改什么（移植工作清单）

### 4.1 构建系统（必改，工作量小但决定骨架）
- 移除 Stonecutter 多加载器编排、`forge.gradle.kts` / `neoforge.gradle.kts`。
- 建立单一 Fabric 26.2 构建（参考目标仓库 `build.gradle`）。
- Java 21 → **25**；26.x **无映射**（删掉 mappings / parchment 相关）。
- `settings.gradle.kts` 去掉 4 个版本矩阵，保留单版本。

### 4.2 加载器/平台层（中等）
- 现有 `platform/` 接口 + `kotlin-fabric/` 实现可复用骨架。
- 更新到 26.2 的 Fabric API / Fabric Kotlin（fabric loader 0.19.x）。
- `core/Loader.kt` 的 Fabric 分支（`FabricLoader.getInstance()`、`VersionPredicate`）基本可直接用。
- 网络层：26.2 Fabric 网络 API v1（`CustomPacketPayload`/`StreamCodec` 语义大体保留，需核对注册 API）。
- `fabric.mod.json`、mixins json、accesswidener 更新。

### 4.3 Minecraft 本体 API 迁移（工作量大，主要成本）
1.21.1 → 26.2 的 vanilla 变更面广，mixins 命中的 vanilla 类都要核对签名：
- `LivingEntity` / `Player` / `EnderMan` / `Explosion` / `BlockBehaviour` / `ProtectionEnchantment` / `MouseHandler` / `ItemInHandLayer` / `SoundEngine` / `PlayerRenderer` / `ClipContext` 等。
- 26.x 大量改名（例如目标仓库已改的 `GuiGraphics`→`GuiGraphicsExtractor`、`drawString`→`text`）。
- 注册表、数据组件（Data Component）系统、`ResourceKey` 等在 26.x 的变化。

### 4.4 配置 GUI（决策点）
- 现用 **YACL**；目标生态用 **Cloth Config + Forge Config API Port**。
- 需确认 YACL 是否有 26.2 构建；没有则迁移到 Cloth（会改 `config/` 相关与 GUI 代码）。

### 4.5 兼容模块（逐个核查 26.2 可用性）
本 mod 含大量第三方兼容 mixin，需确认目标在 26.2 Fabric 是否有对应构建：
- Valkyrien Skies、Sable / Create Aeronautics、FirstAid、LRTactical（目标仓库已内置支持）、
  MTS、Legendary Survival Overhaul、Pillagers' Gun、Cuffed、Sound Physics Remastered。
- 没有 26.2 构建的模块走"编译期排除 + 门面禁用"路线（目标仓库对 AR/KubeJS/Controllable 就这么处理）。

### 4.6 数据包系统
- `feature/datapack/`（子弹交互、护盾、自定义结果等）依赖 TaCZ 资源加载器，
  基线相同大概率可直接迁移，需编译验证。

## 5. 做哪个版本（建议）

**推荐：`26.2(main)` 分支。**

理由：
1. 它是默认分支，且**未混淆**（mojmap 直编、无 mappings）——移植和后续维护最省力；
   而 `1.21.11` 是**混淆分支**（Loom remap），mixin 目标须用 intermediary 的 `method_NNNNN`，错误成本高得多。
2. 与目标仓库当前主开发线对齐，LRTactical 等兼容已内置。
3. 26.1.2 与 26.2 差距很小，适配 26.2 后反推 26.1.2 成本低。

## 6. 总体规模判断

- 若目标锁定 26.2（未混淆）：**大部分 mixin 只是方法签名/类名映射**，但 vanilla API 迁移仍涉及
  上百处改动，属于"大工程"，不是一次会话能零风险做完的。
- 建议**分阶段**：
  1. 骨架：单一 Fabric 26.2 构建能编译出空壳（主入口、platform、网络注册）。
  2. 逐 feature 迁移 + 编译修错（每个模块单独过）。
  3. 兼容模块逐个启用/禁用。
  4. 运行验证（客户端启动、配置 GUI、关键功能冒烟）。

### 6.1 本工作环境限制（重要）
经检测，本沙箱**无法**本地编译/运行 26.2 工程：
- 未安装 JDK（26.2 需要 **Java 25**），系统内 `java` 不存在。
- 出站 HTTPS 被阻断：`services.gradle.org`、`maven.fabricmc.net`、`repo.maven.apache.org`、
  `api.modrinth.com` 等均连不通（只有 fetch 代理与 GitHub git 可用），Gradle 无法拉依赖。
因此本会话只能交付**源码级适配**（无法编译验证），或停留在**计划**。真正的构建/运行验证需在有
JDK 25 且能访问 maven 的环境完成。

## 7. 待确认的问题
- 目标版本：26.2（推荐）/ 26.1.2 / 1.21.11？
- 配置库：YACL（若 26.2 有构建）还是切 Cloth Config？
- 范围：全部功能完整移植，还是先移植核心（配置、属性、平衡、数据包）再补兼容？
- 是否需要在本会话直接动手（从"骨架能编译"开始），还是先只交付本计划？

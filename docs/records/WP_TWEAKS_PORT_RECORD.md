# WP-TWEAKS 移植执行记录：TaCZTweaks → NeoForge 1.21.11

> 基线：姊妹 TaCZTweaks_Unofficial `1.21.11` 分支，同步至 `f38f2ff`（2026-08-22）。
> 产物：`tacztweaks-2.14.2+neoforge.1.21.11.Beta-1.jar`（已过用户实机 PASS）。

## 一、构建骨架

- MDG 2.0.144 / NeoForge 21.11.45 / JDK 21 / Gradle 9.2.1（与 TaCZ-Renovated 1.21.11 同骨架）
- Kotlin（55 个 kt 文件，核心逻辑所在）：KGP 2.4.10 + in-process 编译 +
  kotlin-stdlib 2.4.10 jarJar 内嵌（NeoForge 无 FLK 等价物）
- 依赖 `libs/` 内置：tacz 1.21.11（compileOnly+localRuntime）、YACL 3.8.2-neoforge、
  SPR 1.5.1-neoforge、commons-math3
- 低内存构建档案写入 `gradle.properties`

## 二、Fabric→NeoForge 翻译面（要点）

- 入口：`@Mod` 构造器 + `TaCZTweaksClient.init`（`FMLLoader.getCurrent().getDist()` 门控）
- 事件：`ServerTickEvent.Post`（21.11 在 `event.tick` 包）/ `ServerStoppedEvent` /
  `BlockEvent.BreakEvent` / `PlayerEvent.LoggedIn/Out`
- 网络：`RegisterPayloadHandlersEvent` 一次注册 8 条载荷；handle 改 `IPayloadContext` +
  `enqueueWork`；发送 `ClientPacketDistributor` / `PacketDistributor`
- 数据管理器：`AddServerReloadListenersEvent`（游戏总线事件）+ `SimplePreparableReloadListener`
- 配置屏：`ModContainer.registerExtensionPoint(IConfigScreenFactory.class, ...)`
- 注册：`DeferredRegister`；`KeyMapping.Category` 为 vanilla 原生类零改写

## 三、mixin 层（76 个全部落地，含用户实机驱动修复）

1. 4 个 lambda 注点零重定位（同构源码树，javap 实证）
2. `method_5773` → `tick` + `<init>` 描述符 intermediary 名官方名化
3. `tacztweaks.mixins.json` 移除 refmap 字段（NeoForge 无 refmap 机制）
4. `LivingEntity.applyItemBlocking` 内 `hurtBlockingItem` 专服/合并 jar 签名分裂
   （`(…FI)V` vs `(…F)V`）→ 双 wrap + `require=0`
5. `SoundBufferLibraryMixin`（实机崩溃#1）：真实注点 `lambda$getCompleteBuffer$0`，
   采用上游 `f38f2ff` 通配符修法 `lambda$getCompleteBuffer$*` + `require=0`
6. `SoundPlayManagerMixin`（实机黑屏#2）：本线 tacz 无 `playCompositeAnimationContainerSound`
   收口方法 → PC10 `require=0` + 新增 4 处 PC5 广播注点（reload/inspect 各 2 处）

## 四、验证

- 全量源码构建 BUILD SUCCESSFUL；专服冒烟 Done（多次回归 PASS）；客户端实机用户 PASS
- 防复发工具：`tools/audit_tweaks_source.py`（源级全量注点审计）

## 五、已知边界

- SPR 运行矩阵、多人游戏内功能完整实测未完成；First Aid / Pillager's Gun 门控不触发
- 版本门：tacz `1.1.8+neoforge.1.21.11` 前缀（r0/R1/R2 同线）

# WP-TWEAKS 移植执行记录：TaCZTweaks → NeoForge 1.21.11

> 基线：姊妹 TaCZTweaks_Unofficial `1.21.11` 分支，同步至 Fabric hotfix `f38f2ff`（2026-08-22）。
> 产物：`tacztweaks-2.14.2+neoforge.1.21.11.Beta-1-hotfix.jar`。
> 相对 `Beta-1` 的 hotfix 增量：严格 TaCZ 版本门禁（见 §三.7），音频 mixin 已在 `Beta-1` 时同步上游。

## 一、构建骨架

- MDG 2.0.144 / NeoForge 21.11.45 / JDK 21 / Gradle 9.2.1（与 TaCZ-Renovated 1.21.11 同骨架）
- Kotlin（55 个 kt 文件，核心逻辑所在）：KGP 2.4.10 + in-process 编译 +
  kotlin-stdlib 2.4.10 jarJar 内嵌（NeoForge 无 FLK 等价物）
- 依赖 `libs/` 内置：tacz 1.21.11（compileOnly+localRuntime）、YACL 3.8.2-neoforge、
  SPR 1.5.1-neoforge、commons-math3
- 低内存构建档案写入 `gradle.properties`
- Hotfix 追加：最小 JUnit Jupiter 单元测试源集（`src/test/java`），仅覆盖纯 JDK 的
  版本门禁校验类，**不**接入 NeoForge game-test、不引入 Fabric Loom / example pack。

## 二、Fabric→NeoForge 翻译面（要点）

- 入口：`@Mod` 构造器 + `TaCZTweaksClient.init`（`FMLLoader.getCurrent().getDist()` 门控）
- 事件：`ServerTickEvent.Post`（21.11 在 `event.tick` 包）/ `ServerStoppedEvent` /
  `BlockEvent.BreakEvent` / `PlayerEvent.LoggedIn/Out`
- 网络：`RegisterPayloadHandlersEvent` 一次注册 8 条载荷；handle 改 `IPayloadContext` +
  `enqueueWork`；发送 `ClientPacketDistributor` / `PacketDistributor`
- 数据管理器：`AddServerReloadListenersEvent`（游戏总线事件）+ `SimplePreparableReloadListener`
- 配置屏：`ModContainer.registerExtensionPoint(IConfigScreenFactory.class, ...)`
- 注册：`DeferredRegister`；`KeyMapping.Category` 为 vanilla 原生类零改写

## 三、mixin/门禁层（76 个全部落地，hotfix 项已标注）

1. 4 个 lambda 注点零重定位（同构源码树，javap 实证）
2. `method_5773` → `tick` + `<init>` 描述符 intermediary 名官方名化
3. `tacztweaks.mixins.json` 移除 refmap 字段（NeoForge 无 refmap 机制）
4. `LivingEntity.applyItemBlocking` 内 `hurtBlockingItem` 专服/合并 jar 签名分裂
   （`(…FI)V` vs `(…F)V`）→ 双 wrap + `require=0`
5. `SoundBufferLibraryMixin`（实机崩溃#1）：真实注点 `lambda$getCompleteBuffer$0`，
   采用上游 `f38f2ff` 通配符修法 `lambda$getCompleteBuffer$*` + `require=0`
   ——**本 hotfix 不重复改动该文件，仅确认其仍保留通配 + `require=0` 形态**。
6. `SoundPlayManagerMixin`（实机黑屏#2）：本线 tacz 无 `playCompositeAnimationContainerSound`
   收口方法 → PC10 `require=0` + 新增 4 处 PC5 广播注点（reload/inspect 各 2 处）
7. **【hotfix】TaCZ 版本门禁**：原 `String.startsWith("1.1.8+neoforge.1.21.11")` 可被任意
   “前缀 + 垃圾文本”(如 `1.1.8+neoforge.1.21.11EVIL`)绕过，也无法区分 Fabric 串/错误
   MC 家族。替换为独立工具类 `TaczVersionSupport`：
   - 严格正则：`^1\.1\.8\+neoforge\.1\.21\.11\.[rR](0|[1-9][0-9]*)(?:-[A-Za-z0-9]+(?:\.[A-Za-z0-9]+)*)*$`
   - revision 以 `Integer.parseInt` 数值比较（避免 R10 < R2 字典序陷阱）
   - 接受：`r0`（当前 shipped jar）、`R1`、`R2`、`r10`、带合理 `-hotfix.N` 后缀
   - 拒绝：null / 空串 / 错误 core(1.1.9) / 错误 MC 家族(26.2) / Fabric 串 /
     缺失或畸形 revision(`.r`、`.rx`、`.r-1`、`.r01`) / 前缀+垃圾
   - **明确不采用** Fabric 侧 “revision ≥ R2” 规则：NeoForge 1.21.11 线的目标依赖
     本身就是 `r0`，照抄会误拒现有发布件。
   - 单测位于 `src/test/java/me/muksc/tacztweaks/TaczVersionSupportTest.java`，
     `./gradlew test` 运行。

## 四、验证

- 静态：`git diff --check` 无空白错误；版本门禁单测代码审阅通过（accept/reject 矩阵完整覆盖任务要求）。
- **注意**：本沙箱环境未预装 JDK 21，且外网不可用以安装，`./gradlew test` 与 `./gradlew build`
  未能在 Agent 侧实际执行；维护者需在本地 JDK 21 环境运行以下命令确认：
  - `./gradlew test`（JUnit 平台运行版本门禁单测，期望全部 PASS）
  - `./gradlew build`（产物 `build/libs/tacztweaks-2.14.2+neoforge.1.21.11.Beta-1-hotfix.jar`）
- `Beta-1` 基线已确认：全量源码构建 BUILD SUCCESSFUL；专服冒烟 Done（多次回归 PASS）；
  客户端实机用户 PASS。Hotfix 改动面仅限：版本校验工具类 + 其调用点 + 单测 + 版本号/文档。

## 五、已知边界

- SPR 运行矩阵、多人游戏内功能完整实测未完成；First Aid / Pillager's Gun 门控不触发
- 版本门：tacz NeoForge 1.21.11 release family（r0/R<n>，数值比较；详见 §三.7 与 README）
- Agent 环境 JDK 21 缺失，Gradle 级测试/构建需本地复验

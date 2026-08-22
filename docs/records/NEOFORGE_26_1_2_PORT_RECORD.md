# WP-NF 移植执行记录：TaCZTweaks Fabric 26.1.2 → NeoForge 26.1.2

> 基线：本仓库 `26.1.2` Fabric 分支（`d74ce01`，`2.14.2+fabric.26.1.2.Beta-1-hotfix`）
> 目标依赖：[TaCZ: Renovated](https://github.com/q14433686-arch/TaCZ_Renovated) 分支 `26.1.2`，
> `mod_version=1.1.8+neoforge.26.1.2.R1`，NeoForge `26.1.2.97`，Java 25，未混淆
> 参考范式：本仓库 `1.21.11-neoforge` 分支（`27bcb31`）
> 计划文档：[`NEOFORGE_26_1_2_PORT_PLAN.md`](NEOFORGE_26_1_2_PORT_PLAN.md)

## 0. 完成范围与未完成项（先说清楚）

**已完成（源码级）**：WP-NF1 骨架、WP-NF2 入口/生命周期、WP-NF3 网络、WP-NF4 数据管理器与
方块破坏、WP-NF5 配置屏，外加 compat 门控改写、审计/一致性脚本改造、README/BUILD/CHANGELOG/
COMPATIBILITY/KNOWN_ISSUES 更新。

**未完成（必须由维护者在本地做）**：

1. `./gradlew build`、`./gradlew test` —— 移植环境**没有 JDK**，也**无法访问** Maven /
   NeoForged / CurseForge / GitHub release 附件，一行都没有编译过；
2. 75 个 mixin 的**描述符级**复核（需要 TaCZ: Renovated jar 与 MDG 产出的 Minecraft jar）；
3. 四个可选 compat（SPR / First Aid / Pillager's Gun / LRTactical）对真 jar 的类名与方法核对；
4. 专服冒烟（`scripts/check_server_log.py`）与客户端实机；
5. `RESOURCE_IMPORT_MANIFEST.tsv` 中 6 条依赖的 SHA-256（当前是 `pending`）。

在 1–4 完成前，PR 应保持 Draft，任何对外文案不得写"支持/已修复"。

## 1. 构建骨架（WP-NF1）

| 项 | Fabric 原状 | 现状 |
|---|---|---|
| 构建脚本 | `build.gradle.kts` + Loom 1.17-SNAPSHOT | `build.gradle`（Groovy）+ `net.neoforged.moddev` 2.0.144 |
| settings | `settings.gradle.kts`（FabricMC 仓库） | `settings.gradle`（NeoForged 仓库 + foojay toolchain resolver） |
| Java | 25 | 25（`JavaLanguageVersion.of(25)` + `kotlin { jvmToolchain(25) }`） |
| Kotlin 运行时 | FLK 提供 | `jarJar "org.jetbrains.kotlin:kotlin-stdlib:2.4.10"` |
| 依赖 | Modrinth/Maven + `libs/` TaCZ jar | 全部 `libs/` 本地 jar（tacz / YACL / SPR / firstaid / pillagers_gun / commons-math3） |
| 元数据 | `src/main/resources/fabric.mod.json` | `src/main/templates/META-INF/neoforge.mods.toml` + `generateModMetadata` |
| 反编译 | — | `enable { disableRecompilation = true }`（避免 Vineflower OOM） |
| 门禁任务 | checkModIcon / vendored deps / examplePackZip / 测试暂存 | checkModIcon / checkVendoredDependencies / examplePackZip（测试暂存逻辑随 Loom 一起删除） |

## 2. 加载器 API 翻译（WP-NF2/3/4/5）

改动共涉及 14 个原本引用 Fabric API 的文件，其余 130+ 个源文件未动（纯 MC/Kotlin 逻辑）。

| 文件 | 翻译 |
|---|---|
| `TaCZTweaks.java` | `ModInitializer` → `@Mod` 构造器；`ServerTickEvents.END_SERVER_TICK` → `ServerTickEvent.Post`；`SERVER_STOPPED` → `ServerStoppedEvent`；`END_DATA_PACK_RELOAD` → `AddServerReloadListenersEvent` 内的收尾 `SimplePreparableReloadListener`；`PlayerBlockBreakEvents.AFTER` → `BlockEvent.BreakEvent`；`ServerPlayConnectionEvents.JOIN/DISCONNECT` → `PlayerEvent.PlayerLoggedIn/OutEvent`；`GunShootEvent.CALLBACK.register` → `@SubscribeEvent`（证据：Renovated `LivingEntityShoot.java:146-147` 用 `NeoForge.EVENT_BUS.post(gunShootEvent)`）；版本门改用 `TaczVersionSupport` |
| `TaCZTweaksClient.java` | `ClientModInitializer` → `TaCZTweaks` 在 `FMLLoader.getCurrent().getDist().isClient()` 下调用；`KeyMappingHelper` → `RegisterKeyMappingsEvent`；`ClientTickEvents.END_CLIENT_TICK` → `ClientTickEvent.Post`；`ClientPlayConnectionEvents.DISCONNECT` → `ClientPlayerNetworkEvent.LoggingOut`；ModMenu → `ModContainer#registerExtensionPoint(IConfigScreenFactory.class, …)` |
| `TaczVersionSupport.java`（新增） | 严格正则 `^1\.1\.8\+neoforge\.26\.1\.2\.[rR](0\|[1-9][0-9]*)(-…)*$` + revision 数值比较，**最低 R1**（Renovated 26.1.2 线首发即 R1；不照抄 Fabric 的 ≥R2，也不照抄 NF 1.21.11 的 ≥r0） |
| `registry/ModStatusEffects.kt` | `Registry.registerForHolder` → `DeferredRegister` + `DeferredHolder`（仍是 `Holder<MobEffect>`，调用点不变） |
| `network/NetworkHandler.kt` | 三段式 → 一次 `RegisterPayloadHandlersEvent`（`playToServer`/`playToClient` 同时声明 codec 与 handler，不存在单端注册风险）；发送改 `ClientPacketDistributor` / `PacketDistributor` |
| 8 个 message 类 | handler 签名 → `(msg, IPayloadContext)` + `ctx.enqueueWork {}`；玩家取自 `ctx.player() as? ServerPlayer`；服务器取自 `ServerLifecycleHooks.getCurrentServer()`；客户端取自 `Minecraft.getInstance()` |
| `ClientMessagePlayerUnload.kt` | 另外把 refabricated 专有的 `cn.sh1rocu.tacz.util.itemhandler.ItemHandlerHelper` 换成 `net.neoforged.neoforge.items.ItemHandlerHelper`（证据：Renovated `AbstractGunItem.java:4,202` 使用同一类） |
| `data/manager/BaseDataManager.kt` | Fabric `ResourceLoader` 注册 → 暴露 `id()`，由 `AddServerReloadListenersEvent` 注册（证据：Renovated `CommonAssetsManager.java:61,118`） |
| `core/ProtectedBlockBreaking.kt` | 三段事件链 → `mayInteract` + 可取消 `BlockEvent.BreakEvent`（**语义降级**，已写入 KNOWN_ISSUES 与 README） |
| `ModMixinPlugin.kt` | `FabricLoader.isModLoaded` → `FMLLoader.getCurrent().loadingModList.getModFileById(id) != null`（mixin 阶段注册表未就绪，不能用 `ModList`；证据：Renovated `CarryOnCompatMixinPlugin.java:24`） |
| `FirstAidCompat` / `PillagersGunCompat` / `SoundPhysicsCompat` / `LRTacticalCompat` | 运行期门控改 `ModList.get().isLoaded(id)` |
| `compat/ModMenuApiImpl.java` | 删除 |
| `tacztweaks.mixins.json` | 无 refmap 字段（NeoForge 无该机制）；`compatibilityLevel` `JAVA_17` → `JAVA_25`（Renovated `tacz.carryon.mixins.json` 已用 JAVA_25，证明该枚举被接受） |

## 3. mixin 层静态核对（已做的部分）

用脚本把 75 个 mixin 的 `@Mixin` 目标、`method = "…"`、`@At(target = "…")` 全部抽出来，
与 `TaCZ_Renovated` 26.1.2 源码树逐条比对：

- 75/75 目标类命中（含 `me.xjqsh.lrtactical.*`，Renovated 内置该层源码）；
- 所有 `method=` 的方法名在目标类源码中存在；
- 56 个 owner 为 `com/tacz` 或 `me/xjqsh` 的 `@At target` 全部命中；
- 24 个 owner 为原版 / `com.mojang` / `com.sonicether` / commons-math3 的 target **未验证**
  （沙箱没有 Minecraft 26.1.2 与 SPR NeoForge jar）；26.1.x 未混淆，两个加载器共用同一份原版类，
  预期可逐字沿用，但仍需 `javap` 复核。

lambda 注点抽样复核（源码级，非字节码级）：

| 注点 | Renovated 证据 |
|---|---|
| `LocalPlayerInspect.lambda$inspect$0` + `GunData#getBolt` | `LocalPlayerInspect.java:41-56`，`inspect()` 内唯一 lambda，且内部调用 `gunData.getBolt()` |
| `GunAnimationStateContext.lambda$shouldSlide$0` + `Entity#isCrouching` | `GunAnimationStateContext.java:340-342` |
| `InaccuracyModifier.lambda$initCache$0` + `GunData#getInaccuracy` | `InaccuracyModifier.java:92-104` |
| `TickAnimationEvent.lambda$tickAnimation$0` + `LocalPlayer#isSprinting` | `TickAnimationEvent.java:13-31`（`tickAnimation(Minecraft)` 是类内第一个含 lambda 的方法） |
| `SoundPlayManager.lambda$playMessageSound$0(ServerMessageSound, LivingEntity, GunDisplayInstance)` | `SoundPlayManager.java:193-212` |

**仍未复核**：`SoundBufferLibraryMixin`（原版 lambda，1.21.11 线曾因此崩溃，现为通配 + `require=0`）、
`SoundPlayManagerMixin` 的收口方法是否存在于 Renovated、`BlocksAttacks#hurtBlockingItem` 的
专服/合并 jar 描述符分裂、四个 compat mixin 的第三方类面。

## 4. 测试与脚本

- 删除需要 Minecraft 引导的 Kotlin 测试（`CodecSmokeTest`、`ExamplePackTest`、`MonoConversionTest`）
  与其 fixtures：NeoForge 测试源集不经 MDG，没有 Minecraft classpath；
- 保留纯 JDK 测试：`SafeMathTest`、`StackSplitterTest`、`ProjectileIndexAllocatorTest`；
- 新增 `TaczVersionSupportTest.kt`（accept/reject 矩阵，覆盖 r0 拒绝、Fabric 串拒绝、
  1.21.11/26.2 家族拒绝、`R01`/`R-1`/前缀+垃圾拒绝、R10 数值比较）；
- `scripts/check_mod_icon.py`：读 `neoforge.mods.toml` 的 `logoFile`；
- `scripts/download_dependencies.py`：支持 `pending` 摘要（存在性可查，发布前必须补真实 SHA-256）；
- `scripts/check_release_consistency.py`：改查 toml、版本门常量、发布 jar 内的 `META-INF/neoforge.mods.toml`；
  未提交的 `libs/*.jar` 缺失时降级为 NOTE；
- `scripts/audit_port.py`：TaCZ jar 名/元数据、`@OnlyIn(Dist.CLIENT)` 判定、版本正则、
  `build.gradle` 门禁项、测试清单全部改为 NeoForge 形态。

当前本地运行结果：

```
python3 scripts/check_mod_icon.py            -> OK
python3 scripts/check_release_consistency.py -> RELEASE CONSISTENCY: OK（libs 缺失为 NOTE）
python3 scripts/audit_port.py                -> 1 error / 63 warning
```

那 1 个 error 与 63 个 warning 全部源于同一件事：`libs/tacz-1.1.8+neoforge.26.1.2.R1.jar`
不在沙箱内，导致目标类检查无法进行。放入 jar 后必须复跑 `--strict` 并要求 0 error / 0 warning。

## 4.1 首次本地构建反馈（2026-08-23，维护者 Windows / JDK 25）

第一次 `gradlew build` 的两个失败点及处置：

1. **`compileKotlin` 数百条 `Unresolved reference 'tacz' / 'dev' / 'xjqsh'`**
   —— 根因是 `libs/` 里没有任何 jar（Gradle 只给出 6 条 `Specified Dependency Does Not Exist`
   警告后继续编译）。处置：新增 `checkRequiredDependencies` 任务并让 `compileJava` /
   `compileKotlin` 依赖它，缺件时直接给出"缺哪个文件 + 去哪下"的报错。
   同时确认 **First Aid 与 Pillager's Gun 的 jar 编译期并不需要**（前者走 `Class.forName` 反射，
   两者的 mixin 分别是 `targets=` 字符串与 tacz 自己的类），已改为"存在才加入 classpath"；
   编译必需的是 tacz / YACL / Sound Physics / commons-math3 四个。
2. **`checkModIcon` 以退出码 112 失败** —— 该任务原本 fork `py`/`python`/`python3` 调用
   `scripts/check_mod_icon.py`，Windows 上解释器缺失/Store 占位程序会返回非 0，而原实现把任何
   非 0 都当成校验失败。处置：`checkModIcon` 与 `checkVendoredDependencies` 改为**纯 Groovy 实现**
   （自己算 SHA-256、解析 PNG IHDR 尺寸、读 `neoforge.mods.toml` 的 `logoFile`、核对
   `THIRD_PARTY_NOTICES.md`），构建不再依赖 Python；Python 脚本仅作为 CI/命令行工具保留。

**仍未验证**：补齐 jar 之后的实际编译结果。第一轮构建只证明了 MDG/NeoForge 26.1.2 环境本身能
起来（`createMinecraftArtifacts` 成功）以及 Kotlin 2.4.10 + JDK 25 工具链可用。

## 4.2 第二、三轮构建反馈（2026-08-23）

- **依赖识别**：`libs/` 里的 jar 改为按文件名关键词匹配，且匹配前忽略 `-` `_` `.` 与空格
  （维护者的 YACL 文件名是 `yet_another_config_lib_v3-...`，下划线拼写）。名字含 `fabric`
  的文件一律忽略，防止误用 Fabric 版编译。四个必需件已全部识别成功。
- **真实 API 缺口（计划文档 §2 标注"仍缺证据"的那一条得到证实）**：
  NeoForge 26.1.x **没有** `BlockEvent.BreakEvent`。等价物是
  `net.neoforged.neoforge.event.level.block.BreakBlockEvent`，构造器
  `(Level, BlockPos, BlockState, Player)`，实现 `ICancellableEvent`。
  证据：`neoforged/NeoForge` 分支 `26.1.x`
  `src/main/java/net/neoforged/neoforge/event/level/block/BreakBlockEvent.java`，
  以及 `common/CommonHooks.java:601-620` 的 `fireBlockBreak(...)` —— 它 `new BreakBlockEvent(...)`
  后 `NeoForge.EVENT_BUS.post(event)`。
  处置：
  - `ProtectedBlockBreaking` 直接 post `BreakBlockEvent`（**不**走 `CommonHooks.fireBlockBreak`，
    因为后者会按手持物品的 `canDestroyBlock` 预取消——子弹命中不是手持物品在挖方块）；
  - `TaCZTweaks#onBlockBreak` 订阅 `BreakBlockEvent`，用 `EventPriority.LOWEST` +
    跳过已取消事件来逼近原来的 "AFTER" 语义。
- **摘要**：维护者本机下载的 tacz / SPR / commons-math3 三件的 SHA-256 已写回
  `RESOURCE_IMPORT_MANIFEST.tsv`，并注明"取自维护者下载，未与上游公布校验值交叉核对"。
  YACL 条目的文件名已改为实际拼写，摘要仍为 `pending`。

## 4.3 首次实机加载反馈（2026-08-23，客户端 26.1.2 + NeoForge 26.1.2.97）

**加载已经推进到 mixin 应用阶段**：jar 被 FML 识别为
`TaCZ Tweaks (Renovated) 2.14.2+neoforge.26.1.2.Beta-1`，内嵌的 `kotlin-stdlib-2.4.10.jar`
被 jar-in-jar 正确展开，mixin `Compatibility level set to JAVA_25` 被接受，
`TaczVersionSupport` 版本门放行了 `tacz 1.1.8+neoforge.26.1.2.R1`。

**崩溃点**：`features.bullet_interactions.LivingEntityMixin` 的
`tacztweaks$applyItemBlocking$customDurability` ——
`Critical injection failure ... (0/1) succeeded. Scanned 0 target(s).`

根因（NeoForge 对原版打的补丁，分支 `26.1.x`）：

- `patches/net/minecraft/world/item/component/BlocksAttacks.java.patch` 新增重载
  `hurtBlockingItem(Level, ItemStack, LivingEntity, InteractionHand, float, int fixedDamage)`；
- `patches/net/minecraft/world/entity/LivingEntity.java.patch` 把 `applyItemBlocking` 里的调用
  改成了这个 6 参重载，传入 `CommonHooks.onDamageBlock(...)` 返回的 `ev.shieldDamage()`。

因此只认原版 5 参描述符 `(…F)V` 的 wrap 一个目标都扫不到。处置：

1. 新增 6 参 wrap（`…FI)V`）作为 NeoForge 主路径，并且**不再自己重写耐久逻辑**——把自定义耐久值
   当作 `fixedDamage` 传回 `original.call(...)`，让 NeoForge 自己的 `hurtAndBreak` +
   `onPlayerDestroyItem` + 破盾修复照常执行；
2. 原 5 参 wrap 保留为 `require = 0` 的回退（万一某个构建仍用原版调用点）；
3. 两个 wrap 都 `require = 0` 会带来"静默失效"风险，因此在 RETURN 处新增一次性 `LOGGER.warn`：
   当数据包解析出了盾牌规则、却没有任何耐久 wrap 命中时明确告警，而不是安静地少一个功能。

**顺带对其余注入原版类的 mixin 做了 NeoForge patch 核对**（同一分支的 `patches/` 目录）：

| 目标 | NeoForge 是否打补丁 | 我们的注入点是否受影响 |
|---|---|---|
| `LivingEntity#applyItemBlocking` | 是 | **是**（已修，见上） |
| `EnderMan#hurtServer` | 该类有补丁 | 否（补丁只动 `setTarget` / `isBeingStaredBy` / 搬运 AI） |
| `EnchantmentHelper#getDamageProtection` | 该类有补丁 | 否（补丁未触及该方法） |
| `LocalPlayer#tick` / `aiStep` | 该类有补丁 | 否 |
| `MouseHandler#turnPlayer` / `handleAccumulatedMovement` | 该类有补丁 | 否（补丁只动屏幕鼠标事件/滚轮/拖拽） |
| `SoundBufferLibrary` / `ClipContext` / `AvatarRenderer` | 无补丁 | 否 |

另外重跑了一次"注入点落在 Renovated 对应方法体内"的加强版静态核对：没有发现 TaCZ 侧调用点漂移
（脚本对 lambda 与构造器有解析局限，逐个人工复核了 `EntityUtil#findEntitiesOnPath`、
`EntityKineticBullet#createDamageSources`、`AdsModifier#initCache/getPropertyDiagramsData`、
`GunSoundInstance` 的两个构造器描述符，均存在且形参一致）。

## 5. 未来提交者注意

1. 源码里仍有若干注释描述的是 **Refabricated**（Fabric 目标端）的方法/lambda 命名由来；
   在对 Renovated 做完描述符复核后应逐条更新措辞。
2. `docs/` 下 `CONFIGURATION.md`、`SUPPORT.md`、`docs/data/*`、`docs/publish/*`、`AUDIT.md`、
   `PORTING_NOTES.md` 仍以 Fabric 口径书写，属于 WP-NF8 剩余工作。
3. CI（`.github/workflows`）本分支尚无 workflow，需要按 NeoForge 构建重建。

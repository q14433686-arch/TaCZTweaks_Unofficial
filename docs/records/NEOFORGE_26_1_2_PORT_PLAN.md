# TaCZ Tweaks → NeoForge 26.1.2 移植分析（WP-NF0 立项文档）

> 生成日期：2026-08-22 · 工作分支：`arena/01a02a8c-tacztweaks-unofficial`（基线 `26.1.2-neoforge` @ `d74ce01`）
>
> **执行状态（2026-08-23 更新）**：WP-NF1~NF5 与文档/脚本改造已落地，执行细节见
> [`NEOFORGE_26_1_2_PORT_RECORD.md`](NEOFORGE_26_1_2_PORT_RECORD.md)。
> WP-NF6（mixin 描述符复核）、WP-NF7（compat 对真 jar 核对）、构建与实测仍未完成。
> 本文件是**分析与计划**，不是完成声明。任何"已实现/已验证"字样只出现在明确标注的
> 「已核对证据」小节，其余一律标注为待办或待验证。

---

## 0. 结论摘要

| 判断 | 结论 |
|---|---|
| 可行性 | **高**。整个 mod 只有 14 个源文件触碰 Fabric API；其余 131 个源文件是纯 Minecraft/Kotlin/mixin 代码 |
| mixin 层风险 | **低-中**。75 个 mixin 的目标类 **75/75** 在 TaCZ_Renovated 26.1.2 源码中存在；56 个指向 TaCZ/LR 的 `@At target` 描述符全部命中；24 个指向原版/第三方的 target 因沙箱无 MC jar **未验证** |
| 主要新增工作 | 构建骨架（Loom → ModDevGradle）、8 条网络载荷改注册方式、Kotlin stdlib 内嵌（NeoForge 无 FLK）、ModMenu → `IConfigScreenFactory`、`fabric.mod.json` → `neoforge.mods.toml`、审计脚本改写 |
| 阻断项 | 沙箱内**没有 JDK**、**Maven/CurseForge 网络不可达**、**GitHub release 附件不可下载**（`git clone` 可用）。因此本轮无法执行 `./gradlew build`，编译验证必须在维护者本地完成 |

---

## 1. 基线与目标矩阵

| 角色 | 仓库/分支 | 事实 |
|---|---|---|
| **移植源（语义权威）** | 本仓库 `26.1.2` Fabric 分支（= 当前工作树） | `2.14.2+fabric.26.1.2.Beta-1-hotfix`；90 个 `.java` + 55 个 `.kt`；75 个 mixin |
| **移植范式参考** | 本仓库 `1.21.11-neoforge`（`27bcb31`） | 已完成的 NeoForge 化范例；`docs/records/WP_TWEAKS_PORT_RECORD.md` 记录了全部翻译面与踩坑 |
| **目标依赖** | [`q14433686-arch/TaCZ_Renovated`](https://github.com/q14433686-arch/TaCZ_Renovated) 分支 `26.1.2` | `mod_version=1.1.8+neoforge.26.1.2.R1`，modId 恒为 `tacz`，`neo_version=26.1.2.97`，Java 25，未混淆 |
| **产物目标** | 本分支 | 拟定 `tacztweaks-2.14.2+neoforge.26.1.2.Beta-1.jar` |

**注意基线陷阱**：远端 `26.1.2-neoforge` 目前与 `26.1.2` 指向同一 commit，也就是说这条 NeoForge 线
**现在装的是 Fabric 代码**。本分支的第一步是「就地改造」，不是「继续叠加」。

### 与 1.21.11 NeoForge 移植的关键差异

1. **未混淆**：26.1.x 官方名，`@At target` 里的原版描述符可**逐字沿用** Fabric 分支，不需要
   1.21.11 线做过的 `method_5773 → tick` intermediary→官方名转换（`WP_TWEAKS_PORT_RECORD.md` §三.2）。
2. **Java 25 / Gradle 9.2.1 / MDG 2.0.144**（`TaCZ_Renovated/build.gradle`：`JavaLanguageVersion.of(25)`），
   而 1.21.11 线是 JDK 21。Kotlin 2.4.10 编译到 JVM 25 已在本仓库 Fabric 侧证明可行
   （`build.gradle.kts:78` `jvmTarget.set(JvmTarget.JVM_25)`）。
3. **可选兼容 mod 这次真的存在 NeoForge 26.1.2 目标**（1.21.11 线是"门控永不触发"）：
   - Sound Physics Remastered `1.5.1+26.1.2` NeoForge
   - YetAnotherConfigLib `3.9.6 for neoforge 26.1`（Fabric 侧用的是 `3.9.6+26.1-fabric`）
   - First Aid New `1.2.8+neoforge26.1`
   - Pillager's Gun (Unofficial Port) `3.2.2 neoforge 26.1.2`
   → 这些 compat mixin 从"死代码"变成"要负责的活代码"，**但它们的内部类/方法名必须对着 NeoForge 版 jar
     重新核对**，不能假设与 Fabric 版一致。
4. **26.1.2 Fabric 线比 1.21.11 线多 6 个源文件**，NeoForge 侧没有先例，需要单独处理：
   `core/SprintReloadContext.java`、`mixin/gun/movement/LocalPlayerSprintReloadMixin.java`、
   `mixin/gun/movement/LocalPlayerReloadCancelMixin.java`、`mixin/tweaks/GunSoundInstanceTaczSoundMixin.java`、
   `mixininterface/tweaks/MonoTaczSound.java`、`compat/ModMenuApiImpl.java`（后者是要**删除**的那个）。

---

## 2. 已核对证据（本轮实际执行的静态检查）

方法：把 Fabric 分支全部 mixin 的 `@Mixin` 目标与 `method = "…"` / `@At(target = "…")` 抽出来，
对 `git clone --depth 1 -b 26.1.2 TaCZ_Renovated` 的源码树逐一比对（脚本：本轮临时脚本，结论如下）。

| 检查 | 结果 |
|---|---|
| 75 个 mixin 的目标类在 Renovated 26.1.2 源码中存在 | **75 / 75 命中**（含 `me.xjqsh.lrtactical.*`：Renovated 内置了 LR 层源码） |
| 所有 `method = "…"` 的方法名在目标类源码中出现 | **全部出现**（文本级，不等于描述符级） |
| 56 个 owner 为 `com/tacz` 或 `me/xjqsh` 的 `@At target` | **全部命中类 + 方法名** |
| 24 个 owner 为原版/`com.mojang`/`com.sonicether`/`commons-math3` 的 `@At target` | **未验证**（沙箱无 MC 26.1.2 jar、无 SPR NeoForge jar） |
| NeoForge 26.1.2 侧 API 存在性 | 用 Renovated 源码作证：`RegisterPayloadHandlersEvent`/`PayloadRegistrar`（`com/tacz/guns/network/NetworkHandler.java:20-33`）、`AddServerReloadListenersEvent`（`com/tacz/guns/resource/CommonAssetsManager.java:61,118`）、`ServerTickEvent`（`net.neoforged.neoforge.event.tick`）、`RegisterKeyMappingsEvent`（`client/init/ClientSetupEvent.java:55,71`）、`IConfigScreenFactory` + `registerExtensionPoint`（`client/gui/compat/ClothConfigScreen.java:15,34`）、`ClientPacketDistributor` / `PacketDistributor`、`DeferredRegister`、`FMLLoader.getCurrent()`、`ModList.get()` |

**仍缺证据的 NeoForge API**（Renovated 未使用，需要在拿到 NeoForge 26.1.2.97 jar 后确认签名）：
`net.neoforged.neoforge.event.level.BlockEvent.BreakEvent` 的构造器与是否仍走 `NeoForge.EVENT_BUS.post`
（1.21.11 线用的是 `BlockEvent.BreakEvent(level, pos, state, player)`，26.1.x 可能已改为 `EventHooks`/新签名）。

---

## 3. 翻译面 A：构建骨架

| 项 | Fabric 现状 | NeoForge 目标 | 证据/来源 |
|---|---|---|---|
| 构建插件 | `fabric-loom 1.17-SNAPSHOT`（`build.gradle.kts`） | `net.neoforged.moddev 2.0.144` | Renovated `build.gradle` / 1.21.11-neoforge |
| 脚本语言 | Kotlin DSL | 跟随 1.21.11-neoforge 用 Groovy `build.gradle`（与 Renovated 同骨架，减少差异） | 决策项 D-1 |
| Java | 25 | 25（`java.toolchain.languageVersion = 25`） | Renovated `build.gradle` |
| Gradle | 9.5.1 | 9.2.1（与 Renovated 对齐）或保留 9.5.1 | 决策项 D-1 |
| NeoForge | — | `neo_version=26.1.2.97`，`minecraft_version_range=[26.1.2]` | Renovated `gradle.properties` |
| Kotlin | FLK 运行时提供 | `jarJar "org.jetbrains.kotlin:kotlin-stdlib:2.4.10"`（NeoForge 无 FLK 等价物） | 1.21.11-neoforge `build.gradle` |
| MixinExtras | Fabric Loader 自带 0.5.4 | NeoForge 自带；`mixins.json` 中保留 `mixinextras.minVersion`，**删除 refmap 字段** | `WP_TWEAKS_PORT_RECORD.md` §三.3 |
| 元数据 | `src/main/resources/fabric.mod.json` | `src/main/templates/META-INF/neoforge.mods.toml` + `generateModMetadata` 任务 | 1.21.11-neoforge |
| 反编译 | — | `enable { disableRecompilation = true }`（沙箱/低内存机器必需，Vineflower 默认 -Xmx4g 会 OOM） | Renovated `build.gradle` 注释 |
| 本地依赖 | `libs/` + `scripts/download_dependencies.py` 校验 SHA-256 | 同机制，换清单（见 §6） | 保留 |
| 测试 | Loom 下的 JUnit + fixtures（`src/test`，约 6 个测试类） | 保留纯 JDK 部分（codec/数学/版本门），**不接 NeoForge game-test** | 1.21.11-neoforge 只保留了版本门单测 |

---

## 4. 翻译面 B：加载器 API（14 个文件，逐条映射）

| 文件 | Fabric 用法 | NeoForge 26.1.2 替代 |
|---|---|---|
| `TaCZTweaks.java` | `ModInitializer#onInitialize` | `@Mod(MOD_ID)` 构造器 `(IEventBus modEventBus, ModContainer container)` |
| " | `ServerTickEvents.END_SERVER_TICK`（:115） | `@SubscribeEvent ServerTickEvent.Post`（游戏总线） |
| " | `ServerLifecycleEvents.SERVER_STOPPED`（:124） | `ServerStoppedEvent` |
| " | `ServerLifecycleEvents.END_DATA_PACK_RELOAD`（:129） | 挂在 `AddServerReloadListenersEvent` 里注册一个收尾 `SimplePreparableReloadListener`（1.21.11 线做法） |
| " | `PlayerBlockBreakEvents.AFTER`（:135） | `BlockEvent.BreakEvent`（**无 AFTER 等价物**，见风险 R-3） |
| " | `ServerPlayConnectionEvents.JOIN/DISCONNECT` | `PlayerEvent.PlayerLoggedInEvent` / `PlayerLoggedOutEvent` |
| " | `FabricLoader#getModContainer("tacz").version` | `ModList.get().getModContainerById("tacz")…getModInfo().getVersion()` |
| `TaCZTweaksClient.java` | `ClientModInitializer` | 由主类在 `Dist.CLIENT` 下调用 `init(modEventBus, container)`（`FMLLoader.getCurrent().getDist()`） |
| " | `KeyMappingHelper.registerKeyMapping`（:26-28） | `RegisterKeyMappingsEvent#register`（mod 总线；Renovated `ClientSetupEvent.java:71`） |
| " | `ClientTickEvents.END_CLIENT_TICK`（:32） | `ClientTickEvent.Post` |
| " | `ClientPlayConnectionEvents.DISCONNECT`（:38） | `ClientPlayerNetworkEvent.LoggingOut` |
| `network/NetworkHandler.kt` | `PayloadTypeRegistry` + `ServerPlayNetworking` + `ClientPlayNetworking`（三段式，8 条载荷） | 一次 `RegisterPayloadHandlersEvent` → `registrar.playToServer/playToClient(TYPE, CODEC, ::handle)` |
| 4 个 `network/message/ClientMessage*.kt` | handler 签名 `(payload, ServerPlayNetworking.Context)`；`PacketSender` | `IPayloadContext` + `context.enqueueWork {}`；发送用 `ClientPacketDistributor.sendToServer` / `PacketDistributor.sendToPlayer` |
| `data/manager/BaseDataManager.kt` | `ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener`（:42） | `AddServerReloadListenersEvent#addListener(Identifier, PreparableReloadListener)`（Renovated `CommonAssetsManager.java:118`） |
| `core/ProtectedBlockBreaking.kt` | `PlayerBlockBreakEvents.BEFORE / CANCELED / AFTER` 完整链（:29-48） | `level.mayInteract` + `BlockEvent.BreakEvent` 取消判定 + `level.destroyBlock`；语义降级见 R-3 |
| `ModMixinPlugin.kt` | `FabricLoader.isModLoaded`（:15,17） | `FMLLoader.getCurrent().getLoadingModList().getModFileById(id) != null`（mixin plugin 阶段注册表未就绪，**不能用 `ModList.get()`**；Renovated `mixin/carryon/CarryOnCompatMixinPlugin.java:24`） |
| `compat/FirstAidCompat.java`、`compat/PillagersGunCompat.java`、`compat/soundphysics/SoundPhysicsCompat.kt`、`compat/lrtactical/LRTacticalCompat.kt` | `FabricLoader.isModLoaded` | `ModList.get().isLoaded(id)`（运行期，安全） |
| `compat/ModMenuApiImpl.java` | ModMenu 入口 | **删除**；改 `container.registerExtensionPoint(IConfigScreenFactory.class, …)` 返回 YACL 屏 |
| `src/main/resources/fabric.mod.json` | 依赖/入口/mixin 声明 | `neoforge.mods.toml` 模板（`${mod_version}` 等由 `generateModMetadata` 展开） |

---

## 5. 翻译面 C：mixin 层（75 个）

- **`remap = false`**：Fabric 侧给 TaCZ mixin 标了 `remap=false`，NeoForge 侧同样保留（26.1.x 未混淆，
  官方名即运行名），迁移零改动。
- **原版目标**：24 个原版 `@At target` 描述符（`LivingEntity#getPose`、`BlocksAttacks#hurtBlockingItem`、
  `AvatarRenderState.isVisuallySwimming` 等）在两个加载器下是同一份 MC 26.1.2 类，**理论上逐字可用**，
  但 NeoForge 会打补丁/AT，仍需拿到 jar 后 `javap` 复核。
- **必须逐条复核的高危项**（来自 1.21.11 线的实机事故记录）：
  1. `tweaks/SoundBufferLibraryMixin` —— 真实注点是 lambda（1.21.11 线崩溃 #1），当前 Fabric 26.1.2 版本
     已用通配 `lambda$getCompleteBuffer$*` + `require=0`；NeoForge 下 lambda 序号可能再变，保持通配。
  2. `tweaks/SoundPlayManagerMixin` —— 1.21.11 线因缺收口方法导致黑屏 #2；26.1.2 的 Renovated
     `SoundPlayManager` 需重新确认是否有 `playCompositeAnimationContainerSound` 之类的收口方法。
  3. `BlocksAttacks#hurtBlockingItem` 的专服/合并 jar 描述符分裂（`(…FI)V` vs `(…F)V`）→ 双 wrap + `require=0`。
  4. `mixin/compat/*` 四组（firstaid / lrtactical / pillagers_gun / soundphysics）：目标是**第三方 NeoForge jar**，
     类名与 Fabric 版可能不同。`ichttt/mods/firstaid/common/EventHandler` 在 1.21.11-neoforge 里是一个
     **编译占位桩**（`src/main/java/ichttt/mods/firstaid/common/EventHandler.java`），26.1.2 线要么继续用桩，
     要么把真 jar 放进 `libs/` 作 `compileOnly`。
- **`tacztweaks.mixins.json`**：删 `refmap`；`compatibilityLevel` 由 `JAVA_17` 提到 `JAVA_21`（NeoForge 26.1.x 惯例，
  待用 jar 确认支持值）；`plugin` 指向改造后的 `ModMixinPlugin`。
- **总线冲突**：TaCZ Renovated 自身也有大量 mixin，需检查 `priority`（现有两处 `priority = 1500`：
  `MouseHandlerMixin`、`LocalPlayerMixin`）与 Renovated 的相同目标是否冲突。

---

## 6. 依赖清单与版本门禁

| 类型 | 依赖 | 目标版本 | 沙箱可获取性 |
|---|---|---|---|
| 必需 | Minecraft | 26.1.2 | MDG 拉取（需 Maven 网络） |
| 必需 | NeoForge | 26.1.2.97 | 同上 |
| 必需 | TaCZ: Renovated | `1.1.8+neoforge.26.1.2.R1` | ❌ release 附件下载被沙箱阻断；✅ 源码可 clone |
| 必需（客户端） | YACL | `3.9.6+26.1-neoforge` | ❌ CurseForge 不可达 |
| 内嵌 | kotlin-stdlib 2.4.10（jarJar） | — | ❌ Maven 不可达 |
| 可选 | Sound Physics Remastered | `1.5.1+26.1.2` NeoForge | ❌ |
| 可选 | First Aid New | `1.2.8+neoforge26.1` | ❌ |
| 可选 | Pillager's Gun (Unofficial Port) | `3.2.2 neoforge 26.1.2` | ❌ |

**版本门禁**：照搬 1.21.11 线的 `TaczVersionSupport`（严格正则 + revision 数值比较，避免 `R10 < R2` 字典序陷阱），
参数改为：core `1.1.8`、family `neoforge.26.1.2`、最低 revision **R1**（Renovated 26.1.2 线当前发布件就是 R1，
不能照抄 Fabric 侧的 "≥R2"，也不能照抄 1.21.11 侧的 "≥r0"）。
`neoforge.mods.toml` 的 `[[dependencies]]` 只能做粗粒度 `[1.1.8,)`，精确家族判定仍由 `TaczVersionSupport` 在
构造器里抛异常完成。

---

## 7. 文档 / 脚本 / CI

- `scripts/audit_port.py`：现在校验 `fabric.mod.json`、Loom 产物名、FLK/MixinExtras 版本 → 需要一个
  NeoForge 变体（校验 `neoforge.mods.toml`、mixin 注册表、语言键、图标哈希、无行为配置项）。
- `scripts/download_dependencies.py` + `RESOURCE_IMPORT_MANIFEST.tsv`：换 7 条新记录（URL + SHA-256）。
- `scripts/check_release_consistency.py`：版本串规则改为 `2.14.2+neoforge.26.1.2.*`。
- 文档需整体改写口径：README / BUILD / AUDIT / PORTING_NOTES / docs/*（"Fabric 26.1.2" → "NeoForge 26.1.2"，
  依赖表、安装说明、ModMenu 段落、兼容矩阵）。**按 AGENTS.md §4，不得把"绕开/未验证"写成"支持"。**
- `tacz-tweaks-example-pack/`：纯数据包，与加载器无关，**原样保留**。

---

## 8. 风险清单

| 编号 | 风险 | 缓解 |
|---|---|---|
| R-1 | 沙箱无 JDK / 无 Maven 网络 → 无法 `./gradlew build`，与 1.21.11 线同样只能交付"静态正确"的代码 | 全流程记录 + 维护者本地 `./gradlew build` / 专服冒烟 / 客户端实测三级门；PR 保持 Draft |
| R-2 | Renovated 是**洁净室重写**，方法体与 Fabric 版可能不同（lambda 序号、内联、收口方法有无） | 所有 `@At` 注点在拿到 jar 后 `javap -p -c` 复核；容易漂移的注点一律 `require=0` + 通配 |
| R-3 | NeoForge 无 `PlayerBlockBreakEvents.AFTER/CANCELED` 三段链，领地类 mod 的兼容性弱于 Fabric 侧 | 用 `BlockEvent.BreakEvent` + `mayInteract`；文档明确写"语义降级，未实测领地 mod" |
| R-4 | 4 个 compat mixin 这次有真实 NeoForge 目标，写错就是真崩溃（不再是死代码） | 每个 compat 单独一个工作包，无 jar 不落地；`ModMixinPlugin` 严格门控 |
| R-5 | Kotlin stdlib jarJar 与其他内嵌 Kotlin 的 mod 冲突 | 与 1.21.11 线同版本策略（2.4.10），并在 KNOWN_ISSUES 记录 |
| R-6 | 专服常量池混入客户端类导致 `NoClassDefFoundError` | 沿用 Renovated 的 `ClientPacketBridge` 模式；客户端逻辑只在 `Dist.CLIENT` 分支反射/隔离调用 |
| R-7 | 分支基线是 Fabric 内容，改造中途容易与 `26.1.2` Fabric 线互相污染 | 只在本分支操作；Fabric 专属文件一次性删净（`fabric.mod.json`、`ModMenuApiImpl`、Loom 配置、fixtures 中的 Loom 依赖） |

---

## 9. 分阶段计划（每阶段带验收门）

| 包 | 内容 | 验收门 |
|---|---|---|
| **WP-NF1** 骨架 ✅ | `build.gradle`(+`settings.gradle`)、`gradle.properties`、`neoforge.mods.toml` 模板、删 Loom/`fabric.mod.json`、jarJar kotlin-stdlib、`libs/` 清单与 manifest | 结构齐备；`--check-only` 依赖脚本能列出缺失项 |
| **WP-NF2** 入口与生命周期 ✅ | `TaCZTweaks`/`TaCZTweaksClient`/`TaczVersionSupport`（R1 门） + 键位/tick/断线/登录登出 | 版本门单测已写（`TaczVersionSupportTest.kt`），**尚未运行** |
| **WP-NF3** 网络层 ✅ | 8 条载荷 → `RegisterPayloadHandlersEvent`；`IPayloadContext.enqueueWork` 线程归位 | 双端注册对称性已核表 |
| **WP-NF4** 数据管理器与方块破坏 ✅ | `BaseDataManager` → `AddServerReloadListenersEvent`；`ProtectedBlockBreaking` → `BlockEvent.BreakEvent` | 语义降级点已写入 KNOWN_ISSUES / README |
| **WP-NF5** 配置屏 ✅ | 删 ModMenu，接 `IConfigScreenFactory` + YACL neoforge | 客户端启动可开屏（**待维护者实测**） |
| **WP-NF6** mixin 复核 | 75 个 mixin 对 Renovated jar / MC jar 逐条 `javap` 复核，重点 §5 四项 | `./gradlew build` 通过 + 专服冒烟 + 客户端实测 |
| **WP-NF7** compat 四件套 | firstaid / pillagers_gun / soundphysics / lrtactical 对 NeoForge 26.1.2 真 jar 核类名 | 每项要么实测 PASS，要么明确标"未实测/已禁用" |
| **WP-NF8** 文档与脚本 | 审计脚本 NeoForge 变体、README/BUILD/AUDIT/CHANGELOG、发布文案、CI | `audit_port.py --strict` 0 error 0 warning；`check_release_consistency.py` 通过 |

---

## 10. 待决策项（需要项目发起人确认）

- ~~**D-1 构建脚本形态**~~：已定为 Groovy `build.gradle`（与 TaCZ_Renovated / 1.21.11-neoforge 一致）。原选项：跟随 `TaCZ_Renovated` / `1.21.11-neoforge` 的 Groovy `build.gradle`（一致性优先），
  还是保留当前 Kotlin DSL `build.gradle.kts`（迁移量更大但可复用现有 jar 内容门禁任务）。
- **D-2 依赖 jar 获取**：沙箱无法下载 tacz/YACL/SPR/firstaid/pillagers_gun。是由维护者本地放入 `libs/`
  后我再核对哈希，还是我先按源码/文档写代码、把 `javap` 复核整体推到本地阶段。
- ~~**D-3 compat 范围**~~：已定为四个全接（代码路径与门控已就位；对真 jar 的类名核对与实测仍未做）。
- ~~**D-4 版本命名**~~：已采用 `2.14.2+neoforge.26.1.2.Beta-1`。

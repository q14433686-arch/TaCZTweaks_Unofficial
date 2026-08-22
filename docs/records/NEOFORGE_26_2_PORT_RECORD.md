# TaCZ Tweaks → NeoForge 26.2 移植记录

> 日期：2026-08-22
> 工作分支：`arena/01a02ad6-tacztweaks-unofficial`
> Fabric 26.2 基线：`76d29874d112880b5443662937de939bf0766d4a`
> NeoForge 26.1.2 骨架参考：`2f87902d9bb0c28136c3b982870698250cd05c87`

## 0. 状态结论

本轮完成了**源码/静态层**的 26.2 改造：构建与元数据目标、加载器 API、Fabric 26.1.2 →
26.2 业务 delta、版本门、依赖清单、文档、26.2 mixin 源码目标复核、NeoForge 原版补丁复核。

以下门禁没有完成，不能写 PASS：

- `./gradlew test`、`./gradlew build`；
- 26.2 客户端主界面；
- 专服 `Done (...)!`；
- 拿枪、开镜、换弹、卸弹、配置屏、数据包重载。

原因是当前沙箱没有任何 JDK，且 Maven、Modrinth、Oracle/Adoptium 与 GitHub
`release-assets.githubusercontent.com` 的二进制连接失败。GitHub git/API 可用，所以源码与
NeoForge patches 已取得，但发布 jar 无法取得。26.1.2 骨架的客户端 PASS 只能说明加载器翻译曾经
成立，不能代替 26.2 验收。

## 1. 基线与叠加方法

1. 用 `origin/arena/01a02a8c-tacztweaks-unofficial` 的 26.1.2 NeoForge 成品作为加载器骨架；
2. 对 `origin/26.1.2..76d2987` 的 `src/main` 与 `docs` 业务 delta 做三方叠加；
3. 冲突处保留 NeoForge 生命周期/网络/重载 API，只吸收 26.2 类型、包名、描述符和文案变化；
4. 未把 Fabric `ModInitializer`、Fabric networking、`ResourceLoader` 或 `fabric.mod.json` 带回；
5. 保留纯 JDK 测试，删除需要 Loom/Minecraft 引导的测试。

业务 delta 涉及 26.2 的 `EntityTypes`、advancement predicate 新包、渲染/声音/数据类型命名等；
这些文件均来自 Fabric 26.2 语义线，而入口、事件、网络、注册和数据重载保持 NeoForge 形式。

## 2. 构建与版本矩阵

| 项 | 26.2 值 |
|---|---|
| Minecraft | `26.2`，范围 `[26.2]` |
| NeoForge | `26.2.0.64`，范围 `[26.2.0.64,)` |
| ModDevGradle | `2.0.144` |
| Java/Kotlin | Java 25 toolchain；Kotlin 2.4.10 |
| 产物 | `2.14.2+neoforge.26.2.Beta-1` |
| TaCZ | `1.1.8+neoforge.26.2.R1`，modId `tacz` |
| Kotlin runtime | `kotlin-stdlib:2.4.10`，`jarJar` |

TaCZ: Renovated 证据：分支 `26.2`，commit
`edb232cde49445eb5b3fa6b60f17b3ea1f883d11`：

- `gradle.properties`: `minecraft_version=26.2`；
- `gradle.properties`: `neo_version=26.2.0.64`；
- `gradle.properties`: `mod_version=1.1.8+neoforge.26.2.R1`；
- `com.tacz.guns.GunMod#GunMod(IEventBus, ModContainer)`：公共入口范式；
- `com.tacz.guns.client.GunModClient#GunModClient(ModContainer)`：
  `@Mod(value = "tacz", dist = Dist.CLIENT)` 客户端隔离范式。

本项目因此也把 `TaCZTweaksClient` 改成物理客户端 `@Mod` 入口，公共 `TaCZTweaks` 不再调用或
解析客户端类。配置屏构造从公共 `Config` 移到 `client.config.ConfigScreenFactory`；公共配置对象
不再含 `net.minecraft.client.*` 类型。YACL 的 config 基类仍参与双端持久化/同步，因此 metadata
把 YACL 声明为 BOTH，而不是误写成仅客户端依赖。

S2C payload 和 C2S 发送也改走 `ClientPacketBridge#invoke(String, Class<?>[], Object...)`；公共
payload/`NetworkHandler` 不再直接引用 `Minecraft`、`SoundPlayManager` 或
`ClientPacketDistributor`。实际客户端逻辑集中在 `client.network.ClientPacketHandlers`。该范式来源于
Renovated 26.2 的 `com.tacz.guns.network.ClientPacketBridge` / `client.network.ClientPacketHandlers`。

## 3. 版本门

`TaczVersionSupport` 固定：

```text
EXPECTED_CORE_VERSION = 1.1.8
EXPECTED_FAMILY       = neoforge.26.2
MIN_REVISION          = 1
```

接受 R1、r1、R2、R10 与规范 `-hotfix.1` 后缀；拒绝 r0、Fabric、NeoForge 26.1.2/1.21.11、
错误 core、前导零和 prefix+junk。`TaczVersionSupportTest` 覆盖该矩阵，但因无 JDK 尚未执行。

## 4. NeoForge API 证据（新增/复核调用）

### 4.1 方块破坏

NeoForge `26.2.x` commit `943a213228872b48f8b795e14ea7b5b81a49d16d`：

- `net.neoforged.neoforge.event.level.block.BreakBlockEvent#BreakBlockEvent(
  Level, BlockPos, BlockState, Player)`；
- `net.neoforged.neoforge.common.CommonHooks#fireBlockBreak(
  Level, GameType, Player, BlockPos, BlockState): BreakBlockEvent` 在构造后调用
  `NeoForge.EVENT_BUS.post(event)`。

`ProtectedBlockBreaking` 不调用 `CommonHooks#fireBlockBreak`，因为后者会根据玩家手持物
`ItemStack#canDestroyBlock` 预取消，而子弹/近战规则并不是“手持物挖掘”。本项目保留
`Level#mayInteract` 后直接 post `BreakBlockEvent` 的 26.1.2 结论。Fabric 三段链没有等价物，
仍是文档化语义降级。

### 4.2 数据重载、网络、配置屏

TaCZ: Renovated 26.2 源码直接使用：

- `AddServerReloadListenersEvent#addListener(Identifier, PreparableReloadListener)`：
  `com.tacz.guns.resource.CommonAssetsManager#reloadAndRegister`；
- `RegisterPayloadHandlersEvent#registrar(String)` 与 payload registrar：
  `com.tacz.guns.network.NetworkHandler#register`；
- `RegisterKeyMappingsEvent#register(KeyMapping)`：
  `com.tacz.guns.client.init.ClientSetupEvent#onRegisterKeys`；
- `ModContainer#registerExtensionPoint(IConfigScreenFactory.class, ...)`：
  `com.tacz.guns.client.gui.compat.ClothConfigScreen#registerNoClothConfigPage`。

本轮没有凭记忆增加新的加载器调用。

## 5. 75 个 mixin 的 26.2 静态核对

对当前 `tacztweaks.mixins.json` 与 75 个 Java mixin 做了源树扫描：

- **75 个 mixin / 56 个唯一目标类**；
- 63 个直接打 `com.tacz.*` / `me.xjqsh.*` 的 mixin，其目标类在 Renovated 26.2 源码中
  **63/63 命中**；
- 非 lambda、非构造器的 `method=` 方法名全部在目标类源码中命中；
- 4 个 `<init>` 由源码构造器人工确认；
- 59 个 owner 为 `com.tacz` / `me.xjqsh` 的 `@At target` 方法引用，类名与方法名
  **59/59 命中**；
- 5 个 lambda 逐项确认其外层方法体仍有对应唯一调用：
  - `GunAnimationStateContext.lambda$shouldSlide$0` → `shouldSlide()` 内 `Entity#isCrouching`；
  - `LocalPlayerInspect.lambda$inspect$0` → `inspect()` 内 `GunData#getBolt`；
  - `TickAnimationEvent.lambda$tickAnimation$0` → `tickAnimation(Minecraft)` 内
    `LocalPlayer#isSprinting`；
  - `InaccuracyModifier.lambda$initCache$0` → `initCache(...)` 内 `GunData#getInaccuracy`；
  - `SoundPlayManager.lambda$playMessageSound$0` → `playMessageSound(...)` 的 display lambda。

**边界**：这是未编译源码检查，不是发布 jar 的 descriptor/字节码检查。`audit_port.py` 因缺 TaCZ
jar 当前仍正确报告 `1 error / 63 warnings`；不得把上述命中写成严格审计 PASS。

### 5.1 注入原版类的 patch 复核

当前有 10 个 mixin 注入 8 个唯一原版类：

| 原版类 | 26.2.x patch | 结论 |
|---|---|---|
| `ClipContext` | 无 | accessor 不受 NeoForge patch 改写 |
| `SoundBufferLibrary` | 无 | `getCompleteBuffer`/`clear`/lambda 走同一 26.2 原版；仍需编译 jar 确认 lambda descriptor |
| `AvatarRenderer` | 有 | patch 改 arm pose、submit 事件、spyglass 与 hand render；未改 `setupRotations` |
| `LocalPlayer` | 有 | patch 改输入事件、飞行/游泳/声音等；`tick` TAIL 保留，`aiStep` 的 sprint setter 未被 patch 移除 |
| `MouseHandler` | 有 | patch 修改鼠标事件及 `turnPlayer` 灵敏度来源；`handleAccumulatedMovement -> turnPlayer(D)V` 调用与 `turnPlayer` RETURN 仍在 |
| `LivingEntity` | 有 | 普通字段扩展 mixin无注点；`applyItemBlocking` 调用点被改为六参，见 §5.2 |
| `EnderMan` | 有 | patch 改 `setTarget`、凝视和搬运 AI，未改 `hurtServer` |
| `EnchantmentHelper` | 有 | patch 改物品附魔读取/迭代/loot 等，未改 `getDamageProtection` |

### 5.2 `BlocksAttacks#hurtBlockingItem`（高风险项）

NeoForge 26.2.x：

- `patches/net/minecraft/world/item/component/BlocksAttacks.java.patch` 新增
  `BlocksAttacks#hurtBlockingItem(Level, ItemStack, LivingEntity, InteractionHand, float, int): void`；
- `patches/net/minecraft/world/entity/LivingEntity.java.patch` 把
  `LivingEntity#applyItemBlocking(ServerLevel, DamageSource, float): float` 的调用改成六参，最后一参为
  `ShieldBlockEvent#shieldDamage()`。

因此保留两个 wrap：

1. 六参 `(Level, ItemStack, LivingEntity, InteractionHand, float, int)` 为 NeoForge 主路径，把规则计算的
   耐久写回 `fixedDamage` 后调用原方法；
2. 五参 vanilla 路径 `require = 0`，仅作未打补丁构建的 fallback；
3. 两者都没命中时，RETURN 注入在首次解析到盾牌规则后输出明确警告。

这与 26.1.2 实机崩溃的修复方向一致，并由 26.2.x patch 再次确认；运行时是否应用成功仍待客户端日志。

## 6. 可选兼容证据

| 兼容项 | 静态证据 | 未完成 |
|---|---|---|
| Sound Physics Remastered 1.5.1+26.2 | commit `ca0f8fe2b17c3c38d1bc231581f37fd1be3531a8`；`SoundPhysics#evaluateEnvironment(int,double,double,double,SoundSource,Identifier,boolean): Vec3` 内仍调用 `calculateOcclusion(...)`、`ReflectedAudio#getSharedAirspaces(): int` 和 10-float `setEnvironment(...)` | 发布 jar、mixin apply、airspace 实测 |
| First Aid New NeoForge 26.2 | commit `8fc4dd579c02ba3d3b29b96b2d22a2e12c48a5c5` 的 `neoforge26.2` 模块；`EventHandler#handleCustomPlayerDamage(Player,DamageSource,float): boolean` 内有 `DamageSource#is(TagKey)`；`#recordProjectileHit(Player,Entity,Vec3): void` 存在 | 1.3.0-patched 发布 jar 与 gameplay；1.2.8 虽存在，但 1.3.x shader override 未对它核验，故不在声明范围 |
| Pillager’s Gun 3.3.5 NeoForge 26.2 | 发行列表存在；公开仓库 commit `6fb06402641b84726943360503613393605f3f63` 有 `PillagersGunConfig#values(): Values` 与 `Values#friendlyFire(): boolean` | 仓库版本元数据仍是旧线，故不能当 3.3.5 jar 证据；真 jar 与 gameplay 待核 |
| LRTactical | Renovated 26.2 内置 `me.xjqsh.lrtactical.*`，目标类/方法源树命中 | melee 实测 |

LSO / Valkyrien Skies / MTS 本轮未得到可验证 NeoForge 26.2 目标，因此明确列“暂不支持”，没有空开关。

## 7. 依赖与摘要

目标本地文件：TaCZ R1、YACL 3.9.5+26.2 NeoForge（配置类型继承 YACL，双端必需）、SPR 1.5.1+26.2 NeoForge、
First Aid 1.3.0-patched（可选）、Pillager's Gun 3.3.5（可选）、commons-math3 3.6.1。
构建按规范化文件名匹配，并强制目标 jar 含 `26.2`、排除含 `fabric` 的文件。

TaCZ release API 公布 SHA-256：
`26003ae476d4d5dc4f85a82998b9622f59aca71d73757c8136184890440ed6ec`。
其余新二进制按工单要求暂记 `pending`；发布前必须本地下载、复算并写回。

## 8. 本轮实际执行结果

```text
python3 -m py_compile scripts/*.py              PASS
python3 scripts/check_mod_icon.py               PASS
python3 scripts/check_release_consistency.py    PASS（缺失 libs 只输出 NOTE）
python3 scripts/download_dependencies.py --check-only
                                                FAIL（缺 TaCZ R1 jar，正确阻断）
python3 scripts/audit_port.py                    1 error / 63 warnings（缺 TaCZ jar，预期未通过）
java -version                                    command not found
./gradlew --version                              无 JAVA_HOME / java，未运行
./gradlew test                                   未运行
./gradlew build                                  未运行
客户端 / 专服 / 游戏内                           未运行
```

发布者必须按 `BUILD.md` 从测试、构建、客户端、专服到游戏内场景顺序补齐证据，不能跳级声明。

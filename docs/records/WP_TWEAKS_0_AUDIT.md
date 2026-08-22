# WP-TWEAKS-0 审计报告：TaCZTweaks_Unofficial → NeoForge 1.21.11 可移植性

> 2026-08-22 联网 + 本地逐文件核实。基线：姊妹 `1.21.11` 分支冻结 commit `9d7d7b0`。
> 证据层级：① maven/Modrinth/官方映射实测；② javap（1.21.11 named jar =
> neoformruntime rename 产物、SPR 1.5.1 neoforge jar）；③ 源级比对
> （本线 release 源码 zip 805 文件 vs tweaks 85 文件）。
> 结论评级：**GO（有条件）**。条件清单见 §F。

## 一、体量与结构事实

| 项 | 值 |
|---|---|
| tweaks 源码 | 85 个 java（88 文件含测试）；76 个 mixin/accessor；纯 Java |
| @Mixin 目标 | 44 个 tacz 内部类 + 1 个 LR 接口 + 8 个原版类 + 2 个第三方类，无 targets= 字符串形式 |
| Fabric 耦合 | 12 个 import，集中在 4 个文件（§B） |
| mixin 配置 | 单一 `tacztweaks.mixins.json` |

## 二、A. mixin 目标全量对账

### A1. 内部目标（tacz + LR）源级存在性

**45 个目标在我们的 NeoForge 树中全部存在，缺失 0。** 全清单（tacz-internal 44）：

`ObjectAnimationSoundChannel / GunAnimationStateContext / CameraSetupEvent / RenderCrosshairEvent /
TickAnimationEvent / LocalPlayerBolt / LocalPlayerCrawl / LocalPlayerDataHolder / LocalPlayerDraw /
LocalPlayerFireSelect / LocalPlayerInspect / LocalPlayerReload / LocalPlayerShoot / LocalPlayerSprint /
GunSmithTableScreen / GunPackList / RefitKey / EntityBulletRenderer / GunSoundInstance / SoundPlayManager /
EntityKineticBullet / LivingEntityAim / LivingEntityAmmoCheck / LivingEntityCrawl / LivingEntityReload /
LivingEntityShoot / LivingEntitySprint / ModernKineticGunItem / ModernKineticGunScriptAPI / AdsModifier /
AmmoSpeedModifier / ArmorIgnoreModifier / DamageModifier / HeadShotModifier / InaccuracyModifier /
RecoilModifier / RpmModifier / Modifier / GunRecoil / InaccuracyType / AttachmentDataUtils / EntityUtil /
BlockRayTrace / ProjectileExplosion` + LR `me.xjqsh.lrtactical.api.item.IMeleeWeapon`。

### A2. 11 处源级"未命中"注点的逐一结论（全部可解，无一阻塞）

| 注点 | 结论 |
|---|---|
| `lambda$shouldSlide$16`（GunAnimationStateContext） | 姊妹注释自证 lambda 编号随重编译漂移（$18→$0→$16）。**移植动作项**：编译后 javap 产物定位真实编号 |
| `lambda$tickAnimation$0` / `lambda$inspect$0` / `lambda$initCache$0` | 同上，编译后定位 |
| `method_5773` ×2（EntityKineticBullet） | **Fabric intermediary 名**（Fabric refmap 运行期译成混淆名）。官方映射 client.txt 实证 1.21.11 `Entity.tick() -> g`；本线源码实证 `EntityKineticBullet#tick()` 存在。**NeoForge 端写 `"tick"`**（mod 不 remap） |
| `GunSoundInstance <init>(…ZZ)V` 9 参全描述符 | 本线构造器 `(SoundEvent,SoundSource,float,float,Entity,int,Identifier,boolean,boolean)` 逐参匹配 ✓ |
| `EntityKineticBullet <init>` 全描述符 ×3 | 构造器签名一致 ✓（tacz 内部类跨加载器同形） |
| `SoundPlayManager#playCompositeAnimationContainerSound` / `playHeadHitSound` / `playFleshHitSound` / `playKillSound` | 本线 SoundPlayManager 方法面与姊妹 tacz 1.21.11 一致（同类同名）✓ |

### A3. 原版目标（8 个）+ 全部 INVOKE 目标 —— javap 实证 **0 问题**

MouseHandler / LocalPlayer / AvatarRenderer / SoundBufferLibrary / LivingEntity / EnderMan /
EnchantmentHelper / ClipContext 全部存在；方法注点（handleAccumulatedMovement、turnPlayer、
lambda$getCompleteBuffer$1、applyItemBlocking、hurtServer、getDamageProtection 等）全部命中；
40 个 INVOKE 目标中原版部分（SoundBuffer、PoseStack、DamageSource、BlocksAttacks、
AvatarRenderState、Holder$Reference 等）全部命中。

### A4. 第三方目标

| 目标 | 结论 |
|---|---|
| `com.sonicether.soundphysics.SoundPhysics` | **SPR neoforge-1.21.11-1.5.1 存在**（Modrinth ① 下载 jar ② 实证 SoundPhysics/ReflectedAudio 类在、`setEnvironment(IFFFFFFFFFF)V` 在）。`calculateOcclusion` 未在公共面——**移植动作项**：该 compat mixin 适配前对 1.5.1 私有面细化 |
| `ichttt.mods.firstaid.common.EventHandler` | **First Aid 官方最新止步 1.20.1 Forge（2024-07 停更），无 1.21.11 构建**（CurseForge ①）。结论：保留代码 + require=0 静默跳过 + COMPATIBILITY 标注不可用 |
| Pillager's Gun（com.tacz.guns.util.EntityUtil + PillagersGunCompat 门控） | 实际目标是 tacz 内部类 ✓；原 mod 1.21.11 仅 Fabric（姊妹 COMPATIBILITY ①）→ 保留代码 + NeoForge 运行期不触发 |

## 三、B. Fabric 依赖 → NeoForge 等价映射（12 import / 4 文件，全部有成熟习语）

| 文件 | Fabric API | NeoForge 等价 |
|---|---|---|
| TaCZTweaks | ModInitializer | `@Mod` + 构造器（本线 GunMod 模式） |
| | ServerTickEvents.END_SERVER_TICK | `TickEvent.ServerTickEvent.Post` |
| | ServerLifecycleEvents.SERVER_STOPPED | `ServerStoppedEvent` |
| | ServerLifecycleEvents.END_DATA_PACK_RELOAD | `AddServerReloadListenersEvent`（本线 WP④ 已用） |
| | PlayerBlockBreakEvents | `BlockEvent.BreakEvent` |
| | ServerPlayConnectionEvents | `PlayerEvent.PlayerLoggedIn/OutEvent` |
| | FabricLoader（mod 探测） | `ModList.get().isLoaded(...)`（本线 IrisCompat 模式） |
| TaCZTweaksClient | ClientModInitializer | 客户端事件面（本线 GunModClient/ClientGameEvents 模式） |
| | KeyBindingHelper.registerKeyBinding ×3 | `KeyMapping` + `RegisterKeyMappingsEvent` |
| | ClientTickEvents.END_CLIENT_TICK | `ClientTickEvent.Post` |
| | ClientPlayConnectionEvents | `ClientPlayerNetworkEvent.LoggingIn` |
| FirstAidCompat / PillagersGunCompat | FabricLoader | `ModList`（同上） |

网络层：tweaks 的 payload/同步（含姊妹补强的配置同步 1 MiB 上限、C2S 服务端复验）→
`PayloadRegistrar` 平移，沿用本线 ServerMessageXxx 纪律（EMPTY 检查等）。

## 四、C. 非 mixin 文件改写清单（9 文件）

| 文件 | 规模 | 要点 |
|---|---|---|
| TaCZTweaks.java | 中 | 入口注解 + 6 类事件注册改写（§B） |
| TaCZTweaksClient.java | 中 | 入口 + 按键注册 + tick + YACL 屏入口（ModMenu 不存在 → 用 YACL NeoForge API 或本线 tacz 的 NeoForge 原生 ConfigurationScreen 惯例） |
| CrawlPitchController.java | 小 | 纯逻辑，预期零改 |
| ReduceSensitivityKey / TiltGunKey / UnloadKey | 小 | 键位类本身纯逻辑；注册面在 TaCZTweaksClient 改 |
| FirstAidCompat / PillagersGunCompat | 小 | 仅 FabricLoader→ModList |
| ModMenuApiImpl.java | 删/替换 | ModMenu 无 NeoForge 构建；YACL 屏换 NeoForge 注册路径 |

## 五、D. 依赖钉版结论（① 实证）

| 依赖 | 结论 |
|---|---|
| tacz | modId 同为 `tacz`；依赖谓词改 `>=1.1.8` 本线构建 |
| YACL 3.8.2 | Modrinth 有 `3.8.2+1.21.11-neoforge`；isxander maven 只有旧线（3.3.x/1.20.4）→ **libs/ escape hatch 落盘 jar**（本线 PAL/Controllable/SSR 同款模式） |
| MixinExtras 0.5.4 | 本线 tacz 已用 `com.llamalad7.mixinextras.*` 且 build.gradle 无显式声明——NeoForge 21.11 运行期内置 + MDG 编译期可用，**无需新增依赖** |
| SPR | `neoforge-1.21.11-1.5.1` 存在（类级已证，见 A4） |
| First Aid | 无 1.21.11 构建（停更）→ compat 代码保留 + 标注 |
| Pillager's Gun | 1.21.11 仅 Fabric → 同上门径 |

## 六、E. 数据/资源面

`assets/tacztweaks/`（textures/mob_effect、sounds、lang）纯资源平移；
data-driven 配置 schema 随源码整体平移。无 Fabric 专属路径引用。

## 七、F. 结论评级：GO（有条件）

**条件（全部为移植期动作项，无一为阻塞项）：**

1. **lambda 注点 ×4**：编译后按产物 javap 重定位编号（姊妹自证漂移史）；
2. **`method_5773` ×2 → `tick`**：NeoForge 官方名化（已实证映射）；
3. **SPR compat**：`calculateOcclusion` 对 1.5.1 私有面细化后再定兼容级别；
4. **YACL 屏入口**：换 NeoForge 注册路径（无 ModMenu）；
5. 其余 76 个 mixin 目标源级/字节码级已证一致，预期平移后编译收敛面小。

**风险提示（持续维护面）：**

- 76 个 mixin 打在 tacz 内部类上，本线 tacz 后续改渲染/scope/网络面时连带检查 tweaks；
- 姊妹 Beta-1 的未实测项（SPR 实机、多人矩阵）在 NeoForge 侧同样待验收，不得宣称已覆盖；
- lambda 漂移意味着 tweaks 与 tacz 的重编译都会牵动注点——把「编译后 javap 定位」写成项目惯例。

**建议工作包**（沿用 WP-11211 节奏）：
WP-TWEAKS-1 骨架（gradle/libs/YACL 落盘/入口改写）→ WP-TWEAKS-2 编译收敛
（含 lambda 重定位、method_5773 官方名化、SPR 细化）→ WP-TWEAKS-3 专服+兼容矩阵
（tacz 联调、SPR/无依赖三态）→ WP-TWEAKS-4 发布。

---

*审计快照：2026-08-22。证据文件：/tmp/tweaks-audit.json（机器可读全量）。
链接与版本以执行当日实况为准。*

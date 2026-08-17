# 移植笔记 — TaCZ Tweaks → Fabric 26.2

记录从 Forge 1.20.1 原版（MUKSC/TaCZTweaks v2.14.2）移植到
`TaCZ_Refabricated_Unofficial`（26.2 主分支）的要点，供后续维护者参考。

---

## 1. 目标仓库关键事实

- 26.2 分支：**未混淆**（直接用 Mojang 类名，无 mappings），Java 25，Fabric Loom 1.17。
- `com.tacz.guns.*` 包结构**完整保留**（853 个类），移植胶水在 `cn.sh1rocu.tacz`。
- 依赖：Fabric API + Forge Config API Port（硬依赖）。
- 自带 `cn.sh1rocu.tacz.api.event.*` 事件总线；`com.tacz.guns.api.event.common.*`
  事件重实现为 **Fabric 原生 Event API**（如 `GunShootEvent.CALLBACK.register(...)`）。
- LRTactical 部分内置（`provides: ["lrtactical"]`）。

## 2. Minecraft API 迁移（1.20.1 → 26.2，影响所有打 net.minecraft.* 的 mixin）

| 旧 | 新 |
|---|---|
| `ResourceLocation` | `Identifier`（`net.minecraft.resources.Identifier`） |
| `GuiGraphics` | `GuiGraphicsExtractor`；`drawString` → `text` |
| 颜色 | **必须带 alpha**，否则 `text()` 直接不画 |
| `PlayerRenderer` | `AvatarRenderer` |
| `singleplayerServer` 字段 | `getSingleplayerServer()` |
| `hasPermissions(int)` | `permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)` |
| `writeResourceLocation/readResourceLocation` | `writeIdentifier/readIdentifier` |
| `isUnderWater()` | 移到 `Entity` 上（继承可用） |
| 按键 `KeyMapping(name, type, key, "category")` | category 改为 `KeyMapping.Category` 记录，需 `Category.register(Identifier)` |
| `KeyBindingHelper` | `KeyMappingHelper`（包 `net.fabricmc.fabric.api.client.keymapping.v1`） |
| 数据包目录 | 单数化：`recipe/` `loot_table/` `tags/block/` 等 |
| 配方 | 全面 codec 化；`result.item`→`result.id`，`nbt`→`components` |

## 3. Forge → Fabric 语义替换

| Forge | Fabric |
|---|---|
| `net.minecraftforge.items.ItemHandlerHelper` | `cn.sh1rocu.tacz.util.itemhandler.ItemHandlerHelper`（目标端自带垫片） |
| Forge 事件（RenderCrosshairEvent 等） | 目标端 `api.event.*` / Fabric API 事件 / 直接 mixin |
| `SimpleChannel` | `CustomPacketPayload` + `PayloadTypeRegistry` + `ServerPlayNetworking` |
| access transformer | access widener |
| `mods.toml` | `fabric.mod.json` |
| Kotlin for Forge | Fabric Language Kotlin |
| MixinSquared TargetHandler | 优先直接 mixin 目标类（如 `LocalPlayerCrawl`），避免 MixinSquared |

## 4. 网络层范式（26.2）

```java
class X implements CustomPacketPayload {
    static final Type<X> TYPE = new Type<>(Identifier.fromNamespaceAndPath("tacztweaks", "id"));
    static final StreamCodec<FriendlyByteBuf, X> CODEC = StreamCodec.ofMember(X::write, X::new);
    // 注册：PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
    //       ServerPlayNetworking.registerGlobalReceiver(TYPE, (msg, ctx) -> msg.handle(ctx.player(), ctx.responseSender()));
    // 客户端 ctx：ctx.client() / ctx.player() / ctx.responseSender()
}
```

Kotlin 注意：`StreamCodec.of(...)` 的 lambda 类型推断会失败，改用
`StreamCodec.ofMember(X::write, { buf -> X(buf) })`。

## 5. 与目标端实现差异（重要）

- **`AbstractGunItem.dropAllAmmo` 已被重写**：不再是 Forge 版的 `lambda$dropAllAmmo$2/3`
  结构，原版卸弹 mixin 无法照搬，需要按新方法体重写。
  - 新方法体已自带：`useInventoryAmmo` 早退、创造模式回填、FUEL 清空、虚拟备弹返还。
  - **生存模式退弹已可直接工作**：本移植的卸弹处理器直接调 `dropAllAmmo`。
- **`LocalPlayerCrawl` / `LocalPlayerMixin`**：字段名、handler 名（`crawl`/`tickCrawl`）
  与 Forge 版一致；本移植直接 mixin `LocalPlayerCrawl` 实现禁用，无需 MixinSquared。
- TaCZ 自身的 `LivingEntityMixin` 仍用 `@Unique ShooterDataHolder tacz$data` 字段，
  原版的字段 shadow 技巧理论可迁移，但**未实测**（本移植已改用主手物品，避免依赖它）。

## 6. 混入（mixin）移植清单

原版 95 个 mixin，按分组：

| 分组 | 数量 | 状态 |
|---|---|---|
| accessor | 5 | `ModifierAccessor` ✅、`LocalPlayerShootAccessor` ✅；其余按需 |
| crawl | 4 | `LocalPlayerCrawlMixin`（禁用）✅；俯仰角/视觉 ⏳ |
| gun/movement | ~13 | ✅ 已移植（见下） |
| gun（bolt/draw/reload/inspect/dataholder 等） | ~9 | ✅ 已移植 |
| gun/unload | 2 | ⏳（目标端 dropAllAmmo 已变，卸弹改走主手 `dropAllAmmo`） |
| modifiers | ~14 | ⏳ |
| tweaks | ~14 | ⏳ |
| features/bullet_* | ~8 | ⏳（数据驱动系统，核心范围外） |
| compat/*（FirstAid/LSO/MTS/VS/SPR/PillagersGun） | ~25 | ❌ 砍掉 |

### 6.1 已移植的 gun 组（18 个 mixin）

| Mixin | 功能 | 相对原版改动 |
|---|---|---|
| `LivingEntitySprintMixin` | 换弹奔跑 | 无 |
| `LivingEntityAimMixin` | 换弹奔跑（aim tick） | 无 |
| `LivingEntityReloadMixin` | 射击换弹 | `lambda$reload$0` → `reloadWithIndex` |
| `LivingEntityShootMixin` | 跑打 | 5 参 `shoot` → `shootInternal` |
| `LocalPlayerSprintMixin` | 换弹奔跑 + 跑打 | 无 |
| `LocalPlayerShootMixin` | 跑打（客户端） | 无 |
| `LocalPlayerFireSelectMixin` | 射击中切换开火模式 | 无 |
| `LocalPlayerReloadMixin`(movement) | 射击换弹（客户端） | `lambda$reload$2` → `reloadWithDisplay`；`doReload` 部分跳过 |
| `TickAnimationEventMixin` | 跑打动画 | 无 |
| `LocalPlayerBoltMixin` | 手动拉栓 + 换弹前先拉栓 | 无 |
| `LocalPlayerDrawMixin` | 切枪重置拉栓标记 | 无 |
| `LocalPlayerDataHolderMixin` | 拉栓标记存储 | 无 |
| `LocalPlayerReloadMixin`(gun) | 换弹前先拉栓 | `lambda$reload$2` → `reloadWithDisplay` |
| `LocalPlayerInspectMixin` | 再次检视取消检视 | 无（lambda 参数序一致） |
| `ModernKineticGunScriptAPIMixin` | 换弹丢弃弹匣 | `gunId` 类型 ResourceLocation→Identifier |
| `GunAnimationStateContextMixin` | 持枪倾斜 | `lambda$shouldSlide$18` → `lambda$shouldSlide$0` |
| `EntityBulletRendererMixin` | 禁用子弹剔除 | 无 |
| `LivingEntityMixin`(vanilla) | 滑铲状态接口 | 无 |

### 6.2 目标端重构导致的 lambda 名变化（本移植最重要的坑）

目标仓库（TaCZ_Refabricated_Unofficial）**刻意把编译器生成的 `lambda$xxx$N` 具名成了稳定 hook**，
以避免扩展依赖 javac 合成名。移植时必须逐一对号：

| Forge 1.20.1（原版 mixin 目标） | 26.2 Refabricated（新目标） |
|---|---|
| `LivingEntityReload.lambda$reload$0` | `LivingEntityReload.reloadWithIndex` |
| `LivingEntityShoot.shoot(Supplier,Supplier,J,F,Z)` | `LivingEntityShoot.shootInternal(Supplier,Supplier,J,F,Z)` |
| `LocalPlayerReload.lambda$reload$2` | `LocalPlayerReload.reloadWithDisplay` |
| `LocalPlayerReload.doReload` | `LocalPlayerReload.triggerClientReloadAnimation`（语义不同，movement 组已跳过该注入） |
| `GunAnimationStateContext.lambda$shouldSlide$18` | `GunAnimationStateContext.lambda$shouldSlide$0`（仅编号变了） |
| `LocalPlayerInspect.lambda$inspect$0` | 同名，参数序一致，可直接用 |
| `TickAnimationEvent.lambda$tickAnimation$0` | 同名，可直接用 |

### 6.3 因 26.2 已不存在的 vanilla 方法而跳过的

- `LocalPlayer.canStartSprinting()` 在 26.2 已不存在 → 跳过 vanilla `LocalPlayerMixin`；
  「倾斜键取消冲刺」改由 `TiltGunKey.onClientTick()` 每 tick 处理。
- `MouseHandlerMixin`（降低灵敏度）原版走 MixinSquared TargetHandler，暂未移植。

## 7. 第二批移植记录（modifiers / crawl pitch / tweaks / 无尽弹药 / zh_cn）

### 7.1 平衡修饰器（modifiers 组，12 mixin）

目标端仍使用 `net.minecraftforge.common.ForgeConfigSpec$DoubleValue`（依赖 Forge Config API Port），
`SyncConfig.DAMAGE_BASE_MULTIPLIER` 等字段名不变；但多处代码从 Forge 版的 double 改为 float，
且被重构成具名 hook。因此**不用原版的 `@Expression` 字节码匹配**，全部改写为编译期可验证的
结构化注入：

| Mixin | 注入点 |
|---|---|
| `DamageModifierMixin` | `ExtraDamage$DistanceDamagePair.<init>(FF)V` 的 index=1（@ModifyArg） |
| `HeadshotModifierMixin` | `CacheValue.<init>(Object)V` index=0 |
| `ArmorIgnoreModifierMixin` | 同上 |
| `AdsModifierMixin`（aimTime） | 同上 |
| `AmmoSpeedModifierMixin`（speed） | 同上 |
| `RecoilModifierMixin` | `getMaxInGunRecoilKeyFrame` ordinal 0/1 |
| `RPMModifierMixin` | `getRoundsPerMinute(FireMode)I` |
| `InaccuracyModifierMixin` | `lambda$initCache$0` 的 `getInaccuracy(InaccuracyType;F)F` |
| `AttachmentDataUtilsMixin` | 3 个 get 方法的 `eval(List;D)D` index=1 |
| `EntityKineticBulletMixin` | `<init>` 的 `getGravity()/getFriction()`；`onHitEntity` 的 `getDamage` ordinal=1、`headShot` 字段 |
| `GunRecoilMixin` | `getSplineFunction` 的 `? * (double) modifier`（结构与 Forge 版一致） |
| `CameraSetupEventMixin` | `initialCameraRecoil` 的 `getCrawlRecoilMultiplier`（@WrapOperation 设 crawl 标志）、`getClientAimingProgress`、`genPitch/YawSplineFunction` |

客户端显示部分（`getPropertyDiagramsData` / `buildNormal` / `buildAim` / `ClientGunTooltip`）
一律跳过——它们只影响改装界面图表，且依赖脆弱的 `@Local LocalFloatRef`。

### 7.2 匍匐俯仰角 / 降低灵敏度（`gun.MouseHandlerMixin`）

目标端 TaCZ 的 `MouseHandlerMixin#reduceSensitivity` 仍 wrap `LocalPlayer#turn(DD)`，
`getCrawlPitch` 里的常量 `45 / -30` 也与 Forge 版一致。为**避免引入 MixinSquared**，
改为直接 `@Mixin(MouseHandler.class, priority = 1500)` 再 wrap 同一个 `turn` 调用：
- 降敏键：把 yaw/pitch 乘配置倍率（与 TaCZ 的 wrap 链式执行，乘法可交换，顺序无关）；
- 俯仰角：在 TaCZ 的 `[-30, 45]` 硬限制之后再收紧到 `[pitchLowerLimit, pitchUpperLimit]`
  （默认 `[-10, 25]`，本就是收紧；若用户放宽到超过 30/45 则仍被 TaCZ 硬限制卡住，
  **动态俯仰模式暂未实现**）。

### 7.3 音效开关 / 命中标记（tweaks 组）

- `SoundPlayManagerMixin`：`playHeadHitSound/playFleshHitSound/playKillSound` 的
  `WrapWithCondition`（抑制开关）+ `getSounds(String)` 的 `@ModifyExpressionValue`（强制默认音，
  返回值类型 `ResourceLocation`→`Identifier`）。
- `RenderCrosshairEventMixin`：`renderHitMarker(GuiGraphicsExtractor, Window)`（`GuiGraphics`→`GuiGraphicsExtractor`）。

### 7.4 第三人称修复

**无需移植**。目标端 TaCZ 的 `ItemInHandLayerMixin` 已按 26.2 的 extract→submit 管线重写，
`submitTail` 内已调用 `HumanoidOffhandRender.renderGun`（并修复了左利手与 `isSelf` 问题）。
`thirdPersonGunRenderingFix` 配置项保留但无对应 mixin。

### 7.5 无尽弹药

- `EndlessAmmoStatusEffect`（Kotlin）：26.2 的 `MobEffect(category, color)`；
- `ModStatusEffects`：`Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, ...)` 返回 `Holder`；
- `LivingEntityAmmoCheckMixin`：`consumesAmmoOrNot` HEAD cancel，
  `player.hasEffect(Holder<MobEffect>)`（26.2 签名）。

## 7.6 崩溃修复记录（2026-08-17，重要）

用户实机报"进存档显示资源包损坏"，日志根因：

```
Mixin apply for mod tacztweaks failed ... modifiers.GunRecoilMixin
InvalidMixinException: In order to use @Expression, Mixin Config needs to declare
a reliance on MixinExtras >=0.5.0-beta.1
Failed to load level data or datapacks, can't proceed with server load
```

`GunRecoilMixin` 用了 `@Expression`（MixinExtras 的字节码匹配器），但：
1. mixins.json 未声明 `"mixinextras": {"minVersion": ...}`；
2. 内嵌的 MixinExtras 是 0.4.1，@Expression 需要 >=0.5.0-beta.1。

修复（双保险）：
1. `GunRecoilMixin` 改为 wrap `SplineInterpolator#interpolate`，在插值前改写 values 数组，
   彻底移除 `@Expression`/`@Definition`（编译期可验证）；
2. mixins.json 补上 `"mixinextras": {"minVersion": "0.5.0"}`。

教训：**不要用 @Expression**——它运行时才验证，版本/字节码对不上就崩进不了存档。

## 7.7 第三批移植记录（endermenEvadeBullets / disableRefitOnAdventure / rps / alwaysFilterByHand / 共享枪声）

| Mixin | 功能 | 移植要点 |
|---|---|---|
| `tweaks.EnderManMixin` | 末影人躲避子弹 | `hurt` → 26.2 的 `hurtServer(ServerLevel, DamageSource, float)`；第一个 `DamageSource.is(TagKey)` 即 `IS_PROJECTILE` 检查（已核字节码） |
| `tweaks.RefitKeyMixin` | 冒险模式禁用改装 | `onRefitPress()` → `onRefitPress(InputEvent.Key)`；`getPlayerMode()/GameType.ADVENTURE` 26.2 仍存在 |
| `tweaks.RPMModifierMixin` | RPS 显示 | `getPropertyDiagramsData` 常量 `1200.0` 出现两处（ordinal 0/1 各改一处）；`getCache` 改返回值（无 @Expression） |
| `tweaks.GunPackListMixin` | 始终按手持过滤 | 只做 `isByHandSelected` 的 `@ModifyReturnValue`；构造函数 `addEntry` 结构变化大，跳过隐藏复选框 |
| `tweaks.SoundPlayManagerMixin`（广播） | 共享枪声 | 10 个 `@WrapOperation` wrap 各 play 方法的 5 参 `playClientSound`；`ClientMessageBroadcastSound` 主构造改为 public 供 Java 调用 |

## 7.8 审计修复记录（2026-08-17 第二批，用户实测反馈）

### 末影人躲避子弹"没生效"的根因

`endermenEvadeBullets` 在原版是**两个 mixin 配合**，上一轮只移植了半个：

1. `tweaks.EntityKineticBulletMixin`（本次补上）：TaCZ 的 `tacz:use_magic_damage_on` tag
   包含 `minecraft:enderman`，所以子弹命中末影人时走 `createDamageSources` 的
   `indirectMagic` 分支（magic 伤害，`DamageType=minecraft:magic`），永远不匹配
   `tacz:bullets` tag。此 mixin 让末影人改用 bullet 伤害。
2. `tweaks.EnderManMixin`（上轮已有）：让 bullet 伤害触发末影人 `hurtServer` 里的
   projectile 躲避分支（`source.is(IS_PROJECTILE)` → `|| source.is(BULLETS_TAG)`）。

实现：`@WrapOperation` over `Holder$Reference#is(TagKey)`，按 `USE_MAGIC_DAMAGE_ON`
这个具体 tag 过滤（`createDamageSources` 里 3 次调用：PRETEND_MELEE / USE_MAGIC / USE_VOID），
避免 ordinal 依赖。

### 26.2 重命名坑：EntityType → EntityTypes

`EntityType.ENDERMAN` 在 26.2 已移到 `EntityTypes.ENDERMAN`（复数类，与
`GuiGraphics→GuiGraphicsExtractor`、`ResourceLocation→Identifier` 同一批重命名）。
编译期 `cannot find symbol` 暴露。全局 grep 确认仅此一处。

### DamageModifier 两处构造（多数枪伤害倍率失效）

`DamageModifier#initCache` 里有**两处** `DistanceDamagePair.<init>(FF)V`（字节码 offset
152 距离衰减循环 / 197 普通伤害分支）。原 `@ModifyArg` 默认 ordinal=0 只改第一处，
导致**没有距离衰减配置的枪（大多数）伤害倍率不生效**。已补 ordinal=1。

### playReloadSound / playInspectSound 两处 playClientSound（共享枪声漏广播）

这两个方法各有 2 次 5 参 `playClientSound` 调用（空仓/正常分支）。原无 ordinal 的
`@WrapOperation` 只广播第一处。已为两方法补 ordinal=0/1，并抽公共 `tacztweaks$broadcastSound`
helper。

### 审计结论（其余 mixin 的 ordinal 均核对通过）

- `EntityKineticBullet#onHitEntity` 的 `getDamage` 2 次 → `modifiers` 组 ordinal=1 正确；
- `RecoilModifier#initCache` 的 `getMaxInGunRecoilKeyFrame` 2 次 → ordinal 0/1 正确；
- `HeadShot/ArmorIgnore/Ads/AmmoSpeed` 的 `CacheValue.<init>` 各 1 次 → 正确；
- `EnderMan#hurtServer` 的 `DamageSource.is(TagKey)` 1 次 → 正确。

## 7.9 第四批移植记录（动态俯仰角 / betterInaccuracy / betterGunTilt / 强制第一人称射击音 / 完整卸弹）

### 关键 API 修正（前几轮的"不可行"判断被推翻）

1. **卸弹枪膛子弹**：之前误判"R2 无 hasBulletInBarrel"。实际上 `IGun` 接口声明了
   `hasBulletInBarrel/setBulletInBarrel`，default 实现由 `GunItemDataAccessor`（IGun 子接口）提供。
   因此卸弹改为**服务端自实现退弹**（不 mixin `dropAllAmmo` 的 lambda）：
   - 弹匣：按 `stackSize` 拆分成弹药物品 give 给玩家（FUEL 类型只清空不返还；dummy 备弹走原 `dropAllAmmo`）；
   - 创造模式：与生存一致（自实现，天然绕过 `dropAllAmmo` 的 creative 回填）；
   - 枪膛：`unloadBulletInBarrel` 时 `setBulletInBarrel(false)` + 额外 give 一颗。

2. **forceFirstPersonShootingSound**：目标端把 `lambda$shootOnce$2` 重命名为具名 hook
   `runShootCycle`；其内 `SoundManager.SILENCE_3P_SOUND` / `SHOOT_3P_SOUND` 各一处 GETSTATIC，
   直接 `@ModifyExpressionValue(FIELD GETSTATIC)` 即可（无 @Expression）。

3. **betterInaccuracy**：`shootOnce` 里 `Map.get(Object)` 仅一处（扩散 map），
   `@WrapOperation` 无歧义；`InaccuracyType.isMove` 是私有静态方法，用 `@Invoker` accessor
   访问（原版同款 `InaccuracyTypeAccessor`）。

4. **betterGunTilt**：补齐两块——vanilla `LocalPlayerMixin`（每 tick 检测 `shouldSlide`
   并发 `ClientMessagePlayerShouldSlide`，字段实现复用 `LivingEntityMixin`）；`InaccuracyTypeMixin`
   用 `@WrapOperation` wrap `getInaccuracyType` 里**第二次** `LivingEntity.getPose()`
   （ordinal=1，即 `== Pose.CROUCHING` 那次），`shouldSlide` 时返回 `CROUCHING`。

5. **动态俯仰角**：26.2 里 `Level.clip` 移到了 `BlockGetter.clip(ClipContext)`（default 方法，
   Level 继承）。`LocalPlayer.clientLevel` 字段没了，改用 `player.level()`。

### 已隐藏的配置项（GUI 不再显示，JSON 字段保留）

`betterMonoConversion`、`bulletProtection` 从 `Config.kt` 的 `generateConfigScreen` 中移除。
`thirdPersonGunRenderingFix` 保留显示（目标端已原生修复，开/关均无害）。

## 7.10 俯仰角失效审计与修复（2026-08-17）

用户实测：`pitchUpperLimit` / `pitchLowerLimit` / `dynamicPitchLimit` 均无效果。

审计结论（字节码逐条核对，无法本地跑游戏）：

1. 注入点正确：26.2 `MouseHandler.turnPlayer(double)` 内 `LocalPlayer.turn(DD)V` 仅一处（offset 292）；
   TaCZ 的 `MouseHandlerMixin.reduceSensitivity` 与我的 wrap 是**同一个注入点**，两个 `@WrapOperation`
   嵌套（TaCZ priority 1000 先应用=内层，我 1500 后应用=外层）。
2. 嵌套顺序对"截断增量"语义是脆弱的：TaCZ 在内层会重新计算 `finalPitch = pitch * denominator`
   并做它自己的 45/-30 截断，与外层我的截断产生顺序耦合。
3. 姿势判断正确：TaCZ 26.2 用 `ForcePoseInjection` 把匍匐姿势强制为 `Pose.SWIMMING`，且
   `Entity.isSwimming()` = `getSharedFlag(4)`（水中标志），与 `getPose()==SWIMMING` 解耦，
   所以 `!isSwimming() && getPose()==SWIMMING` 在匍匐时成立。
4. 动态俯仰角有单位 bug：原版 `Math.max(Math.acos(distance), lower)` 把弧度与度混用。

修复（改为**顺序无关**的实现）：
- 把"截断 turn 增量"改为**事后 clamp**：`original.call(...)` 执行后直接
  `player.setXRot(-upper / -lower)`，无论 wrap 嵌套顺序如何，clamp 都在 turn 之后生效；
- `Math.toDegrees(Math.acos(distance))` 修正动态俯仰角的弧度→度单位；
- 用 `if/else if` 避免上仰/下俯两个分支同时触发。

> 若新 jar 仍无效，下一步加日志（输出 playerPitch/upper/lower）定位，请用户反馈 log。

## 7.11 数据驱动子弹交互系统 —— 规模评估与移植计划（未完成）

原版 `MUKSC/TaCZTweaks` 的示例包 + 数据驱动系统（glass 穿透 / 滴水石 / 金属击中音 / 擦弹音等）
规模约 40+ 文件，依赖大量需替换/考古的 API：

| 类别 | 原版依赖 | 26.2/Fabric 现状 |
|---|---|---|
| 注册表 codec | `ForgeRegistries.BLOCKS/ENTITY_TYPES.codec` | `Registry.byNameCodec()` ✓ 可用 |
| reload listener | Forge `AddReloadListenerEvent` | `SimpleJsonResourceReloadListener` 仍在 ✓；注册改 Fabric 事件 |
| 方块/实体/物品谓词 | `BlockPredicate/EntityPredicate/ItemPredicate` | **26.2 已不存在**（predicate 系统 codec 化），需重新设计 |
| 挖掘等级 | `TierSortingRegistry` | Fabric 无，需砍掉 `tier` 类型 |
| 方块破坏事件 | Forge `BlockEvent.BreakEvent` | 改 Fabric `PlayerBlockBreakEvents` / 直接 `destroyBlock` |
| 盾牌格挡事件 | Forge `ShieldBlockEvent` | 需 mixin `LivingEntity.hurtServer` 自行检测 |
| 假玩家 | Forge `FakePlayer` | 需砍掉或自实现 |
| TaCZ 内部 | `EntityKineticBulletAccessor(onHitEntity)`、`BlockRayTrace.rayTraceBlocks` 的 `lambda$rayTraceBlocks$1/2`、`onBulletTick`、`BulletHoleOption`、`TacHitResult` | 需逐一对 26.2 字节码考古（lambda 名几乎肯定变了） |

### 移植计划（分两轮）

- **第 1 轮：data 层**。codec 基础设施（`DispatchCodec`、`strictOptionalFieldOf`、`singleOrListCodec`、
  `ValueRange`）+ 精简 data 定义（`BlockTarget`/`EntityTarget`/`Target`/`BulletInteraction`/`BulletSounds`/
  `BulletParticles`，砍掉 tier/predicate/shield）+ 4 个 manager + 示例包（`tags/blocks`→`tags/block`）。
- **第 2 轮：行为层**。`BlockRayTracer`/`BlockBreakingManager` + `features.*` mixin + `accessor`，
  考古 26.2 的 `EntityKineticBullet` 碰撞流程与 `BlockRayTrace`。

## 8. 已知待办

- [ ] 匍匐动态俯仰角（基于方块碰撞；需 MixinSquared 或改 TaCZ 常量）
- [ ] betterInaccuracy / betterGunTilt / betterMonoConversion / bulletProtection /
      endermenEvadeBullets / disableRefitOnAdventure / alwaysFilterByHand / rps /
      audibleFirstPersonGunSounds / forceFirstPersonShootingSound
- [ ] 卸弹的创造模式 / 枪膛内子弹支持（针对新 `dropAllAmmo` 重写）
- [ ] 示例包、数据驱动的子弹交互系统
- [ ] 运行时实测（沙箱无法启动游戏，所有 mixin 仅通过编译验证）

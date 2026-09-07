# 移植笔记 — TaCZ Tweaks → Fabric 26.2

记录从 Forge 1.20.1 原版（MUKSC/TaCZTweaks v2.14.2）移植到
`TaCZ_Refabricated_Unofficial`（26.2 主分支）的要点，供后续维护者参考。

> 本文件 §1–§7.16 是按时间保留的迁移日志，其中“砍掉/不存在/暂未实现”描述的是**当轮状态**，
> 不是最终能力结论。2026-08-18 复审后的当前事实见 §7.17、§8 和 [`AUDIT.md`](AUDIT.md)。

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

## 7.11 动态俯仰角抖动修复（2026-08-17，用户实测反馈）

用户报：开启动态俯仰角后，面对贴脸障碍物视角疯狂上下抖动。

根因（两处叠加）：
1. **动态下限值算错成巨大正值**：上一轮用 `toDegrees(acos(distance))`，贴脸时
   distance≈0.1 → 下限 ≈ 84°（本应是负值）。clamp 条件 `playerPitch < 84` 几乎恒真，
   于是每帧把视角硬掰到向下 84°。
2. **事后 `setXRot` 硬掰视角**：在 `turn` 之后直接 `setXRot` 会与玩家的鼠标输入和
   TaCZ 内层的 zoom 缩放互相拉扯，产生振荡。

修复：
1. 改为**限制 turn 增量**（与 TaCZ 自身 `getCrawlPitch` 同款手法）：
   - 上仰超过上限：`finalPitch = max(finalPitch, 0)`（只允许向下转）；
   - 下俯超过下限：`finalPitch = min(finalPitch, 0)`（只允许向上转）。
   挡输入不掰视角，天然不会抖动，且与 TaCZ 的 zoom 缩放（内层乘法）顺序无关。
2. 动态下限改为**渐变**：前方 1 格内有墙时，`lower = 配置下限 * clamp(distance, 0, 1)`，
   贴脸 → 0（禁止下俯），1 格 → 恢复配置下限。语义：越贴墙越不能低头穿墙看墙后地面。

符号约定（与 TaCZ 一致）：`playerPitch = -getXRot()`（上仰为正）；turn 增量正 = 下俯。

## 7.12 数据驱动子弹交互系统 —— 规模评估与移植计划（未完成）

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
| 假玩家 | Forge `FakePlayer` | 当时误判需砍；Fabric API 26.2 实际提供 `net.fabricmc.fabric.api.entity.FakePlayer`，本移植最终选择可测试的纯数学路径 |
| TaCZ 内部 | `EntityKineticBulletAccessor(onHitEntity)`、`BlockRayTrace.rayTraceBlocks` 的 `lambda$rayTraceBlocks$1/2`、`onBulletTick`、`BulletHoleOption`、`TacHitResult` | 需逐一对 26.2 字节码考古（lambda 名几乎肯定变了） |

### 移植计划（分两轮）

- **第 1 轮：data 层**。codec 基础设施（`DispatchCodec`、`strictOptionalFieldOf`、`singleOrListCodec`、
  `ValueRange`）+ 精简 data 定义（`BlockTarget`/`EntityTarget`/`Target`/`BulletInteraction`/`BulletSounds`/
  `BulletParticles`，砍掉 tier/predicate/shield）+ 4 个 manager + 示例包（`tags/blocks`→`tags/block`）。
- **第 2 轮：行为层**。`BlockRayTracer`/`BlockBreakingManager` + `features.*` mixin + `accessor`，
  考古 26.2 的 `EntityKineticBullet` 碰撞流程与 `BlockRayTrace`。

## 7.13 俯仰角"始终无效"的根因定位与修复（2026-08-17，最终版）

用户反馈：三个版本（增量限制 → 事后 setXRot → 增量限制）中只有"事后 setXRot"版有反应
（但抖动），其余两个版本**俯仰角配置完全无效**。log 无任何 Mixin 报错 → mixin 应用成功。

根因分析（对比三个版本的行为自洽）：
- 我的旧实现用 `@WrapOperation` wrap 与 TaCZ **同一个** `LocalPlayer#turn(DD)V` 调用点，
  与 TaCZ 的 `MouseHandlerMixin#reduceSensitivity` 形成双层 wrap 嵌套；
- 我做的"增量截断"（`finalPitch = max/min(pitch, 0)`）在外层，会被 TaCZ **内层**的
  `getCrawlPitch` 重新计算 `finalPitch = pitch * denominator` 覆盖，所以无效；
- "事后 setXRot"版绕过了 TaCZ 内层所以有效，但当时动态下限算成了 +84°（弧度→度 bug）
  导致抖动。

最终修复（本轮，彻底绕开冲突点）：
1. **俯仰角**：改用 `@Inject(method="turnPlayer", at=@At("TAIL"))`，在 turn 完成后直接
   clamp `LocalPlayer#getXRot()`。TAIL 与 TaCZ 的 wrap 完全正交，不存在嵌套覆盖问题；
   动态下限用渐变 `lower * clamp(distance,0,1)`（贴脸→0°、1 格外→恢复配置值），
   并加节流日志（`[TaCZ Tweaks] crawlPitch crawling=... pitch=... upper=... lower=... clamped=...`）。
2. **降低灵敏度键**：同样从 `LocalPlayer#turn` 冲突点挪走，改为 wrap
   `handleAccumulatedMovement` 里对 `turnPlayer(D)V` 的调用（该点 TaCZ 未触碰），
   直接缩放灵敏度系数。

> 若用户再测仍无效，日志里的 `crawling=` 字段能直接区分是"条件不成立（姿势判断错）"
> 还是"clamp 未触发（配置/值问题）"，无需再猜。

## 7.14 数据驱动系统 data 层移植记录（2026-08-17）

原版 40+ 文件的 data 层已完成移植并编译通过。**DFU 10（随 MC 26.2）有一批破坏性 API 变更**，
是移植的主要坑：

| 原版（DFU 6 / 1.20.1） | 26.2（DFU 10.0.21） | 处理 |
|---|---|---|
| `Codec.unit(x)` | **已移除** | 改 `MapCodec.unitCodec(x)` |
| `Codec.dispatch(getKey) { it.codecProvider() }`（provider 返回 `Codec`） | `dispatch` 改为要求 **`MapCodec`** provider | 新增扩展 `Codec.dispatchBy`，内部 `MapCodec.assumeMapUnsafe(...)` 桥接 |
| `RecordCodecBuilder.create(...)` 返回 `MapCodec` | `create(...)` 返回 **`Codec`**、`mapCodec(...)` 返回 `MapCodec` | 现有 `val CODEC: Codec<X> = create{...}` 无需改 |
| `EntityType.is(TagKey)` | **已移除** | 改 `entity.type.builtInRegistryHolder().\`is\`(tag)` |
| `ForgeRegistries.BLOCKS.codec` | `BuiltInRegistries.BLOCK.byNameCodec()` | 替换 |
| `SimpleJsonResourceReloadListener(Gson, dir)` | `SimpleJsonResourceReloadListener(Codec, FileToIdConverter.json(dir))` | `BaseDataManager` 重构为 codec 驱动，丢掉 `parseElement` 与 old-format fallback |
| Forge `AddReloadListenerEvent` | Fabric `ResourceManagerHelper.get(SERVER_DATA).registerReloadListener(...)`（`IdentifiableResourceReloadListener`） | `BaseDataManager.register()` |
| `BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), …)` | `parseForBlock(BuiltInRegistries.BLOCK, …)`（Registry 直接是 HolderLookup）；`BlockResult` 变 record（`blockState()/properties()/nbt()`） | `BlockInputCodec` 重写 |

### 刻意砍掉 / 延后的部分

- `BlockTarget.TIER`、`EntityTarget.PREDICATE`、`Target.PREDICATE/BURST_INDEX/PELLET_INDEX`、
  `BulletInteraction.Shield`、`PredicateCodecs`、`TierSortingRegistryCodec`、`MinMaxBoundsCodecs`、
  `data/old/*` —— 依赖 Forge（TierSortingRegistry、ShieldBlockEvent）或 26.2 已删除的
  predicate/MinMaxBounds，全部砍掉。示例包同步去掉 `shield.json` 与 `50bmg.json` 的 `tier` 字段。
- `BulletInteractionManager` 等 4 个 manager 目前只做**加载**（comparator + debugEnabled + register），
  行为方法（`handleBlockInteraction` / 音效 / 粒子 / 近战）留在行为层轮次。

### 示例包 26.2 适配

- `data/.../tags/blocks/` → `data/.../tags/block/`（26.2 目录单数化）
- `#forge:glass` → `#c:glass_blocks`、`#forge:glass_panes` → `#c:glass_panes`（Fabric convention tags）

## 7.15 数据驱动系统 行为层 移植记录（2026-08-17）

行为层已完成并编译通过。核心差异：**26.2 的 `EntityKineticBullet#onBulletTick` 已内联整个碰撞流程**
（rayTraceBlocks → findEntitiesOnPath → onHitEntity×N → onHitBlock），原版依赖的
"在 `BlockRayTrace.lambda$rayTraceBlocks$1/2` 拦截"架构改成了对 26.2 具名方法的注入：

| 原版（1.20） | 26.2 移植 |
|---|---|
| `BlockRayTraceMixin` 拦 `lambda$rayTraceBlocks$1`（block 命中）+ `$2`（miss） | 拦 **`getBlockHitResult(Level,ClipContext,BlockPos,BlockState)`**（具名、参数自带 BlockState），返回 null 让 `performRayTrace` 跳过该方块 = 穿透 |
| `BulletRayTracer` 负责实体查找 + 实体交互 | 实体查找/onHitEntity 交给 onBulletTick 原生；实体交互改在 **`EntityKineticBulletMixin` 里 wrap `onHitEntity` 调用点**（damage modifier + 音效粒子） |
| `features.EntityKineticBulletMixin` 拦 `onBulletTick` 的 rayTraceBlocks 调用后 cancel | 不再 cancel；保留 `<init>` 存 gunStack、`getDamage` 应用 damageModifiers、`tick` 播 constant/whizz |

### 砍掉 / 简化的部分

- **FakePlayer + DestroySpeedModifierHolder + BlockBehaviourMixin**：此处“Fabric 无 FakePlayer”是早期误判；
  Fabric API 26.2 实际提供 FakePlayer。本移植选择独立 `SafeMath.blockBreakingDelta`，直接计算
  `(1 + damage) / (effectiveHardness * 30)`，并对零硬度、非有限值和 armor-ignore 边界做测试。
- **melee 行为层**：`melee_interactions` 的 mixin 依赖 `Suppliers.memoize`/@Share sugar 且示例包无
  数据，manager 保留加载、行为砍掉。
- **airspace 行为**（Sound Physics Remastered 相关）：数据保留解析，播放砍掉。
- **shield 交互**：数据层已砍（Forge ShieldBlockEvent），行为随之无。
- **entity pierce 规则**：26.2 原 onBulletTick 已有自己的 pierce 循环（枪械 pierce 值），
  数据规则的 entity `pierce` 字段解析保留但行为依赖该原生循环；示例包 `ender_dragon.json`
  只用到 damage modifier，已完整支持。

### 其它 26.2 API 变更

- `Level.gameTime` **已删除** → BlockBreakingManager 改用 `System.currentTimeMillis()`（20s 过期）；
- `Level.blockUpdated` → `sendBlockUpdated(BlockPos, BlockState, BlockState, int)`；
- `Level.destroyBlock(pos, drop, entity)` 3 参 → 4 参 `destroyBlock(pos, drop, entity, flags)`；
- `Entity.soundSource` 字段 → `getSoundSource()`；`createCommandSourceStack` 删除 → 粒子坐标
  改为直接 Vec3 计算（砍掉 WorldCoordinates/LocalCoordinates）；
- `ParticleArgument.readParticle(reader, RegistryAccess)`（RegistryAccess 即 HolderLookup.Provider）；
- `ServerLevel.sendParticles` 带 force 的版本是 12 参；
- Fabric tick 事件：`ServerTickEvents.END_SERVER_TICK` 遍历 `server.getAllLevels()` 驱动
  BlockBreakingManager + BulletParticlesManager。

## 7.16 粒子解析崩溃修复（2026-08-18，用户实测）

用户报崩溃（`Ticking entity` → `tacz:bullet`）。根因：

```
Caused by: CommandSyntaxException: 无法解析粒子选项：No key block_state in MapLike[{}]
```

**26.2 把粒子参数解析从旧的命令式字符串改成了 SNBT/codec 格式**（`ParticleArgument.readParticle`
内部用 `TagParser` 解析 `{...}`，再喂给 `ParticleType.codec()`）：

| 旧语法（1.20，示例包原样） | 26.2 正确语法 |
|---|---|
| `minecraft:block minecraft:redstone_block` | `minecraft:block{block_state:"minecraft:redstone_block"}` |

示例包 `blood.json` / `particle.json` 里的旧语法在 26.2 下解析失败（`minecraft:redstone_block`
部分被忽略 → emptyMap → 缺 `block_state` 键 → 抛异常），而 `BulletParticlesManager.summon`
未捕获，异常穿透 `guardEntityTick` 直接崩游戏。

修复（双保险）：
1. 示例包粒子语法改为 26.2 SNBT 格式（`particle.json` 的 `%s` 占位仍保留，格式化后为
   `minecraft:block{block_state:"<block id>"}`）；
2. `BulletParticlesManager.summon` 的 `ParticleArgument.readParticle` 包 try-catch——粒子只是
   视觉效果，解析失败记日志并跳过，**绝不**再让实体 tick 崩溃。第三方枪包的非法粒子
   语法也不会拖垮游戏。

## 7.17 “不可移植”复审与 R2 完成项（2026-08-18）

本轮不再按 1.20 类名判断能力，而是核对 26.2 class 调用链和实际 Fabric 发行物：

- `Identifier` 实际是普通 final class，并非 record；共享资源标识也不应承载每次播放状态。mono 改为请求上下文和 mono/stereo 独立缓存，并在 `SoundBuffer` 构造前 downmix；
- 数据驱动附魔删除了 `ProtectionEnchantment` 类，但 `EnchantmentHelper#getDamageProtection` 仍是汇总点；
- predicate 移到 `net.minecraft.advancements.predicates`，并未删除；MinMaxBounds 同样只是换包；
- tier 语义由 `ToolMaterial.incorrectBlocksForDrops` 承担；
- shield 走 26.2 `BlocksAttacks` component，不再寻找旧 `hurtCurrentlyUsedShield`；
- `Level#getGameTime()` 实际存在，破坏进度已从 wall clock 改回 400 game ticks；
- SPR、First Aid New、Pillager’s Gun 均已有 Fabric 26.2 发行物，已恢复可选兼容；
- melee、airspace、shield、predicate/tier、burst/pellet 均已接回行为层。

同时修复了配置 payload 复用/长度校验、跨维度粒子、方块保护事件、静态 raytrace 上下文竞争、
`sprintWhileReloading` 被目标端二次取消等完整性问题。详细证据和测试矩阵见 `AUDIT.md`。

## 7.18 隐藏删减复审（2026-08-18）

第二轮不再只看配置 getter，而是把原版同路径源码、95 个 mixin、空方法/固定返回和注释代码一起纳入。
恢复了旧数据格式 converter、改装属性图全局基线、筛选复选框隐藏、射击换弹动画判定和倾斜阻止起跑。

所有仍不同路径/缺失的原版源码记录在 `scripts/upstream_omissions.json`；提供上游 checkout 运行
`audit_port.py --upstream-root` 时，新增未解释缺失和已经失效的豁免都会失败。当前允许缺失仅包括：
26.2 原生替代、已合并到新 hook 的行为、共享对象/数据驱动 API 的重设计，以及确实没有 Fabric 26.2
目标的 LSO/MTS/VS 系列。

## 7.19 外部差距报告复核与 Beta-1 加固（2026-08-18）

外部审计基于旧提交 `69bc17a`，但其中多数边界指控在当前代码上仍可复现，因此没有因提交过旧而忽略：

- 无 shield 规则时明确返回 null，保留 `BlocksAttacks` 的 vanilla blocked damage/durability；
- tilt C2S 改为短时输入请求：服务端每次使用都复核 alive、主手枪和 `GunData.canSlide`，40 tick 超时；
- 共享第一人称枪声要求 alive + 主手持枪 + 当前枪包/TaCZ namespace，并收紧为 96 格、16 次/秒；
- `SafeMath.blockBreakingDelta` 让 armor ignore 降低有效硬度，处理零硬度/负值/NaN/Infinity；
- bullet/melee 共用 `ProtectedBlockBreaking`，执行 `mayInteract` 和 BEFORE/CANCELED/AFTER 完整链；
- 每颗子弹记录已播放 whizz 的 UUID，实体 PIERCE sound/particle 分支恢复，并完整排序候选；
- 粒子只替换 `%s`、限制 finite/range/1024 emitters；constant interval codec 强制为正；
- physical/dummy/FUEL/inventory/chamber 卸弹分支拆开，畸形计数和 stack size 直接拒绝；
- `ValueRange.DEFAULT` 改为 `-Double.MAX_VALUE..Double.MAX_VALUE` 并验证 finite/min<=max；
- airspace payload 限 64 candidates × 32 sounds；mono/stereo 按请求使用独立缓存；
- 示例包修正 `#minecraft:chains`，恢复缺失 ogg，并加入 predicate/tier/burst/pellet/airspace smoke data；
- 发布标识统一调整为 Fabric/SemVer 可解析的 `Beta-1`，补单测、wrapper checksum、example zip task 和第三方 notices。

CI workflow 仍需要仓库维护者以具备 workflow 权限的身份写入；当前 Agent GitHub App 无此权限，
所以不能把“本地有验证入口”误写成“GitHub 已有 check run”。

## 7.20 dedicated-server environment stripping（2026-08-18）

`AdsModifier` 等目标类本身是 common，但 `getPropertyDiagramsData` 标有
`@Environment(EnvType.CLIENT)`。Fabric 专服会在 Mixin 应用前剥离该方法；把 cache/gameplay 和
property diagram 注入放在同一个 common mixin，会造成 `InvalidInjectionException` 并阻断启动。

修复不是 `require=0`：八组 modifier 均拆为 common `initCache` mixin 与登记在 JSON `client`
数组中的 `*DiagramMixin`。`audit_port.py` 现在解析 class 的 RuntimeVisible/InvisibleAnnotations，
任何 common mixin 注入 `@Environment(CLIENT)` 方法都会失败。另增 `check_server_log.py`，要求专服
日志真实出现 `Done (...)!` 且不含 fatal mixin/startup marker，避免 Loom 子进程失败但 Gradle 返回 0。

## 7.21 盾牌 Mixin 参数数量崩溃修复（2026-09-07）

用户报告持盾格挡（苦力怕爆炸）时 MixinExtras `IncorrectArgumentCountException`
（`throwIncorrectArgumentCount`，"Expected 7 but got 6" 一类信息）导致游戏退出。

- 机制：`@WrapOperation` 的生成 bridge 按 **INVOKE 点实参个数（含接收者）** 校验
  `original.call(...)` 的实参个数；handler 必须按自己所包调用点的个数调用。
- 根因定位在 `1.21.11-neoforge` 分支：为兼容专服 jar 的
  `hurtBlockingItem(...;FI)V`（6 参形态）加的 wrap，其 handler 把 6 参调用点的
  `Operation` 交给共享 helper，helper 以 6 个值调用它 → 期望 7 个值 → 首次格挡崩溃。
- 本分支（26.2 main，Fabric）对已核实的原版调用点算术正确，但单 descriptor +
  默认 `require` 在调用点漂移的构建上会整体硬失败（NeoForge 26.2.x 已把调用点改成
  6 参形态，见 `neoforged/NeoForge` 26.2.x 的 `BlocksAttacks`/`LivingEntity` patch）。
- 修复：`LivingEntityMixin` 同时包住 5 参（原版）与 6 参（`fixedDamage`）两种调用点，
  均 `require = 0`，每个 handler 只按自己调用点实参个数调用 `original`；6 参目标
  `remap = false`（26.2 未混淆且不在原版编译 classpath）；新增
  `shieldDurabilityApplied` + 一次性告警兜底未来第三种形态（降级不崩）。
- 证据与验证状态见 `AUDIT.md`「盾牌 Mixin 参数数量崩溃排查与修复（2026-09-07）」；
  `audit_port.py` 对 `remap = false` 可选目标降级为警告，
  `scripts/test_audit_optional_targets.py` 以模拟 classpath 覆盖该检查。
- 1.21.11-neoforge 分支需按同一模式单独修复（6 参 handler 必须 7 值调用 `original`，
  或不再委托 5 参 helper）；Fabric 1.21.11 / 26.1.2 两条线的同款单 wrap 建议同步加固。

## 8. 当前待办

### 8.1 已完成

- [x] 全部在 GUI 中公开的 gun/crawl/tweaks/modifier/debug 选项都有行为读取点
- [x] betterMonoConversion / bulletProtection / crawl visualTweak
- [x] melee（枪械 + 内置 LRTactical）、shield、airspace 行为层
- [x] predicate / tier / burst_index / pellet_index 数据兼容
- [x] First Aid New / Sound Physics Remastered / Pillager’s Gun Fabric 26.2 可选兼容
- [x] 无目标的 LSO / MTS / VS 与已原生修复的 thirdPerson 开关从配置 codec/GUI 删除
- [x] `scripts/audit_port.py` 系统审计（注入调用点字节码、配置死项、语言键、版本一致性）
- [x] 57 项上游源码缺失解释门禁、旧数据格式、属性图表与隐藏 UI/动画路径复原

### 8.2 发布前验证

- [ ] JDK 25 `./gradlew clean build` 和产物 remap 检查
- [ ] 纯必需依赖的客户端、集成服、独立服务端启动
- [ ] SPR / First Aid / Pillager’s Gun 单独和组合安装测试
- [ ] mono、四件弹射物保护与 void bullet、盾牌、近战、领地取消破坏、多维度粒子实测
- [ ] 第三方数据包对 predicate/tier/burst/pellet/airspace 的兼容回归

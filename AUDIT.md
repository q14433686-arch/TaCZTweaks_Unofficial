# 审计：TaCZ Tweaks Unofficial（Fabric 26.2）

对照仓库：[`MUKSC/TaCZTweaks`](https://github.com/MUKSC/TaCZTweaks) v2.14.2（Forge 1.20.1）
目标端：`libs/TACZ-Refabricated-26.2-1.1.8+fabric.26.2.R2.jar`（未混淆，`doMelee` / `GunSoundInstance` / `ItemInHandLayerMixin` 均在）

结论先说：**README「已确认不可移植」表基本不成立。** 前几轮自己也写过「不可行判断被推翻」（PORTING_NOTES §7.9），这轮剩下的几条同样是「原路走不通」而不是「功能做不了」。

---

## 1. 「不能做」逐条打脸

### 1.1 `betterMonoConversion` —— **能做**

README 原文：

> 26.2 里 `ResourceLocation` 改名 `Identifier` 且是 **record，无法 mixin 打标记**

事实：

- 原版并不是「必须给 Identifier 加字段」。它只是把 `mono` 标志挂在 `ResourceLocation` 上，再在 `SoundBufferLibrary` 读 buffer 时取出来。
- record 不能 `@Unique` 加字段，这只封死了 **那一种** 打标方式。
- 标准替代：**`Collections.newSetFromMap(new WeakHashMap<>())` / `WeakHashMap<Identifier, Boolean>` sidecar**，identity 语义与原版 duck-type 字段等价，Identifier 被回收时自动掉。
- 目标端 `GunSoundInstance` **两个构造都还在**，且 10 参构造最后仍是 `boolean mono, boolean relative`：

```
<init>(SoundEvent, SoundSource, FF, Entity, I, Identifier, Z)V
<init>(SoundEvent, SoundSource, FF, Entity, I, Identifier, ZZ)V
```

`GunSoundInstance$TaczSound.getPath()` 也还在。移植路径：

1. `GunSoundInstanceMixin`：`<init>` 存 `mono`，`resolve` 里 `NEW TaczSound` 时写入 sidecar。
2. `GunSoundInstance$TaczSoundMixin`：`getPath` 返回值把该 Identifier 标进 sidecar。
3. `SoundBufferLibraryMixin`：wrap `SoundBuffer.<init>(ByteBuffer, AudioFormat)`（或 26.2 对应的 lambda / 具名方法），sidecar 命中则双声道平均成单声道。

**不需要 mixin Identifier。** 「record 无法打标 = 不能做」是把实现细节当成了功能边界。

### 1.2 `bulletProtection` —— **能做**

README 原文：

> 26.2 已删除 `ProtectionEnchantment` 类（附魔系统改为数据驱动），需重新设计

事实：

- `ProtectionEnchantment` 从 1.21 起确实没了。原版 mixin 做的事非常窄：`getDamageProtection` 里 `source.is(IS_PROJECTILE)` 再 `|| source.is(tacz:bullets)`。
- 数据驱动之后保护走 enchantment effect + damage type tag。两条都能落地：
  1. **数据包**：把 `tacz:bullets` 加进 `#minecraft:is_projectile`（或 projectile protection 自己的 `damage_type` tag）。零 mixin。
  2. **mixin `EnchantmentHelper` / `CombatRules` / `LivingEntity.hurtServer` 里算 EPF 的那次 `DamageSource.is(TagKey)`**，语义与原版一行 `|| BULLETS_TAG` 相同。
- 目标端 `ModDamageTypes.BULLETS_TAG` **还在**。

「类删了要重新设计」≠「做不了」。最多换钩子。

### 1.3 `thirdPersonGunRenderingFix` —— **不必做，但不是不能做**

这条理由是对的：目标端 `ItemInHandLayerMixin.submitTail` 已经调 `HumanoidOffhandRender.renderGun(ArmedEntityRenderState, …)`，26.2 extract→submit 管线重写时把原 bug 修了。

但：

- 配置项仍显示在 GUI 里，开关是空操作。
- 原版 mixin 是「关掉 TaCZ 的坏 mixin + 自己重画」。目标端已修，应 **从 GUI 拿掉或改成只读说明**，不要假装用户还能开关。

### 1.4 compat 组 —— **「Forge 独占、无 Fabric 版」是错的**

| 模组 | README 说法 | 实情 |
|---|---|---|
| Sound Physics Remastered | Forge 独占 | **已有 Fabric 26.2**：CurseForge `[FABRIC][26.2] Sound Physics Remastered 1.5.1+26.2`（2026-06-18）。原版 airspace 整条链路（探测音 → mixin `evaluateEnvironment` → 按 airspace/occlusion/reflectivity 选枪声）可以原样迁，只换 `ModList` → `FabricLoader.isModLoaded("sound_physics_remastered")`，mixin 用 `plugin` 在模组不在时 skip。 |
| LRTactical | 当 compat 砍掉 | **目标端已经 `provides: ["lrtactical"]` 内置**。`me.xjqsh.lrtactical.api.item.IMeleeWeapon` 就在 R2 jar 里。近战破坏的武器 id 查询 3 行就能接上。 |
| Valkyrien Skies | Forge 独占 | VS2 历来双端；26.2 官方包尚未看到，但 hook 全是 mixin `BlockHitResult` / `ClipContext` / `Explosion`，**不依赖 Forge 事件**。有 Fabric 包就能做；没包就把 mixin 标 optional，不是「不能移植」。 |
| FirstAid / LSO / MTS / PillagersGun | Forge 独占 | 目前确实没看到 Fabric 26.2。正确表述是 **「目标模组不存在，compat 无意义」**，不是 Tweaks 做不了。GUI 里这五个开关现在是 **纯空操作**，应隐藏或标「未实现」。 |

### 1.5 数据驱动子弹交互 —— **README 自己打自己**

README 上半截写「行为层本轮完成，全链路可运行」，下半截「已确认不可移植」表又写「示例包 / 数据驱动系统范围过大，暂缓」。后一张表是过期的。

真正没做、却被写成「砍掉 / 做不了」的：

| 项 | 被写成 | 实际 |
|---|---|---|
| melee 近战破块 | 依赖 `Suppliers.memoize` / `@Share`，示例包无数据 | `ModernKineticGunItem.doMelee(LivingEntity, FFFFF, List)` **R2 里还在**，`doPerLivingHurt` 也在。原版 mixin 就是 TAIL 时没打到生物就 `player.pick` + `destroyBlock`。`@Share` 只是糖，改成字段或局部变量即可。LRTactical 已内置。 |
| shield 格挡 | 依赖 Forge `ShieldBlockEvent` | 原版 **主体已经是 mixin**（`LivingEntity.isDamageSourceBlocked` / `hurt` / `isBlocking` + `Player.hurtCurrentlyUsedShield` / `disableShield`）。Forge 事件只是入口，用来塞自定义耐久和缴械时长。26.2 改 mixin `hurtServer` + 自己在 `onHitEntity` 前调用 `handleShieldInteraction` 即可。`ItemPredicate` 换成 `item` id / `#tag` 两字段。 |
| airspace | 依赖 Sound Physics | 见 1.4，**Fabric 26.2 已有**。数据层 `BulletSounds.AirSpace` **已经移植完**，只差播放。 |
| `tier` | Fabric 无 `TierSortingRegistry` | 1.21+ 挖掘等级是方块 tag（`#minecraft:needs_stone_tool` 等）。codec 里 `tier` 改成 `TagKey<Block>` / 硬度下限就行；示例包本来也几乎不用。 |
| FakePlayer | Fabric 无 | 现有解析式 `digSpeed/(hardness*30)` 已经够用；真要 1:1，可以临时 `ServerPlayer` 或自己实现 `getDestroyProgress`。 |
| `EntityPredicate` / `BlockPredicate` / `MinMaxBounds` | 26.2 已删 | 谓词系统 codec 化了，不是消失。`burst_index` / `pellet_index` 是子弹自己的计数器，mixin 加字段即可（`EntityKineticBulletExtension` 已有 pierce 计数，同款）。 |

---

## 2. 配置界面的「假开关」（更严重）

这些选项 **GUI 看得到、JSON 存得了、运行时没有任何 mixin 读它们**：

| 配置 | GUI | 实现 |
|---|---|---|
| `crawl.visualTweak` | ✅ 显示 | ❌ 无 `PlayerRenderer`/`AvatarRenderer` mixin。原版改游泳平移让匍匐不「滑行」。26.2 类名 `AvatarRenderer`，`setupRotations` 还在，换签名即可。 |
| `compat.firstAidCompat` 等 5 项 | ✅ 显示 | ❌ 整个 `mixin/compat/` 没搬。开/关无差别。 |
| `gun.thirdPersonGunRenderingFix` | ✅ 显示 | 目标端已修，开关无意义。 |
| `tweaks.betterMonoConversion` | ❌ 已藏 | 能做，见 1.1。 |
| `tweaks.bulletProtection` | ❌ 已藏 | 能做，见 1.2。 |
| `debug.meleeInteractions` | ✅ 显示 | manager 只加载 JSON，**`handleBlockInteraction` 没接 `doMelee`**。 |

`visualTweak` 是最典型的「写了配置当移植完」。

---

## 3. 已移植部分的缺口（不是「不能」，是漏了）

对照原版 95 个 mixin，本仓库约 50 个。除 compat 外，核心漏项：

| 原版 | 作用 | 26.2 可行性 |
|---|---|---|
| `ObjectAnimationSoundChannelMixin` | 检视/拉栓等动画音也走共享枪声 | `playAnimationSound` 大概率还在，补一个 `@WrapOperation` |
| `ProjectileExplosionMixin` | 爆炸伤害吃 `playerDamage` 倍率 | `Entity.hurt` → `hurtServer`，改一行 |
| `GunSmithTableScreenMixin` | 配件类型检查 + 滚轮 NPE | 工作台 GUI 还在，可直接迁 |
| `AnimationManagerMixin` | 开火动画闪烁 | 若目标端还用 playerAnimator |
| `ClientGunTooltipMixin` | 改装图显示倍率 | 刻意跳过（`@Local` 脆），但不是不能做 |
| `features/ModernKineticGunScriptAPIMixin` | 射击相关声音/粒子钩 | 看 26.2 具名 hook |
| `burst_index` / `pellet_index` Target | 连发/弹丸匹配 | extension 加两个 int |

文档债：

- `PORTING_NOTES.md` §8 待办还列着 `betterInaccuracy` / `betterGunTilt` / `endermenEvadeBullets` / 卸弹创造模式 / 数据驱动系统 —— **这些 README 已经标 ✅**。§8 是过期的。
- README 同时说数据系统「已完成」和「暂缓」。

---

## 4. 真正做不了 / 暂时不该做的

很少。

1. **FirstAid / LSO / MTS / PillagersGun**：目标模组没有 Fabric 26.2。不是 Tweaks 不能写 mixin，是没东西可 hook。有包再接，mixin plugin + `optional`。
2. **Valkyrien Skies**：等有 26.2 Fabric 包。架构上能做。
3. **`thirdPersonGunRenderingFix`**：目标端已修，重做是倒退。
4. **运行时实测**：沙箱起不了 MC 26.2。这限制验证，不限制实现。

没有一条是「26.2 / Fabric / record / 数据驱动附魔」这种语言级别的不可能。

---

## 5. 建议落地顺序（按性价比）

全部都能做。建议：

1. **纠文档 + 藏空开关**（compat 五件套、thirdPerson 改说明）。成本最低，停止误导。
2. **`visualTweak`**：`AvatarRenderer.setupRotations`，原版 30 行。
3. **`betterMonoConversion`**：sidecar + 3 个 mixin，原版逻辑可抄。
4. **`bulletProtection`**：优先 datapack tag；不行再 mixin EPF。
5. **melee 行为层**：`doMelee` TAIL + 把 `MeleeInteractionManager.handleBlockInteraction` 从原版搬过来（`BlockEvent` 改 Fabric `PlayerBlockBreakEvents` / 直接 `destroyBlock`；`tier` 忽略或改 tag）。LRTactical 直接 `IMeleeWeapon.of(stack).getId(stack)`。
6. **shield**：恢复 `BulletInteraction.Shield` codec（`predicate` → item id/tag），mixin `hurtServer` + `hurtCurrentlyUsedShield`。
7. **Sound Physics airspace**：optional mixin，数据层已就绪。
8. 补漏：`ObjectAnimationSoundChannel`、`ProjectileExplosion`、`GunSmithTableScreen`。

---

## 6. 本轮已落地（2026-08-18）

按上面的换路全部接上了，**没有照搬 1.20 类名**：

| 功能 | 26.2 路径 |
|---|---|
| `betterMonoConversion` | `MonoAudio` ConcurrentHashMap sidecar + `GunSoundInstance` / `TaczSound` / `SoundBufferLibrary` |
| `bulletProtection` | ThreadLocal 仅包住 `EnchantmentHelper.getDamageProtection`，`DamageSource.is(IS_PROJECTILE)` 临时认 `tacz:bullets` |
| `visualTweak` | `AvatarRenderer.setupRotations` + `LivingEntityRenderState` 字段（`require = 0` 防字段改名崩档） |
| melee | `ModernKineticGunItem.doMelee` + 内置 `IMeleeWeapon.performAttack` |
| shield | `LivingEntity.hurtServer` + `ItemMatch`（id/tag），不碰 Forge 事件 |
| airspace | 自研 S2C 包（不序列化 `ClientboundSoundPacket`）+ 字符串目标 SPR mixin + plugin 门闩 |
| 漏搬 | `ObjectAnimationSoundChannel` / `ProjectileExplosion.hurt(DS,F)V` / `GunSmithTableScreen.mouseScrolled(DDDD)` |
| GUI | compat 五件套和 thirdPerson 已藏；mono / bulletProtection 重新显示 |

沙箱仍然起不了 26.2，**需要实机编一次**。最可能对不上的点：`AvatarRenderer.swimAmount` / `isVisuallySwimming` 字段名、`SoundBufferLibrary.getCompleteBuffer` 方法名、`Player.disableShield()` 是否还是无参、`SoundEngine.play` 返回值。

## 7. 一句话

「不能做」里只有 **「目标模组不存在」** 和 **「目标端已经修了」** 两条站得住。其余全是把「原版那一个 hook 没了」写成了功能死刑。换 sidecar / tag / `hurtServer` / 具名 hook 就能做。本仓库自己的历史已经证明过一次（卸弹枪膛、`forceFirstPersonShootingSound`、`betterInaccuracy`），剩下这几条没有更硬。

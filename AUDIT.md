# 「不能做」审计 — 2026-08-18

对照对象：

- 本仓库 README / `PORTING_NOTES.md` 里标成「不可移植 / 砍掉 / 暂缓」的条目
- 原版 [MUKSC/TaCZTweaks](https://github.com/MUKSC/TaCZTweaks) v2.14.2（Forge 1.20.1）实现
- 目标端 `TACZ-Refabricated-26.2-1.1.8+fabric.26.2.R2.jar` 字节码
- 26.2 官方变更与当前 Fabric 生态

**总评：你的判断对。** 文档里「已确认不可移植」几乎全是「当时那条注入路走不通」，不是功能本身做不了。真正被堵住的只有「对面模组在 Fabric 26.2 不存在」的那几条 compat。

---

## 1. 结论速查

| 条目 | 文档说法 | 实际 | 难度 |
|---|---|---|---|
| `betterMonoConversion` | Identifier 是 record，无法 mixin 打标记 | **能做**。原路（给 Identifier 加字段）确实走不通，但根本不需要打标记 | 低 |
| `bulletProtection` | `ProtectionEnchantment` 已删 | **能做**。类没了，投射物保护的计算点还在 | 中 |
| `thirdPersonGunRenderingFix` | 不可移植 | **不必做**。目标端已原生修，不是做不到 | — |
| Sound Physics / airspace | Forge 独占，无 Fabric 26.2 | **能做**。[SPR 1.5.1+26.2 Fabric](https://www.curseforge.com/minecraft/mc-mods/sound-physics-remastered/files/all) 2026-06-18 已发 | 中 |
| LRTactical 近战破坏 | 跟 compat 一起砍了 | **能做，而且更简单**。目标端 `provides: ["lrtactical"]`，类就在 TaCZ jar 里 | 低 |
| melee 行为层 | `@Share` / `Suppliers.memoize` 太糖，砍掉 | **能做**。`ModernKineticGunItem.doMelee` / `doPerLivingHurt` 签名还在 | 低 |
| shield 格挡交互 | 依赖 Forge `ShieldBlockEvent` | **能做**。原版主体本来就是 mixin `LivingEntity`，不是那个事件 | 中 |
| FakePlayer / 方块破坏速度 | Fabric 无 FakePlayer | **已经用公式替代了**；真要玩家上下文直接用射手自己 | 低 |
| `BlockTarget.TIER` | 无 `TierSortingRegistry` | **能做**。改走 tool component / `#minecraft:incorrect_for_*` 标签 | 中 |
| `EntityPredicate` / `Target.PREDICATE` | 「26.2 已不存在」 | **说错了**。26.2 谓词还在，只是 key 改成 Identifier | 中 |
| `burst_index` / `pellet_index` | 推到行为层就没了 | **能做**。子弹实体上加两个扩展字段 | 低 |
| 匍匐 `visualTweak` | 笔记里 ⏳ | **能做**。`PlayerRenderer` → `AvatarRenderer`，原版 30 行 mixin | 低 |
| 示例包 / 数据驱动系统 | README 还写「暂缓」 | **已经做了**。文档过时 | — |
| FirstAid / LSO / MTS / VS | Forge 独占 | **现在做不了**。Fabric 26.2 没有对应模组 | 堵死（缺目标） |

---

## 2. 被写成「不能做」、其实能做

### 2.1 `betterMonoConversion` — 换挂点，不要打 Identifier

原版三件套：

1. `ResourceLocationMixin`：给 `ResourceLocation` 加 `tacztweaks$monoAudio` 布尔字段
2. `GunSoundInstanceMixin`：构造时把 `mono` 参数记下来，`resolve()` 里新建 `TaczSound` 时写回
3. `SoundBufferLibraryMixin`：加载 ogg 时若该 id 被打标且是立体声，就平均左右声道变成单声道

文档卡在第 1 步：26.2 的 `Identifier` 是 record，Mixin 拒绝给 record 加实例字段。**这只封死了「往 Identifier 上打标」这一招。**

26.2 的 `GunSoundInstance` 构造还在（已对 jar 反查）：

```
<init>(SoundEvent, SoundSource, FF, Entity, I, Identifier, Z)V
<init>(SoundEvent, SoundSource, FF, Entity, I, Identifier, ZZ)V
```

最后一个 `Z` 就是原来的 `mono`。标记完全可以挂在：

- `GunSoundInstance` 自己（原版已经存了）
- `ThreadLocal<Boolean>`，在 `resolve()` / `getCompleteBuffer` 前后夹一下
- `IdentityHashMap` / 弱引用旁表（语义和往 Identifier 上加字段一样，还不用 mixin record）

`SoundBufferLibrary.getCompleteBuffer` 还在。功能 = 「立体声枪音按 mono 标志做 downmix」，跟 Identifier 是不是 record 无关。

配置项已经注册，只是从 YACL 界面藏起来了。

### 2.2 `bulletProtection` — 换 `EnchantmentHelper`，不要找已删的类

原版 `ProtectionEnchantmentMixin` 只做一件事：

```
source.is(IS_PROJECTILE)  ||  source.is(tacz:bullets)
```

也就是让 **弹射物保护** 也吃 TaCZ 子弹。不是给所有保护附魔开后门。

26.2 附魔数据驱动后 `ProtectionEnchantment` 类没了，计算挪到 `EnchantmentHelper` / `damage_protection` 效果。目标端 `tacz:bullets` **没有** 进 `#minecraft:is_projectile`（已核 jar 内 tags），所以这个开关现在确实是空的。

可落地的三条路，由窄到宽：

1. mixin 26.2 的 `EnchantmentHelper.modifyDamageProtection`（或等价入口），对 `tacz:bullets` 补上弹射物保护公式
2. mixin 数据驱动效果里检查 `IS_PROJECTILE` 的那次 `DamageSource.is(TagKey)`
3. **不要** 把 `tacz:bullets` 写进 `#minecraft:is_projectile` 数据包——末影人躲避、盾牌、其它投射物逻辑会被一起改掉，原版也没这么干

「类删了要重新设计」= 换注入点，不是功能不可做。

### 2.3 Sound Physics / airspace — Fabric 26.2 版已经存在

文档把整个 compat 组写成「Forge 独占，Fabric 26.2 无对应版本」。

[Sound Physics Remastered 1.5.1+26.2](https://www.curseforge.com/minecraft/mc-mods/sound-physics-remastered/files/all) **Fabric 构建 2026-06-18 已发布**。原版自己就有 `SoundPhysicsMixin$1_5_x.java`，就是冲着 1.5.x API 写的。

本仓库 `BulletSounds.AirSpace` codec 还在，`BulletSoundsManager` 对 airspace 直接 `false` 跳过。缺的是：

- 可选依赖 SPR
- 把原版 `compat/soundphysics/*` 迁到 1.5.1+26.2（表达式可能要按新字节码对一下）
- `handleAirspace` 播放路径接回去

没有 SPR 时保持现在这样静默跳过即可。

### 2.4 LRTactical / melee — 目标端已经内置

目标端 `fabric.mod.json`：

```json
"id": "tacz",
"provides": ["lrtactical"]
```

`me.xjqsh.lrtactical.item.MeleeItem`、`IMeleeWeapon`、`MeleeAttackHandler` 都在同一只 jar 里。原版还要按 0.3 / 0.4 分两套 mixin，这边 **只有一个版本**。

枪械近战这边更直：

```
ModernKineticGunItem.doMelee(LivingEntity, FFFFF, List)V
ModernKineticGunItem.doPerLivingHurt(LivingEntity, LivingEntity, FF, List)V
```

和原版同签名。原版用 `@Share` + `Suppliers.memoize` 只是为了「这次近战有没有打到生物」；26.2 一样可以：

- `@WrapOperation` 包住 `doPerLivingHurt`，打到就举手
- 或 `doMelee` 的 `@Inject(TAIL)`，没举手就走方块破坏

`MeleeInteractionManager` 现在只加载 JSON，`handleBlockInteraction` 没写。数据层已经齐了。

### 2.5 shield — 原版就不是靠 Forge 事件

`PORTING_NOTES` 写「依赖 Forge `ShieldBlockEvent`」。去看原版 `features/bullet_interactions/LivingEntityMixin.java`：

- wrap `LivingEntity.hurt`
- 改 `isDamageSourceBlocked` 里的 `BYPASSES_SHIELD`
- 自定义盾牌失效时长
- 跳过举盾 5 tick 冷却

Forge 事件最多是旁路。26.2 对应 `hurtServer` / `isDamageSourceBlocked` / `isBlocking`，全部还在。

数据层把 `BulletInteraction.Shield` 类型砍掉了，要恢复的是 codec + 这一只 mixin，不是去找一个不存在的 Fabric `ShieldBlockEvent`。

### 2.6 FakePlayer、TIER、Predicate、burst/pellet

| 砍掉的理由 | 事实 |
|---|---|
| Fabric 无 FakePlayer | 方块破坏已经用 `digSpeed/hardness*30`。要「用玩家工具速度」直接拿射手 `ServerPlayer`，不必造假人 |
| 无 `TierSortingRegistry` | 26.2 挖掘等级是 item `minecraft:tool` component + `#minecraft:incorrect_for_<tier>_tool`。`BlockTarget.TIER` 改成标签/component 匹配即可 |
| Predicate「已不存在」 | [26.2 变更](https://minecraft.wiki/w/Java_Edition_26.2)：实体谓词还在，只是 key 改成 Identifier，未知子谓词改为拒绝。`EntityPredicate.CODEC` 能接着用 |
| `burst_index` / `pellet_index` | 原版挂在 `EntityKineticBulletExtension` 上。本仓库扩展接口已经有 pierce 计数，再加两个 int 就行 |

### 2.7 匍匐 `visualTweak`

配置开着、界面也有，**没有 mixin**。原版 `crawl/PlayerRendererMixin` 30 行：强制走游泳位移、lerp Y/Z。26.2 `PlayerRenderer` 改名 `AvatarRenderer`，`setupRotations` 还在。和俯仰角那套已经打过的仗不是一类问题。

---

## 3. 真的做不了 / 不必做

只有这几条成立。

### 3.1 对面模组不存在（compat）

2026-08-18 检索结果：

| 模组 | Fabric 26.2 | 说明 |
|---|---|---|
| First Aid | 无 | 肢体伤害转发，没目标就没得 mixin |
| LSO（Legendary Survival Overhaul） | 无 | 同上 |
| MTS（Minecraft Transport Simulator） | 无 | 载具伤害修复 |
| Valkyrien Skies | 无（官方停在 1.20.1） | 物理船碰撞 / 爆炸，代码量还不小 |
| PillagersGun | 未见 26.2 | README 点过名，原版 `compat/` 目录里已经没有独立目录 |

这些不是「API 做不到」，是「没有注入目标」。哪天有人移植过来，再挂 `optional` mixin 即可。**现在 GUI 里还摆着 FirstAid / LSO / VS / MTS 四个开关，全是空操作。**

### 3.2 `thirdPersonGunRenderingFix`

目标端 `ItemInHandLayerMixin` 已按 26.2 extract→submit 重写，并修了左利手 / `isSelf`。开关留着开关都没效果。建议从界面拿掉，或改成只读说明。

### 3.3 被误伤的「技术细节」

这些是「这条具体技巧不行」，不要再写成功能不行：

- 给 `Identifier` record 加 `@Unique` 字段 — Mixin 会拒
- 直接 mixin 已删除的 `ProtectionEnchantment`
- 照搬 Forge `FakePlayer` / `ShieldBlockEvent` / `TierSortingRegistry` 的类名

---

## 4. 文档自己已经过时

README / `PORTING_NOTES` §8 还把已经落地的东西标成未做：

| 文档还在说 | 仓库现状 |
|---|---|
| 动态俯仰「暂未实现」 | `gun.MouseHandlerMixin` + `dynamicPitchLimit` 已做，还修过抖动 |
| 示例包 / 数据驱动「范围过大，暂缓」 | data 层 + 行为层 + `tacz-tweaks-example-pack/` 都在 |
| §8 待办还列着 betterInaccuracy / betterGunTilt / 卸弹枪膛 / endermen… | 这些 mixin 都已进 `tacztweaks.mixins.json` |
| `MouseHandlerMixin`「暂未移植」 | 已移植，而且改过三版才稳住 |

前几轮笔记自己也写过：「关键 API 修正（前几轮的『不可行』判断被推翻）」。这次这张表是同一类误判。

---

## 5. 建议开工顺序

按「收益 / 工作量」：

1. [x] **藏起来的两个开关做掉** — `betterMonoConversion`（`GunSoundInstance` + id 旁表 + `SoundBufferLibrary`）、`bulletProtection`（`EnchantmentHelper.getDamageProtection`）
2. [x] **界面有、逻辑没有** — `visualTweak`（`AvatarRenderer`，注入点 `require=0`）；compat 空开关与第三人称空开关从 GUI 拿掉
3. [x] **数据层已齐、差最后一跳** — melee `handleBlockInteraction` + `ModernKineticGunItem.doMelee` + 内置 LRTactical `IMeleeWeapon.performAttack`
4. [ ] **shield 类型加回 codec + `hurtServer` mixin**
5. [ ] **SPR 做成 optional mixin**，接回 airspace
6. [ ] **predicate / tier / burst+pellet** — 恢复数据包兼容，不挡主功能

第 1–3 项已在本轮落地。沙箱没有 JDK 25，未做 `./gradlew build` 实测。

---

## 6. 证据摘要

- 原版 mixin 清单与实现：<https://github.com/MUKSC/TaCZTweaks/tree/main/src/main/java/me/muksc/tacztweaks/mixin>
- `GunSoundInstance` / `doMelee` / `ModDamageTypes` / 伤害标签：本仓库 `libs/TACZ-Refabricated-26.2-1.1.8+fabric.26.2.R2.jar` 常量池与 `data/` 
- 26.2 实体谓词未删除：<https://minecraft.wiki/w/Java_Edition_26.2>
- SPR Fabric 26.2：<https://www.curseforge.com/minecraft/mc-mods/sound-physics-remastered/files/all>
- VS 仍停在 1.20.1：<https://www.curseforge.com/minecraft/mc-mods/valkyrien-skies/files/all>

# TaCZ Tweaks (Refabricated)

**Fabric 移植版 TaCZ Tweaks**，适配非官方 Fabric 移植
[`TaCZ_Refabricated_Unofficial`](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)（Minecraft **26.2** 分支）。

原项目：**[MUKSC/TaCZTweaks](https://github.com/MUKSC/TaCZTweaks)**（Forge 1.20.1 / TaCZ 1.1.8），
代码遵循 **GPL-3.0** 许可证发布。

> 本移植与 MUKSC 或 TACZ Dev Team 无任何从属/背书关系。

---

## 现状（WIP）

已完成、可编译的部分：

- ✅ 完整工程骨架：Fabric Loom 1.17 + Kotlin 2.4.10 + Java 25 + MixinExtras
- ✅ 配置系统（YACL v3）：六大配置组（gun / modifiers / crawl / compat / tweaks / debug）
  全部条目、JSON 持久化、客户端↔服务端同步、ModMenu 配置界面入口
- ✅ 网络层（Fabric Networking API）：配置同步、枪械卸弹、广播音效、滑铲状态 6 个数据包
- ✅ 按键绑定：卸弹（默认 U）、压枪、降低灵敏度
- ✅ 匍匐禁用（客户端 `LocalPlayerCrawlMixin` + 服务端 `LivingEntityCrawlMixin`）
- ✅ 水下禁射（`GunShootEvent`）
- ✅ 枪械卸弹（生存模式，走目标端已重写的 `dropAllAmmo`）
- ✅ **移动/射击 tweak**（18 个 mixin）：
  - 跑打（`shootWhileSprinting`）、换弹奔跑（`sprintWhileReloading`）、
    射击换弹（`reloadWhileShooting`）、射击中切换开火模式（`fireSelectWhileShooting`）、
    手动拉栓 + 换弹前先拉栓（`manualBolting`）、再次检视取消检视（`cancelInspection`）、
    换弹丢弃弹匣（`reloadDiscardsMagazine`）、禁用子弹剔除（`disableBulletCulling`）、持枪倾斜
- ✅ **平衡修饰器（全局倍率）**（12 个 mixin）：
  - 伤害 / 对玩家伤害 / 爆头 / 对玩家爆头 / 穿甲 / 弹速 / 重力 / 摩擦 / 举枪时间 /
    扩散（整体/站立/瞄准/移动/潜行/匍匐）/ 射速 / 后坐力（垂直/水平/瞄准/匍匐）
- ✅ **匍匐俯仰角限制**（`MouseHandlerMixin`：上限/下限；动态模式暂未实现）
- ✅ **音效与命中标记开关**（`SoundPlayManagerMixin`：抑制爆头/躯干/击杀音 + 强制默认音；
  `RenderCrosshairEventMixin`：隐藏命中标记）
- ✅ **降低灵敏度键实际生效**（`MouseHandlerMixin`，直接 wrap `LocalPlayer#turn`）
- ✅ **无尽弹药效果**（`EndlessAmmoStatusEffect` + `ModStatusEffects` + `LivingEntityAmmoCheckMixin`）
- ✅ **zh_cn 语言文件**（全部 173 条翻译）
- ✅ **更多 tweak**：
  - 末影人躲避子弹（`endermenEvadeBullets`）：`tweaks.EnderManMixin` + `tweaks.EntityKineticBulletMixin`
  - 冒险模式禁用改装（`disableRefitOnAdventure`）：`tweaks.RefitKeyMixin`
  - 显示 RPS 而非 RPM（`rps`）：`tweaks.RPMModifierMixin`
  - 始终按手持物品筛选（`alwaysFilterByHand`）：`tweaks.GunPackListMixin`
  - 共享枪声（`audibleFirstPersonGunSounds`）：`tweaks.SoundPlayManagerMixin` 的广播注入
- ✅ **匍匐动态俯仰角**（`dynamicPitchLimit`）：`gun.MouseHandlerMixin` 基于方块碰撞的动态下限
- ✅ **更好的扩散**（`betterInaccuracy`）：`tweaks.ModernKineticGunScriptAPIMixin` + `TaCZTweaks.getBetterInaccuracy`
- ✅ **更好的枪械倾斜**（`betterGunTilt`）：`gun.LocalPlayerMixin`（滑铲状态同步）+
  `tweaks.InaccuracyTypeMixin`（滑铲计为潜行扩散）+ 既有 `gun.GunAnimationStateContextMixin`
- ✅ **强制第一人称射击音**（`forceFirstPersonShootingSound`）：`tweaks.ModernKineticGunScriptAPIMixin`
- ✅ **完整卸弹**：`ClientMessagePlayerUnload` 自实现退弹逻辑——创造模式也退弹、支持卸出枪膛内子弹
  （`unloadBulletInBarrel`，基于 `IGun.hasBulletInBarrel/setBulletInBarrel`）

### 已确认不可移植、已从配置界面隐藏的选项

| 选项 | 原因 |
|---|---|
| `betterMonoConversion` | 26.2 里 `ResourceLocation` 改名 `Identifier` 且是 **record，无法 mixin 打标记** |
| `bulletProtection` | 26.2 已删除 `ProtectionEnchantment` 类（附魔系统改为数据驱动），需重新设计 |
| `thirdPersonGunRenderingFix` | **目标端已原生修复**，无需移植 |
| compat 组（FirstAid/LSO/MTS/VS/SoundPhysics/PillagersGun） | Forge 独占，Fabric 26.2 无对应版本 |
| 示例包 / 数据驱动子弹交互系统（debug 组） | 依赖数据加载子系统，范围过大，暂缓 |

详见 [`PORTING_NOTES.md`](PORTING_NOTES.md)。

---

## 依赖

| 依赖 | 版本 |
|---|---|
| Minecraft | 26.2 |
| Fabric Loader | >=0.19.3 |
| Fabric API | * |
| [UNOFFICIAL] TaCZ Refabricated | >=1.1.8（mod id `tacz`） |
| Fabric Language Kotlin | >=1.13.0 |
| YetAnotherConfigLib (YACL) | 3.9.6+26.2-fabric（mod id `yet_another_config_lib_v3`） |
| Java | >=25 |

## 构建

```bash
# 需要 JDK 25
./gradlew build
# 产物：build/libs/tacztweaks-2.14.2+fabric.26.2.R1.jar
```

> `libs/` 下的 `TACZ-Refabricated-26.2-*.jar`（compileOnly）与 `yacl-fabric.jar`
> 由仓库 owner 提供；yacl 也可换用 Modrinth maven 坐标。

## 许可

- 代码：GPL-3.0（继承原项目 MUKSC/TaCZTweaks）
- 原作者：MUKSC

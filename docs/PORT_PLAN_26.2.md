# 适配 TaCZ_Refabricated_Unofficial —— 分析与移植方案

> 状态：Phase 1 (构建) 完成；Phase 2 (机械改名) 第一批完成；剩余工作见 §7 与 §10。
> 改动前请先读本文件。

## 1. 结论速览

| 项目 | 结论 |
|---|---|
| 目标 | [q14433686-arch/TaCZ_Refabricated_Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial) |
| 选定版本 | **MC 26.2（Fabric）主分支**，`1.1.8+fabric.26.2.R2`（R2，2026-08-16 发布） |
| TaCZ 侧 API | ✅ 几乎 100% 保留（同 `0.7.0-forge1.1.8-hotfix` 源码） |
| 进展 | ✅ 构建系统脚手架完成；24 个 mixin/core 文件的 `ResourceLocation→Identifier` 覆盖完成 |

## 2. 目标仓库关键事实（已核对）

三条并行分支、mod id `tacz`、`provides: lrtactical`、`depends: fabricloader>=0.19.3, fabric-api, minecraft=26.2, java>=25, forgeconfigapiport>=26.2.1`。源码基线 = `0.7.0-forge1.1.8-hotfix`（与上游 upstream 1.21.1 分支 `mod_version` 一致）；不在任何 maven，**只能从 GitHub Releases 下载**。

**无混淆模式（26.1+）**：无 mappings、`modImplementation`→`implementation`、Loom 1.17+、关掉 legacy mixin AP、`Access Widener v2 official`、Java 25（Loom 1.17 把 plain `fabric-loom` 与 `fabric-loom-remap` 分工：plain = 无混淆 26.x+，remap = 混淆 1.21.x）。

## 3. 依赖可用性矩阵（26.2 Fabric）

| 依赖 | 26.2 版本 | 状态 |
|---|---|---|
| **TaCZ（unofficial）** | `1.1.8+fabric.26.2.R2` | ✅ GitHub Release → `libs/` + `scripts/fetch-tacz-26.2.sh` |
| Fabric API | `0.155.2+26.2` | ✅ maven.fabricmc.net |
| Fabric Loader | `0.19.3` | ✅（per-version overlay in `versions/26.2-fabric/`） |
| Fabric Language Kotlin | `1.13.13+kotlin.2.4.10` | ✅（per-version overlay） |
| YACL | `3.9.6+26.2-fabric` | ✅ Modrinth maven（2026-07-19） |
| Forge Config API Port | `26.2.1`（tacz 硬依赖） | ✅ Fuzss maven |
| Sound Physics Remastered（可选） | `fabric-1.5.1+26.2` | ✅ Modrinth maven |
| MixinSquared | `0.3.7-beta.1`（版本无关） | maven.bawnorton.com |
| Valkyrien Skies / Sable / FirstAid / LSO / PillagersGun / Cuffed / MTS / LRTactical | — | ❌ 无 26.2 构建 → 在 26.2 中排除（见 §6.4） |
| `fabric-loom` | `1.17-SNAPSHOT` | ✅ maven.fabricmc.net |
| `fabric-loom-remap`（旧目标用） | `1.17.19` 提供 ✅ | ✅ maven.fabricmc.net（不动旧目标的版本） |

## 4. 版本决策：26.2（已定）

主分支即 26.2、最新 R2、依赖全就绪、无混淆让 mixin 直接走官方名（TaCZ Tweaks 本来就 mojmap 写）。备选 1.21.11 / 26.1.2 同分析覆盖。

## 5. 构建改造（已完成 — Phase 1）

| 文件 | 改动 |
|---|---|
| `settings.gradle.kts` | 26.2 改用独立 buildscript `26.2-fabric.gradle.kts`（旧 `fabric.gradle.kts` 不动） |
| `buildSrc/.../multiloader-common.gradle.kts` | 加 per-MC-版本 overlay srcDirs（java/kotlin **前**插；resources **后**插）；`pack_format` 加 26.2 分支（值不写盘，文件被排除） |
| **`26.2-fabric.gradle.kts`（新建）** | plain `net.fabricmc.fabric-loom 1.17-SNAPSHOT`；tacz 用 `files("${jar}")`（带存在性 check）；runtimeOnly forgeconfigapiport；Loom `useLegacyMixinAp` 默认关；`tasks.processResources { exclude("pack.mcmeta") }` |
| `src/main/resources-fabric-26.2/tacztweaks.accesswidener`（新建） | `v2 official`；保留 `AbstractSelectionList$Entry` |
| `src/main/resources-fabric/fabric.mod.json` | 加 `"java": ">=${java_version}"`（对所有版本均正确） |
| `versions/26.2-fabric/gradle.properties` | java=25、mc=26.2、tacz=1.1.8+fabric.26.2.R2、loader=0.19.3、FLK=1.13.13、yacl=3.9.6+26.2-fabric、modmenu=20.0.1、SPR=fabric-1.5.1+26.2 |
| `scripts/fetch-tacz-26.2.sh`（新建） | 从 GitHub Release 拉 jar 到 `libs/` |
| `libs/README.md`（新建） | 解释取 jar 方式 |
| `.gitignore` | `libs/*` + `!libs/README.md` |

### 5.1 Loom 选择策略（重要）

- 旧 `1.20.1-fabric` / `1.21.1-fabric` 沿用既有 `fabric.gradle.kts`，根 classpath 上的 `fabric-loom-remap` 不动（已升版到支持的 `1.17.x` 即可，最稳妥仍是当前兼容版本；待 Phase 1 build 验证）。
- **26.2-fabric** 自身用独立的 buildscript 声明 `id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"`（无混淆流水线）。

这是按"重写 vs 共用 classpath 会冲突"取舍的结果。

### 5.2 AW + 资源

`src/main/resources-fabric-26.2/` 的 `tacztweaks.accesswidener` 通过 `loom.accessWidenerPath` 显式指向；源集 srcDirs 中 `resources-fabric-26.2` 排在 shared / fabric 之后，processResources 的 DuplicatesStrategy=INCLUDE 下后到者赢，故 `26.2` 文件最终覆盖前两者（旧版本对 jar 内位置无要求，因 `resources-fabric-26.2/` 仅 `minecraft.version == "26.2"` 项目存在）。待用户构建核对。

## 6. 源改造现状（Phase 2 部分完成）

### 6.1 覆盖目录约定（overlay convention）

新增 `src/main/{java,kotlin,resources}-fabric-26.2/`。**前两个前插 srcDirs（first-wins on java compile 冲突）**、**resources 后插（last-copies-wins on processResources）**。详见 `multiloader-common.gradle.kts`。

每个文件的决策：
- 26.2 与 1.20.1/1.21.1 行为完全一致 → 共享，不复制
- 26.2 需要重写 → 复制到 overlay 并手工编辑（**无 `//~` swaps**，因为 overlay 是 26.2-only）

### 6.2 已完成 24 个 overlay 副本（Phase 2 第一批，机械重命名 `ResourceLocation` → `Identifier`）

```
src/main/java-fabric-26.2/me/muksc/tacztweaks/mixin/accessor/SoundBufferAccessor.java
src/main/java-fabric-26.2/me/muksc/tacztweaks/mixin/feature/attribute/stats/damage/EntityKineticBulletMixin.java
src/main/java-fabric-26.2/me/muksc/tacztweaks/mixin/feature/attribute/stats/... (other EntityKineticBullet variants)
src/main/java-fabric-26.2/me/muksc/tacztweaks/mixin/feature/audio_and_visuals/... (broadcast/force/.../mix_to_mono 一组)
src/main/java-fabric-26.2/me/muksc/tacztweaks/mixin/feature/balancing/{friction,gravity,explosion_damage,player_damage,player_explosion_damage,player_headshot}/EntityKineticBulletMixin.java
src/main/java-fabric-26.2/me/muksc/tacztweaks/mixin/feature/datapack/(EntityKineticBulletMixin,bullet_sounds/EntityKineticBulletMixin).java
src/main/java-fabric-26.2/me/muksc/tacztweaks/mixin/feature/general/fixes/crashes/GunSmithTableScreenMixin.java
src/main/java-fabric-26.2/me/muksc/tacztweaks/mixin/feature/keyactions/unload/AbstractGunItemMixin.java
src/main/java-fabric-26.2/me/muksc/tacztweaks/mixin/feature/sound_physics_evaluate/SoundPhysicsMixin_1_5_x.java
src/main/java-fabric-26.2/me/muksc/tacztweaks/mixininterface/feature/audio_and_visuals/system/mix_to_mono/MonoObject.java
src/main/kotlin-fabric-26.2/me/muksc/tacztweaks/core/Identifier.kt  (26.2 完整重写，绕过 /*? 条件)
src/main/kotlin-fabric-26.2/me/muksc/tacztweaks/feature/audio_and_visuals/system/StereoToMonoMixer.kt
src/main/kotlin-fabric-26.2/me/muksc/tacztweaks/core/compatibility/ModCompatibilityManager.kt  (26.2 缩短为 仅 SoundPhysicsManager)
```

`TcZTweaks/feature/general/compatibility/`（除 SoundPhysicsManager 之外的所有管理器 & 与 Sable/VS/LRTactical 相关的 mixin）在 `26.2-fabric.gradle.kts` 中以 `sourceSets.main.{java,kotlin}.exclude(...)` 排除 —— 这些 mod 在 26.2 没有可用构建。

### 6.3 关键 overlay 内容（与旧版差异）

| 文件 | 26.2 overlay 关键改动 |
|---|---|
| `core/Identifier.kt` | 26.2 重写为不用 `/*?` 条件：`fun Identifier(ns,path) = Identifier.fromNamespaceAndPath(...)` |
| `MonoObject.java` | 26.2 重写为 `MonoObject.class.cast(instance)`（1.21+ 分支） |
| `ModCompatibilityManager.kt` | `ALL` 砍至 `listOf(SoundPhysicsManager)` |
| `*EntityKineticBulletMixin.java` (8 个) | 把 `<init>` 描述符里的 `Lnet/minecraft/resources/ResourceLocation;` → `Identifier`；datapack 那个还修了 `@Inject` 方法参数类型 |
| `GunSoundInstanceMixin` | `<init>` 描述符 `ResourceLocation`→`Identifier` |

类名一律保留（如 `ResourceLocationMixin.java` 文件名、`public class ResourceLocationMixin` 都没动 —— mixins.json 是用类名路径注册的，重命名会引出一波配置改动，无收益）。

### 6.4 暂未做且需在 Phase 3-6 处理

（留给后续 turn；详见 §7）

## 7. 剩余工作清单（按本文档继续接龙）

### Phase 3 音频系统
- `SoundEngineMixin`（mix_to_mono）依赖 26.2 vanilla `SoundBufferLibrary.getCompleteBuffer(Identifier)` 调用点，已在 overlay 副本里改名为 `Identifier`；**编译/运行时需验证**：26.2 SoundEngine.play 是否仍调用 `SoundBufferLibrary.getCompleteBuffer` 及 `SoundBuffer` 字段 `data`/`format` 存活、构造器 `(ByteBuffer, AudioFormat)`（已知 port 注释表明 `SoundBuffer` 存在 & `ChannelHandle.execute` 行为不变）。
- `ObjectAnimationSoundChannelMixin`（broadcast）涉及 TaCZ `ObjectAnimationSoundChannel` 的 26.2 差异（已确认类存在，方法签名需逐点核对）。
- `SoundPlayManagerMixin` ×3 涉及 TaCZ `SoundPlayManager.playClientSound/playAnimationSound/...` 签名（已在 26.2 port diff 里确认 `ResourceLocation→Identifier`，mixin 描述符已改；运行时验证）

### Phase 4 渲染 / GUI / 视角
```
src/main/.../mixin/feature/audio_and_visuals/visuals/hide_hit_markers/RenderCrosshairEventMixin.java   # GuiGraphics→GuiGraphicsExtractor，渲染改为 extractRenderState
src/main/.../mixin/feature/audio_and_visuals/visuals/smoother_crawl_animation/PlayerRendererMixin.java # PlayerRenderer→AvatarRenderer，setupRotations 目标方法需重新定位
src/main/.../mixin/feature/disarm/GunItemRendererWrapperMixin.java                                       # TaCZ GunItemRendererWrapper 369→866 行大改写
src/main/.../mixin/feature/general/fixes/third_person_gun_rendering_fix/ItemInHandLayerMixinMixin.java  # MultiBufferSource 移除 + MuzzleFlashRender/ShellRender/HumanoidOffhandRender 改写
src/main/.../mixin/feature/general/fixes/crashes/GunSmithTableScreenMixin.java                          # 部分 console rendering API 变化（已部分 overlay；可能还需调试）
src/main/.../mixin/feature/general/misc/dynamic_attachment_slots/GunRefitScreenMixin.java              # API 验证后 overlay
src/main/.../mixin/feature/gameplay/behaviour/bullet_protection/ProtectionEnchantmentMixin.java        # 26.2 ProtectionEnchantment 类名/包名/符号（? — 需编译验证）
src/main/.../mixin/feature/gameplay/behaviour/endermen_evade_bullets/EnderManMixin.java                 # 26.2 Enderman 类名 (? — 大概率 Enderman)
src/main/.../mixin/feature/general/fixes/attachment_compatibility_check_fix/GunSmithTableScreenMixin.java # GUI 提取 API
src/main/.../mixin/feature/effect/CrippledAttributeMixin / DisarmEffect ... 看情况
src/main/.../mixin/feature/attribute/AttachmentPropertyManagerMixin / ...（校验 0.7.0 vs 0.6.1 行为）
```

### Phase 5 数据包 / 命令 / 网络 / 工具类
```
src/main/kotlin/me/muksc/tacztweaks/TaCZTweaks.kt, ..., core/Identifier.kt 已完成, 剩余:
TaCZTweaks.kt   # ComponentUtils / metadataHook 等
client/input/ReduceSensitivityKey.kt   # ToggleKeyMapping 26.2 行为 (?); 可能在 26.2 改为普通 KeyMapping + isDown
client/input/TiltGunKey.kt             # 同上
command/admin/RefillAmmoCommand.kt    # commands.arguments EntityArgument（26.2 重构 ?）
core/codec/Collection.kt               # StreamDecoder/Encoder 包/类 (? 需验证)
core/codec/JsonCodecs.kt               # 用 MC Codec 体系，部分重构
core/codec/MinecraftCodecs.kt          # 同上
core/codec/StreamCodec.kt              # 同上
core/codec/TierSortingRegistryCodec.kt # item.Tier / record ref (? — 26.2 Tier 可能是 TagKey)
core/codec/Util.kt                     # datafixers.util.Either (?); Pair (relies on DFU, port 已删 Pair)
core/extension/IdentifierExt.kt        # Identifier extension (已 partly 通过 overlay Identifier.kt 处理)
core/extension/ItemPredicateExt.kt     # adv critereon.ItemPredicate 26.2 重构 (?)
core/network/CustomPacketPayload.kt    # StreamCodec 重构
core/registry/DeferredHolder.kt        # datafixers.util.Either
core/resource/IdentifiableResourceReloadListener.kt  # PreparableReloadListener / SimpleJsonResourceReloadListener 26.2 重命名 (?)
core/tacz/GunStack.kt                  # 工具类 — 看 TaCZ API 差异
feature/audio_and_visuals/system/StereoToMonoMixer.kt # Identifier rename + 实现细节
feature/datapack/legacy/**             # bullet interactions 旧协议，使用 datafixers/BlockPredicate/loot 在 26.2 大改 — 需要重写 codec
feature/datapack/BulletInteraction.kt  # 新协议 — 需用 0.7.0 TaCZ 字段
feature/datapack/core/NumberRange.kt   # ExtraCodecs (?)、NumberProvider (?) 在 26.2 重构
feature/datapack/legacy/core/BlockTarget/EntityTarget/Target/*  # BlockPredicate/EntityPredicate 26.2 重写
feature/datapack/legacy/manager/BaseDataManager.kt 等
feature/datapack/legacy/manager/BulletInteractionManager/BulletParticlesManager/BulletSoundsManager/MeleeInteractionManager.kt
feature/gameplay/behaviour/BoatRowing.kt    # world.entity.vehicle.Boat → AbstractBoat/Boat 重命名 (?)
feature/general/compatibility/LRTacticalManager.kt   # 已 excluded by sourceSets exclude
feature/raytracer/BulletRayTracer.kt   # util.block.BlockRayTrace.API ✓ (26.2 类存在); EntityCollisionContext (?)
network/message/ClientMessageBroadcastSound.kt / ServerMessage*.kt   # record accessor （entityId()→getEntityId() 等）和 ComponentUtils / ClientboundSoundPacket (?)
registry/ModRegistries.kt              # ComponentUtils / Codec / critereon.Predicate
```

### Phase 6 配置 / 收尾 + 验证
- 26.2 专用 `tacztweaks.mixins.json`（从我已 overlay 与共享保留的 mixin 中筛出可用集合；驱动 `ModMixinPlugin.shouldApplyMixin`）
- README / CHANGELOG 更新（声明支持情况）
- 测试矩阵：上古版本构建（1.20.1/1.21.1）+ 26.2 + R3（如果上游发布）

## 8. UNVERIFIED 列表（首次跑 build 会暴露的项）

下列项需要把仓库克隆到能跑 Gradle/Java 的环境（**本沙盒无 JDK 且非 GitHub 网络被限制**），跑一次 `./gradlew :26.2-fabric:build` 看错误再补齐：

- `net.minecraft.client.ToggleKeyMapping` 在 26.2 是否仍在，沿用同名 / 内部被基类 `KeyMapping` 吸收
- `net.minecraft.world.entity.monster.EnderMan` 在 26.2 名称（极可能是 `Enderman`，因 Mojang 在 26.x 大批重命名）
- `net.minecraft.world.entity.projectile.windcharge.WindCharge` 在 26.2 名称
- `net.minecraft.world.entity.vehicle.Boat` 在 26.2 名称
- `net.minecraft.world.item.Tier` / `Tiers` 在 26.2 名称（很可能改为 `Tier` 记录 + TagKey 处理）
- `net.minecraft.world.item.enchantment.ProtectionEnchantment` 在 26.2 名称
- 数据包 codecs（`BlockPredicate`, `EntityPredicate`, `NumberProvider`, `LootItemCondition`, `Deserializers`, `LootContextParam`）在 26.2 重构
- 命令参数（`BlockInput`, `BlockStateParser`, `coordinates.*`）在 26.2 重构
- 网络 `StreamDecoder`, `StreamEncoder`, `ClientboundSoundPacket`, `ConfigurationTask`, `ComponentUtils`, `HolderOwner` 在 26.2 重构
- 资源加载 `SimpleJsonResourceReloadListener`、loot `SimpleJsonResourceReloadListener` 在 26.2 名称
- Raytrace's `EntityCollisionContext` 在 26.2 位置
- 音频：`SoundBufferLibrary.getCompleteBuffer` 是否仍在 26.2 `SoundEngine.play` 内被调用、`SoundBuffer(ByteBuffer, AudioFormat)` 构造器和 `data`/`format` 字段存活
- MixinSquared 0.3.7-beta.1 在 Java 25 + Fabric Loader 0.19.3 + Loom 1.17-SNAPSHOT（无 Mapping）下行为正常

---

## 9. 本机/沙盒构建限制

本沙盒内：
- 无 JDK
- 除 GitHub（git/gh + 部分 API）外的网络（maven.fabricmc.net / Modrinth maven / Sonatype 等）被拦截

→ **Phase 1 验证（`./gradlew :26.2-fabric:build`）必须在用户本地或 CI 执行**。建议先在 GitHub Actions 拉一个 26.2-fabric 矩阵 job：

```yaml
strategy:
  fail-fast: false
  matrix:
    version: [1.20.1-forge, 1.21.1-neoforge, 1.20.1-fabric, 1.21.1-fabric, 26.2-fabric]
runs-on: ubuntu-22.04
steps:
  - uses: actions/checkout@v4
  - uses: actions/setup-java@v4
    with: { distribution: temurin, java-version: ${{ matrix.version == '26.2-fabric' && '25' || matrix.version == '1.21.1-neoforge' && '21' || '17' }} }
  - run: chmod +x scripts/fetch-tacz-26.2.sh && (matrix.version == '26.2-fabric' && bash scripts/fetch-tacz-26.2.sh || true)
  - run: ./gradlew :${{ matrix.version }}:build
```

## 10. 下一步

按 §7 的 Phase 3→4→5→6 顺序继续；每个 Phase 在你本地跑一次 26.2 build，按 §8 的 UNVERIFIED 列表对症补齐。Phase 1 的初始 commit（`docs: analysis + scaffold for TaCZ_Refabricated_Unofficial (26.2-fabric) port`）已包含分析文档 + 脚手架。本 turn 的 commit（`feat(26.2-fabric): Phase 1 build config + Phase 2 batch-A rename overlays`）包含构建系统改造 + 24 个 rename overlay。

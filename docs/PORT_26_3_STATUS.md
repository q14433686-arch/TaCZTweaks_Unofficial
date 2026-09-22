# 26.3 移植状态与待办

> 建立日期：2026-09-22。适用分支：`26.3`（Fabric）。
> 基线：本仓 `26.2(main)` @ `88738fb` → 目标 TaCZ `q14433686-arch/TaCZ_Refabricated_Unofficial` 分支 `26.3`
> （发行版 `26.3_R1`，`1.1.8+fabric.26.3.R1`，2026-09-20 发布）。

## 0. 先读这个：本轮到底做了什么、没做什么

**已经做到的（有 CI 实证）**

| 项 | 状态 | 证据 |
|---|---|---|
| 四条 CI 流程 | ✅ 全绿 | run `cb516a2` |
| 依赖坐标全部 26.3 化 | ✅ | manifest 两行均已 pinned |
| Java + Kotlin 编译 | ✅ 通过 | `compileJava` / `compileKotlin` |
| `./gradlew build` 出包 | ✅ 通过 | 产物已上传为 artifact |
| 静态审计（**含真实 26.3 Minecraft jar**） | ✅ 0 error | `AUDIT(minecraft): OK` |
| 实机启动 | ⚠️ 已修两处启动崩溃（§2.6 FLK 区间、§2.7 版本闸门），**待你复测** | 用户实测日志 |
| 发布一致性检查 | ✅ 通过 | `check_release_consistency.py --jar` |

**明确还没做的**

- ❌ **游戏内实测（客户端 / 集成服 / 独立服三件套）一次都没跑过。**
  静态审计只证明「符号存在」，不证明「行为正确」——
  `@Local(ordinal=N)` 绑错变量、`@ModifyArgs` 命中错调用，这两类问题它都看不出来。
  按仓库规矩，三项矩阵没跑完之前 PR 保持 Draft。
- ❌ §4 各条的**语义**复核（静态部分已全过，见 §4 顶部）。
- ❌ `audit.yml` 步骤名「0 error / 0 warning required」名不副实（warning 不影响退出码），
  需要你手工改 workflow。

## 1. 依赖坐标（已改，CI 已证实可解析）

| 项 | 26.2 旧值 | 26.3 新值 | 依据 |
|---|---|---|---|
| Minecraft | 26.2 | `26.3` | TaCZ 26.3 `gradle.properties` |
| Fabric Loader | 0.19.3 | `0.19.5` | 同上 |
| Fabric API | 0.155.2+26.2 | `0.160.7+26.3` | 同上；GitHub 上该 tag 存在 |
| TaCZ | `1.1.8+fabric.26.2.R2` | `1.1.8+fabric.26.3.R1` | release `26.3_R1` 资产 |
| YACL | 3.9.6+26.2-fabric | `3.9.7+26.3-fabric` | Modrinth 版本 `s9SjoFu1`（2026-09-20 发布，标 26.3） |
| ModMenu | 20.0.1 | `21.0.0-beta.1` | 26.3 目前只有 beta 通道，与 TaCZ 同口径 |
| Kotlin / FLK | 1.13.13+kotlin.2.4.10 | **1.14.1+kotlin.2.4.20** | ⚠️ 起初误判为「未动」，实机启动即崩（见 §2.6）。FLK **确实**按 MC 版本打标签：Modrinth 上只有 1.14.1 带 26.3 标签 |
| MixinExtras | 0.5.4 | **未动** | 无 26.3 相关变更证据 |

**YACL 校验和是占位符**：`RESOURCE_IMPORT_MANIFEST.tsv` 里那一行写的是 `UNVERIFIED_PENDING_CI`。
Modrinth 只公布 sha1/sha512，本沙箱拿不到文件本体算 sha256。
第一次 CI 会用 `--print-sha256` 打印真实值，回填后把占位符换掉。
（TaCZ 那一行的 sha256 是真的，取自 GitHub release 资产的 `digest` 字段。）

---

## 2. 已适配的 API 破坏（✅ = 有 26.3 源码实证）

### ✅ 2.1 `EntityRenderer#shouldRender` 新增 `float partialTicks`

`EntityBulletRendererMixin` 的 `@Inject` 形参表尾部补 `float partialTicks`。

证据：TaCZ 26.3 `EntityBulletRenderer.java:379-385` 明确写了
「26.3: EntityRenderer#shouldRender 尾部新增 float partialTicks……签名必须对上才算覆写」。

> 这条如果不改，`@Inject` 在 APPLY 阶段直接找不到方法；因为 `defaultRequire=1`，会是
> 启动崩溃而不是静默失效 —— 属于「好发现」的那一类。

### ✅ 2.2 `SoundInstance#resolve` → `getOrResolve`

`GunSoundInstanceMixin` 的注入目标方法名改为 `getOrResolve`。

证据：TaCZ 26.3 `GunSoundInstance.java:64-67` 的注释与 `@Override public WeighedSoundEvents getOrResolve(...)`。

### ✅ 2.3 `InputConstants` 键位常量改名

三个按键类：`Type.KEYSYM` → `Type.KEYBOARD`，`GLFW.GLFW_KEY_U` → `InputConstants.KEY_U`，
删掉 `org.lwjgl.glfw.GLFW` import。

证据：TaCZ 26.3 `RefitKey.java` 的 diff（`KEYSYM`+`GLFW_KEY_Z` → `KEYBOARD`+`KEY_Z`）、
`ReloadKey`/`InspectKey`/`CrawlKey` 等同款写法。
`InputConstants.UNKNOWN` **仍然存在**（ModMenu v21.0.0-beta.1 与 CustomPlayerModels 26.3 都在用），
所以 `TiltGunKey` / `ReduceSensitivityKey` 的 `UNKNOWN.getValue()` 保持不变。

### ✅ 2.4 `FriendlyByteBuf#write/readCollection` 被移除

`Config.kt` 里 `reloadDiscardsMagazineExclusions` 的 encoder/decoder 用了这两个方法，
26.3 把它们连同 `write/readMap` 一起从 `FriendlyByteBuf` 删了。
新增 `config/sync/BufCollectionCodec.kt` 顶上，**线格式与 26.2 完全一致**
（varint 条目数 + 逐条元素），所以同步协议没有任何变化。
顺带删掉了只为这一行存在的 `com.google.common.collect.Lists` import。

证据来源与其它条目不同，**这条是 CI 编译器报的**，不是比对源码猜的：

```
e: .../config/Config.kt:70:43 Unresolved reference 'writeCollection'.
e: .../config/Config.kt:71:36 Unresolved reference 'readCollection'.
```

（run `35702835805`，日志由 `compile-check` 回推至 `build-reports/compile-java.log`。）
同批次还有 `writeUtf`/`readUtf` 方法引用的重载歧义报错，改成显式 lambda 后一并消失。
TaCZ 26.3 侧用 `cn.sh1rocu.tacz.util.BufMapCodec` 解决了 map 那一半，做法与此一致。

### ✅ 2.5 `EnderMan` → `Enderman`（类名改了大小写）

`net.minecraft.world.entity.monster.EnderMan` 在 26.3 改名为 `Enderman`（**包没变**，只是
第二个词的 `M` 变小写）。`EnderManMixin.java` 的 import 和 `@Mixin(...)` 已同步。
mixin 类自身的文件名保持 `EnderManMixin` 不变，这样 `tacztweaks.mixins.json` 不用动。

证据：CI 编译器报错（run `35703300829`）

```
EnderManMixin.java:10: error: cannot find symbol
import net.minecraft.world.entity.monster.EnderMan;
  symbol:   class EnderMan
  location: package net.minecraft.world.entity.monster
```

加上 NeoForged 26.3 移植指南的 `net.minecraft.world.entity.monster` 小节明确写着
`EnderMan` -> `Enderman`, not one-to-one（<https://docs.neoforged.net/primer/docs/26.3/>）。
同包的 `Vex` 没报错，可见包路径本身没变。

> ⚠️ **注意 "not one-to-one"**：指南这句话意味着改的不只是名字。本轮只做了「让它能编译」，
> `hurtServer` 里第一个 `DamageSource#is(TagKey)` 是否仍是 `IS_PROJECTILE` 判断
> **尚未在 26.3 上验证**，留待 §6 第 5 步的 `--minecraft-jar` 审计与实机确认。
> 这条注入即使静默失效也只是「末影人不再躲子弹」，不会崩游戏。

---

### ✅ 2.6 FLK 版本区间写错，导致游戏直接启动不了（用户实测发现）

**这是本次移植唯一一个「CI 全绿但游戏根本起不来」的问题，也是最严重的一个。**

```
Mod 'TaCZ Tweaks (Refabricated)' 需要 'Fabric Language Kotlin'
从 1.13.13（含）到 1.14.0（不含）的任意版本，但已经安装了的版本 1.14.1+kotlin.2.4.20 不对！
```

**根因**：我升级依赖坐标时，把 minecraft / loader / fabric-api / tacz / yacl 都改成了 26.3，
**唯独漏了 `flk_version`** —— 它从 26.2 基线原样继承下来。更糟的是我还在本文档里写下了错误结论：
「FLK 未声明与 MC 绑定；如 CI 报错再升」。**这个判断是错的**，而且 CI 永远不可能报错。

查 Modrinth 的实际发布情况：

| FLK 版本 | 打了 26.3 标签？ |
|---|---|
| **1.14.1+kotlin.2.4.20** | ✅ **只有这一个** |
| 1.14.0+kotlin.2.4.20 | ❌ 到 26.2 为止 |
| 1.13.14+kotlin.2.4.20 | ❌ 到 26.2 为止 |
| 1.13.13+kotlin.2.4.10 | ❌ 到 26.2 为止（我们声明的就是它） |

我们声明 `>=1.13.13 <1.14.0`，26.3 上唯一可用的是 `1.14.1` ——
**交集为空**，任何人装上都必崩，100% 复现。

**修复**：

- `fabric.mod.json`：`>=1.13.13 <1.14.0` → `>=1.14.1+kotlin.2.4.20 <2.0.0`
- `gradle.properties`：`flk_version` → `1.14.1+kotlin.2.4.20`
- `build.gradle.kts`：Kotlin 插件 `2.4.10` → **`2.4.20`**（与 FLK 1.14.1 内置的 Kotlin 对齐，
  否则我们的类会按 2.4.10 的 stdlib ABI 编译，却在 2.4.20 的 stdlib 上运行）
- README / COMPATIBILITY / CHANGELOG 的依赖表同步

**为什么四条 CI 全绿却没拦住**：编译、mixin 静态审计、打包**都不读 `depends` 区间** ——
这个区间只有 Fabric loader 在**运行时**才会执行。这正是「编译通过 ≠ 能玩」的教科书案例。

**补的门禁**（见 §6 第 6 步）：新增 `scripts/check_dependency_availability.py`，
直接问 Modrinth「你给 26.3 发了哪些版本」，再判断我们声明的区间能不能命中其中之一。
已用真实 API 数据验证：**对当前仓库通过，对当初那个区间报错退出 1**。

---

### ✅ 2.7 运行时 TaCZ 版本闸门仍写死 26.2，进游戏即崩（用户第二次实测发现）

修好 FLK 之后再启动，崩在另一处 —— **同一类错误的第二个实例**：

```
java.lang.IllegalStateException: TaCZ Tweaks requires TaCZ 1.1.8+fabric.26.2.R2
or a later R<n> build for Minecraft 26.2, found 1.1.8+fabric.26.3.R1
    at me.muksc.tacztweaks.TaCZTweaks.onInitialize(TaCZTweaks.java:75)
```

**根因**：`TaCZTweaks.java` 里有一道硬编码的运行时闸门，我全程没动过它：

| 常量 | 改前 | 改后 |
|---|---|---|
| `SUPPORTED_TACZ_VERSION` | `1.1.8+fabric.26.2.R2` | `1.1.8+fabric.26.3.R1` |
| `SUPPORTED_TACZ_VERSION_PREFIX` | `1.1.8+fabric.26.2.R` | `1.1.8+fabric.26.3.R` |
| `MIN_SUPPORTED_TACZ_REVISION` | `2` | **`1`** |

最后一行是关键：26.2 那条线要求 ≥R2（因为它的 R1 早于本模组需要的 API），
但 **26.3 就是以 R1 发布的**。只改前缀、不改下限，照样起不来。

**这次为什么连单元测试都没拦住**：`TaCZTweaksVersionTest.kt` 里写着
`assertFalse(isSupportedTaczVersion("...26.2.R1"))` —— 它把 26.2 的期望固化了下来，
包括「R1 必须被拒绝」。**一个断言了错误期望的测试，比没有测试更糟**：
它让 `./gradlew build` 一路绿灯，反而给了「已验证」的错觉。该测试已重写为对
`SUPPORTED_TACZ_VERSION` 本身求值，不再手抄版本号。

**补的门禁**：`check_release_consistency.py` 新增 `check_runtime_version_gate()`，
从 Java 源码里解析这三个常量，验证 `fabric.mod.json` 声明的 tacz 版本**能通过这道闸门**。
两种失效都验证过会被拦下（家族写错、下限过高），消息里直接点明
「The mod would refuse to start against its own declared dependency」。

> 🔑 **两次崩溃的共同教训**：真正危险的不是编译错误，而是**只在运行时才生效的约束** ——
> `depends` 区间（§2.6）和这道版本闸门（§2.7）都属于此类。
> 编译、mixin 静态审计、打包**三者都不读它们**。这类约束必须有专门的门禁去交叉验证，
> 否则「四条 CI 全绿」只能证明代码编得过，证明不了游戏起得来。

---

---

## 3. 交叉核对结论：我们碰的 TaCZ 类里，26.3 改了哪些

把「本仓 `src/` 里 import 的 71 个 `com.tacz.*` 类」与「TaCZ `26.2(main)...26.3` 的 152 个改动文件」
求交集，只有 **7 个**命中：

| TaCZ 类 | 26.3 改了什么 | 对我们的影响 | 状态 |
|---|---|---|---|
| `client.renderer.entity.EntityBulletRenderer` | `shouldRender` 加 `float partialTicks` | 直接破坏 | ✅ 已改（§2.1） |
| `client.sound.GunSoundInstance` | `resolve` → `getOrResolve` | 直接破坏 | ✅ 已改（§2.2） |
| `client.input.RefitKey` | `InputConstants` 改名 | 我们的 `RefitKeyMixin` 打在 `onRefitPress` 上，**方法名与参数未变** | 🔧 应无需改动 |
| `client.event.CameraSetupEvent` | 只改了 `KeepingItemRenderer.getRenderer().getCurrentItem()` → `getCurrentRenderItem()` | 我们的 `CameraSetupEventMixin` 打的是 `initialCameraRecoil`，该方法体内 `getCrawlRecoilMultiplier` / `getClientAimingProgress` / `genPitchSplineFunction` / `genYawSplineFunction` **四个注入点全在**（26.3 源码 189-232 行逐行核对） | 🔧 应无需改动 |
| `client.gameplay.LocalPlayerDraw` | `doPutAway` 里给 `getRenderer()` 加 null 检查 | 我们只注入 `resetData`（26.3 仍在，134 行） | 🔧 应无需改动 |
| `client.gui.GunSmithTableScreen` | 开链接改 `Blaze3D.openUri(URI)` | 我们注入 `isSuitableForMainHand`（220 行，内含 `allowAttachment`）与 `mouseScrolled`，并 `@Shadow` `selectedRecipeList`（81 行，仍是 `List<Identifier>`） | 🔧 应无需改动 |
| `entity.EntityKineticBullet` | `invulnerableTime = 0` → `DamageCooldownUtil.clear(...)` | 我们三个 mixin 打的是 `<init>` / `onBulletTick` / `getDamage` / `onHitEntity` / `createDamageSources`，**都不碰那两行**；`onHitEntity`、`tacAttackEntity`、`createDamageSources` 在 26.3 仍在（401 / 590 / 573 行） | 🔧 应无需改动 |

**其余 64 个我们 import 的 TaCZ 类，26.3 一个字节都没动。** 这是本轮最值得记下的结论：
我们的 mixin 面主要打在 TaCZ 的逻辑层（modifier / shooter / resource pojo），而 26.3 的改动
几乎全部集中在渲染层（`Scope*`、`Iris*`、`PolyMesh*`、第一人称拆分），那些类我们一个都没碰。

---

## 4. 还没验证的（按风险从高到低）

> ### ✅ 本节 4.1–4.4 的**静态**部分已于 2026-09-22 全部通过
>
> `build.gradle.kts` 新增的 `auditAgainstMinecraft` 任务会把 Loom 拉下来的真实
> Minecraft jar 喂给 `audit_port.py --strict`。CI（`3b2c88f`）日志确认它**确实跑到了**、
> 而不是走「找不到 jar」的跳过分支：
>
> ```
> > Task :auditAgainstMinecraft
> AUDIT(minecraft): using minecraft-merged-a1f5b1e0f5-26.3.jar
> AUDIT: 0 error(s), 1 warning(s)
> AUDIT(minecraft): OK
> ```
>
> 也就是说，下面 4.1–4.4 里所有「类名/方法名/descriptor 在 26.3 上还存不存在」的疑问
> **都已被证伪为「存在」**，包括 4.1 那条硬编码 descriptor。唯一那条 warning 是
> `LivingEntityMixin` 里 `remap=false` 的可选目标，属于**预期行为**，
> 由 `scripts/test_audit_optional_targets.py` 专门锁死（见下方注）。
>
> ⚠️ **但静态通过 ≠ 行为正确。** 审计只比对符号是否存在，**不检查语义**。
> `@Local(ordinal = 2)` 取的是不是原来那个插值量、`@ModifyArgs` 命中的是不是原来那次
> `translate` 调用 —— 这类「顺序/身份」问题静态审计看不出来，**仍必须实机确认**（§6 第 6 步）。
>
> 📌 **一处名不副实**：`audit.yml` 里那一步叫「0 error / 0 warning required」，但
> `audit_port.py` 只在 **error** 非空时返回 1，warning 不影响退出码（见 `main()` 末行
> `return 1 if errors else 0`）。所以当前这条 warning 是被容忍的。步骤名与实际门禁
> 不一致，建议改名为「0 error required」，或给脚本加 `--fail-on-warning` 后在
> workflow 里显式豁免这条已知项。**机器人无 `workflows` 权限，需你手改。**


### ❓ 4.1 原版侧 mixin：`AvatarRendererMixin`（风险最高）

`crawl/AvatarRendererMixin` 硬编码了一个 descriptor：

```
setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V
```

并且 `@ModifyArgs` 打在 `PoseStack;translate(FFF)V` 上、`@Local(ordinal = 2)` 取插值量。
26.3 把 `com.mojang.blaze3d.*` 大面积搬到 `com.mojang.renderpearl.*`，**但 `PoseStack` 明确
留在 `blaze3d`**（移植指南 §2.1 原话：「`PoseStack`、`Blaze3D`、`InputConstants`、
`GraphicsResourceAllocator` 仍在 `blaze3d`」）。所以 descriptor **大概率**仍然成立。

风险在于 `setupRotations` 的形参表或 `translate` 调用次序是否被 Mojang 动过 —— 这个
**只能靠 Loom 拉下 26.3 的 Minecraft jar 后用 `--minecraft-jar` 审计**，CI 里可以做（见 §6）。
`@Local(ordinal = 2)` 这种按序号取局部变量的写法对版本变化最敏感。

### ❓ 4.2 `SoundBufferLibraryMixin`：lambda 名与 `blaze3d.audio`

它 `@Invoker("lambda$getCompleteBuffer$1")` 且 `@ModifyArgs` 打在
`Lcom/mojang/blaze3d/audio/SoundBuffer;<init>(...)`。两个隐患：
1. **lambda 合成名**随编译产物变化，原版方法体一改就会错位；
2. `blaze3d.audio` 是否随 renderpearl 迁移未知（移植指南只列了渲染相关子包，音频未提及，
   且 TaCZ 自身不碰这个类，无从旁证）。

### ❓ 4.3 `RenderCrosshairEventMixin` 的 `Window` 形参

我们的注入形参是 `(GuiGraphicsExtractor graphics, Window window, CallbackInfo ci)`，
`Window` 来自 `com.mojang.blaze3d.platform.Window`。26.3 TaCZ 侧
`RenderCrosshairEvent.java:4` 仍 `import com.mojang.blaze3d.platform.Window`，
且 `renderHitMarker(GuiGraphicsExtractor, Window)` 签名未变 —— 这条**大概率安全**，
列在这里是因为它依赖原版类未迁移这一前提。

### ❓ 4.4 其它

- `MouseHandlerMixin` 打 `handleAccumulatedMovement` / `turnPlayer`；
- `EnchantmentHelperMixin`、`EnderManMixin`、`LivingEntity*` 等原版目标；
- `net.minecraft.client.gui.GuiGraphicsExtractor`、`KeyMapping.Category.register`、
  `player.permissions()`、`ResourceLoader` v1 等 26.2 期写法在 26.3 是否仍然成立。

以上全部要等 CI 编译 + `--minecraft-jar` 审计才能定性。

---

## 5. 明确**不适用**于本仓的 26.3 改动

TaCZ 的 26.3 移植量很大（130 文件 +2940/−1033），但绝大部分与本仓无关，别照抄：

- `blaze3d` → `renderpearl` 包迁移、`Scope*` 全家、PIP/掩码/高模、Iris 适配、shaderc/SPIR-V
  着色器方言 —— **本仓没有自定义渲染管线，也没有 GLSL**（只有两个 First Aid 的 post shader
  覆盖文件，不属于 26.3 渲染栈）；
- 第一人称 `ItemInHandRenderer` 拆分 —— 本仓不注入第一人称渲染；
- 战利品表 schema、`Block#codec()` 删除、`PushReaction.POPPED` —— **本仓不注册任何方块**；
- `FriendlyByteBuf#readMap/writeMap` 移除 —— 本仓的 payload 没用 map 编解码；
- `swing(hand, SwingAnimation, boolean)`、`invulnerableTime` 私有化 —— 本仓不调这两处；
- 配方同步 / JEI —— 本仓不注册配方。

---

## 6. 建议的下一步（按顺序）

1. ~~**先安装 CI**~~ —— **已完成**。你用网页 UI 装好了 `.github/workflows/` 四条，
   内容与当时的 `ci/workflows/` 逐字节一致，`ci/` 目录随后删除。
2. ~~**让四条 CI 跑一遍**~~ —— **已完成**，首跑三条全红（`Restore vendored dependencies`
   被哨兵大小写 bug 卡死，已修）。
3. ~~从 `build-reports/compile-java.log` 读第一批编译错误~~ —— **已完成，回推链路验证可用**。
   第一批只有一个真实破坏（§2.4 的 `write/readCollection`），已修。
   **下一步是等新一轮 `compile-check` 给出第二批错误**，`Config.kt` 之后的文件此前根本没被编译到。
4. ~~回填 YACL 的真实 sha256~~ —— **已完成**，manifest 现在两行全部 pinned。

   过程值得记一笔，因为中途出现过一次「看起来像投毒」的假警报：两轮 CI 对同一个
   Modrinth 链接打印了两个不同的 sha256（`477d5890...` / `c52d41d4...`）。当时没有盲抄
   任何一个，而是先加了 `sha512_upstream` 列 —— 填 Modrinth 官方公布、且 3 个互不相关的
   packwiz 锁文件逐字一致的 sha512 —— 让 `download_dependencies.py` 对待定行强制校验。

   第 5 轮 CI（`8999fdb`）四条全绿，即这道 sha512 关卡**实际执行且通过**
   （已本地确认 YACL 行确实落在 `ENFORCED` 分支，不是因为读不到列而被跳过），
   说明拿到的字节确实是上游发布的那一份。此时 `c52d41d4...` 才从「CI 打印的一个数」
   升级为「经上游 sha512 佐证的值」，于是写入 manifest。

   那次 `477d5890...` 最可能的解释是首轮的哨兵 bug 让脚本把**半下完/出错的临时文件**
   也算了一次哈希（那一轮正是 `Restore vendored dependencies` 崩掉的那轮）。
   现在这种字节即使出现也会被 sha512 拦下，不会再悄悄进 `libs/`。

5. ~~给 audit 补 `--minecraft-jar`~~ —— **已完成**。因为机器人改不了 workflow，
   改为在 `build.gradle.kts` 里加 `auditAgainstMinecraft` 任务：从 compile classpath
   上找 Loom 准备好的 Minecraft jar，喂给 `audit_port.py --strict`。
   已确认它真的跑到了（日志打印 `using minecraft-merged-a1f5b1e0f5-26.3.jar`），**0 error**。
   详见 §4 顶部的说明。
6. **把依赖可用性检查接进 CI**（新增，优先级高于实机测试）：
   `python3 scripts/check_dependency_availability.py` 会直接查 Modrinth，确认我们声明的
   依赖区间在当前 `minecraft_version` 上**真的有版本能命中**。§2.6 那次崩溃就是它能拦下的。
   建议加进 `consistency.yml`（该流程本来就联网、也不依赖 libs/）：

   ```yaml
   - name: Dependency availability against upstream
     run: python3 scripts/check_dependency_availability.py
   ```

   ⚠️ 机器人改不了 workflow，**这一步需要你手动加**。没加也不影响其它流程，
   只是少一道网兜；本地随时可以手动跑。

7. 进游戏实测。**参考 TaCZ 的教训**（移植指南 §0）：26.3 这一轮他们 17 个实质提交里有 9 个是
   「CI 绿、进游戏就错」。本仓渲染面小，风险低于他们，但 `AvatarRendererMixin` 的匍匐视觉、
   准星/命中标记、单声道音频转换这三处必须肉眼确认。
   **§2.6 已经证明这一步不可跳过**：四条 CI 全绿的版本，游戏连启动都做不到。
8. 全绿后再改 README 的「状态」段落，并按 §3 的结论决定哪些 🔧 可以升级成 ✅。

---

## 7. 本轮改动清单

| 文件 | 改动 |
|---|---|
| `gradle.properties` | MC/Loader/FabricAPI/ModMenu 版本；`mod_version` → `2.14.2+fabric.26.3.Beta-1` |
| `build.gradle.kts` | TaCZ jar 文件名 → 26.3 R1 |
| `src/main/resources/fabric.mod.json` | `depends` 全套 26.3 区间 |
| `RESOURCE_IMPORT_MANIFEST.tsv` | 两条依赖换 26.3 来源/版本/校验和（YACL 待定） |
| `.gitignore` / `libs/README.txt` | `libs/*.jar` 不再进 Git，改由脚本重建 |
| `scripts/download_dependencies.py` | 支持 `UNVERIFIED_PENDING_CI`、`--print-sha256`、`--require-pinned` |
| `src/main/kotlin/.../config/sync/BufCollectionCodec.kt` | **新增**，替代 26.3 删掉的 `FriendlyByteBuf#write/readCollection`，线格式不变 |
| `src/main/kotlin/.../config/Config.kt` | 改用 `BufCollectionCodec`；删掉多余的 `Lists` import |
| `src/main/java/.../mixin/tweaks/EnderManMixin.java` | `EnderMan` → `Enderman`（26.3 改名），注入点语义待复验 |
| `build.gradle.kts` | `checkVendoredDependencies` 接受 `UNVERIFIED_PENDING_CI` 并打印真实 sha256（此前正则硬卡 64 位十六进制，导致 `build` 必挂） |
| `RESOURCE_IMPORT_MANIFEST.tsv` | 新增 `sha512_upstream` 列（上游公布的 sha512，用于给待定行做真伪校验） |
| `scripts/download_dependencies.py` | 待定行现在强制比对 `sha512_upstream`，不符即拒收；该列留空则维持原行为 |
| `scripts/test_audit_optional_targets.py` | 改成 pytest 兼容（断言进 `test_optional_targets()`），仍可 `python3` 直接跑 |
| `scripts/test_download_dependencies.py` | **新增**，6 个用例锁住 manifest 哨兵/校验和语义 |
| `scripts/check_release_consistency.py` | 依赖缺失时可跳过（`--require-deps` 才强制）；禁用词改用 `minecraft_version` |
| `scripts/audit_port.py` | jar 路径 → 26.3；版本正则 → `26.3`；TaCZ 依赖串 → R1；缺 jar 时直接报错退出 2 而非刷 60 条假错误 |
| `src/main/java/.../EntityBulletRendererMixin.java` | §2.1 |
| `src/main/java/.../GunSoundInstanceMixin.java` | §2.2 |
| `src/main/java/.../input/{UnloadKey,TiltGunKey,ReduceSensitivityKey}.java` | §2.3 |
| `ci/workflows/*.yml` + `ci/install-workflows.sh` | 四条新流程（待安装到 `.github/workflows/`，见 `ci/README.md`） |
| `README.md` / `BUILD.md` / `CHANGELOG.md` / `AGENTS.md` / `docs/BRANCHES.md` | 版本与流程说明同步 |

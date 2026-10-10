# NEOFORGE 26.3 移植执行记录（26.2-neoforge → 26.3）

- 日期：2026-10-10。执行：`arena/e6fcb671-tacztweaks-unofficial` 会话。
- 基线：本仓 `26.3-neoforge` 起点 `dcdf70ac`（= 26.2 线 Beta-1 内容）。
- 权威输入：
  1. 姊妹线《26.3 移植指南》（`q14433686-arch/TaCZ_Refabricated_Unofficial` `26.3` 分支
     `docs/lineage/PORT_GUIDE_26_3_FOR_RENOVATED_NEOFORGE_20260921.md`，实机验证级）；
  2. `q14433686-arch/TaCZ_Renovated` `26.2` → `26.3` 分支实际源码 diff（本会话 codeload 实拉）；
  3. Minecraft 26.3 反编译源码镜像（`0khs/minecraft_source_26.3`，逐项核对）；
  4. `neoforged/NeoForge` `26.2.x` 与 `26.3.x` 分支 patches（sparse clone 实读）。
- **状态红线：本轮为编译级移植。移植沙箱无 JDK 25、无出网构建，`gradlew build` 与全部
  游戏内验证未执行；26.2 线的构建/实机证据不适用于 26.3。** 新增的四条 CI 工作流
  （`.github/workflows/`）承接编译门禁，实机项由维护者按 §7 清单补做。

## 1. 依赖矩阵（2026-10-10 逐项实查）

| 依赖 | 26.2 现值 | 26.3 目标 | 证据 |
|---|---|---|---|
| Minecraft | 26.2 | 26.3 | 姊妹线 stable；TaCZ_Renovated 26.3 已发 R1 |
| NeoForge | 26.2.0.64 | `26.3.0.51-beta`，range `[26.3.0.51-beta,)` | 与 TaCZ_Renovated 26.3 钉选一致（其注释：26.3 仅 beta 通道；2026-10-05 自 `.7-beta` 重钉，因 FML 12.0.8 删 `ModConfig.Type.COMMON/SERVER`，旧 beta 二进制不兼容） |
| TaCZ: Renovated | `1.1.8+neoforge.26.2.R<n>` | `1.1.8+neoforge.26.3.R<n>`（n≥1） | release `26.3的mod文件_R1`，asset `tacz-1.1.8+neoforge.26.3.R1.jar`（56,724,843 B，2026-10-10 发布）；SHA-256 待首台取件机器回填 |
| YACL | 3.9.6+26.2 | 3.9.x+26.3（元数据下限 3.9.5→3.9.4） | 上游 `port/26.3` 分支存在，其 `gradle.properties` `modVersion=3.9.4`（最新提交「Port to 26.3-snapshot-1」，2026-06-23）；发布文件名以 Modrinth/CurseForge 取件为准 |
| Sound Physics | 1.5.1+26.2 | 1.5.1+26.3 | 上游 master `minecraft_version=26.3`、`mod_version=1.5.1+26.3`（2026-10-10 实查 henkelmax/sound-physics-remastered） |
| First Aid / Pillager's Gun | 26.2 可选 | 26.3 发行物**未核实**，仍为可选 | 维护者只确认两个硬性前置有 26.3 构建；这两个可选项未在本沙箱核实 |
| Java / Kotlin / MDG | 25 / 2.4.10 / 2.0.144 | 不变 | TaCZ_Renovated 26.3 同值 |

## 2. 26.3 破坏性变更 × 本仓命中情况

对照移植指南 §2（Mojang 侧）与 §3（Iris）逐项排查本仓源码：

| 26.3 变更 | 本仓命中 | 处置 |
|---|---|---|
| `EntityRenderer#shouldRender` 尾部新增 `float partialTicks` | ✅ `EntityBulletRendererMixin`（TaCZ 覆写跟随 vanilla 签名变化） | 处理器补 `float partialTicks` 形参 |
| `SoundInstance#resolve` → `getOrResolve` | ✅ `GunSoundInstanceMixin` method 目标 | `resolve` → `getOrResolve`（TaCZ 26.3 同步改名，构造器与 `TaczSound`/`getPath` 未变） |
| `InputConstants.Type.KEYSYM` 删除（仅余 `KEYBOARD`/`MOUSE`）；`GLFW_KEY_*` → `InputConstants.KEY_*` | ✅ `UnloadKey`/`ReduceSensitivityKey`/`TiltGunKey` | 改 `Type.KEYBOARD`；`GLFW.GLFW_KEY_U` → `InputConstants.KEY_U`（26.3 源码确认 `UNKNOWN`、`PRESS`、`KEY_U` 均存在） |
| `PoseStack#mulPose(Quaternionf)` → `rotate` | ❌ 本仓 0 处 | 无需处置 |
| `Player#swing` 三参化 | ❌ 0 处 | — |
| `Entity.invulnerableTime` 私有化 + `damageCooldownTime`/`lastHurt` 拆分 | ❌ 本仓不触碰（TaCZ 自身 `EntityKineticBullet` 内部改用 `DamageCooldownUtil.clear`，纯方法体变化） | — |
| `openUri`、`KeyEvent#keycode`、`ItemInHandRenderer` 三分裂、render pass 归属、shader 方言、管线预热、战利品表 schema、配方 codec/同步、`Block#codec()`、`PushReaction.POPPED`、`AbstractPackResources` | ❌ 本仓均不使用 | — |
| `FriendlyByteBuf#readMap/writeMap/readCollection/writeCollection` 同批移除 | ✅ `Config.reloadDiscardsMagazineExclusions` 的 encoder/decoder | 改为手写 `writeVarInt` + 逐元素 `writeUtf` / `readVarInt` + `List(size) { readUtf() }`，线格式与 26.2 完全一致（做法同 TaCZ_Renovated 26.3 的 `BufMapCodec`） |
| NeoForge `net.neoforged.neoforge.items.ItemHandlerHelper`（含 `giveItemToPlayer`）移除 | ✅ `ClientMessagePlayerUnload` 退还弹药 | 改用 vanilla `Inventory#placeItemBackInInventory(ItemStack, Prediction.SERVER_ONLY)`（26.3 新增 `Prediction` 形参；TaCZ_Renovated 26.3 同款，语义等价：塞背包、塞不下掉落） |
| `blaze3d.*` → `renderpearl.*` 包迁移 | ❌ 本仓只引用 `blaze3d.platform.InputConstants`（未迁移）、`blaze3d.platform.Window`（未迁移）、`blaze3d.audio.SoundBuffer`（未迁移）、`blaze3d.vertex.PoseStack`（未迁移） | — |
| FML 12.0.8 `ModConfig.Type.COMMON/SERVER` 删除 | ❌ 本仓不使用 ModConfig（YACL 自管配置） | 仅体现为 NeoForge 钉选跟随 TaCZ |
| `EntityRenderDispatcher#getPlayerRenderer` 删除 | ❌ 0 处 | — |

## 3. mixin 目标逐项核对（TaCZ 侧）

TaCZ_Renovated 26.2→26.3 全树 diff 后，与本仓 `tacztweaks.mixins.json` 的 74 个 mixin 交叉：

**发生变化的目标类（7 个），逐个核对：**

| TaCZ 类 | 26.2→26.3 变化 | 本仓注入点现状 |
|---|---|---|
| `EntityKineticBullet` | 仅方法体：`invulnerableTime = 0` ×3 → `DamageCooldownUtil.clear(...)` | `<init>` 构造器（含 protected 全参构造器）逐参一致；`onBulletTick`/`onHitEntity`/`getDamage(Vec3)`/`tick`/`createDamageSources`/`headShot:F` 全部保留 |
| `LocalPlayerDraw` | `resetData` 内的 `KeepingItemRenderer.getRenderer()` 加空安全 | `resetData` TAIL 注入，不受影响 |
| `GunSmithTableScreen` | `Util.getPlatform().openUri` → `Blaze3D.openUri`（方法体） | `isSuitableForMainHand(GunSmithTableRecipe)`、`mouseScrolled(double×4)` 原样保留 |
| `GunSoundInstance` | `resolve` → `getOrResolve` 改名 | 已随改（§2 表）；两个构造器描述符逐参一致 |
| `EntityBulletRenderer` | `shouldRender` 跟随 vanilla 新增 `float partialTicks`；`mulPose`→`rotate`（方法体） | 已补形参（§2 表） |
| `RefitKey` | `onRefitPress` 方法体（GLFW→InputConstants） | `onRefitPress(InputEvent.Key)` 及内部 `IGun#hasAttachmentLock` 调用点保留 |
| `CameraSetupEvent` | 4 处 `KeepingItemRenderer.getRenderer().getCurrentItem()` → `getCurrentRenderItem()` | `initialCameraRecoil` 及 `getCrawlRecoilMultiplier`/`getClientAimingProgress`/`genPitchSplineFunction`/`genYawSplineFunction` 调用点原样保留 |

其余全部目标类（`ModernKineticGunScriptAPI`、`ModernKineticGunItem`、`LocalPlayer{Bolt,DataHolder,Inspect,Reload,Shoot,Sprint,FireSelect}`、
`LivingEntity{Aim,Reload,Shoot,Sprint}`、`ShooterDataHolder`、`SoundPlayManager`、`ObjectAnimationSoundChannel`、
`GunPackList`、`GunRecoil`、`TickAnimationEvent`、`GunAnimationStateContext`、`BlockRayTrace`、
`LivingEntityCrawl`/`LocalPlayerCrawl`、`LivingEntityAmmoCheck`、`ProjectileExplosion`、`InaccuracyType`、
`AttachmentDataUtils`、`RenderCrosshairEvent`、全部 9 个 Modifier、`IMeleeWeapon` 等）在两个分支间
**字节级一致**（`diff -q` 为空），描述符无需调整。

## 4. mixin 目标逐项核对（vanilla / NeoForge 侧，26.3 反编译源码 + patches）

| 目标 | 26.3 证据 | 结论 |
|---|---|---|
| `MouseHandler#handleAccumulatedMovement()` / `turnPlayer(double)` | 26.3 源码第 247/310 行原样存在；TaCZ 26.3 自己的 `MouseHandlerMixin`（wrap `turnPlayer` 内 `LocalPlayer#turn(DD)V`）与 26.2 逐字节未变 | ✅ |
| `AvatarRenderer#setupRotations(AvatarRenderState,PoseStack,float,float)` | 26.3 源码第 247 行签名一致；`state.isVisuallySwimming` 字段读取与唯一 `translate(FFF)` 调用点（游泳分支）保留，`@Local(ordinal=2)` 的 float 局部（`swimAmount`）仍是两 float 形参后第一个 | ✅ |
| `SoundBufferLibrary#getCompleteBuffer(Identifier)` / `clear()` / `lambda$getCompleteBuffer$1(Identifier)→SoundBuffer` | 26.3 方法体仍是 `cache.computeIfAbsent(l -> supplyAsync(() -> {…new SoundBuffer(data, format)…}, pool))`：两个 lambda、顺序不变，被捕获的 `Identifier` 仍使内层合成方法带 `(Identifier)` 形参；`new SoundBuffer(ByteBuffer,AudioFormat)` 调用点保留 | ✅（lambda 编号属编译期约定，运行时以 mixin 日志复核） |
| `LivingEntity#applyItemBlocking(ServerLevel,DamageSource,float)→float` | 26.3 源码第 1265 行签名一致；NeoForge 26.3.x patch 的 `resolveBlockedDamage`/`hurtBlockingItem` hunk 与 26.2.x **完全一致**（唯一差异为 NeoForge 侧多一行 `damageBlocked = ev.getBlockedDamage();`）；6 参 `hurtBlockingItem(..., float, int)` 仍是主路径 | ✅ |
| `EnderMan#hurtServer(ServerLevel,DamageSource,float)` | 26.3 源码第 347 行签名一致；方法体内第一个 `DamageSource.is(TagKey)` 仍是 `IS_PROJECTILE`（第 355 行，其后无其他同描述符调用） | ✅ |
| `EnchantmentHelper#getDamageProtection(ServerLevel,LivingEntity,DamageSource)` | 26.3 源码第 176 行静态方法原样存在 | ✅ |
| `ClipContext#collisionContext` | 26.3 源码字段保留 | ✅ |
| `LocalPlayer#tick` / `Frustum` / `GuiGraphics`（RenderCrosshairEvent 处理器形参） | 常规目标，TaCZ 26.3 同类调用无变化 | ✅ |
| `SoundBuffer`/`Window`/`InputConstants`/`PoseStack`（blaze3d） | 均留在 `blaze3d` 包（与移植指南 §2.1 一致） | ✅ |

## 5. 本轮实际改动清单

- `gradle.properties`：26.3 全套版本与钉选（含 `-beta` 说明注释）。
- `build.gradle`：`libs/` 匹配 token 26.2→26.3；必需依赖提示与来源更新。
- `TaczVersionSupport`（+单测）：family `neoforge.26.3`，R1 起；新增「拒绝 26.2 末版」用例。
- `EntityBulletRendererMixin`、`GunSoundInstanceMixin`、三个按键类（§2）。
- `Config.kt`、`ClientMessagePlayerUnload.kt`：CI 首跑暴露的两处 26.3 API 移除（§2 表最后两行），
  错误证据为 CI 回推的 `build-reports/compile-java.log`（commit `2b06116`）。
- `tacztweaks.mixins.json`/`ModMixinPlugin`/相关 javadoc 的当前目标版本表述。
- `neoforge.mods.toml` 模板：26.3 文案、YACL 下限 3.9.4。
- `RESOURCE_IMPORT_MANIFEST.tsv`、`libs/README.txt`：26.3 依赖行（SHA 全部 pending，待取件回填）。
- `scripts/audit_port.py`、`scripts/check_release_consistency.py`：26.3 钉选。
- `.github/workflows/`：新增 `compile-check.yml`、`build.yml`、`consistency.yml`、`release.yml`
  流程稿（布局对齐 TaCZ_Renovated 移植仓，正式件保存在 `docs/ci/`）+ `scripts/fetch_ci_dependencies.py`。
  **沙箱 token 无 `workflows` 权限，四个文件无法随分支 push**；上线方式写在每个文件头部
  （GitHub 网页 Create new file 粘贴，与 TaCZ 各线的既有做法一致）。上线前本仓没有任何
  Actions 会跑，首次编译证据以维护者本地或上线后首跑为准。
- 文档：README、BUILD、CHANGELOG、AGENTS、docs/COMPATIBILITY、docs/KNOWN_ISSUES、docs/README、
  docs/publish/*、本记录。

## 6. 明确未做 / 降级项

- `gradlew build`、`gradlew test`：沙箱无 JDK 25 与出网构建 → 交给 CI 首跑。
- `audit_port.py --strict` 的目标类字节码核验：沙箱无 `libs/*.jar` → 当前全部降级 WARN，
  **不算通过**；取件后必须复跑至 0 error / 0 warning。
- `SoundBufferLibraryMixin` 的 lambda 编号、`EnderManMixin` 的 ordinal 行为：静态证据充分，
  但按家族纪律仍需启动日志无 `InvalidInjectionException` 才算装上。
- First Aid / Pillager's Gun 26.3：发行物未核实，不声明支持也不移除门面。
- 光影（Iris）路径：本仓不注入渲染管线，无直接风险；但 TaCZ 26.3 的 scope 渲染改动
  属上游，实机仍应开一次镜确认无回归。

## 7. 维护者验收清单（承接 CI 绿之后）

1. `gh` Actions 四条工作流首跑全绿（尤其 `build` 的 jar 产物与 L0 检查）。
2. 取件回填：`tacz`/`yacl`/`soundphysics` 三个 `pending` SHA-256 写回 manifest，
   `python3 scripts/download_dependencies.py --check-only` 通过。
3. `python3 scripts/audit_port.py --strict`（有 jar）0 error / 0 warning。
4. 客户端：进世界 → 拿枪、开镜、换弹、卸弹、拉栓、跑打、配置屏开合保存、数据包重载；
   启动日志无 `MixinApplyError` / `InvalidInjectionException` / `Scanned 0 target(s)`。
5. 专服：`Done (...)!`，无 mixin/startup fatal（`scripts/check_server_log.py`）。
6. 可选兼容：Sound Physics 1.5.1+26.3 声音重定向；First Aid / Pillager's Gun 若出现 26.3
   发行物，先核对目标签名再点亮。
7. 通过后：CHANGELOG 状态红线改写为实测口径，版本号去 Beta 按发布纪律走
   `scripts/check_release_consistency.py` 与 release 工作流。

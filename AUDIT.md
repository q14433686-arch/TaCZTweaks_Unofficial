# “不能做”与完整性审计（2026-08-18）

## 审计基线

- 原版：`MUKSC/TaCZTweaks` tag `v2.14.2`（commit `74ba2412`）；
- 当前目标：`TACZ-Refabricated-26.1.2-1.1.8+fabric.26.1.2.R2.jar` 及
  `q14433686-arch/TaCZ_Refabricated_Unofficial` 的 `26.1.2` 源码；
- Minecraft 26.1.2 未混淆类文件（逐项读取 class 方法、字段和调用点）；
- 2026-08-18 的 Fabric 26.1.2 模组发行情况；
- 本仓库所有配置 getter、mixin JSON、Java/Kotlin 源码和三份语言文件。

可复查命令：

```bash
python3 scripts/audit_port.py --strict \
  --minecraft-jar /path/to/minecraft-merged-26.1.2.jar \
  --upstream-root /path/to/MUKSC-TaCZTweaks-v2.14.2
```

## 版本门禁（TaCZ R2+）

- 审计和编译基线仍是 **R2 包** `TACZ-Refabricated-26.1.2-1.1.8+fabric.26.1.2.R2.jar`；
- 运行时版本门禁从“仅等于完整 friendly string `1.1.8+fabric.26.1.2.R2`”放宽为接受
  `1.1.8+fabric.26.1.2.R<n>`，其中 `n >= 2`（含同系列后缀版本，如 `R2-hotfix`、`R10-preview`）；
- revision 采用数字比较，`R10` 不会被误判为低于 `R2`；
- 错误的 Minecraft 版本、TaCZ 核心版本、错误 fabric release family、pre-R2 revision 和
  畸形版本字符串仍然拒绝。
- 该门禁由 `TaCZTweaks.isSupportedTaczVersion` 实现，并有
  `src/test/kotlin/me/muksc/tacztweaks/TaCZTweaksVersionTest.kt` 覆盖；当前仅静态/单元验证，
  未进行客户端、集成服、独立服游戏矩阵实测。

## Fabric API 依赖范围（2026-10-15，同步 26.3 线修复）

- 26.3 线的 `fabric-api: ">=0.160.7+26.3 <0.162.0"` 让 Fabric Loader 拒绝了用户已安装的
  `0.162.0+26.3`（Issue #34）。本线有同形上界 `>=0.155.2+26.1.2 <0.157.0`，现改为
  `>=0.155.2+26.1.2`：只删上界，最低版本保留本线自己的 `fabric_version`，不照搬 26.3 的数值；
- 查证（FabricMC/fabric 的 GitHub tags/releases，2026-10-15）：26.1.2 已发布的最高版本是
  `0.155.3+26.1.2`（2026-09-07），旧范围也接受，所以本线此前没有已发布版本被实际拒绝，属预防性修复；
- `minecraft` 仍精确为 `=26.1.2`；Fabric Language Kotlin 与 YACL 当时未改动（26.3 的跨线同步说明只要求删
  Fabric API 上界），2026-10-16 另行处理，见下一节；
- 门禁：`check_release_consistency.py` 新增 `check_fabric_api_range_is_lower_bound_only`
  （要求 `depends.fabric-api == ">=" + fabric_version`）与 `check_declared_ranges_include_built_versions`
  （构建所用 Loader / Fabric API / FLK 必须落在声明范围内），由 `scripts/test_dependency_ranges.py`
  覆盖，并验证过：把旧上界改回去时检查确实失败；
- 验证级别（2026-10-15，GitHub Actions，commit `c496024`）：`audit_port.py --strict` 用真实 TaCZ R2 jar
  为 0 error / 0 warning；`check_release_consistency.py`（含 jar 校验和）与依赖范围测试通过；Linux 与 Windows
  的 `./gradlew build`（含 JUnit、`checkModIcon` / `checkVendoredDependencies` / `checkJarContents`）均通过，
  产物为 `tacztweaks-2.14.2+fabric.26.1.2.Beta-1-hotfix2.jar`。这也是 #26 盾牌 mixin 加固在本线的首次编译与测试。
  **尚未**用更新的 Fabric API 做客户端 / 集成服 / 独立服实机启动。

## FLK / YACL 依赖范围（2026-10-16，Issue #33 同类问题）

- 26.2 线 Issue #33 里，Fabric Loader 除了卡在 Fabric API 上界，还因 `fabric-language-kotlin: ">=1.13.13 <1.14.0"`
  拒绝了 `1.14.2+kotlin.2.4.21`，因 `yet_another_config_lib_v3: "=3.9.6+26.2-fabric"` 拒绝了 `3.9.7+26.2-fabric`。
  本线写法相同，现改为 `>=1.13.13` 与 `>=3.9.6+26.1-fabric`：只删上界和精确锁定，最低版本保持本线构建所用的版本；
- 查证（2026-10-16）：
  - FLK（FabricMC/fabric-language-kotlin 的 GitHub releases）：`1.13.14`、`1.14.0`、`1.14.1`（2026-09-07）与
    `1.14.2+kotlin.2.4.21`（2026-10-09）均已发布。旧范围拒绝其中的 1.14.0 ~ 1.14.2，所以本线的 FLK 上界**已经在拒绝**
    现行版本，不是预防性修复；
  - FLK `1.13.13 → 1.14.2` 的上游 diff：内置 Kotlin stdlib / reflect 从 `2.4.10` 升到 `2.4.21`，kotlinx 库版本不变；
    FLK 自身的依赖从 `fabricloader >=0.16.9` 提到 `>=0.19.5`（自 1.14.0 起）。本模组允许 Loader `>=0.19.3`，所以
    Loader 0.19.3 / 0.19.4 的用户只能用 FLK 1.13.x —— 这是 FLK 自己的约束，与本模组的声明无关；
  - YACL（isXander/YetAnotherConfigLib `main`）：3.9.7 的 changelog 列出 26.1 Fabric 目标，`versions/26.1-fabric`
    为 `meta.mcDep=~26.1`，发布目标是 26.1 / 26.1.1 / 26.1.2；3.9.6 → 3.9.7 只改了 `gui/` 下的实现和资源，本模组用到的
    `config.v3` / `dsl` / `api` / `platform` 包没有改动。沙箱访问不了 Modrinth API，`3.9.7+26.1-fabric` 的 Modrinth
    条目未直接核对；
  - Fabric Loader 0.19.3 的 `SemanticVersionImpl.compareTo` 只比较版本号与预发布标签，忽略 `+` 后的构建元数据，所以
    `>=3.9.6+26.1-fabric` 等价于 `>=3.9.6`；其他游戏版本的 YACL 仍会被它自己的 `minecraft` 依赖拒绝；
- 门禁：`check_release_consistency.py` 新增 `check_flk_and_yacl_ranges_are_lower_bound_only`（FLK 必须是 `>=` 加
  `flk_version` 的版本号部分；YACL 必须是 `>=3.9.6+26.1-fabric`，且 `build.gradle.kts` 仍解析
  `maven.modrinth:1eAoo2KR:svTkvBec`，只换 YACL 构建、不改下界会失败），`check_metadata()` 不再要求 YACL 精确等于
  `3.9.6+26.1-fabric`。`scripts/test_dependency_ranges.py` 增至 15 个用例；在临时副本里把旧 FLK 上界、旧 YACL 精确锁定、
  26.3 的 `<2.0.0` 写法、26.2 的 YACL 构建名、换掉的 YACL 坐标逐一放回去，测试与 `check_metadata()` 都会失败；
- 验证级别（2026-10-16，GitHub Actions，commit `5b924ef`）：`audit_port.py --strict` 用真实 TaCZ R2 jar 为
  0 error / 0 warning；`check_release_consistency.py`（含 manifest 校验和）与 15 个依赖范围测试通过；Linux 与 Windows
  的 `./gradlew build` 均通过并输出 `JAR CONTENTS: OK`（`processResources` 只展开 `fabric.mod.json` 的 `${version}`，
  depends 原样打包）。本地另做了上述变异测试。**尚未**用 FLK 1.14.x / YACL 3.9.7 做客户端 / 集成服 / 独立服实机启动。

## Fabric Loader 依赖范围（2026-10-16，Loader 0.20.0 发布）

- Fabric Loader 0.20.0 于 2026-10-15 发布（FabricMC/fabric-loader GitHub release，非预发布），旧声明
  `fabricloader: ">=0.19.3 <0.20.0"` 会拒绝它。现改为 `>=0.19.3`：只删上界，最低版本保持 `loader_version`；
- 查证（2026-10-16）：
  - 0.19.3 → 0.20.0 共 18 个提交：Mixin `0.17.3 → 0.17.5`、MixinExtras `0.5.4 → 0.5.5`、ASM `9.10.1 → 9.11`，
    另有可转换包白名单、环境剥离开关、`.x` 谓词修正、只读 zip fs、错误界面提示等。提交标题里没有 API 移除；
    `remove nested mod candidates for env disabled mods` 只影响按环境禁用的模组，本模组 `environment` 为 `*`；
  - Mixin 兼容级别：`FabricMixinBootstrap.getMixinCompat` 取模组 `fabricloader` 依赖的**下界**，再查
    `FabricMixinVersions` 表（0.20.0：`0.20.0 → 0_17_5`、`0.19.4 → 0_17_4`、`0.19.0 → 0_17_1`）。本模组下界
    `0.19.3` 在 Loader 0.19.3 和 0.20.0 上都对应 `COMPATIBILITY_0_17_1`，上界从来不参与这个计算，所以去掉上界
    不改变 mixin 的兼容行为。反过来，把下界抬到 0.19.4 及以上会切换兼容级别，需要重新实测；
  - mixin 配置为 `minVersion: 0.8`、`compatibilityLevel: JAVA_17`，0.20.0 自带的 Mixin（0.8.7 基线）满足。本模组内嵌
    MixinExtras 0.5.4，Loader 0.20.0 自带 0.5.5；
  - 必需依赖链里没有别的 Loader 上界：TaCZ R2 / R3 声明 `>=${loader_version}`（即 `>=0.19.3`），TaCZ 依赖的
    Forge Config API Port 由构建脚本生成 `>=<最低版本>` 或 `*`，Fabric API `0.155.3+26.1.2` 为 `>=0.18.4`，
    FLK 为 `>=0.16.9`（1.13.x）/ `>=0.19.5`（1.14.x），YACL 为 `>=0.18.0`；
- 门禁：`check_release_consistency.py` 新增 `check_fabric_loader_range_is_lower_bound_only`（要求
  `depends.fabricloader == ">=" + loader_version`）。`scripts/test_dependency_ranges.py` 增至 18 个用例；在临时副本里
  放回旧上界、26.3 的 `>=0.19.5 <0.20.0`、`>=0.19.5`，或只把 `loader_version` 改成 0.20.0 而不改范围，测试与
  `check_metadata()` 都会失败。顺带修正了一个旧用例：它把 `>=0.20.0` 硬编码成“高于构建版本”的范围，Loader 真升级到
  0.20.0 时会误报，现改为由 `loader_version` 推出；
- 验证级别：本地脚本测试与上述变异测试通过；CI 结果待补；**尚未**在 Loader 0.20.0 上做客户端 / 集成服 / 独立服实机启动。

## 总结

用户的判断基本正确：旧文档把“原注入点/原 Forge 类型不存在”误写成了“功能不能做”。
`Identifier` 不适合承载每次播放的可变状态、`ProtectionEnchantment` 被删除、Forge 事件不存在，都只意味着需要换挂点。26.1.2 的 `Identifier` 实际是普通 final class，并非 record。

本轮也没有直接照收此前两个未验证 PR：逐条对字节码后发现其中存在会静默失效或逻辑不完整的实现，
例如 `AvatarRenderer` 字段 owner 写错且 `require=0`、完成后的 `SoundBuffer` 字节数未同步、弹射物保护只取
护甲最高等级、Sound Physics 上下文在异步播放前已经清除、airspace 缺 reflectivity 永远无法选中。
这些均按 26.1.2 实际调用链重写。

## 逐项结论

| 项目 | 旧结论 | 查证结果 | 本轮处理 |
|---|---|---|---|
| Stereo→mono | 误称 Identifier 为 record，进而断言不能做 | 不应把每次播放状态写入共享资源标识 | 请求级 mono 标记 + mono/stereo 独立缓存 + `SoundBuffer` 构造前 downmix；补回资源重载清理 |
| Bullet Protection | 类被删除 | JSON 附魔仍从 `EnchantmentHelper` 汇总 `damage_protection` | 按每件护甲累计 `2 × level`，并保持 `bypasses_invulnerability=false` 条件 |
| Crawl visual | PlayerRenderer 消失 | 26.1.2 对应 `AvatarRenderer.setupRotations` | 使用准确 descriptor、字段 owner 和 `AvatarRenderState.swimAmount`，不再 `require=0` |
| melee | Forge/FakePlayer/Share 难迁 | 枪械 `doMelee` 仍在，LRTactical 已内置稳定 `performAttack` | 两条近战链均接入方块规则和 Fabric 破坏事件 |
| shield | 无 ShieldBlockEvent | 26.1.2 统一改为 `BlocksAttacks` data component | 在 `resolveBlockedDamage` / `hurtBlockingItem` 处理剩余伤害、耐久、禁用 |
| predicates | “26.1.2 已删除” | 移到 `net.minecraft.advancements.criterion.*` | 恢复 Target/BlockTarget/EntityTarget predicate |
| tier | 无 Forge TierSortingRegistry | `ToolMaterial.incorrectBlocksForDrops` 是原版替代语义 | 恢复 wood/stone/copper/iron/diamond/gold/netherite codec 和规则 |
| burst/pellet | 合成 lambda 不稳定 | R2 提供 `runShootCycle` / `spawnProjectiles` | 恢复实体字段和选择器 |
| airspace | “SPR Forge 独占” | SPR 1.5.1+26.1.2 有 Fabric 发行版 | 可选 mixin，使用 SPR 实际 ray count/bounce count、occlusion、reflectivity |
| First Aid | “无 Fabric 26.1.2” | First Aid New 1.2.8 已发布 Fabric 26.1.2 | 记录 TaCZ 命中点；只在查询 `IS_PROJECTILE` 时扩展 bullets 标签 |
| Pillager’s Gun | “无 Fabric 26.1.2” | 非官方 3.2.2 已发布 Fabric 26.1.2 | 在 TaCZ 选最近目标前过滤友方袭击者并尊重 friendlyFire |
| 3P render fix | 需移植 | R2 已原生修复 | 不重复 patch，删除无效开关 |
| LSO / VS / MTS | 暂不能做 | 当前确无 Fabric 26.1.2 目标 | 删除无效 GUI/同步字段，保留生态结论 |

## 生态查证

- Sound Physics Remastered：
  <https://www.curseforge.com/minecraft/mc-mods/sound-physics-remastered/files/all>，
  `1.5.1+26.1.2` Fabric，2026-04-10；源码 `henkelmax/sound-physics-remastered` 当前
  `minecraft_version=26.1.2`。
- First Aid New：<https://modrinth.com/project/yNbhkSj2> /
  <https://www.curseforge.com/minecraft/mc-mods/first-aid-new>；1.2.8 Fabric 发行物支持 26.1–26.1.2，
  源码 `maoruiQa/FIrst-Aid-New` 的 `fabric26.1` source set 中
  `EventHandler.recordProjectileHit(Player, Entity, Vec3)` 与
  `handleCustomPlayerDamage(Player, DamageSource, float)` 已核对。26.2 的 1.3.x shader 覆盖不适用于本分支，未复制。
- Pillager’s Gun (Unofficial Port)：<https://modrinth.com/project/OU2Rfx0G> /
  <https://www.curseforge.com/minecraft/mc-mods/pillagers-gun-unofficial-port/files/all>，
  `3.2.2 26.1.2 Fabric`，2026-04-11。
- Valkyrien Skies：<https://www.curseforge.com/minecraft/mc-mods/valkyrien-skies/files/all>，
  最新公开 Fabric 文件仍为 1.20.1。
- Legendary Survival Overhaul 非官方新版：
  <https://www.curseforge.com/minecraft/mc-mods/legendary-survival-overhaul-unofficial-port>，
  NeoForge；26.1.2 标注 planned/experimental。

## 代码审计额外修复

### Mixin / 运行时

- `mixinextras_version=0.4.1` 低于 mixin JSON 声明的 `0.5.0`：升级到目标端日志实际使用的 0.5.4；
- `BlockRayTraceMixin` 的静态上下文会被集成服务器的客户端线程互相覆盖：改为 ThreadLocal，并在 RETURN 清理；
- `sprintWhileReloading` 仍会被 TaCZ 的 `LocalPlayerMixin#swapSprintStatus` 取消：在同一 sprint setter 外层建立
  ThreadLocal scope，只屏蔽该来源的 `cancelReload`；
- 匍匐俯仰限制不再只依赖鼠标事件：鼠标 TAIL 保证即时限制，客户端 END tick 覆盖手柄/外部输入和
  “进入匍匐但没有移动鼠标”的情况；进入控制状态与实际 clamp 都有节流日志可验证；
- 所有本轮已确认的 hook 都设为 required，不用 `require=0` 隐藏失效；
- modifier 的 common cache/gameplay hook 与 `@Environment(CLIENT)` property-diagram hook 已拆成
  独立 mixin；审计脚本解析目标 class 的环境注解，禁止 common mixin 指向专服会剥离的方法。

### 数据驱动行为

- 方块子弹/近战破坏统一经过 `Level.mayInteract` 和 Fabric BEFORE/CANCELED/AFTER，并只在实际销毁后替换方块；
- 破坏裂纹原用 wall clock，且 stage 0 不清除、空 level map 泄漏：改回存在的 `getGameTime()`、完整清理；
- 粒子原来发到所有维度，`local` 坐标直接退化为原点：存产生维度并按子弹 forward/left/up 变换；
- 实体命中没有更新粒子基点、无方块命中的 whizz 终点为 `Vec3.ZERO`：每 tick 跟踪真实终点/命中点；
- 非法粒子字符串继续保持“记录并跳过”，不再允许视觉数据导致 ticking entity 崩服。

### 网络 / 边界

- 配置 payload 可声明任意/负长度，且复用 payload 时 `writeBytes(buf)` 会消耗 reader index：限制 1 MiB、
  使用绝对索引复制、失败回滚并释放 ByteBuf；同时恢复客户端 `syncedWithServer=true`，并用同步到
  `LocalPlayer` 的 26.1.2 PermissionSet 判断 OP（旧实现强转 ServerPlayer，导致所有客户端都无权保存）；
- 倾斜 C2S 只作为 40 tick 的输入租约：调度到 server thread，且服务端每次用于扩散前重新验证
  alive/removed、主手持枪和 `GunData.canSlide()`；客户端不能永久写入 gameplay 状态；
- 枪声 C2S 要求 alive + 主手持枪，namespace 只能是 TaCZ/TaCZTweaks/当前枪包；同时增加
  服务端开关、finite/range 检查、16 次/秒限流、96 格上限、double 平方和维度过滤；
- 卸弹栈拆分循环改为明确的 `while remaining > 0`，缺 ammo index 时不再先删除弹药；
- `Inventory.hasInfiniteAmmo` 的 `0..containerSize` 越界改为 `0 until containerSize`。

## 第二轮：隐藏删减 / 空实现审计

在实机暴露“视觉微调其实仅为第三人称”后，审计范围从配置可见项扩大到源码级差分：

- 对比原版 173 个 Java/Kotlin 文件与本端全部同路径文件；
- 扫描 TODO/FIXME/no-op/固定返回值/空方法/注释代码；
- 对照原版 95 个与本端 mixin 清单，并检查每个 `@At` 是否真实存在于目标方法字节码；
- 新增 `scripts/upstream_omissions.json`：57 个同路径缺失文件必须逐项有替代实现或外部阻塞理由，
  出现新的无解释缺失或过期规则时 `--upstream-root` 审计直接失败。

本轮找到并恢复的真实隐藏删减：

1. `alwaysFilterByHand` 原只强制返回 true，却没有像原版一样隐藏失效的“按手持筛选”复选框；
2. `reloadWhileShooting` 漏了空仓/战术换弹动画的弹匣+膛内联合判定；
3. 全局平衡值已影响实战和 tooltip，但改装台 `getPropertyDiagramsData/buildNormal/buildAim` 基线注入被整组跳过，
   导致图表把全局规则误显示成配件差值；现恢复 damage/headshot/armor/speed/ADS/RPM/recoil/inaccuracy 图表路径；
4. `data/old` 与旧 `bullet_interactions` codec fallback 被整块删除；现用 `Codec.either` 读取新旧格式，写出仍统一为新格式；
5. 倾斜键原版在 sprint 起始判断处阻止奔跑，本端只在 tick 尾强制停跑；现也在真实 sprint setter 前处理，避免每 tick 反复启停。

剩余同路径缺失项目前全部进入机器可检查的解释清单，不再接受“文件名不同所以大概移植了”的口头结论。

## 从 TaCZ Refabricated 26.1.2 当前仓库吸收的维护规则

2026-08-18 再次对照目标仓库 `26.1.2` 的 `AGENTS.md`、
`STATE_LIFECYCLE_AUDIT.md`、版本一致性脚本及当前 Fabric API 用法后，补入以下门禁：

- **注释只作为线索**：能力结论必须由调用链、class descriptor 或实际发行物支撑；
- **编译通过不代表 mixin 安全**：审计脚本同时检查目标方法和 `@At` 方法/字段 descriptor；
- **静态状态必须有生命周期出口**：服务器停止时清理 ServerLevel/粒子/限流状态，客户端断线时
  清理 mono 与 Sound Physics 待处理队列；
- **优先当前 26.1.2 API**：实体标签使用 `BuiltInRegistries.*.wrapAsHolder`，数据 reload 使用
  `ResourceLoader` v1，不保留仅仅“还能编译”的 deprecated 接口；
- **版本号与文档同改**：`audit_port.py` 会要求 `gradle.properties`、README、BUILD 的版本标识唯一一致；
- **未实测不写成实测完成**：PR 在真实构建和游戏矩阵完成前保持 Draft。

## 仍需运行时矩阵

静态/字节码审计不能替代真实 Minecraft 客户端。发布前应至少验证：

1. 纯必需依赖启动客户端和专服；
2. SPR、First Aid、Pillager’s Gun 分别单装及同时安装；
3. mono 资源重载前后、四件弹射物保护、虚空弹；
4. 枪托/刺刀/LRTactical 近战，领地模组取消方块破坏；
5. 盾牌正面/背面、固定/动态耐久、禁用时长；
6. airspace 数据包、predicate、tier、burst、pellet 示例数据；
7. 多维度粒子和多人 whizz/共享枪声。

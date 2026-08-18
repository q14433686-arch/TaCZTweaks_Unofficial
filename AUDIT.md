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
- **版本号与文档同改**：`audit_port.py` 会要求 `gradle.properties`、README、BUILD 的 R 版本唯一一致；
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

# AI Agent 执行单：TaCZ Tweaks Fabric 1.21.11 完整差距修复

- 生成日期：2026-08-18
- 目标产品分支：`1.21.11`
- 目标分支即时 HEAD：`efc70d253a427722c5d60eb50e4a2071e8165019`
- 26.2 已验证参考实现：PR #8 / `arena/01a01393-tacztweaks-unofficial`，生成本文时 HEAD `f91546f`
- TaCZ 目标：`TaCZ_Refabricated_Unofficial` 分支 `1.21.11`，Release `1.21.11_R2`
- 精确依赖：`TACZ-Refabricated-1.21.11-1.1.8+fabric.1.21.11.R2.jar`
- Minecraft / Java：1.21.11 / Java 21 / **混淆 + Loom remap + refmap**

> 这是给独立 Arena Agent 的工作说明。你的 Arena 会话分支名是固定的；只在该会话分支工作，
> PR base 应为 `1.21.11`。不要直接推产品分支，也不要为了拿 26.2 文件切换分支。
> 远端 `arena/01a01396-tacztweaks-unofficial` 是已关闭 PR #7 的旧 26.2 实验，不是本任务基线。

## 1. 最高优先级规则：这是混淆分支

事实来源顺序：

1. 你工作树中的 1.21.11 源码、refmap 和实际 runtime descriptor；
2. 1.21.11 R2 TaCZ jar；
3. `q14433686-arch/TaCZ_Refabricated_Unofficial:1.21.11` 的 `AGENTS.md`、源码及
   `docs/EXPLICIT_GAPS_AUDIT_R12.md`；
4. Loom 的 named / intermediary Minecraft jars 与生成的 refmap；
5. 26.2 PR #8 的行为规格、安全修复和审计思想；
6. MUKSC/TaCZTweaks v2.14.2（`74ba2412`）。

26.2 PR #8 **不能整批 cherry-pick**：

- 26.2 是未混淆 Java 25；本分支是混淆 Java 21；
- vanilla 方法在开发环境可见 Mojang 名，运行时需要 intermediary/refmap；
- `remap=false` Mixin 中引用 vanilla owner/method 时要逐点决定 `remap=true/false`；
- 不得引入 26.2 的 `lambda$xxx$N` 合成名；继承的 vanilla 方法可能必须使用稳定 intermediary
  `method_NNNNN`（当前 bullet tick 已使用 `method_5773`）；
- 每个 descriptor、LVT ordinal、`@Environment` 和 client/common 分类必须按 1.21.11 实测。

目标 TaCZ 仓库明确要求运行：

```bash
python3 docs/verify_mixin_targets.py
python3 docs/verify_shader_imports.py
```

如果这些脚本只存在于 TaCZ 仓库，应检出只读副本或把适用检查安全移植进本项目，不能假装跑过。

只读参考命令：

```bash
git fetch origin arena/01a01393-tacztweaks-unofficial 1.21.11
git diff origin/1.21.11..origin/arena/01a01393-tacztweaks-unofficial -- src/main
```

## 2. 当前进度快照

当前 1.21.11 分支已有：

- 102 个 Java/Kotlin 源文件；
- 50 个 Mixin（29 common + 21 client）；
- obfuscated Loom 工程、official mappings、legacy Mixin AP 和 refmap；
- 配置/YACL、网络、按键、基础卸弹；
- 基础 movement/shoot/reload tweak；
- 全局 cache modifier；
- 基础 bullet interaction/sound/particle data 与行为；
- 1.21.11 R2 TaCZ 编译依赖说明及示例包。

相对 26.2 已验证实现：

- 少 47 个源码/资源路径；
- 另有 57 个同路径文件行为已落后；
- 0 个测试；
- 没有 `audit_port.py`、omissions allowlist、notices、server-log gate；
- 无当前 GitHub check，也没有可引用的最新客户端/专服矩阵。

当前源码版本为：

```text
2.14.2+fabric.1.21.11.R3
```

但 README / BUILD 仍多处写 R2，属于已存在的发布一致性错误。完成本轮后不要复用 R3；确认没有 R4
发布后提升到 R4，并同步所有文档、artifact 名和 metadata。

另一个现成文档错误：README 依赖表写 YACL `3.9.6+26.2-fabric`，而 BUILD/真实下载是
`3.8.2+1.21.11-fabric`。以 jar metadata 和实际构建为准后统一修正。

## 3. 立即阻断项

### P0-A：MixinExtras 版本不满足自己的 JSON

`gradle.properties` 是 `0.4.1`，Mixin JSON 要求 `>=0.5.0`。升级到对 Java 21 / 1.21.11 remap
实测兼容的版本（26.2 使用 0.5.4），核对 annotation processor、include jar 和 TaCZ 自带版本。

### P0-B：common/client 分组

当前 JSON 把 `crawl.LocalPlayerCrawlMixin` 放在 common。重新扫描全部 target：

- vanilla `net.minecraft.client.*`；
- TaCZ `com.tacz.guns.client.*`；
- target class/method 上的 `@Environment(CLIENT)`。

全部必须登记在 `client`。恢复 modifier property diagrams 时，从一开始拆为 common cache 与 client
`*DiagramMixin`；不得使用 `require=0`。

### P0-C：缺少映射感知的 Mixin 审计

把 PR #8 `audit_port.py` 的思路适配到混淆环境，而不是原样复制：

- TaCZ 自有方法用实际 named class；
- vanilla inherited/调用点同时核对 named descriptor、intermediary runtime 名和 refmap；
- 检查目标方法存在，还要检查其 Code attribute 中真实包含 `@At` owner/name/descriptor；
- 解析 RuntimeVisible/InvisibleAnnotations，阻止 common → CLIENT stripped method；
- Mixin source ↔ JSON 双向检查；
- 不接受 `require=0` 或“编译过了所以能运行”。

## 4. 当前可直接复现的安全/边界缺口

### 网络

- shared first-person sound C2S 无开关、alive、主手持枪、namespace、finite/range、维度和限流；
  `distance * distance` 可 Int overflow。实现服务端验证（参考 96 格、16 次/秒），但 descriptor/API
  按 1.21.11。
- slide packet 直接在网络线程强转 nullable player 并永久写 boolean；改为 executor、alive/held gun/
  `GunData.canSlide()`、短 lease/heartbeat、逐 tick 清理。
- config payload 允许负/超大长度，`writeBytes(buf)` 消耗 readerIndex；增加 1 MiB 上限、absolute copy、
  失败回滚/释放；客户端权限不能强转 `ServerPlayer`。

### 方块/实体

- armor-ignore 公式方向相反；实现纯 `SafeMath.blockBreakingDelta`，覆盖 zero/negative/NaN/Infinity。
- bullet destroy 直接调用 `destroyBlock`，缺 `mayInteract` + Fabric BEFORE/CANCELED/AFTER；melee 共用 helper。
- entity custom pierce、gun-pierce consume、持久 damage falloff、完整候选排序和 PIERCE sound/particle 未闭环。
- 无 shield rule 时必须保留 vanilla blocked damage；1.21.11 的格挡 API 不能假设等同 26.2，先读 merged jar。

### sound/particle/data

- whizz 对同一玩家每 tick 重播；每颗子弹记录成功播放 UUID。
- particle 跨维度发送、`String.format` 任意 directive、无 emitter cap/finite/range；保存维度，只替换 `%s`。
- constant interval 允许 0；codec/runtime 都限制为正。
- airspace payload 无集合上限。
- `ValueRange.DEFAULT = Double.MIN_VALUE..MAX` 不包含 0，且不拒绝 reversed/NaN。
- 示例包 `minecraft:chain` 需按 1.21.11 实际 tag 核对；不能因为 26.2 用
  `#minecraft:chains` 就未经 reload 照抄。
- 旧 v2 bullet interaction fallback 缺失。

### 卸弹

现实现先清膛再确认 ammo index，dummy/FUEL/inventory/chamber 混在一起，异常 ammoCount/stackSize
可造成错误循环或吞弹。按 physical/dummy/FUEL/inventory/closed-bolt 分支重写，并增加 pure splitter 测试。

## 5. 功能恢复目标

不要继续接受 README 的“不能做”表：

- `betterMonoConversion`：当前文档错误引用 26.2 并把 Identifier 称为 record。先检查 1.21.11 实际
  `Identifier`/sound cache class；采用 per-request mono 状态和 mono/stereo 独立 cache，不向共享标识
  挂可变播放状态。
- `bulletProtection`：按 1.21.11 数据驱动附魔的真实 protection 汇总点实现每件护甲 `2 × level`，
  排除 bypass damage；不要找删除的 `ProtectionEnchantment`。
- crawl：第一人称 pitch controller + 第三人称 renderer 过渡；1.21.11 可能不是 26.2
  `AvatarRenderer` descriptor，必须查 merged jar。
- melee：枪托/刺刀 + TaCZ R2 提供的 LRTactical `IMeleeWeapon#performAttack`。
- shield：按本版 block component/hurt path；无规则 fallback 保留 vanilla。
- predicate / tier / burst_index / pellet_index；包名、codec、spawn hook 按 1.21.11。
- projectile explosion playerDamage、真正 sprintWhileReloading、reloadWhileShooting 动画、filter checkbox、
  modifier diagrams（client-only）、old data converter。
- 生命周期：server stop、player/client disconnect、resource reload 清所有 static map/ThreadLocal/pending state。

### 可选兼容特别规则

不要复制 26.2 First Aid shader：它只对 First Aid 1.3.x / Minecraft 26.2 验证，放进 1.21.11 会无条件
覆盖错误版本资源。SPR、First Aid、Pillager’s Gun 必须分别查询真实 1.21.11 Fabric 发布物：

- 有目标：核对源码/class descriptor，限定 metadata 版本，再实现；
- 无目标：删除无行为 GUI/同步字段并写外部阻塞理由；
- TaCZ R2 明确 `provides: lrtactical`，该 melee 路径优先恢复。

## 6. 推荐实施顺序

1. **映射与门禁先行**：参数化 audit、环境注解、refmap/intermediary 验证、omissions、server log；
2. **修 P0**：MixinExtras、common/client、所有现存注入运行期 target；
3. **网络与数学安全**：config/sound/slide、SafeMath、ValueRange、unload、particle/airspace；
4. **实体/方块行为**：protection chain、persistent pierce、whizz/PIERCE；
5. **功能 parity**：mono、protection、crawl、melee、shield、predicate/tier/burst/pellet；
6. **隐藏 parity**：diagram/filter/reload/sprint/old schema；
7. **真实生态 compat**；
8. **测试与发布工程**。

每加入一个 Mixin，先确认完整 descriptor 和内部调用，再写 handler；不要等启动崩溃后猜。

## 7. 测试与发布工程

至少增加：

- SafeMath；
- stack splitter；
- mono signed 16 / signed 8 / unsigned 8；
- ValueRange reversed/zero；
- 新 schema + v2 codec smoke；
- burst/pellet allocator；
- 示例包 1.21.11 reload fixture。

测试 JVM 必须 Java 21。Windows 中文路径会触发 Gradle test worker args-file 编码问题；采用 PR #8
把 main/test/runtime dependencies staging 到 ASCII-only `GRADLE_USER_HOME` 的方案，不能删除测试。

发布工程：

- 精确验证 TaCZ `1.1.8+fabric.1.21.11.R2` 的完整 friendly string（SemVer build metadata 比较会忽略 R2）；
- 收紧 Fabric API、FLK、YACL 和可选依赖范围；
- MixinExtras notices、如有第三方资源则 notices/license；
- Gradle distribution checksum；
- example-pack zip task；
- 修 processResources Gradle 10 warning；
- 版本统一提升并修 README/BUILD 当前 R2/R3 冲突。

## 8. 验收命令与运行矩阵

完成前至少执行：

```bash
python3 scripts/audit_port.py --strict \
  --tacz-jar libs/TACZ-Refabricated-1.21.11-1.1.8+fabric.1.21.11.R2.jar \
  --minecraft-named-jar <1.21.11 named jar> \
  --minecraft-intermediary-jar <1.21.11 intermediary/runtime jar> \
  --upstream-root <TaCZTweaks-v2.14.2>
./gradlew clean build
python3 scripts/check_server_log.py <dedicated-server-latest.log>
git diff --check
```

如果 audit 的参数尚不存在，就实现它们；不要把命令删掉来“通过”。

运行时矩阵：

1. 纯必需专服到 `Done`，无 MixinApplyError/InvalidInjectionException/refmap failure；
2. 纯必需客户端完成资源 reload、主菜单和进世界；
3. 示例包真实 reload，无 tag/codec error；
4. mono/protection/crawl/shield/melee；
5. burst/pellet、多实体 pierce、双人 whizz/共享枪声；
6. OP/non-OP config、slide lease、恶意 C2S；
7. 可选 compat 单装/组合（仅存在且已适配的 1.21.11 版本）；
8. property diagrams、filter、reload/sprint UI/动画。

Loom/Gradle 退出 0 不代表 Minecraft 子进程启动成功；日志必须出现 `Done (...)!`，并由 checker 排除
fatal marker。

## 9. 禁止事项与最终交付

- 不得用 26.2 named descriptor 覆盖 1.21.11 intermediary/refmap 目标；
- 不得使用不稳定 `lambda$...$N` 作为运行时 vanilla 目标；
- 不得用 `require=0`、删除测试、默认关闭或跳过行为冒充完成；
- 不得把 First Aid 26.2 shader 放进本分支；
- 不得继续写“Identifier 是 record”“Fabric 没有 FakePlayer”；
- 不得声称未实测内容 PASS；
- 不要尝试由 Agent 写 GitHub workflow（通常无 workflow 权限），提供门禁脚本/模板即可。

最终回复必须包含：提交 SHA、mapping/refmap 审计结果、P0/P1/功能/发布清单、实际命令及输出、专服
日志 checker 结果、未执行矩阵、剩余外部阻塞。完整客户端和多人矩阵前 PR 保持 Draft。

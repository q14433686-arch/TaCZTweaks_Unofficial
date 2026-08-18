# AI Agent 执行单：TaCZ Tweaks Fabric 26.1.2 完整差距修复

- 生成日期：2026-08-18
- 目标产品分支：`26.1.2`
- 目标分支即时 HEAD：`314a7ca08cfa9b102b06cea36c34561f92428acf`
- 26.2 已验证参考实现：PR #8 / `arena/01a01393-tacztweaks-unofficial`，生成本文时 HEAD `f91546f`
- TaCZ 目标：`TaCZ_Refabricated_Unofficial` 分支 `26.1.2`，Release `26.1.2_R2`
- 精确依赖：`TACZ-Refabricated-26.1.2-1.1.8+fabric.26.1.2.R2.jar`
- Minecraft / Java：26.1.2 / Java 25 / **未混淆**

> 这是给独立 Arena Agent 的工作说明。你的 Arena 会话分支名是固定的；只在该会话分支工作，
> PR base 应为 `26.1.2`，不要直接推送产品分支，也不要切到本文提到的参考分支。
> 远端 `arena/01a01396-tacztweaks-unofficial` 是已关闭 PR #7 的旧 26.2 实验，不是本任务基线。

## 1. 先读，禁止直接整分支复制

事实来源顺序：

1. 你工作树里的 26.1.2 当前源码与实际 class descriptor；
2. 26.1.2 R2 TaCZ jar；
3. `q14433686-arch/TaCZ_Refabricated_Unofficial:26.1.2` 当前源码与 `AGENTS.md`；
4. 26.1.2 Loom 生成的 Minecraft merged jar；
5. 26.2 PR #8 的最终行为与审计门禁；
6. MUKSC/TaCZTweaks v2.14.2（`74ba2412`）。

可以把 26.2 PR #8 当作**行为规格和已知事故清单**，但不得 wholesale cherry-pick：

- 26.1.2 和 26.2 都未混淆、Java 25，很多实现可复用；
- 但 README 所称“唯一差异只是 EntityType”未经完整证明；每个 Mixin descriptor、LVT ordinal、
  `@Environment` 和 Fabric API 都必须对 26.1.2 jar 单独核验；
- `EntityTypes.ENDERMAN` 等 26.2 专有 API 不得直接复制；26.1.2 仍可能使用 `EntityType.ENDERMAN`。

建议只读参考：

```bash
git fetch origin arena/01a01393-tacztweaks-unofficial 26.1.2
git diff origin/26.1.2..origin/arena/01a01393-tacztweaks-unofficial -- src/main
```

## 2. 当前进度快照

当前 26.1.2 分支不是空工程，已有：

- 102 个 Java/Kotlin 源文件；
- 50 个 Mixin（JSON：29 common + 21 client）；
- 配置持久化/同步、ModMenu/YACL、按键、基础卸弹；
- 跑打、换弹奔跑、射击换弹、手动拉栓、换弹丢弹匣等基础移动逻辑；
- 全局伤害、爆头、穿甲、弹速、ADS、扩散、RPM、后坐力 cache modifier；
- 基础 bullet interaction/sound/particle data 与行为；
- TaCZ R2 编译依赖和 26.1.2 示例包。

但与 26.2 已验证实现相比：

- 少 47 个源码/资源路径；
- 另有 51 个同路径文件行为已经落后；
- 0 个测试；
- 没有 `audit_port.py`、上游 omission allowlist、第三方 notices 或专服日志门禁；
- 没有可证明的当前客户端/专服矩阵；README 的“可运行”不能替代重新验证。

当前版本是：

```text
2.14.2+fabric.26.1.2.R1
```

完成这轮大改后至少应提升 revision，并同步 README / BUILD / metadata；先检查是否已有同 revision
发布，不能制造同版本不同内容。

## 3. 立即阻断项（先修再谈功能）

### P0-A：MixinExtras 版本自相矛盾

`gradle.properties` 是 `0.4.1`，Mixin JSON 要求 `>=0.5.0`。升级到经目标环境验证的版本
（26.2 使用 0.5.4），同时核对 TaCZ 自带版本和最终 jar 内嵌内容。

### P0-B：common/client Mixin 分组必须重新审计

当前 JSON 把 `crawl.LocalPlayerCrawlMixin` 放在 common。所有 `net.minecraft.client.*`、TaCZ client
类以及注入 `@Environment(CLIENT)` 方法的 Mixin 都必须在 `client` 数组。

当恢复改装台 property diagrams 时，必须从第一天就拆分：

- common：`initCache` / gameplay；
- client：`getPropertyDiagramsData`、`buildNormal`、`buildAim`；
- 不得用 `require=0` 隐藏专服 stripping。

把 PR #8 的 class annotation 解析门禁移植并改为 26.1.2 jar。专服必须真实到 `Done`。

### P0-C：静态审计缺失

移植并参数化 `scripts/audit_port.py`：

- Mixin source ↔ JSON 双向一致；
- target class/method/descriptor；
- 每个目标方法内部真实包含 `@At` member reference；
- common 不得指向 client class 或 `@Environment(CLIENT)` method；
- persisted option 必须有行为 reader；
- 语言键、版本、依赖、test/fixture/release guard；
- 与 v2.14.2 的 173 个源码文件做 omissions allowlist，禁止无解释缺失。

## 4. 已确认的安全和行为缺口

以下问题在 26.1.2 当前源码可直接复现，不能只修编译：

### 网络 / 权威

- `ClientMessageBroadcastSound` 无服务端开关、alive/持枪/namespace/finite/range/维度/限流验证，
  且 `Int * Int` 可溢出；移植 96 格、16 次/秒、当前枪包 namespace 的服务端校验和生命周期清理。
- slide packet 在网络线程直接强转 nullable player 并永久接受 boolean；改为 server executor、
  alive + 主手持枪 + `GunData.canSlide()` 验证、短期 lease/heartbeat、逐 tick 失效。
- config payload 可声明负数/超大长度；`writeBytes(buf)` 会推进 readerIndex；移植 1 MiB 上限、
  absolute copy、失败回滚/释放和正确 OP 权限判断。

### 方块 / 实体

- armor-ignore 方向反了：当前 `digSpeed *= exp(-2*armorIgnore)` 导致穿甲越高越难破坏；
  使用独立、可测试的 `SafeMath.blockBreakingDelta`。
- bullet block break 直接 `destroyBlock`，缺 `mayInteract` 和 Fabric BEFORE/CANCELED/AFTER；
  bullet/melee 必须共享 protection helper。
- 当前 26.1.2 仍错误声称 `gameTime` 不存在并使用 wall clock；实际 descriptor 后恢复 game tick，
  清理 stage 0、空 map 和 server stop 生命周期。
- entity custom pierce/gun-pierce consume、damage falloff、全候选距离排序、PIERCE sound/particle 分支
  尚未闭环；按目标 R2 `onBulletTick` 字节码恢复。

### sound / particle / data

- whizz 对同一玩家每 tick 重播；每颗子弹记录成功播放过的 UUID。
- particle 向所有维度广播，`particle.format` 可抛异常，emitter 无上限，数值无 finite/range 校验；
  保存产生维度，只替换 `%s`，限制 count/speed/duration 和 emitter 总数。
- constant `interval=0` 会除零；codec 与运行时双重限制为正。
- `ValueRange.DEFAULT` 使用 `Double.MIN_VALUE`（最小正数），不包含 0；改为正确下界并校验 finite、
  `min <= max`。
- 示例包仍用 `minecraft:chain`；按 26.1.2 实际 tag registry 核对是否应为
  `#minecraft:chains`，不得照抄而不 reload。
- 旧 v2 `bullet_interactions` fallback 已删除；恢复读取旧格式、统一写新格式。
- airspace payload 需要候选/每候选声音上限和 finite/range 校验。

### 卸弹

当前实现会在 ammo index 缺失时仍清弹，dummy/FUEL/inventory/chamber 分支混在一起，stack size/计数
无上限。按 physical、dummy、FUEL、inventory feed、closed-bolt chamber 分支重写；先构建/返还成功，
再清枪状态，并用纯函数 stack splitter 测试。

## 5. 功能恢复清单

逐项对 26.1.2 API 实现，不要把旧文档的“不能做”当结论：

- `betterMonoConversion`：先确认 26.1.2 `Identifier` 实际 class header；不要以“record”为理由。
  采用具体 sound request 上下文和 mono/stereo 独立 buffer cache，覆盖 signed 16-bit、signed/unsigned
  8-bit 测试及资源 reload。
- `bulletProtection`：寻找本版 `EnchantmentHelper#getDamageProtection`/damage-protection component 汇总点，
  每件护甲累计 `2 × level`，排除 bypasses-invulnerability；不能找已删除的类。
- crawl：区分第一人称 pitch controller 与第三人称 renderer 过渡，覆盖鼠标、手柄、无输入 tick。
- melee：枪托/刺刀和 TaCZ R2 内置 LRTactical `IMeleeWeapon#performAttack`。
- shield：按 26.1.2 实际格挡 API 实现；无数据规则时必须保留 vanilla blocked damage。
- predicate / tier / burst_index / pellet_index；确认 26.1.2 包名与 codec，不凭 26.2 猜。
- projectile explosion `playerDamage`、真实 sprintWhileReloading、reloadWhileShooting 战术换弹动画、
  alwaysFilterByHand 隐藏失效复选框、倾斜键阻止起跑。
- modifier property diagrams：全局 damage/headshot/armor/speed/ADS/RPM/recoil/inaccuracy 基线，
  **client-only Mixin**。
- 生命周期：server stop、player disconnect、client disconnect、resource reload 清所有 static 状态。

可选兼容不能机械复制 26.2：SPR、First Aid、Pillager’s Gun 必须先查 26.1.2 的真实 Fabric 发布物和
class descriptor。存在才实现并约束版本；不存在则删除无效 GUI/同步字段并记录外部阻塞。TaCZ R2
提供 `lrtactical`，该兼容应优先完成。

## 6. 推荐实施顺序

1. **建立门禁**：复制并参数化 audit、omissions、server-log checker、测试框架、中文路径 test-runtime
   staging；先让当前已知错误被脚本检出。
2. **修启动阻断**：MixinExtras、common/client 分组、环境注解、完整 descriptor。
3. **修网络和数学边界**：sound/slide/config payload、SafeMath、ValueRange、particle/airspace、unload。
4. **恢复实体/方块行为**：protection chain、persistent pierce、whizz/PIERCE、shield/melee。
5. **恢复隐藏 parity**：old codec、diagram、filter、animation、reload/sprint。
6. **按真实生态恢复 compat**，不为不存在的目标保留假开关。
7. **发布工程**：精确 TaCZ R2 运行期版本检查、依赖范围、checksum、tests、notices、example zip、
   Gradle 10 warning、README/BUILD 一致。

## 7. 必须增加的测试

至少移植/适配：

- SafeMath armor-ignore 单调性、zero hardness、NaN/Infinity/负值；
- stack splitter 和异常 ammo count/stack size；
- mono PCM：signed 16、signed 8、unsigned 8；
- ValueRange 0 与 reversed range；
- 新 schema + v2 codec smoke；
- burst/pellet allocator；
- 示例包 JSON 和 26.1.2 tag reload。

Windows 中文项目路径会触发 Gradle test worker classpath 编码 bug；复用 PR #8 将 runtime staging 到
ASCII-only `GRADLE_USER_HOME` 的方案，不能通过删测试或 `-x test` 规避。

## 8. 验收矩阵

静态与运行时分开报告，完成条件：

```bash
python3 scripts/audit_port.py --strict \
  --tacz-jar libs/TACZ-Refabricated-26.1.2-1.1.8+fabric.26.1.2.R2.jar \
  --minecraft-jar <26.1.2 merged jar> \
  --upstream-root <TaCZTweaks-v2.14.2>
./gradlew clean build
python3 scripts/check_server_log.py <dedicated-server-latest.log>
git diff --check
```

运行时矩阵至少包括：

1. 纯必需依赖 dedicated server 到 `Done`，且无 MixinApplyError/InvalidInjectionException；
2. 纯必需客户端到主菜单/进世界；
3. 示例包真实 reload，无 tag/codec error；
4. 可选兼容分别单装和组合（仅限已证实存在的 26.1.2 版本）；
5. 双人 shield、whizz、共享枪声、slide/config 权限；
6. mono reload 前后、SPR OpenAL（若有）、First Aid body-part（若有）；
7. modifier diagram 和 filter/reload/sprint UI/动画路径。

Loom `runServer` 子进程失败可能仍让 Gradle 返回 0；必须检查日志 `Done (...)!`，不能只看
`BUILD SUCCESSFUL`。

## 9. 禁止事项与交付格式

- 不得用 `require=0`、禁用开关或删除测试伪装修复；
- 不得声称未实测客户端/专服/可选模组 PASS；
- 不得继续引用“Identifier 是 record”“Fabric 没有 FakePlayer”之类已证伪事实；
- 不得只修 README 可见项；必须扫描注释行为、空实现、固定返回、同路径方法遗漏和 Mixin `@At`；
- 不要写 GitHub workflow（Agent token 通常无 workflow 权限）；把可执行门禁和模板交给维护者。

最终回复应给出：提交 SHA、按 P0/P1/功能/发布分类的完成清单、所有实际运行命令及结果、未执行矩阵、
剩余外部阻塞。PR 在完整矩阵前保持 Draft。

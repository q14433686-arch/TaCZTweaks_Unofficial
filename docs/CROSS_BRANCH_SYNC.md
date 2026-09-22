# 跨分支同步指南：运行时约束门禁 + CI 流程

> **给接手的 AGENT**：这份文档是可执行的工单，不是背景阅读。
> 请**从头到尾读完 §0 再动手** —— §0 里有两条「不要做什么」，照抄会把别的分支弄坏。
>
> 来源分支：`26.3`（本文档随 26.3 移植一并提交）
> 撰写日期：2026-09-22

---

## 0. 先读这个：本次同步的性质

2026-09-22，`26.3` 线连续出现**两次「四条 CI 全绿、但游戏根本启动不了」**的崩溃。
两次的根因属于**同一类**，这才是需要跨分支同步的东西。

### 0.1 两次事故

| # | 现象 | 根因 |
|---|---|---|
| 1 | `Incompatible mods found` | `fabric.mod.json` 声明 `fabric-language-kotlin: >=1.13.13 <1.14.0`，但 26.3 上唯一存在的 FLK 是 `1.14.1`。**交集为空** |
| 2 | `IllegalStateException: requires TaCZ ...26.2.R2, found ...26.3.R1` | `TaCZTweaks.java` 里硬编码的运行时版本闸门没跟着改 |

### 0.2 共同教训（这才是重点）

> **真正危险的不是编译错误，而是「只在运行时才生效的约束」。**
>
> `fabric.mod.json` 的 `depends` 区间、以及代码里硬编码的版本闸门，
> **编译不读、mixin 静态审计不读、打包也不读** —— 只有游戏启动时才执行。
>
> 所以「CI 全绿」只能证明**代码编得过**，证明不了**游戏起得来**。

第 2 次事故还有一个附带教训：

> **一个断言了错误期望的测试，比没有测试更糟。**
> `TaCZTweaksVersionTest.kt` 当时断言 `assertFalse(isSupportedTaczVersion("...26.2.R1"))`，
> 把上一条线的期望固化了下来。`./gradlew build` 因此一路绿灯，
> 反而制造了「已验证」的错觉。

### 0.3 ⚠️ 不要做的两件事

1. **不要把 FLK 区间统一改成 `>=1.14.1`。**
   这是**本次同步最容易犯的错**。`>=1.13.13 <1.14.0` 在 26.2 / 26.1.2 / 1.21.11 上
   **是正确的** —— 已核实 FLK `1.13.13` 的 `game_versions` 确实覆盖这三个版本。
   要同步的是**门禁工具**，不是版本号本身。
   每条线的正确值必须由 §2 的脚本**查出来**，不是抄过去。

2. **不要照抄 `26.3` 的 `SUPPORTED_TACZ_VERSION` 等常量。**
   每条线的 TaCZ 发行系列和**最低修订号都不一样**（见 §3.2 表格）。
   26.2 要求 ≥R2，26.3 是 ≥R1 —— 抄错会让该分支直接起不来。

---

## 1. 同步范围：哪些分支要做、做什么

仓库当前 8 条发行线。

> **「从 `26.3` 复制」的确切含义**：本文档随移植 PR 一起进入 `26.3`。
> 四条 CI 流程**此前在任何分支上都不存在**（已核实：8 条发行分支的
> `.github/workflows/` 全为空），它们是随本次 PR 首次落地的。
> 所以请**等本 PR 合并后**再从 `26.3` 拉取，或直接从该 PR 的 head 分支取文件。
>
> `scripts/check_release_consistency.py` 则是既有文件，除 `1.21.11-neoforge` 外各分支都有
> —— 对它是**追加函数**，不是整文件覆盖。**不要用 26.3 的版本直接覆盖其它分支的同名文件**，
> 那会把该分支特有的检查逻辑冲掉。

| 分支 | 加载器 | 运行时闸门位置 | 本次要做 |
|---|---|---|---|
| `26.3` | Fabric | `TaCZTweaks.java` | ✅ 已完成（来源） |
| `26.2(main)` | Fabric | `TaCZTweaks.java` | A + B + C |
| `26.1.2` | Fabric | `TaCZTweaks.java` | A + B + C |
| `1.21.11` | Fabric | `TaCZTweaks.java` | A + B + C |
| `26.2-neoforge` | NeoForge | `TaczVersionSupport.java` | ⚠️ 需改写，见 §2.2 / §3.3 |
| `26.1.2-neoforge` | NeoForge | `TaczVersionSupport.java` | ⚠️ 同上 |
| `1.21.11-neoforge` | NeoForge | `TaczVersionSupport.java` | ⚠️ 同上（且**缺** `check_release_consistency.py`，要先补） |
| `26.3-neoforge` | NeoForge | — | ⚠️ 见 §5，**这条线还没开始移植** |

- **A = 依赖可用性门禁**（§2）：新增脚本，查上游实际发布了什么
- **B = 运行时闸门交叉校验**（§3）：确保代码里的闸门与 `fabric.mod.json` 一致
- **C = 四条 CI 流程**（§4）：可选但强烈建议

---

## 2. A：依赖可用性门禁

### 2.1 它解决什么

回答一个此前没人回答的问题：**我们声明的依赖区间，在当前 Minecraft 版本上真的有版本能命中吗？**

事故 1 中，`gradle.properties` 和 `fabric.mod.json` **彼此自洽**（都是 26.2 时代的旧值），
只是双双与现实脱节。任何只比对这两个文件的检查都发现不了 —— 必须去问上游。

### 2.2 怎么做

从 `26.3` 分支复制这个文件：

```
scripts/check_dependency_availability.py
```

**Fabric 三条线（`26.2(main)` / `26.1.2` / `1.21.11`）：逐字复制，无需修改。**
它从 `gradle.properties` 读 `minecraft_version`、从 `fabric.mod.json` 读 `depends`，
自动适配所在分支。

**⚠️ NeoForge 三条线：这是「重写」，不是「复制」。** 已核实的差异：

| | Fabric 线 | NeoForge 线 |
|---|---|---|
| 元数据文件 | `src/main/resources/fabric.mod.json` | `src/main/templates/META-INF/neoforge.mods.toml` |
| 区间语法 | Fabric 的 `>=1.14.1 <2.0.0` | Maven 的 `[3.9.5,3.10.0)` |
| FLK | 有 | **没有**（Kotlin stdlib 由 jarJar 内嵌，见 `gradle.properties` 注释） |
| 可查的第三方依赖 | `fabric-api`、`fabric-language-kotlin` | 实际只剩 `yet_another_config_lib_v3` |

所以移植到 NeoForge 时至少要改四处：`fetch_versions()` 的 `loaders` 改 `"neoforge"`；
`CHECKABLE` 只保留 YACL；解析对象换成 TOML 的 `versionRange`；
并且 **`_satisfies()` 看不懂 Maven 方括号区间，必须另写一个比较函数**。

> 鉴于 NeoForge 线只剩一个可查依赖，**投入产出比明显低于 Fabric 线**。
> 建议先把 Fabric 三条做完，NeoForge 线视精力再说。

### 2.3 验证它真的有效

**不要只跑一遍看到 OK 就算完。** 必须确认它能*失败*：

```bash
# 1. 当前分支应通过
python3 scripts/check_dependency_availability.py; echo "exit=$?"

# 2. 故意把 fabric.mod.json 里的 FLK 区间改成一个不存在的值，例如 ">=99.0.0"
#    再跑一次 —— 必须 exit=1 并打印 "matches NONE of the builds"
# 3. 改回去
```

**两个容易误判的行为，先知道再验收：**

- 脚本默认失败退 1；**加了 `--warn-only` 则永远退 0**，只打印 `WARN:`。
  接进 CI 时若用了 `--warn-only`，**它不会让流程变红** —— 这是刻意的降级开关，
  但别误以为装了就有强制力。
- 访问不了 `api.modrinth.com` 时打印 `SKIP (network unavailable)` 并**退出 0**
  （不能因为断网卡住构建）。**此时 `OK` 那行不会打印** ——
  所以验收要认「有没有 `SKIP`」，而不是只看退出码。这种情况请用 §2.4 的离线办法。

### 2.4 离线环境下的替代验证

用 `fetch_page` 或浏览器取这个 URL（把 `26.2` 换成该分支的 `minecraft_version`）：

```
https://api.modrinth.com/v2/project/fabric-language-kotlin/version?game_versions=["26.2"]
```

人工核对返回的 `version_number` 是否落在 `fabric.mod.json` 声明的区间内。
**已核实的结论**（2026-09-22，可直接采信）：

| Minecraft | FLK 有哪些版本打了该标签 | `>=1.13.13 <1.14.0` 是否成立 |
|---|---|---|
| 1.21.11 | 1.13.12 / 1.13.13 / 1.13.14 / 1.14.0 / 1.14.1 | ✅ 成立，**不用改** |
| 26.1.2 | 同上 | ✅ 成立，**不用改** |
| 26.2 | 同上 | ✅ 成立，**不用改** |
| **26.3** | **仅 1.14.1** | ❌ **不成立**，26.3 已改为 `>=1.14.1+kotlin.2.4.20 <2.0.0` |

> 换句话说：**A 这一步在现有 Fabric 老分支上大概率不会改出任何版本号**，
> 它的价值是**以后**升级 Minecraft 版本时能当场拦下同类错误。这正是同步它的意义。

---

## 3. B：运行时闸门交叉校验

### 3.1 它解决什么

代码里硬编码的 TaCZ 版本闸门，**只有游戏启动时才执行**。
本检查从 Java 源码里解析出闸门常量，验证 `fabric.mod.json` 声明的 TaCZ 版本
**能通过这道闸门**。两者不一致 = 用户必崩。

### 3.2 每条线的正确值（务必逐条确认，不要照抄）

| 分支 | `SUPPORTED_TACZ_VERSION` | 最低修订 |
|---|---|---|
| `26.3` | `1.1.8+fabric.26.3.R1` | **R1** |
| `26.2(main)` | `1.1.8+fabric.26.2.R2` | **R2** |
| `26.1.2` | `1.1.8+fabric.26.1.2.R2` | **R2** |
| `1.21.11` | `1.1.8+fabric.1.21.11.R2` | **R2** |
| NeoForge 各线 | `EXPECTED_FAMILY = neoforge.<mc>` | **R1** |

> 各线最低修订号不同是**有意为之**：26.2 那条线的 TaCZ R1 早于本模组需要的 API，
> 所以要求 ≥R2；而 26.3 与 NeoForge 各线都是以 R1 首发的。**照抄会崩。**

### 3.3 怎么做

在 `scripts/check_release_consistency.py` 里加入 `check_runtime_version_gate()`，
并在 `main()` 中调用。从 `26.3` 复制函数体后，**按所在分支改这两处**：

- **Fabric 线**：默认即可（解析 `TaCZTweaks.java` 的
  `SUPPORTED_TACZ_VERSION` / `SUPPORTED_TACZ_VERSION_PREFIX` / `MIN_SUPPORTED_TACZ_REVISION`）。
  注意 `isSupportedTaczVersion` 是 **包级可见**（无 `public`），测试与它同包才调得到。

- **NeoForge 线**：三处都不一样，已核实如下。
  - 闸门在 **`src/main/java/me/muksc/tacztweaks/TaczVersionSupport.java`**，不在 `TaCZTweaks.java`。
  - 常量名是 `EXPECTED_CORE_VERSION` / `EXPECTED_FAMILY`（如 `neoforge.26.2`）/ `MIN_REVISION`，
    且 `MIN_REVISION` 是 **`int`**，不是 `BigInteger` —— 26.3 版的正则匹配
    `BigInteger.valueOf(n)|ONE|ZERO`，**在这里一个都匹配不到**，必须改写。
  - **最关键**：NeoForge 线没有 `fabric.mod.json`，且 `neoforge.mods.toml` 里
    tacz 的 `versionRange` 是宽松的 **`[1.1.8,)`**，并不锁定发行系列。
    也就是说「声明」本身不含系列信息，**26.3 版那种「代码闸门 vs 声明」的比对无从做起**。
    该分支真正的系列约束写在 `neoforge.mods.toml` 的**描述文字**里
    （"Requires TaCZ ... 1.1.8+neoforge.26.2.R1 or a later R\<n\> build"）。
    可行做法是改为校验「代码闸门 ↔ 描述文字所述系列」一致，
    或收紧 `versionRange` 后再比对。**这需要设计判断，不要机械照搬。**

### 3.4 顺带修掉「断言了错误期望的测试」

检查该分支的 `TaCZTweaksVersionTest.kt`（NeoForge 线文件名可能不同）。
**如果它把版本号写成字面量，就改成对常量求值**：

```kotlin
// 好：不会与 fabric.mod.json 脱节
assertTrue(TaCZTweaks.isSupportedTaczVersion(TaCZTweaks.SUPPORTED_TACZ_VERSION))

// 坏：手抄字面量，升级时必然遗漏
assertTrue(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.2.R2"))
```

同时**删掉/改写**那种「断言上一条线的版本必须被拒绝」的用例。

### 3.5 验证

```bash
python3 scripts/check_release_consistency.py      # 当前分支必须 OK
```

然后**故意破坏**再跑一次，确认两种失效都能被拦下：

1. 把 `SUPPORTED_TACZ_VERSION_PREFIX` 改成别的发行系列 → 应报
   `runtime gate accepts only '...' but fabric.mod.json requires '...'`
2. 把最低修订号调到比实际发布号更高（例如该线是 R1 却要求 ≥R2）→ 应报
   `runtime gate demands revision >= R2 ...`

两条都能报错，才算装好。

---

## 4. C：四条 CI 流程（可选但强烈建议）

从 `26.3` 复制 `.github/workflows/` 下四个文件：

| 流程 | 作用 | 移植时要改什么 |
|---|---|---|
| `compile-check.yml` | 编译并**把日志回推进仓库** | `on.push.branches` 加入该分支名 |
| `build.yml` | `./gradlew build` + 上传产物 | 同上 |
| `audit.yml` | `audit_port.py --strict` + pytest | 同上 |
| `consistency.yml` | 版本/文档一致性 | 同上；建议加 §2 的脚本 |

### 4.1 两个必须知道的坑

1. **机器人 token 通常没有 `workflows` 权限。**
   带 `.github/workflows/*.yml` 的提交会被拒：
   `refusing to allow a GitHub App to create or update workflow ... without 'workflows' permission`。
   届时请把文件内容交给**人类**从网页 UI 提交，不要反复重试。

2. **Actions 日志正文在受限沙箱里读不到**
   （`gh api .../jobs/<id>/logs` 会 302 到不可达的 blob 域）。
   这正是 `compile-check.yml` 要把编译日志回推进 `build-reports/compile-java.log` 的原因。
   替代读法：

   ```bash
   # 看每一步的结论
   gh api repos/<owner>/<repo>/actions/runs/<run_id>/jobs \
     --jq '.jobs[].steps[] | "\(.number). \(.name) -> \(.conclusion)"'

   # 看编译日志正文（compile-check 回推后）
   gh api "repos/<owner>/<repo>/contents/build-reports/compile-java.log?ref=<branch>" \
     --jq '.content' | base64 -d
   ```

### 4.2 建议给 `consistency.yml` 加一步

```yaml
      - name: Dependency availability against upstream
        run: python3 scripts/check_dependency_availability.py
```

---

## 5. `26.3-neoforge` 特别说明

该分支的 `gradle.properties` 目前仍是：

```
minecraft_version=26.2
minecraft_version_range=[26.2]
mod_version=2.14.2+neoforge.26.2.Beta-1
```

**即这条线只是从 26.2-neoforge 拉出来的空壳，26.3 移植尚未开始。**
在它真正开始移植之前，同步本文档的门禁意义不大；
真要动它，请先完成 26.3 的 NeoForge 移植本身，再回头装门禁。

---

## 6. 验收清单

同步完成后，在该分支上逐条确认：

- [ ] `python3 scripts/check_release_consistency.py` → `RELEASE CONSISTENCY: OK`
- [ ] `python3 scripts/check_dependency_availability.py` → OK 或 `SKIP (network unavailable)`
- [ ] `python3 -m pytest scripts/ -q` → 全绿
- [ ] **故意破坏运行时闸门 → 检查确实报错**（§3.5 的两种失效）
- [ ] **故意写一个不存在的依赖区间 → 检查确实报错**（§2.3）
- [ ] `python3 scripts/audit_port.py --strict` → 0 error
- [ ] `./gradlew build` 通过（沙箱跑不了就交给 CI）
- [ ] **实机启动一次**：客户端 + 集成服 + 独立服

> 最后一条不可省略。**本次两个事故都是 CI 全绿、实机崩溃**；
> 前七条全过也不能替代它。

---

## 7. 相关文件索引

来源分支 `26.3` 上的对应实现：

| 文件 | 说明 |
|---|---|
| `scripts/check_dependency_availability.py` | A：查上游实际发布版本 |
| `scripts/check_release_consistency.py` | B：`check_runtime_version_gate()` 在此 |
| `scripts/test_dependency_ranges.py` | A/B 的回归测试，含两次事故的复现用例 |
| `src/main/java/me/muksc/tacztweaks/TaCZTweaks.java` | Fabric 线运行时闸门 |
| `src/test/kotlin/me/muksc/tacztweaks/TaCZTweaksVersionTest.kt` | 改写后的闸门测试（对常量求值） |
| `.github/workflows/*.yml` | 四条流程（随本 PR 首次落地） |
| `docs/PORT_26_3_STATUS.md` | §2.6 / §2.7 记录了两次事故的完整经过 |

仅存在于 **NeoForge 分支**（在 `26.3` 上查不到，属正常）：

| 文件 | 说明 |
|---|---|
| `src/main/java/me/muksc/tacztweaks/TaczVersionSupport.java` | NeoForge 线运行时闸门 |
| `src/main/templates/META-INF/neoforge.mods.toml` | NeoForge 线元数据（Gradle 模板展开） |

查看方式（无需切分支）：

```bash
git fetch origin 26.2-neoforge
git show origin/26.2-neoforge:src/main/java/me/muksc/tacztweaks/TaczVersionSupport.java
```

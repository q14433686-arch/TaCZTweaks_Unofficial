# 构建指南（BUILD.md）— 1.21.11 分支

本目录是 **TaCZ Tweaks 的 Fabric 1.21.11 移植版**（混淆版），适配
[TaCZ_Refabricated_Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)
的 `1.21.11` 分支。

> 其余分支（26.3 / 26.2 / 26.1.2，未混淆版）用 **JDK 25**；**本 1.21.11 分支必须用 JDK 21**。
>
> 当前测试版本统一对齐为 **Beta-1-hotfix2**：`2.14.2+fabric.1.21.11.Beta-1-hotfix2`

---

## 0. 你需要什么

| 项目 | 要求 |
|---|---|
| JDK | **Java 21** |
| Python | 可选；手动运行 `scripts/*.py` 时需要 **Python 3.8+**，普通 Gradle 构建有 JVM 图标校验后备 |
| 网络 | 首次构建要从 Maven 下载依赖 |
| 磁盘 | 约 1GB（Minecraft 1.21.11 + Fabric API + Loom remap 产物） |

---

## 1. 装 JDK 21

下载 **Eclipse Temurin JDK 21**：

- Windows x64：https://adoptium.net/temurin/releases/?version=21
- 装完验证：
  ```powershell
  java -version
  ```
  应显示 `openjdk version "21.0.x"`。构建时 Gradle 优先使用 `JAVA_HOME`。

手动运行图标检查、静态审计或日志脚本需要 **Python 3.8+**。安装后可验证：

```powershell
py -3 --version
```

普通 `gradlew.bat build` **不强制要求 Python**：`checkModIcon` 会优先探测 Windows
的 `py -3`、`python3` 和 `python` 并运行标准库检查器；如果都不可用，则自动使用
内置的等价 JVM 校验，不会因为 Microsoft Store 的失效 `python.exe` 别名或退出码
9009 跳过门禁或中断构建。需要固定解释器时，可设置 `PYTHON`，或显式传入：

```powershell
gradlew.bat build -Ptacztweaks.python=C:\Python312\python.exe
```

---

## 2. 拿到源码

- **下载 zip**：从 Release 页下载 `tacztweaks-2.14.2+fabric.1.21.11.Beta-1-hotfix2-src.zip`，解压；
- **git clone**：
  ```powershell
  git clone https://github.com/q14433686-arch/TaCZTweaks_Unofficial.git
  ```

---

## 3. 放两个编译期依赖到 `libs/`

这两个大 jar 通过 `flatDir` / `files(...)` 引用。仓库用 `RESOURCE_IMPORT_MANIFEST.tsv`
固定来源、许可证和校验和。

进入项目根目录 `libs/`，放入以下文件（**文件名必须完全一致**）：

### ① TaCZ 本体

```
https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial/releases/download/1.21.11_R2/TACZ-Refabricated-1.21.11-1.1.8%2Bfabric.1.21.11.R2.jar
```
保存为：`TACZ-Refabricated-1.21.11-1.1.8+fabric.1.21.11.R2.jar`

### ② YACL 配置库

```
https://cdn.modrinth.com/data/1eAoo2KR/versions/pHWDw3Vc/yet_another_config_lib_v3-3.8.2%2B1.21.11-fabric.jar
```
保存为：`yacl-fabric.jar`

也可以直接让脚本按 manifest 下载并校验：

```powershell
python scripts/download_dependencies.py
# Linux/macOS 可用 python3 scripts/download_dependencies.py
```

放好之后：

```text
libs/
├── README.txt
├── TACZ-Refabricated-1.21.11-1.1.8+fabric.1.21.11.R2.jar
└── yacl-fabric.jar
```

发布前必须校验哈希：

```powershell
python scripts/download_dependencies.py --check-only
```

---

## 4. 构建

```powershell
# Windows
gradlew.bat build
# Linux / macOS
./gradlew build
```

成功后产物：

```text
build/libs/tacztweaks-2.14.2+fabric.1.21.11.Beta-1-hotfix2.jar
build/distributions/tacz-tweaks-example-pack-2.14.2+fabric.1.21.11.Beta-1-hotfix2.zip
```

本分支使用 `fabric-loom-remap`，上面的 jar 是 **remapJar** 发布产物。

### 测试与门禁

`check` / `build` 生命周期包含 `checkModIcon`、`checkVendoredDependencies` 和
`checkJarContents`：它们会校验 `fabric.mod.json` 的图标路径、带有效 IHDR 的 512×512 PNG、
批准的 SHA-256，`THIRD_PARTY_NOTICES.md` 中的固定来源、作者、使用路径与 GPL-3.0 声明，
本地 `libs/` 校验和，以及 remapped 发布 jar 内的 `META-INF/LICENSE_tacztweaks` 与
`META-INF/THIRD_PARTY_NOTICES_tacztweaks.md`。也可在 Gradle 之外单独运行检查器。

```powershell
# Windows（Linux/macOS 将 `py -3` 换成 `python3`）
py -3 scripts/download_dependencies.py --check-only
py -3 scripts/check_release_consistency.py --require-deps
py -3 scripts/check_dependency_availability.py
py -3 -m pytest scripts/test_dependency_ranges.py -q
py -3 scripts/check_mod_icon.py
py -3 scripts/audit_port.py --strict `
  --tacz-jar libs/TACZ-Refabricated-1.21.11-1.1.8+fabric.1.21.11.R2.jar `
  --minecraft-named-jar <named-jar> `
  --minecraft-intermediary-jar <intermediary-jar> `
  --refmap build/resources/main/tacztweaks.refmap.json
gradlew.bat test
py -3 scripts/check_server_log.py run/logs/latest.log
```

`test` 任务会把运行时 classpath staging 到 ASCII-only `GRADLE_USER_HOME`，
以绕开 Windows 中文路径上的 Gradle test worker args-file 编码问题。

`--refmap` 指向构建后生成的 `tacztweaks.refmap.json`；如果你已经先执行过 `build`，
脚本也会自动尝试从默认输出目录发现它。

#### 只改文档、没下载依赖时

`libs/*.jar` 不入 Git。不带 `--require-deps` 时，`check_release_consistency.py`
对缺失的 manifest 行打印 `SKIP (not restored)` 而不是失败，所以版本号/依赖区间/文档
一致性可以在完全离线的环境里先跑一遍：

```bash
python3 scripts/check_release_consistency.py     # 不需要 libs/
```

#### 两道「只有运行时才生效」的门禁（从 26.3 线同步）

编译、mixin 静态审计、打包**都不读** `fabric.mod.json` 的 `depends` 区间和代码里硬编码的
版本闸门 —— 只有 Fabric Loader 在游戏启动时才执行。26.3 线因此连续出过三次「CI 全绿、
游戏起不来」。这两个脚本就是补这个洞的：

```bash
# 1) 代码里的 TaCZ 闸门，能放行 fabric.mod.json 声明的那个版本吗？
#    本线闸门是 >=R2（1.21.11 R1 早于本模组需要的 API），26.3 线是 >=R1，不可互相照抄。
python3 scripts/check_release_consistency.py     # 内含 check_runtime_version_gate

# 2) 我们声明的区间，上游在当前 Minecraft 版本上真有构建能命中吗？
python3 scripts/check_dependency_availability.py
```

**验收不能只看退出码。** `check_dependency_availability.py` 访问不了 `api.modrinth.com` 时
打印 `SKIP (network unavailable)` 并**退 0**（不因断网卡住构建），此时 `OK` 那行不会打印。
所以要认「有没有 `SKIP`」。离线时的替代办法：用浏览器取
`https://api.modrinth.com/v2/project/fabric-language-kotlin/version?game_versions=["1.21.11"]`，
人工核对返回的 `version_number` 是否落在声明区间内。

**装好之后必须确认它能失败**，否则等于没装：

```bash
# a) 给 fabric-api / fabricloader / FLK / YACL 任意一项加回上界（或把 YACL 改回精确钉版本）
#    → 必须报 "should be the lower-bound-only range"
#    src/main/resources/fabric.mod.json: ">=0.141.6+1.21.11 <0.143.0" / ">=0.19.3 <0.20.0"
#    / ">=1.13.13 <1.14.0" / "=3.8.2+1.21.11-fabric"
python3 scripts/check_release_consistency.py; echo "exit=$?"   # 期望 exit=1

# b) 把 TaCZTweaks.java 的 SUPPORTED_TACZ_VERSION_PREFIX 改成别的发行系列 → 必须报
#    "runtime gate accepts only '...' but fabric.mod.json requires '...'"

# c) 把 fabric-language-kotlin 抄成 26.3 的 ">=1.14.1+kotlin.2.4.20 <2.0.0" → 必须报
#    "excludes the version this build actually uses"（本线编译用的是 1.13.13）

# d) 改 RESOURCE_IMPORT_MANIFEST.tsv 里 libs/yacl-fabric.jar 的 upstream_version → 必须报
#    "RESOURCE_IMPORT_MANIFEST.tsv records YACL ..."（最低版本与 vendored jar 绑定）
```

这几条都已自动化在 `scripts/test_dependency_ranges.py` 里（20 个用例，双向验证；最后一个用例
直接驱动 `check_metadata()`，确认新门禁确实被调用，而不只是写在文件里）。

⚠️ **本线 FLK 的*最低版本*仍是 `>=1.13.13`，不要跟着 26.3 改成 `>=1.14.1`。**
已核实 FLK 1.13.12 / 1.13.13 / 1.13.14 / 1.14.0 / 1.14.1 / 1.14.2 均打了 1.21.11 标签；26.3 上只有
`1.14.1`，所以那条线被迫改了。照抄过来会把本线实际编译用的 `1.13.13` 排除掉，用户直接
`Incompatible mods found`。要同步的是**门禁**与**「依赖不加上界」这条策略**，不是版本号：
本线自己的 `<1.14.0` 上界（以及 `fabricloader` 的 `<0.20.0`、YACL 的精确钉版本）也已在
`Beta-1-hotfix2` 里一并删掉 —— Fabric Loader 0.20.0 于 2026-10-15 发布，FLK 1.14.x 也已发布。

### 可选兼容版本（1.21.11 线已核实）

这些不是编译必需依赖，但做联机/实机兼容验证时应按 **1.21.11 对应发布线** 准备：

| 模组 | 1.21.11 Fabric 线 |
|---|---|
| Sound Physics Remastered | `fabric-1.21.11-1.5.1` |
| First Aid New | `firstaid-1.2.5+fabric1.21.11-legacy.jar` |
| Pillager’s Gun (Unofficial Port) | `pillagers_gun-3.2.2 fabric 1.21.11.jar` |

---

## 5. 常见问题

| 现象 | 解决 |
|---|---|
| `Could not resolve ... TACZ-Refabricated ...` | `libs/` 里 TaCZ jar 缺失或文件名不对 |
| `Could not resolve ... yacl ...` | `libs/yacl-fabric.jar` 缺失 |
| `UnsupportedClassVersionError` / `invalid source release 21` | JDK 版本不对，换成 JDK 21 |
| 旧版 `checkModIcon` 报 Python 退出码 `9009` | 更新到包含 JVM 后备校验的版本；当前构建不强制要求 Python |
| `MixinApplyError` / `InvalidInjectionException` | 不要只看 Gradle 退出码，先跑 `scripts/audit_port.py` 与 `scripts/check_server_log.py` |
| `Out of space in CodeCache for adapters` | 先执行 `gradlew.bat --stop`，再重跑 `gradlew.bat build`，确保新的 `gradle.properties` JVM 参数已生效 |
| Daemon 内存不足 | 调整 `gradle.properties` 的 `org.gradle.jvmargs=-Xmx...` |

---

## 6. 只想改代码、不想每次重新下载

依赖第一次下载后走 `%USERPROFILE%\.gradle` 缓存，二次构建通常几十秒。

---

## 7. CI：GitHub Actions 构建与发布

验证流程运行在 GitHub Actions 上，因为准备本移植的开发沙箱只能访问 `api.github.com`，
`maven.fabricmc.net` / `api.modrinth.com` / `piston-meta.mojang.com` 以及 GitHub release
附件域全部不可达，本地 `./gradlew` 根本跑不起来。仓库当前有四条验证流程和一条
**仅由发行 tag 触发**的 GitHub Release 流程（从 `26.3` 线同步，按本分支改写）：

| 流程 | 文件 | 跑什么 | 触发方式 |
|---|---|---|---|
| `Version consistency` | `consistency.yml` | `check_release_consistency.py`（版本号 + 依赖区间 + **运行时 TaCZ 闸门交叉校验**）、`test_dependency_ranges.py`、`check_dependency_availability.py` | 相关文件 push / PR |
| `audit` | `audit.yml` | `audit_port.py`（**非 strict**，要求 0 error）：mixin 登记与目标方法、配置项死开关、语言键、图标；+ 恢复依赖后的一致性校验；`arena/**` 上回推 `build-reports/audit.log` | 发行线或 `arena/**` push / PR |
| `compile-check` | `compile-check.yml` | Java/Kotlin **主源集与测试源集**编译；`arena/**` 上回推日志到 `build-reports/compile-java.log` | 发行线或 `arena/**` push / PR |
| `build` | `build.yml` | 完整 `./gradlew build`（含 JUnit 与三个 jar 门禁），上传 JAR 和示例包 Actions artifacts（保留 14 天） | 发行线或 `arena/**` push / PR |
| `GitHub Release` | `release.yml` | 复核 tag 与 `mod_version`、验证 tag 属于 `1.21.11` 分支、静态审计、依赖范围测试、完整构建；创建 GitHub Release 并附上正式 JAR 与示例包 | 仅 `1.21.11_*` tag push |

四条验证流程都先跑 `python3 scripts/download_dependencies.py --print-sha256`，按
`RESOURCE_IMPORT_MANIFEST.tsv` 重建 `libs/`。**本分支全部用 JDK 21**（26.x 未混淆线用
JDK 25，不要照抄）。发布流程失败时不会创建 Release；它使用 Actions 自带的
`GITHUB_TOKEN`，无需个人访问令牌。

### CI 里的 audit 是非 strict 的（1.21.11 线特有）

`audit.yml` 跑 `python3 scripts/audit_port.py`，**不带 `--strict`**。本分支的 `--strict`
还额外要求三个输入，缺任何一个都直接记成 error：

- `--minecraft-named-jar`（vanilla 具名 jar，校验 vanilla mixin 目标）
- `--minecraft-intermediary-jar`（混淆 jar，校验运行时 intermediary 名）
- 构建生成的 `tacztweaks.refmap.json`

前两个来自 Loom / Mojang，只有跑过 `./gradlew build` 才会落进 Gradle 缓存；refmap 本身就是
构建产物。所以 strict 审计**必须先有一次完整构建**，放进「最快反馈」那条流程里逻辑上就是错的。
26.x 是未混淆分支，既没有 intermediary 也没有 refmap，那边的 `audit.yml` 可以直接
`--strict` —— **照抄过来必然全红**。`docs/maintenance/ci-workflow.yml` 里那份早期提案
当初就判断对了这一点，已安装的流程沿用了它的结论。

CI 里非 strict 审计要求 **0 error**；warning 不让流程变红。

⚠️ **不要把 CI 的「0 error」读成「所有 mixin 目标都已核对」。** 首次对真实 TaCZ R2 jar 跑审计
的结果是 `0 error(s), 132 warning(s)`，其中 **96 条尚未定性**：12 处 `@At` 声明（及其 46+46 条
回显）报「method reference absent」，另有 4 处报「target method absent」。那 12 处的 descriptor
**全部**含具名 `net/minecraft/*` 类型，而本线的 TaCZ jar 是按 intermediary 编译的，
`audit_port.py` 又没有 descriptor 翻译步骤 —— 所以很可能只是「审计 pre-remap 源码」的结构性
产物，但**尚未验证**。这也意味着 `--strict` 在本分支目前**跑不到 0 error**，即使把 named /
intermediary jar 与 refmap 都补齐也不行（strict 会把这 96 条升级成 error）。

完整分类、逐条清单与优先排查顺序见 [docs/KNOWN_ISSUES.md](docs/KNOWN_ISSUES.md)
「50 untriaged audit warnings against the real TaCZ R2 jar」。发布前仍须按上面的命令手动跑
strict 审计，并先把那 4 条 name-only 的 `target method absent` 定性完。

`audit.yml` 在 `arena/**` 的 push 上把审计输出回推到 `build-reports/audit.log`（理由与
`compile-check.yml` 回推编译日志相同：受限沙箱读不到 Actions 的日志 blob 域）。它和
compile-check 并发写同一分支，所以回推带 409 重试。

### 在受限沙箱里读编译错误

Actions 的日志正文在受限沙箱里读不到（`gh api .../jobs/<id>/logs` 会 302 到不可达的 blob 域），
所以 `compile-check.yml` 在 `arena/**` 分支上把日志通过 Contents API 回推到
`build-reports/compile-java.log`：

```bash
gh api "repos/q14433686-arch/TaCZTweaks_Unofficial/contents/build-reports/compile-java.log?ref=<branch>" \
  --jq '.content' | base64 -d
```

只有 `arena/**` 分支会回推日志（发行线分支不接受 CI commit，状态直接看 Actions 页）。

### 通过 tag 自动发布 GitHub Release

PR 合并到 `1.21.11`、验证通过并准备好发布版本后，只需创建并推送一个与 `mod_version`
对应的 tag。当前版本 `2.14.2+fabric.1.21.11.Beta-1-hotfix2` 对应 tag
`1.21.11_BETA_1_HOTFIX2`（规则：取 `+fabric.1.21.11.` 之后的后缀，转大写，`.`/`-` 换成 `_`）：

```bash
git checkout 1.21.11
git pull --ff-only origin 1.21.11
git tag -a 1.21.11_BETA_1_HOTFIX2 -m "Release 2.14.2+fabric.1.21.11.Beta-1-hotfix2"
git push origin 1.21.11_BETA_1_HOTFIX2
```

CI 会检查 tag 与 `gradle.properties` 的版本后缀一致、且 tag 指向 `1.21.11` 分支上的提交；
通过静态审计、依赖范围测试、完整构建和 JAR metadata 校验后，自动创建 GitHub Release、
生成发行说明并上传模组 JAR 与示例包。**不必再手动创建 GitHub Release 或上传 JAR。**
仅推送 tag 才会发布，普通提交和 PR 不会触发。此流程只负责 GitHub Releases；Modrinth 与
CurseForge 仍需按 `docs/publish/` 的平台文档单独发布。

> **权限提示**：`.github/workflows/` 下的文件需要 `workflows` 权限才能推送。机器人账号
> （GitHub App）默认没有该权限时，push 会被拒绝并报
> `refusing to allow a GitHub App to create or update workflow ... without 'workflows' permission`。
> 届时请把文件内容交给**人类**从网页 UI 提交，不要反复重试。


## TaCZ 版本门禁

运行时接受当前分支的 `1.1.8+fabric.1.21.11.R<n>`，其中 `n >= 2`，包括 R2-hotfix 及之后的 R3、R10 等同一 release family revision。`libs/` 中的 R2 jar 仍是 compile/test/static audit 基线，不代表未来构建已经完成游戏内实测。Minecraft、TaCZ 核心版本和 Fabric release family 仍严格匹配；具体未来构建需通过 descriptor、客户端和服务器验证。

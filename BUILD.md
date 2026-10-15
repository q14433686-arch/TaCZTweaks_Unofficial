# 构建指南（BUILD.md）

本文件只适用于分支 **`26.3`：Fabric 26.3**，适配
[TaCZ_Refabricated_Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)。
NeoForge 线以及 26.2 / 26.1.2 / 1.21.11 线请切换到对应分支再读该分支的 `BUILD.md`，见
[分支对照](docs/BRANCHES.md)。

本线当前开发版本为 **Beta-1-hotfix2**：`2.14.2+fabric.26.3.Beta-1-hotfix2`；
最新已发布版本为 `Beta-1-hotfix1`，改动合并并完成验证后再发布下一个 tag。

---

## 0. 你需要什么

| 项目 | 要求 |
|---|---|
| JDK | **Java 25**（必须，低于 25 无法编译/运行） |
| 网络 | 首次构建要从 Maven 下载依赖（Gradle 会缓存到 `%USERPROFILE%\.gradle`） |
| 磁盘 | 约 500MB（Minecraft 26.3 + Fabric API 等依赖） |

---

## 1. 装 JDK 25

下载 **Eclipse Temurin JDK 25**（免费开源）：

- Windows x64 安装包：https://adoptium.net/temurin/releases/?version=25
- 装完在命令行验证：
  ```powershell
  java -version
  ```
  应显示 `openjdk version "25.0.x"`。

> 如果系统里还有别的旧 JDK，构建时 Gradle 会优先用 `JAVA_HOME` 指向的那个。
> 保险起见把 `JAVA_HOME` 设成 JDK 25 的安装目录。

---

## 2. 拿到源码

两种方式任选：

- **下载 zip**：从本仓库 Release 页下载 `tacztweaks-...-src.zip`，解压；
- **git clone**：
  ```powershell
  git clone https://github.com/q14433686-arch/TaCZTweaks_Unofficial.git
  cd TaCZTweaks_Unofficial
  ```

---

## 3. 准备两个「编译期依赖」（libs/ 目录）

这两个大 jar 通过 `flatDir` / `files(...)` 引用，**不在 Git 里**（见 `.gitignore` 的 `libs/*.jar`）——
TaCZ 本体约 58MB，每条发行线各存一份会把仓库撑大。来源、许可证和 SHA-256 固定在
`RESOURCE_IMPORT_MANIFEST.tsv`；其余依赖会自动从 Maven 拉。

**推荐做法：让脚本按 manifest 下载并逐个校验 SHA-256。**

```powershell
python scripts/download_dependencies.py
# Linux/macOS 可用 python3 scripts/download_dependencies.py
```

CI 每条流程开头跑的也是这一条命令，所以本地与 CI 拿到的是同一份字节。

想手动放也可以，文件名必须完全一致：

### ① TaCZ 本体（compileOnly，提供 mixin 目标类）

从 TaCZ 的 Release 页下载：
```
https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial/releases/tag/26.3_R1
```
下载文件：`TACZ-Refabricated-26.3-1.1.8+fabric.26.3.R1.jar`（约 58MB）。

### ② YACL 配置库（implementation，配置 GUI）

从 Modrinth 下载 YACL 3.9.7 for 26.3-fabric：
```
https://cdn.modrinth.com/data/1eAoo2KR/versions/s9SjoFu1/yet_another_config_lib_v3-3.9.7%2B26.3-fabric.jar
```
保存为：`yacl-fabric.jar`（约 1MB）

放好之后 `libs/` 里应该是：
```
libs/
├── README.txt
├── TACZ-Refabricated-26.3-1.1.8+fabric.26.3.R1.jar
└── yacl-fabric.jar
```

发布前必须校验哈希：

```powershell
python scripts/download_dependencies.py --check-only
```

> YACL 的 `RESOURCE_IMPORT_MANIFEST.tsv` 行已记录固定的 SHA-256 与上游 SHA-512；
> `scripts/download_dependencies.py --check-only` 会校验下载文件。Modrinth 不提供 SHA-256，
> 因此本仓 SHA-256 是在首次可获取文件的 CI 环境中计算并固定的。

---

## 4. 构建

在项目根目录打开命令行（PowerShell / CMD）：

```powershell
# Windows
gradlew.bat build

# Linux / macOS
./gradlew build
```

- **首次构建**会下载 Gradle 9.5.1、Minecraft 26.3、Fabric API 等，视网速可能要几分钟到十几分钟；
- `build` 包含 `checkModIcon`、本地二进制依赖哈希和发布 jar 内容门禁；它会校验 `fabric.mod.json` 图标路径、512×512 PNG、批准的 SHA-256，以及 `THIRD_PARTY_NOTICES.md` 中的来源与 GPL-3.0 声明；
- 也可单独运行 `python scripts/check_mod_icon.py`、`python scripts/check_release_consistency.py`（Linux/macOS 使用 `python3`）；
- 成功后输出：
  ```
  BUILD SUCCESSFUL
  ```

### 产物位置

```
build/libs/tacztweaks-2.14.2+fabric.26.3.Beta-1-hotfix2.jar   ← 模组，放入 .minecraft/mods/
build/distributions/tacz-tweaks-example-pack-2.14.2+fabric.26.3.Beta-1-hotfix2.zip  ← 可重载示例包
```

### 专用服务器 smoke test 门禁

Loom 在 Minecraft 子进程启动失败时可能仍让 Gradle 任务返回 0，不能只看 `BUILD SUCCESSFUL`。
对专服的 `latest.log`（或捕获的 stdout）再运行：

```powershell
python scripts/check_server_log.py run/logs/latest.log
```

只有日志出现 `Done (...)!`，且不含 `MixinApplyError`、`InvalidInjectionException` 或
`Failed to start the minecraft server` 时才通过。

---

## 5. 常见问题

| 现象 | 解决 |
|---|---|
| `Could not resolve ... TACZ-Refabricated ...` | `libs/` 里的 TaCZ jar 缺失或文件名不对，按第 3 步重新放 |
| `Could not resolve ... yacl ...` | `libs/yacl-fabric.jar` 缺失，按第 3 步重新下载 |
| `java.lang.UnsupportedClassVersionError` / `invalid source release 25` | 用了旧 JDK，换成 JDK 25 并设好 `JAVA_HOME` |
| 下载依赖超时 | 重跑一次；国内网络可给 Gradle 配镜像仓库 |
| `Daemon` 内存不足 | 编辑 `gradle.properties` 的 `org.gradle.jvmargs=-Xmx...` 调大（如 `-Xmx4G`） |
| 想临时跳过测试 | 加参数：`gradlew.bat build -x test`（发布构建不得跳过；SafeMath、卸弹拆栈、PCM、codec 和 burst/pellet 均有测试） |

---

## 6. 只想改代码、不想每次重新下载

依赖只在第一次下载，之后都会走 `%USERPROFILE%\.gradle` 缓存，第二次构建一般只需几十秒。

改完源码后重新 `gradlew.bat build` 即可得到新 jar。

---

## 7. CI：GitHub Actions 构建与发布

验证流程运行在 GitHub Actions 上，因为本地开发沙箱无法访问 Minecraft/Fabric Maven、Modrinth CDN 等依赖源。
仓库当前有四条验证流程和一条 **仅由发行 tag 触发**的 GitHub Release 流程：

| 流程 | 文件 | 跑什么 | 触发方式 |
|---|---|---|---|
| `Version consistency` | `consistency.yml` | 版本号/依赖范围一致性、Modrinth 上游可用性、依赖范围与 release-tag 回归测试 | 相关文件 push / PR |
| `audit` | `audit.yml` | `audit_port.py --strict` 与审计脚本自测 | 发行线或 `arena/**` push / PR |
| `compile-check` | `compile-check.yml` | Java/Kotlin 编译；`arena/**` 上回推日志到 `build-reports/compile-java.log` | 发行线或 `arena/**` push / PR |
| `build` | `build.yml` | 完整 `./gradlew build`，上传 JAR 和示例包 Actions artifacts（保留 14 天） | 发行线或 `arena/**` push / PR |
| `GitHub Release` | `release.yml` | 复核 tag 与 `mod_version`、验证 tag 属于 26.3、静态审计、Modrinth 可用性与依赖/tag 回归测试、完整构建；创建 GitHub Release 并附上正式 JAR | 仅 `26.3_*` tag push |

验证流程会按 `RESOURCE_IMPORT_MANIFEST.tsv` 重建 `libs/`。发布流程失败时不会创建 Release；它使用 Actions 自带的 `GITHUB_TOKEN`，无需个人访问令牌。

### 在受限沙箱里读编译错误

Actions 的日志 blob 域在沙箱里同样不可达，所以 `compile-check` 会把日志写回分支文件：

```bash
gh api repos/q14433686-arch/TaCZTweaks_Unofficial/contents/build-reports/compile-java.log?ref=<分支名> \
  --jq '.content' | base64 -d
```

只有 `arena/**` 分支会回推日志（发行线分支不接受 CI commit，状态直接看 Actions 页）。

### 通过 tag 自动发布 GitHub Release（当前仅 26.3）

PR 合并到 `26.3`、验证通过并准备好发布版本后，只需创建并推送一个与 `mod_version` 对应的 tag。例如当前版本
`2.14.2+fabric.26.3.Beta-1-hotfix2` 对应 tag `26.3_BETA_1_HOTFIX-2`：

```bash
git checkout 26.3
git pull --ff-only origin 26.3
git tag -a 26.3_BETA_1_HOTFIX-2 -m "Release 2.14.2+fabric.26.3.Beta-1-hotfix2"
git push origin 26.3_BETA_1_HOTFIX-2
```

CI 会检查 tag 与 `gradle.properties` 的版本后缀一致，且 tag 指向 `26.3` 分支上的提交；通过静态审计、依赖范围测试、完整构建和 JAR metadata 校验后，自动创建 GitHub Release、生成发行说明并上传模组 JAR。**不必再手动创建 GitHub Release 或上传 JAR。** 仅推送 tag 才会发布，普通提交和 PR 不会触发。此流程只负责 GitHub Releases；Modrinth 与 CurseForge 仍需按平台文档单独发布。

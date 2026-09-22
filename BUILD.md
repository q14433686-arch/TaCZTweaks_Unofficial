# 构建指南（BUILD.md）

本文件只适用于分支 **`26.3`：Fabric 26.3**，适配
[TaCZ_Refabricated_Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)。
NeoForge 线以及 26.2 / 26.1.2 / 1.21.11 线请切换到对应分支再读该分支的 `BUILD.md`，见
[分支对照](docs/BRANCHES.md)。

本线当前测试版本统一为 Fabric/SemVer 可解析的 **Beta-1**：
`2.14.2+fabric.26.3.Beta-1`。

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

> YACL 那一行的 `sha256` 目前仍是占位符 `UNVERIFIED_PENDING_CI`：Modrinth 只公布 sha1/sha512，
> 准备本次移植的沙箱访问不到 CDN。第一次 CI 运行会用 `--print-sha256` 打印真实摘要，
> 回填 manifest 后这条提示即可删除。

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
build/libs/tacztweaks-2.14.2+fabric.26.3.Beta-1.jar   ← 模组，放入 .minecraft/mods/
build/distributions/tacz-tweaks-example-pack-2.14.2+fabric.26.3.Beta-1.zip  ← 可重载示例包
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

## 7. CI：在 GitHub Actions 上构建（网络受限时的唯一途径）

仓库在 `.github/workflows/` 下有四条流程。它们存在的直接原因是：**准备本移植的开发沙箱
只能访问 `api.github.com`**，`maven.fabricmc.net` / `api.modrinth.com` /
`piston-meta.mojang.com` 以及 GitHub release 附件域全部不可达，本地 `./gradlew` 根本跑不起来。
把编译放到 Actions 上，是让改动能被验证的唯一办法。

| 流程 | 文件 | 跑什么 | 大概耗时 |
|---|---|---|---|
| `Version consistency` | `consistency.yml` | `check_release_consistency.py`：版本号在 gradle.properties / README / BUILD / fabric.mod.json 之间是否一致 | 最快，不需要依赖 |
| `audit` | `audit.yml` | `audit_port.py --strict`（mixin 登记与目标方法、配置项死开关、语言键、图标）+ 审计脚本自测 | 分钟级 |
| `compile-check` | `compile-check.yml` | `compileJava compileKotlin`；**arena/** 分支上把日志回推到 `build-reports/compile-java.log` | 中等 |
| `build` | `build.yml` | `./gradlew build`（含 JUnit 与三个 jar 门禁）+ 上传 jar 与示例包 artifact（留 14 天） | 最慢 |

四条流程都先跑 `python3 scripts/download_dependencies.py`，按 manifest 重建 `libs/`。

### 在受限沙箱里读编译错误

Actions 的日志 blob 域在沙箱里同样不可达，所以 `compile-check` 会把日志写回分支文件：

```bash
gh api repos/q14433686-arch/TaCZTweaks_Unofficial/contents/build-reports/compile-java.log?ref=<分支名> \
  --jq '.content' | base64 -d
```

只有 `arena/**` 分支会回推日志（发行线分支不接受 CI commit，状态直接看 Actions 页）。

### 触发分支

四条流程都监听 `arena/**` 与发行线分支（`26.3` / `26.2(main)` / `26.1.2` / `1.21.11`）的 push，
以及全部 PR，并都支持 `workflow_dispatch` 手动触发。

> **权限提示**：`.github/workflows/` 下的文件需要 `workflows` 权限才能推送。
> 机器人账号（GitHub App）默认没有该权限，首次落地这四个文件需要由仓库维护者本人
> push，或给对应 App 勾上 Workflows 权限。

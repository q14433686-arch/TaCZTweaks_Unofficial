# 构建指南（BUILD.md）

本模组是 **TaCZ Tweaks 的 Fabric 26.1.2 移植版**，适配
[TaCZ_Refabricated_Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)。

---

## 0. 你需要什么

| 项目 | 要求 |
|---|---|
| JDK | **Java 25**（必须，低于 25 无法编译/运行） |
| 网络 | 首次构建要从 Maven 下载依赖（Gradle 会缓存到 `%USERPROFILE%\.gradle`） |
| 磁盘 | 约 500MB（Minecraft 26.1.2 + Fabric API 等依赖） |

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

## 3. 放 TaCZ 编译期依赖到 libs/ 目录

TaCZ 本体通过本地文件引用，必须手动下载到项目根目录的 `libs/` 文件夹，且文件名必须完全一致。
YACL 3.9.6+26.1-fabric 已由 Gradle 从 Modrinth Maven 自动解析，不需要手动下载。

### TaCZ 本体（compileOnly，提供 mixin 目标类）

从 TaCZ 的 Release 页下载：
```
https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial/releases/tag/26.1.2_R2
```
下载文件：`TACZ-Refabricated-26.1.2-1.1.8+fabric.26.1.2.R2.jar`（约 58MB）

放好之后 `libs/` 里应该是：
```
libs/
├── README.txt
└── TACZ-Refabricated-26.1.2-1.1.8+fabric.26.1.2.R2.jar
```

构建日志不应再出现 `Specified Dependency Does Not Exist ... libs/yacl-fabric.jar`；YACL 将以
`maven.modrinth:1eAoo2KR:svTkvBec` 自动下载。

---

## 4. 构建

在项目根目录打开命令行（PowerShell / CMD）：

```powershell
# Windows
gradlew.bat build

# Linux / macOS
./gradlew build
```

- **首次构建**会下载 Gradle 9.5.1、Minecraft 26.1.2、Fabric API 等，视网速可能要几分钟到十几分钟；
- 成功后输出：
  ```
  BUILD SUCCESSFUL
  ```

### 产物位置

```
build/libs/tacztweaks-2.14.2+fabric.26.1.2.R2.jar   ← 模组，放入 .minecraft/mods/
build/distributions/tacz-tweaks-example-pack-2.14.2+fabric.26.1.2.R2.zip  ← 可重载示例包
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
| `Could not resolve maven.modrinth:1eAoo2KR:svTkvBec` | 检查网络能否访问 `https://api.modrinth.com/maven`，然后重试构建 |
| `java.lang.UnsupportedClassVersionError` / `不支持发行版本 25` / `invalid source release 25` | Gradle daemon 仍在使用旧 JDK。先执行 `gradlew.bat --stop`，确认 `gradlew.bat --version` 的 JVM 为 25，再构建；工程也已固定 Java 25 toolchain |
| 下载依赖超时 | 重跑一次；国内网络可给 Gradle 配镜像仓库 |
| `Daemon` 内存不足 | 编辑 `gradle.properties` 的 `org.gradle.jvmargs=-Xmx...` 调大（如 `-Xmx4G`） |

---

## 6. 只想改代码、不想每次重新下载

依赖只在第一次下载，之后都会走 `%USERPROFILE%\.gradle` 缓存，第二次构建一般只需几十秒。

改完源码后重新 `gradlew.bat build` 即可得到新 jar。

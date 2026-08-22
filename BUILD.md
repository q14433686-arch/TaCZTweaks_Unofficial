# 构建指南（BUILD.md）— 1.21.11 分支

本目录是 **TaCZ Tweaks 的 Fabric 1.21.11 移植版**（混淆版），适配
[TaCZ_Refabricated_Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)
的 `1.21.11` 分支。

> 其余两个分支（26.2 / 26.1.2，未混淆版）用 **JDK 25**；**本 1.21.11 分支必须用 JDK 21**。
>
> 当前测试版本统一对齐为 **Beta-1**：`2.14.2+fabric.1.21.11.Beta-1`

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

- **下载 zip**：从 Release 页下载 `tacztweaks-2.14.2+fabric.1.21.11.Beta-1-src.zip`，解压；
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
build/libs/tacztweaks-2.14.2+fabric.1.21.11.Beta-1.jar
build/distributions/tacz-tweaks-example-pack-2.14.2+fabric.1.21.11.Beta-1.zip
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
py -3 scripts/check_release_consistency.py
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


## TaCZ 版本门禁

运行时接受当前分支的 `1.1.8+fabric.1.21.11.R<n>`，其中 `n >= 2`，包括 R2-hotfix 及之后的 R3、R10 等同一 release family revision。`libs/` 中的 R2 jar 仍是 compile/test/static audit 基线，不代表未来构建已经完成游戏内实测。Minecraft、TaCZ 核心版本和 Fabric release family 仍严格匹配；具体未来构建需通过 descriptor、客户端和服务器验证。

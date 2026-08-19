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

---

## 2. 拿到源码

- **下载 zip**：从 Release 页下载 `tacztweaks-2.14.2+fabric.1.21.11.Beta-1-src.zip`，解压；
- **git clone**：
  ```powershell
  git clone https://github.com/q14433686-arch/TaCZTweaks_Unofficial.git
  ```

---

## 3. 放两个编译期依赖到 `libs/`

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

放好之后：

```text
libs/
├── README.txt
├── TACZ-Refabricated-1.21.11-1.1.8+fabric.1.21.11.R2.jar
└── yacl-fabric.jar
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

### 测试与门禁

```powershell
python scripts/audit_port.py --strict `
  --tacz-jar libs/TACZ-Refabricated-1.21.11-1.1.8+fabric.1.21.11.R2.jar `
  --minecraft-named-jar <named-jar> `
  --minecraft-intermediary-jar <intermediary-jar> `
  --refmap build/resources/main/tacztweaks.refmap.json
./gradlew test
python scripts/check_server_log.py run/logs/latest.log
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
| `MixinApplyError` / `InvalidInjectionException` | 不要只看 Gradle 退出码，先跑 `scripts/audit_port.py` 与 `scripts/check_server_log.py` |
| `Out of space in CodeCache for adapters` | 先执行 `gradlew.bat --stop`，再重跑 `gradlew.bat build`，确保新的 `gradle.properties` JVM 参数已生效 |
| Daemon 内存不足 | 调整 `gradle.properties` 的 `org.gradle.jvmargs=-Xmx...` |

---

## 6. 只想改代码、不想每次重新下载

依赖第一次下载后走 `%USERPROFILE%\.gradle` 缓存，二次构建通常几十秒。

# 构建指南（BUILD.md）— 26.1.2 分支

本目录是 **TaCZ Tweaks 的 Fabric 26.1.2 移植版**，适配
[TaCZ_Refabricated_Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)
的 `26.1.2` 分支。

> 26.1.2 与 26.2 同源（未混淆 26.x、JDK 25），差异极小。

---

## 0. 你需要什么

| 项目 | 要求 |
|---|---|
| JDK | **Java 25**（必须） |
| 网络 | 首次构建要从 Maven 下载依赖（Gradle 缓存到 `%USERPROFILE%\.gradle`） |
| 磁盘 | 约 1GB（Minecraft 26.1.2 + Fabric API + Loom remap 产物） |

---

## 1. 装 JDK 25

下载 **Eclipse Temurin JDK 25**：

- Windows x64：https://adoptium.net/temurin/releases/?version=25
- 验证：
  ```powershell
  java -version
  ```
  应显示 `openjdk version "25.0.x"`。构建时 Gradle 优先用 `JAVA_HOME` 指向的 JDK。

---

## 2. 拿到源码

- **下载 zip**：从 Release 页下载 `tacztweaks-2.14.2+fabric.26.1.2.R1-src.zip`，解压；
- **git clone**：
  ```powershell
  git clone https://github.com/q14433686-arch/TaCZTweaks_Unofficial.git
  ```

---

## 3. 放两个「编译期依赖」到 libs/ 目录

这两个大 jar 通过 `flatDir` 引用，**必须手动下载**（其余依赖自动从 Maven 拉）。

进入项目根目录 `libs/`，放入（**文件名必须完全一致**）：

### ① TaCZ 本体（compileOnly，提供 mixin 目标类）

```
https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial/releases/download/26.1.2_R2/TACZ-Refabricated-26.1.2-1.1.8%2Bfabric.26.1.2.R2.jar
```
保存为：`TACZ-Refabricated-26.1.2-1.1.8+fabric.26.1.2.R2.jar`（约 58MB）

### ② YACL 配置库（implementation，配置 GUI）

```
https://cdn.modrinth.com/data/1eAoo2KR/versions/svTkvBec/yet_another_config_lib_v3-3.9.6%2B26.1-fabric.jar
```
保存为：`yacl-fabric.jar`（约 1MB）

放好之后 `libs/` 里应该是：
```
libs/
├── README.txt
├── TACZ-Refabricated-26.1.2-1.1.8+fabric.26.1.2.R2.jar
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

成功后输出 `BUILD SUCCESSFUL`，产物：

```
build/libs/tacztweaks-2.14.2+fabric.26.1.2.R1.jar   ← 扔进 .minecraft/mods/ 即可
```

---

## 5. 常见问题

| 现象 | 解决 |
|---|---|
| `Could not resolve ... TACZ-Refabricated ...` | `libs/` 里 TaCZ jar 缺失或文件名不对 |
| `Could not resolve ... yacl ...` | `libs/yacl-fabric.jar` 缺失 |
| `UnsupportedClassVersionError` / `invalid source release 25` | JDK 版本不对，换成 JDK 25 |
| `Daemon` 内存不足 | 调小 `gradle.properties` 的 `org.gradle.jvmargs=-Xmx...` |

---

## 6. 只想改代码、不想每次重新下载

依赖第一次下载后走 `%USERPROFILE%\.gradle` 缓存，二次构建通常几十秒。

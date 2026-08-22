# 构建指南（BUILD.md）

本模组是 **TaCZ Tweaks 的 NeoForge 26.1.2 移植版**，适配
[TaCZ: Renovated](https://github.com/q14433686-arch/TaCZ_Renovated)（modId 仍为 `tacz`）。
当前测试版本：`2.14.2+neoforge.26.1.2.Beta-1`。

> 状态提示：本分支的源码翻译（构建骨架、入口、事件、网络、数据管理器、配置屏）已完成，
> **但尚未在任何机器上执行过 `./gradlew build`、专服冒烟或客户端实测**。
> 未完成的验证项见 `docs/records/NEOFORGE_26_1_2_PORT_PLAN.md` 与
> `docs/records/NEOFORGE_26_1_2_PORT_RECORD.md`。

---

## 0. 你需要什么

| 项目 | 要求 |
|---|---|
| JDK | **Java 25**（Minecraft 26.1.2 的目标版本，低于 25 无法编译/运行） |
| 网络 | 首次构建要从 Maven / NeoForged 下载依赖（Gradle 缓存在 `~/.gradle`） |
| 磁盘 | 约 1GB（Minecraft 26.1.2 + NeoForge + 依赖 jar） |
| 内存 | 建议 ≥ 4GB 可用；低内存机器请下调 `org.gradle.jvmargs` |

---

## 1. 装 JDK 25

Eclipse Temurin JDK 25：https://adoptium.net/temurin/releases/?version=25

```powershell
java -version   # 应显示 openjdk version "25.0.x"
```

把 `JAVA_HOME` 指向 JDK 25，避免 Gradle daemon 用到旧 JDK。

---

## 2. 拿到源码

```powershell
git clone https://github.com/q14433686-arch/TaCZTweaks_Unofficial.git
cd TaCZTweaks_Unofficial
git checkout 26.1.2-neoforge
```

---

## 3. 放依赖 jar 到 libs/

本分支的编译期依赖都以本地文件引用（`libs/*.jar` 不进 Git）。清单、来源、许可证见
`RESOURCE_IMPORT_MANIFEST.tsv` 与 `libs/README.txt`。

必需：

```
libs/tacz-1.1.8+neoforge.26.1.2.R1.jar                  # https://github.com/q14433686-arch/TaCZ_Renovated/releases/tag/26.1.2_R1
libs/yet-another-config-lib-v3-3.9.6+26.1-neoforge.jar  # CurseForge YACL «3.9.6 for neoforge 26.1»
libs/commons-math3-3.6.1.jar                            # Maven Central
```

可选（只影响对应兼容层的编译与核对）：

```
libs/sound-physics-remastered-neoforge-1.5.1+26.1.2.jar
libs/firstaid-1.2.8+neoforge26.1.jar
libs/pillagers_gun-3.2.2-neoforge-26.1.2.jar
```

校验：

```powershell
python scripts/download_dependencies.py --check-only
```

> 目前 manifest 中这些条目的 `sha256` 是 `pending`（移植环境无法访问 CurseForge / GitHub 附件）。
> **发布构建前必须把实际文件的 SHA-256 写回 manifest**，否则 `--check-only` 只能检查文件是否存在。

---

## 4. 构建

```powershell
# Windows
gradlew.bat build

# Linux / macOS
./gradlew build
```

- 首次构建会下载 Gradle、NeoForge 26.1.2.97 与 Minecraft 26.1.2；
- `build` 会附带跑 `checkModIcon`（图标 SHA-256/尺寸/许可声明）与
  `checkVendoredDependencies`（libs 依赖存在性与摘要）；
- 单元测试只覆盖不依赖 Minecraft 的纯 JDK 逻辑（版本门、数学、拆栈）：`./gradlew test`。

### 产物位置

```
build/libs/tacztweaks-2.14.2+neoforge.26.1.2.Beta-1.jar                     ← 模组
build/example-pack/tacz-tweaks-example-pack-2.14.2+neoforge.26.1.2.Beta-1.zip ← 可重载示例包
```

Kotlin 标准库通过 `jarJar` 内嵌（NeoForge 没有 Fabric Language Kotlin 这类运行时提供者）。

---

## 5. 静态审计

```powershell
python scripts/audit_port.py --strict
python scripts/audit_port.py --strict --minecraft-jar <ModDevGradle 生成的 26.1.2 jar>
python scripts/check_release_consistency.py
```

`audit_port.py` 会检查 mixin 注册/目标方法、无行为配置项、语言键一致性、
`neoforge.mods.toml` 依赖范围、版本门禁常量与模组图标。
没有把 TaCZ jar 放进 `libs/` 时，目标类检查会全部退化为 WARN。

---

## 6. 专用服务器 smoke test 门禁

Gradle 任务返回 0 不代表游戏进程正常，对专服日志再跑：

```powershell
python scripts/check_server_log.py run/logs/latest.log
```

只有出现 `Done (...)!` 且不含 `MixinApplyError`、`InvalidInjectionException`、
`Failed to start the minecraft server` 才算通过。

---

## 7. 常见问题

| 现象 | 解决 |
|---|---|
| `Could not find ... libs/tacz-1.1.8+neoforge.26.1.2.R1.jar` | 依赖未放好，见第 3 步 |
| 启动即抛 `TaCZ Tweaks requires TaCZ 1.1.8+neoforge.26.1.2.R<n>` | 安装的 TaCZ 不是 NeoForge 26.1.2 线（可能是 Fabric 版或 1.21.11/26.2 版） |
| `invalid source release 25` / `UnsupportedClassVersionError` | Gradle daemon 用了旧 JDK：`gradlew --stop`，确认 `gradlew --version` 的 JVM 为 25 |
| Vineflower/反编译 OOM | 构建脚本已 `disableRecompilation = true`；如需 IDE 源码请在大内存机器上改回 false |
| Daemon 内存不足 | 调大 `gradle.properties` 的 `org.gradle.jvmargs` |

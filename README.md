# TaCZ Tweaks — Unofficial NeoForge 1.21.11 Port
[![CurseForge Downloads](https://cf.way2muchnoise.eu/full_1659175_downloads.svg)](https://www.curseforge.com/minecraft/mc-mods/tacz-tweaks-refabricated)
[![CurseForge Versions](https://cf.way2muchnoise.eu/versions/1659175.svg)](https://www.curseforge.com/minecraft/mc-mods/tacz-tweaks-refabricated/files)
[![GitHub Downloads](https://img.shields.io/github/downloads/q14433686-arch/TaCZTweaks_Unofficial/total?logo=github&label=GitHub%20Downloads)](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/releases)

非官方 NeoForge 1.21.11 移植版 TaCZ Tweaks,适配
[TaCZ-Renovated (NeoForge 1.21.11)](https://github.com/q14433686-arch/TaCZ-Renovated/tree/1.21.11)。

- 语义来源:姊妹项目 [TaCZTweaks_Unofficial](https://github.com/q14433686-arch/TaCZTweaks_Unofficial)
  `1.21.11` 分支(基线同步至 Fabric hotfix `f38f2ff`,2026-08-22)
  ;原项目 MUKSC/TaCZTweaks(Forge 1.20.1,GPL-3.0)
- 版本:`2.14.2+neoforge.1.21.11.Beta-1-hotfix`
  — 本 hotfix 在 `Beta-1` 基础上补入:① 严格 TaCZ 版本门禁(替换原 startsWith 宽松匹配);
  ② 音频 `SoundBufferLibraryMixin` 上游通配符修复(`lambda$getCompleteBuffer$*` + `require=0`,
  与 Fabric `f38f2ff` 对齐)。
- 许可:GPL-3.0-only。**非官方移植**,问题请勿提交给 MUKSC / TACZ Dev Team

## 支持的 TaCZ 版本(release family)

Mod 启动时执行严格校验,仅接受下列 release family 内的 TaCZ NeoForge 构建:

```
1.1.8+neoforge.1.21.11.r<n>
```

其中:

- 核心版本 `1.1.8`、平台 `neoforge`、MC 版本 `1.21.11` 必须严格匹配;
- revision 标记大小写均可(`r` / `R`),后接非负整数(`r0`、`R1`、`R2`、`r10` …);
- revision 以**数值**比较,故 `R10` 正确大于 `R2`(无字典序陷阱);
- 允许同 release family 的合理 hotfix/preview 后缀(例如 `r0-hotfix.1`);
- 不接受 Fabric 版本字符串(如 `1.1.8+fabric.1.21.11.R2`)、
  错误核心版本(如 `1.1.9+…`)、错误 MC 家族(如 `1.1.8+neoforge.26.2.R2`)、
  缺失/畸形 revision(如 `…r`、`…rx`、`…r-1`),
  以及任何“正确前缀 + 任意垃圾文本”的拼接串。

> 注意:NeoForge 线**不**使用 Fabric 侧的 “revision ≥ R2” 规则——NeoForge 1.21.11 线的基线 jar
> 就是 `1.1.8+neoforge.1.21.11.r0`,因此 r0 起全部同 release family 版本均被接受。

版本校验为单元测试覆盖,执行 `./gradlew test` 可回归。

## 构建(可直接编译)

- 环境要求:**JDK 21**;依赖 jar 已随仓库 `libs/` 内置
- 首次构建由 NeoForm 自动下载 Minecraft / NeoForge 21.11.45

Linux / macOS:

```bash
./gradlew build          # 产物: build/libs/tacztweaks-2.14.2+neoforge.1.21.11.Beta-1-hotfix.jar
./gradlew test           # 运行版本门禁等纯 JDK 单元测试
```

Windows(`cmd.exe` / PowerShell,仓库已补齐 `gradlew.bat`,无需装 Gradle):

```bat
gradlew.bat build
gradlew.bat test
REM 或直接:
build.bat                :: 默认执行 build
build.bat test           :: 仅跑单测
```

> 若 `build.bat` 报 "JAVA_HOME is not set",请将 JDK 21 加入 PATH,或设置环境变量
> `set "JAVA_HOME=C:\Program Files\Java\jdk-21"`(路径按实际安装位置调整)。

默认 JVM 参数已在 `gradle.properties` 里调为开发机可用的值
(`-Xmx2g -XX:MaxMetaspaceSize=512m -XX:ReservedCodeCacheSize=256m`)，这是
Kotlin 2.4.10 (K2) 稳定编译所必需的——过低的 `ReservedCodeCacheSize`
(例如 40m)会让 JIT 在编译 Kotlin 时抛
`Out of space in CodeCache for adapters`，并可能连锁触发
`java.lang.InternalError: MethodHandle.linkToStatic(...)`
这种看起来像 JDK 版本错的二次异常。出现这种报错请**加大 CodeCache/heap**，
而不是换 JDK。

低内存机器(可用 RAM < 2GB 的 CI / 沙箱)建议用 daemon + SerialGC + client 模式
的覆盖参数(会慢一些但能跑通):

Linux / macOS:

```bash
export JAVA_TOOL_OPTIONS="-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"
./gradlew build --no-daemon
```

Windows(cmd):

```bat
set JAVA_TOOL_OPTIONS=-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k
build.bat --no-daemon
```

## 安装

`mods/` 放入:

1. `tacz-1.1.8+neoforge.1.21.11.r0.jar`(TaCZ-Renovated 1.21.11;同 release family 的
   R1/R2/…/hotfix 亦受支持)
2. `tacztweaks-2.14.2+neoforge.1.21.11.Beta-1-hotfix.jar`(本 mod)
3. `yet_another_config_lib_v3-3.8.2+1.21.11-neoforge.jar`(必需,配置界面)
4. 可选:`sound-physics-remastered-neoforge-1.21.11-1.5.1.jar`(音效兼容)

## 已知边界(如实声明)

- 实机验收矩阵与姊妹 Beta-1 同口径,未覆盖面见 `docs/records/`
- First Aid 无 1.21.11 NeoForge 构建、Pillager's Gun 仅 Fabric → 兼容代码保留、运行期门控不触发
- 移植台账:`docs/records/WP_TWEAKS_PORT_RECORD.md`、`docs/records/WP_TWEAKS_0_AUDIT.md`

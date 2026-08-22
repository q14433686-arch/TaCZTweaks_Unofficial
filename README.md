# TaCZ Tweaks — Unofficial NeoForge 1.21.11 Port

非官方 NeoForge 1.21.11 移植版 TaCZ Tweaks,适配
[TaCZ-Renovated (NeoForge 1.21.11)](https://github.com/q14433686-arch/TaCZ-Renovated/tree/1.21.11)。

- 语义来源:姊妹项目 [TaCZTweaks_Unofficial](https://github.com/q14433686-arch/TaCZTweaks_Unofficial)
  `1.21.11` 分支(基线同步至 `f38f2ff`,2026-08-22);原项目 MUKSC/TaCZTweaks(Forge 1.20.1,GPL-3.0)
- 版本:`2.14.2+neoforge.1.21.11.Beta-1`
- 许可:GPL-3.0-only。**非官方移植**,问题请勿提交给 MUKSC / TACZ Dev Team

## 构建(可直接编译)

- 环境要求:**JDK 21**;依赖 jar 已随仓库 `libs/` 内置
- 首次构建由 NeoForm 自动下载 Minecraft / NeoForge 21.11.45

```bash
./gradlew build          # 产物: build/libs/tacztweaks-2.14.2+neoforge.1.21.11.Beta-1.jar
```

低内存机器(≤2GB):

```bash
export JAVA_TOOL_OPTIONS="-Xmx384m -XX:MaxMetaspaceSize=160m -XX:+UseSerialGC"
./gradlew build --no-parallel
```

## 安装

`mods/` 放入:

1. `tacz-1.1.8+neoforge.1.21.11.r0.jar`(TaCZ-Renovated 1.21.11)
2. `tacztweaks-2.14.2+neoforge.1.21.11.Beta-1.jar`(本 mod)
3. `yet_another_config_lib_v3-3.8.2+1.21.11-neoforge.jar`(必需,配置界面)
4. 可选:`sound-physics-remastered-neoforge-1.21.11-1.5.1.jar`(音效兼容)

## 已知边界(如实声明)

- 实机验收矩阵与姊妹 Beta-1 同口径,未覆盖面见 `docs/records/`
- First Aid 无 1.21.11 NeoForge 构建、Pillager's Gun 仅 Fabric → 兼容代码保留、运行期门控不触发
- 移植台账:`docs/records/WP_TWEAKS_PORT_RECORD.md`、`docs/records/WP_TWEAKS_0_AUDIT.md`

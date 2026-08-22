# 构建指南

目标：Minecraft 26.2、NeoForge 26.2.0.64、Java 25、
TaCZ: Renovated `1.1.8+neoforge.26.2.R1`。当前产物版本：
`2.14.2+neoforge.26.2.Beta-1`。

> 当前 26.2 工作树尚未在本沙箱构建：这里没有 JDK 25，Maven/Modrinth/GitHub release-assets
> 二进制下载也不可达。以下是必须在具备依赖的机器上执行的验收流程，不是完成声明。

## 1. 环境

| 项目 | 要求 |
|---|---|
| JDK | Java 25；`java -version` 与 `./gradlew --version` 都必须显示 25 |
| 网络 | 首次构建需访问 Gradle、Maven Central 和 NeoForged Maven |
| 磁盘 | 建议至少 2 GB 空闲 |
| 内存 | 建议至少 4 GB；低内存环境可下调 `org.gradle.jvmargs` |

ModDevGradle 版本固定为 `2.0.144`。Minecraft 26.2 未混淆，不配置 mappings。

## 2. 获取源码

```bash
git clone https://github.com/q14433686-arch/TaCZTweaks_Unofficial.git
cd TaCZTweaks_Unofficial
git checkout 26.2-neoforge
```

## 3. 准备 `libs/`

jar 不提交到 Git。来源、用途、许可证和 SHA-256 见
`RESOURCE_IMPORT_MANIFEST.tsv` 与 `libs/README.txt`。

### 编译必需

```text
libs/tacz-1.1.8+neoforge.26.2.R1.jar
libs/yet_another_config_lib_v3-3.9.6+26.2-neoforge.jar
libs/sound-physics-remastered-neoforge-1.5.1+26.2.jar
libs/commons-math3-3.6.1.jar
```

来源：

- TaCZ: Renovated：<https://github.com/q14433686-arch/TaCZ_Renovated/releases/tag/26.2_R1>
- YACL：Modrinth/CurseForge 的 NeoForge 26.2 构建（3.9.5 起；维护者首轮构建使用 3.9.6；双端必需）
- Sound Physics Remastered：**[NEOFORGE][26.2] 1.5.1+26.2**
- Commons Math 3.6.1：Maven Central

### 可选兼容核对件

```text
libs/firstaid-1.3.0-patched+neoforge26.2.jar  # 1.2.8 存在，但当前 shader override 未核验，故不声明支持
libs/pillagers_gun-3.3.5-neoforge-26.2.jar
```

First Aid 与 Pillager's Gun 通过反射/字符串目标接入，不是编译硬依赖；放入后可让人工/jar 审计更接近
实际运行组合。

构建脚本会忽略 `- _ . +` 与空格后按关键词匹配，并要求目标 jar 名同时包含 `26.2`；任何名字含
`fabric` 的 jar 都会被故意排除。这样旧 26.1.2 或错误加载器文件不会被静默拿来编译。

```bash
python3 scripts/download_dependencies.py --check-only
```

`pending` 摘要只允许用于移植阶段；发布前必须下载真实文件、计算 SHA-256 并写回 manifest。
TaCZ 行已记录 GitHub release API 公布的摘要，但仍建议发布机本地复算。

## 4. 一致性与静态审计

```bash
python3 scripts/check_mod_icon.py
python3 scripts/check_release_consistency.py
python3 scripts/audit_port.py --strict
```

如果依赖 jar 尚未放入，`audit_port.py` 会报告无法检查目标类；这不算 mixin 验证通过。
可以用 `--minecraft-jar <path>` 让脚本检查 MDG 生成的 Minecraft 26.2 jar。

## 5. 单元测试与构建

```bash
./gradlew test
./gradlew clean build --stacktrace
```

`build` 同时执行纯 Groovy 的图标、依赖 manifest 门禁，不依赖 Python。测试源集只包含纯 JDK 的
版本门、数学、投射物索引和拆栈测试，不经 ModDevGradle。

预期产物：

```text
build/libs/tacztweaks-2.14.2+neoforge.26.2.Beta-1.jar
build/example-pack/tacz-tweaks-example-pack-2.14.2+neoforge.26.2.Beta-1.zip
```

Kotlin stdlib 2.4.10 通过 `jarJar` 内嵌；依赖模组和本地 `libs/*.jar` 不进入发布 jar。

## 6. 客户端验收

把发布 jar 与匹配的 TaCZ: Renovated、YACL 放进一个干净 NeoForge 26.2.0.64 客户端。
启动日志不得出现：

- `MixinApplyError`
- `InvalidInjectionException`
- `Critical injection failure`
- `Scanned 0 target(s)`

进入主界面后执行最小实测：

1. 进入世界并拿起 TaCZ 枪；
2. 开镜；
3. 换弹；
4. 卸弹；
5. 从 NeoForge 模组列表打开配置屏并保存；
6. 执行数据包重载，确认无 reload error。

只有实际完成后才能在兼容矩阵中写“客户端/游戏验证”。

## 7. 专服验收

```bash
./gradlew runServer --args='--nogui'
python3 scripts/check_server_log.py run/logs/latest.log
```

必须出现 `Done (...)!`，且日志中没有 mixin/injection/startup fatal。客户端类使用独立
`@Mod(..., dist = Dist.CLIENT)` 入口；专服冒烟仍不可省略。

## 8. 常见问题

| 现象 | 处理 |
|---|---|
| `Missing required compile dependencies` | 按第 3 节补齐 26.2 NeoForge jar；检查文件名是否含 `26.2`，且不含 `fabric` |
| `Unresolved reference: tacz / dev / com.sonicether` | 本地依赖未识别；先看 `checkRequiredDependencies` 列出的文件名 |
| `invalid source release 25` / `UnsupportedClassVersionError` | Gradle daemon 使用了旧 JDK；`./gradlew --stop` 后修正 `JAVA_HOME` |
| 启动拒绝 TaCZ 版本 | 只接受 `1.1.8+neoforge.26.2.R1` 及同家族后续 R<n> |
| Vineflower OOM | 默认已 `disableRecompilation = true`；IDE 反编译只应在大内存机器启用 |
| `BlocksAttacks#hurtBlockingItem` 注入为 0 | 核对 NeoForge 26.2.x patches；当前主路径必须是 6 参 `(Level, ItemStack, LivingEntity, InteractionHand, float, int)` |

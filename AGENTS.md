# AGENTS.md — TaCZ Tweaks Fabric 26.3 维护规则

供 AI 编码助手和人类协作者使用。**本文件只约束分支 `26.3`**：
Minecraft 26.3 / Java 25 / 未混淆 `TaCZ_Refabricated_Unofficial` 26.3 R1。
其它发行线见 [`docs/BRANCHES.md`](docs/BRANCHES.md)，不要把本线的 mixin、Java 或依赖假设套到 NeoForge 或 1.21.11 分支。

本线的移植进度、已适配项与未验证项见 [`docs/PORT_26_3_STATUS.md`](docs/PORT_26_3_STATUS.md)。

## 1. 事实来源顺序

1. 当前源码调用链与 26.3 class descriptor；
2. `libs/TACZ-Refabricated-26.3-1.1.8+fabric.26.3.R1.jar`（不在 Git 里，用
   `python3 scripts/download_dependencies.py` 重建）；
3. 目标仓库 `q14433686-arch/TaCZ_Refabricated_Unofficial` 的 `26.3` 当前源码/文档；
4. 原版 MUKSC/TaCZTweaks v2.14.2；
5. 注释和历史移植笔记只作线索，不能单独作为结论。

`PORTING_NOTES.md` §1–§7.16 是过程记录，其中“删除/不存在/不能做”可能已被后文推翻。
当前结论以 `AUDIT.md`、§7.17+ 和实际代码为准。

## 2. 改动门禁

提交前执行：

```bash
python3 scripts/download_dependencies.py   # libs/*.jar 不在 Git 里，先重建
python3 scripts/audit_port.py --strict
python3 scripts/check_release_consistency.py
./gradlew clean build
```

有 Loom 生成的 Minecraft jar 时再执行：

```bash
python3 scripts/audit_port.py --strict --minecraft-jar /path/to/minecraft-merged-26.3.jar
```

**网络受限环境（只能访问 api.github.com）**：`./gradlew` 跑不起来，依赖域全部不可达。
此时唯一的验证途径是把改动 push 到 `arena/**` 分支，让 `.github/workflows/` 里的四条
流程去编译，再用下面这条命令读回编译日志：

```bash
gh api repos/q14433686-arch/TaCZTweaks_Unofficial/contents/build-reports/compile-java.log?ref=<分支> \
  --jq '.content' | base64 -d
```

**不要因为本地跑不了 Gradle 就把「未编译」写成「已编译」**，见 §4。

审计必须保持 0 error / 0 warning。编译通过仍不代表 mixin 运行时安全；新增 mixin 必须核对：

- mixin 所在 common/client 分组；
- 目标方法完整 descriptor；
- `@At` 的 owner、方法/字段名和 descriptor；
- 与 TaCZ 自身 mixin 的优先级、嵌套顺序；
- 是否需要 ThreadLocal，是否在异常/RETURN/断线时清理。

## 3. 版本一致性

修改 `gradle.properties` 的 `mod_version`、Minecraft/Fabric/Kotlin/MixinExtras 版本时，必须在同一
工作分支同步 README、BUILD、CHANGELOG、`fabric.mod.json` 和 `RESOURCE_IMPORT_MANIFEST.tsv`。
`scripts/audit_port.py` 与 `scripts/check_release_consistency.py`（CI 的 `consistency` 流程）
会校验当前发布版本标识唯一一致。

## 4. 不得夸大

- “实现”“静态验证”“编译通过”“游戏内实测”必须分开表述；
- 跳过、禁用、默认关闭不能写成修复；
- 可选模组兼容必须核对真实 Fabric 26.2 发行物与源码/API，不能凭旧文档说不存在；
- 尚未完成客户端、集成服、独立服矩阵时，PR 保持 Draft。

## 5. 生命周期与线程

- 不让 static 集合长期强持有 ServerLevel、Player、Entity；优先值类型、WeakHashMap 或明确清理；
- 集成服务器客户端/服务端线程共享 JVM，普通 static 上下文不得跨线程复用；
- 网络 receiver 中的世界/实体修改必须调度到对应 client/server executor；
- 客户端断线、服务器停止、资源重载都要检查缓存是否需要清理；
- 不假设客户端与服务端在跨维度/重生时以相同方式替换玩家实例。

## 6. 26.3 常用替代写法

- 实体/物品标签：`BuiltInRegistries.<REGISTRY>.wrapAsHolder(value).is(tag)`；
- 键位：`InputConstants.Type.KEYBOARD` / `InputConstants.KEY_*` / `InputConstants.PRESS`
  （26.3 起 `Type.KEYSYM` 与 `GLFW.GLFW_KEY_*` 不再使用；`InputConstants.UNKNOWN` 仍在）；
- 声音实例解析：`AbstractSoundInstance#getOrResolve(SoundManager)`（原 `resolve`）；
- 实体渲染剔除：`EntityRenderer#shouldRender(...)` 末尾多一个 `float partialTicks`；
- 数据 reload：`ResourceLoader` v1，不新增 deprecated `ResourceManagerHelper`；
- 权限：`player.permissions().hasPermission(...)`；
- 空 payload：`StreamCodec.unit(singleton)`；
- 有字段的 payload：`StreamCodec.ofMember(Message::write, Message::new)`；
- `Identifier` 是共享的 final class（不是 record）；不要向共享资源标识挂每次播放的可变状态，使用有生命周期的请求上下文/独立缓存。

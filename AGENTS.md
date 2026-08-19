# AGENTS.md — TaCZ Tweaks Fabric 26.2 维护规则

供 AI 编码助手和人类协作者使用。当前目标是 Minecraft 26.2 / Java 25 / 未混淆
`TaCZ_Refabricated_Unofficial` R2。

## 1. 事实来源顺序

1. 当前源码调用链与 26.2 class descriptor；
2. `libs/TACZ-Refabricated-26.2-1.1.8+fabric.26.2.R2.jar`；
3. 目标仓库 `q14433686-arch/TaCZ_Refabricated_Unofficial` 的 `26.2(main)` 当前源码/文档；
4. 原版 MUKSC/TaCZTweaks v2.14.2；
5. 注释和历史移植笔记只作线索，不能单独作为结论。

`PORTING_NOTES.md` §1–§7.16 是过程记录，其中“删除/不存在/不能做”可能已被后文推翻。
当前结论以 `AUDIT.md`、§7.17+ 和实际代码为准。

## 2. 改动门禁

提交前执行：

```bash
python3 scripts/audit_port.py --strict
./gradlew clean build
```

有 Loom 生成的 Minecraft jar 时再执行：

```bash
python3 scripts/audit_port.py --strict --minecraft-jar /path/to/minecraft-merged-26.2.jar
```

审计必须保持 0 error / 0 warning。编译通过仍不代表 mixin 运行时安全；新增 mixin 必须核对：

- mixin 所在 common/client 分组；
- 目标方法完整 descriptor；
- `@At` 的 owner、方法/字段名和 descriptor；
- 与 TaCZ 自身 mixin 的优先级、嵌套顺序；
- 是否需要 ThreadLocal，是否在异常/RETURN/断线时清理。

## 3. 版本一致性

修改 `gradle.properties` 的 `mod_version`、Minecraft/Fabric/Kotlin/MixinExtras 版本时，必须在同一
工作分支同步 README、BUILD 和依赖表。`scripts/audit_port.py` 会校验当前发布版本标识唯一一致。

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

## 6. 26.2 常用替代写法

- 实体/物品标签：`BuiltInRegistries.<REGISTRY>.wrapAsHolder(value).is(tag)`；
- 数据 reload：`ResourceLoader` v1，不新增 deprecated `ResourceManagerHelper`；
- 权限：`player.permissions().hasPermission(...)`；
- 空 payload：`StreamCodec.unit(singleton)`；
- 有字段的 payload：`StreamCodec.ofMember(Message::write, Message::new)`；
- `Identifier` 是共享的 final class（不是 record）；不要向共享资源标识挂每次播放的可变状态，使用有生命周期的请求上下文/独立缓存。

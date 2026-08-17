# TaCZ Tweaks → TaCZ_Refabricated_Unofficial 适配进度

> 目标分支：`26.2(main)`（未混淆，Java 25）
> 本环境：无 JDK 25、无 maven 网络 → **只能做源码级适配，无法本地编译验证**。
> 凡 `[VERIFY]` 标记处均需在可构建环境实测。

## 阶段总览

- [x] 1. 需求分析 / API 兼容性核查（见 `ADAPTATION_PLAN.md`）
- [ ] 2. 构建系统：Stonecutter 多加载器 → 单一 Fabric 26.2
- [ ] 3. 加载器/平台层迁移（`platform/` + `kotlin-fabric/`）
- [ ] 4. Minecraft API 迁移（1.21.1 → 26.2，含 `ResourceLocation→Identifier` 等）
- [ ] 5. 网络层迁移（Fabric Networking API v1）
- [ ] 6. 资源/元数据更新（`fabric.mod.json`、mixins json、pack.mcmeta）
- [ ] 7. 兼容模块逐个核查/排除
- [ ] 8. 编译与运行验证（需外部环境）

## 进度明细

### 2. 构建系统
- 待办：`settings.gradle.kts` 去掉 Stonecutter 4 版本矩阵；新增单一 `build.gradle.kts`
  参考目标仓库（Loom 1.17、无 mappings、Java 25）；删除 `forge.gradle.kts`/`neoforge.gradle.kts`/`stonecutter.gradle.kts`。
- 版本参数：`minecraft=26.2`、`loader=0.19.3`、`fabric_api=0.155.2+26.2`、`yacl=3.9.5+26.2-fabric`、
  `fabric_kotlin`（需核 26.2 版本）、TaCZ 依赖指向本仓库官方非官方端口的本地 jar（`libs/` flatDir 或 compileOnly 路径）。

### 3. 平台层
- 待办：`core/Loader.kt` 剥离 forge/neoforge 分支，仅保留 fabric 分支；
  核对 `FabricPlatformNetwork` 的 26.2 网络注册 API。

### 4. Minecraft API 迁移
- [x] `ResourceLocation` → `net.minecraft.resources.Identifier`：全局完成（35 个文件）。
  含 import、类型引用、mixin 字符串描述符 `Lnet/minecraft/resources/Identifier;`。
  移除了 `core/Identifier.kt` 里的自定义 `Identifier(...)` 辅助函数（与 26.2 原生 `Identifier` 类型
  冲突），改用原生工厂 `Identifier.fromNamespaceAndPath` / `withDefaultNamespace`，并更新 5 个调用点。
- 待办：其余 26.x 改名（`GuiGraphics→GuiGraphicsExtractor`、`drawString→text` 等）逐个 mixin 核对。

### 3. 平台层
- [x] 用解析脚本 `tools/resolve_stonecutter.py` 把所有 64 个含 `//?` 条件注释的源文件
  解析到 fabric + 26.2 目标（保留 active 分支，剥离 forge/neoforge 分支与 `<1.20.5`/`<1.21` 等版本分支）。
- 待办：核对 `FabricPlatformNetwork` 的 26.2 网络注册 API。

### 5. 网络层
- 待办：核对 Fabric Networking API v1 在 26.2 的注册 API（`PayloadTypeRegistry`、`ServerPlayNetworking`）。

### 6. 资源/元数据
- 待办：`fabric.mod.json`（loader/fabric_api/kotlin/yacl/tacz 版本）、`tacztweaks-fabric.mixins.json`
  （移除 forge 专用 mixin）、`pack.mcmeta` pack_format。

### 7. 兼容模块
- 待办：VS/Sable/FirstAid/LRTactical/MTS/LSO/Pillagers/Cuffed/Sound Physics 逐个核 26.2 可用性；
  无构建者走"编译期排除 + 门面禁用"。

## 关键决定
- 配置 GUI：**保留 YACL**（`3.9.5+26.2-fabric` 已发布）。
- 范围：先核心（配置/属性/平衡/数据包/玩法/音画），兼容模块后补。

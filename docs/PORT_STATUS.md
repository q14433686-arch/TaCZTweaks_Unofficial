# TaCZ Tweaks → TaCZ_Refabricated_Unofficial 适配进度

> 目标分支：`26.2(main)`（未混淆，Java 25）
> 本环境：无 JDK 25、无 maven 网络 → **只能做源码级适配，无法本地编译验证**。
> 凡 `[VERIFY]` 标记处均需在可构建环境实测。

## 阶段总览

- [x] 1. 需求分析 / API 兼容性核查（见 `ADAPTATION_PLAN.md`）
- [x] 2. 构建系统：Stonecutter 多加载器 → 单一 Fabric 26.2
- [x] 3. 平台层：解析全部 `//?` 条件注释到 fabric + 26.2
- [x] 4. Minecraft API 迁移：`ResourceLocation→Identifier`、`GuiGraphics→GuiGraphicsExtractor`
- [ ] 5. 网络层核对（Fabric Networking v1 已确认 API 稳定，待编译验证）
- [x] 6. 资源/元数据：`fabric.mod.json` 更新、`pack.mcmeta` pack_format=61
- [ ] 7. 兼容模块逐个核查/排除
- [ ] 8. 编译与运行验证（**需外部环境**：本沙箱无 JDK 25、无 maven 网络）

## 进度明细

### 2. 构建系统（已完成）
- `settings.gradle.kts`：去掉 Stonecutter，单一 root project。
- 新增 `build.gradle.kts`：Loom 1.17、Java 25、无 mappings（26.2 未混淆）、YACL + ModMenu + mixinsquared。
- 删除 `forge.gradle.kts`/`neoforge.gradle.kts`/`stonecutter.gradle.kts`/`fabric.gradle.kts`。
- 重写 `buildSrc`（去掉 Stonecutter/dotenv/mod-publish 依赖）。
- `gradle.properties`：`minecraft=26.2`、`loader=0.19.3`、`fabric_api=0.155.2+26.2`、
  `yacl=3.9.5+26.2-fabric`、`java.version=25`、`pack_format=61`。
- TaCZ 依赖 `cn.sh1rocu:tacz-refabricated:1.1.8+fabric.26.2.R2` 指向 `libs/` flatDir（需放置本地 jar）。

### 3. 平台层（已完成源码级）
- 新增 `tools/resolve_stonecutter.py`：把所有 64 个含 `//?` 条件注释的源文件解析到
  fabric + 26.2 目标（保留 active 分支，剥离 forge/neoforge 分支与 `<1.20.5`/`<1.21` 版本分支）。
- `core/Loader.kt` 解析后仅保留 fabric 分支。
- `FabricPlatformNetwork`：Fabric Networking v1（`PayloadTypeRegistry`/`ServerPlayNetworking` 等），API 在 26.2 稳定。
- `FabricPlatformEvents[Client]`、`FabricPlatformClient` 已核对。
- 移除 `core/Identifier.kt` 的自定义 `Identifier(...)` 辅助函数（与原生 `Identifier` 类型冲突）。

### 4. Minecraft API 迁移（已完成可确认项）
- [x] `ResourceLocation` → `net.minecraft.resources.Identifier`：35 个文件全局完成，
  含 import、类型引用、mixin 字符串描述符。
- [x] `GuiGraphics` → `GuiGraphicsExtractor`：`hide_hit_markers/RenderCrosshairEventMixin`。
- [VERIFY] 其余 mixin 命中的 vanilla 类（`LivingEntity`/`Player`/`ClipContext`/`SoundEngine`/
  `MouseHandler`/`ItemInHandLayer`/`ProtectionEnchantment`/`EnderMan`/`BlockBehaviour`/`Explosion` 等）
  的方法签名未逐一核对——**必须编译验证**。

### 6. 资源/元数据（已完成）
- `fabric.mod.json`：更新 contact/description（指向非官方端口）。
- `pack.mcmeta`：pack_format=61。

### 7. 兼容模块（待办）
- 待核 26.2 可用性：VS/Sable/FirstAid/LRTactical/MTS/LSO/Pillagers/Cuffed/Sound Physics。

## 关键决定
- 配置 GUI：**保留 YACL**（`3.9.5+26.2-fabric` 已发布）。
- 范围：先核心，兼容模块后补。

## 下一步（需可构建环境）
1. `gradlew build` 收集编译错误 → 逐个修 vanilla 签名。
2. 逐 feature 冒烟测试。
3. 兼容模块逐个启用/禁用。

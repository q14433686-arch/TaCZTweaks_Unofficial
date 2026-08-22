本目录存放 NeoForge 26.2 构建所需的二进制依赖（build.gradle 按文件名关键词匹配）。
完整来源、许可证、用途、是否进入发布 jar 以仓库根目录的 RESOURCE_IMPORT_MANIFEST.tsv 和
LICENSES.md 为准。jar 本身不进入 Git（见 .gitignore 的 libs/*.jar）。

校验/下载：

  python3 scripts/download_dependencies.py --check-only
  python3 scripts/download_dependencies.py

文件名匹配前会忽略 - _ . + 和空格；26.2 依赖还必须同时含 26.2（规范化后为 262）。
名字里含 "fabric" 的文件会被故意忽略，防止误用 Fabric 版。

NeoForge 26.2 线需要：

1) tacz-1.1.8+neoforge.26.2.R1.jar                       （必需）
   来源：https://github.com/q14433686-arch/TaCZ_Renovated/releases/tag/26.2_R1
   作用：com.tacz.guns.* / me.xjqsh.lrtactical.* API 与全部 TaCZ mixin 目标。
   不进入本项目发布 jar；玩家必须单独安装。

2) yet_another_config_lib_v3-3.9.5+26.2-neoforge.jar     （必需）
   来源：Modrinth/CurseForge YACL “3.9.5 for neoforge 26.2”（3.9.5 起）。
   作用：配置持久化/同步基类与 NeoForge 模组列表配置屏；服务端也必须安装。

3) sound-physics-remastered-neoforge-1.5.1+26.2.jar      （当前编译必需，运行可选）
   来源：Modrinth/CurseForge “[NEOFORGE][26.2] 1.5.1+26.2”。
   作用：可选 Sound Physics mixin 目标验证；运行时由 ModList/mixin plugin 门控。

4) commons-math3-3.6.1.jar                               （编译必需）
   TaCZ jar 内已内嵌，本项目仅在编译期需要其类型。

可选核对件（反射 + 字符串目标，构建不强制）：

5) firstaid-1.3.0-patched+neoforge26.2.jar
   1.2.8+neoforge26.2 也有发行物，但当前 1.3.x shader override 未对它核验，因此本移植只声明 >=1.3.0、<1.4.0。

6) pillagers_gun-3.3.5-neoforge-26.2.jar
   26.2 NeoForge 发行物存在；当前元数据限制 >=3.3.5、<3.4.0。

manifest 中标记 pending 的 SHA-256 必须在发布前用实际下载文件补齐。TaCZ 行的摘要来自
GitHub release asset API；本沙箱无法访问 release-assets/Modrinth/Maven 二进制下载端点，
因此没有把这些 jar 留在工作树中，也没有声称本地复算过摘要。

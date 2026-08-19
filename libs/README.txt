本目录存放已固定版本和 SHA-256 的二进制依赖（构建时经 flatDir / files 引用，见 build.gradle.kts）。
完整来源、许可证、用途、是否进入发布 jar 请以仓库根目录的 RESOURCE_IMPORT_MANIFEST.tsv 和 LICENSES.md 为准。

重建/校验方式：

  python3 scripts/download_dependencies.py --check-only
  python3 scripts/download_dependencies.py

1) TACZ-Refabricated-26.1.2-1.1.8+fabric.26.1.2.R2.jar  (compileOnly, testRuntimeOnly)
   来源：https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial/releases/tag/26.1.2_R2
   SHA-256：fcfdfe6e6356ae5f33c7a6a439ed656f2b9f1034d09cd850a2666c7399febf6f
   许可证：其 fabric.mod.json 声明为 GPL3 / CC BY-NC-ND 4.0
   作用：提供 com.tacz.guns.* 与 cn.sh1rocu.tacz.* 的编译类（mixin 目标）。
   是否进入本项目发布 jar：否；玩家需要单独安装 TaCZ Refabricated。

注：YACL 不再需要手动放入 libs/；Gradle 会从 Modrinth Maven 自动解析精确版本 3.9.6+26.1-fabric（version id svTkvBec）。

缺少 TaCZ jar 时构建会失败。请不要替换为同名但不同哈希的文件；校验失败应更新 manifest、许可证说明和测试记录后再合入。

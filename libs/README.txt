本目录存放两个已固定版本和 SHA-256 的二进制依赖（构建时经 flatDir / files 引用，见 build.gradle.kts）。
完整来源、许可证、用途、是否进入发布 jar 请以仓库根目录的 RESOURCE_IMPORT_MANIFEST.tsv 和 LICENSES.md 为准。

重建/校验方式：

  python3 scripts/download_dependencies.py --check-only
  python3 scripts/download_dependencies.py

1) TACZ-Refabricated-26.2-1.1.8+fabric.26.2.R2.jar  (compileOnly, testRuntimeOnly)
   来源：https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial/releases/tag/26.2_R2
   SHA-256：6c0584ca457acb8403816e61a83260d68639f9bda4b3fc0717038f932325d5b5
   许可证：其 fabric.mod.json 声明为 GPL3 / CC BY-NC-ND 4.0
   作用：提供 com.tacz.guns.* 与 cn.sh1rocu.tacz.* 的编译类（mixin 目标）。
   是否进入本项目发布 jar：否；玩家需要单独安装 TaCZ Refabricated。

2) yacl-fabric.jar  (implementation)
   来源：Modrinth "YetAnotherConfigLib" 3.9.6+26.2-fabric
   https://cdn.modrinth.com/data/1eAoo2KR/versions/cnfPzuFU/yet_another_config_lib_v3-3.9.6%2B26.2-fabric.jar
   SHA-256：829396c3b3e7d1801ae0e9e2921d0454c5a3078afdb6c6dda6b3d1819dfa0e3f
   许可证：LGPL-3.0-or-later
   作用：配置系统（YACL v3）。
   是否进入本项目发布 jar：否；玩家需要单独安装 YetAnotherConfigLib。

缺少这两个 jar 时构建会失败。请不要替换为同名但不同哈希的文件；校验失败应更新 manifest、许可证说明和测试记录后再合入。

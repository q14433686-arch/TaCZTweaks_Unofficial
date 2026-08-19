本目录存放两个已固定版本和校验和的二进制依赖（构建时经 flatDir / files 引用，见 build.gradle.kts）。
完整来源、许可证、用途、是否进入发布 jar 请以仓库根目录的 RESOURCE_IMPORT_MANIFEST.tsv 和 LICENSES.md 为准。

重建/校验方式：

  python3 scripts/download_dependencies.py --check-only
  python3 scripts/download_dependencies.py

1) TACZ-Refabricated-1.21.11-1.1.8+fabric.1.21.11.R2.jar  (modCompileOnly, testRuntimeOnly)
   来源：https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial/releases/tag/1.21.11_R2
   SHA-256：34d117f316a2ab6b3ca4d9bfbf61a87fefc416c2919874e057688054d75e788d
   许可证：其 fabric.mod.json 声明为 GPL3 / CC BY-NC-ND 4.0
   作用：提供 com.tacz.guns.* 与 cn.sh1rocu.tacz.* 的编译类（mixin 目标）。
   是否进入本项目发布 jar：否；玩家需要单独安装 TaCZ Refabricated。

2) yacl-fabric.jar  (modImplementation)
   来源：Modrinth "YetAnotherConfigLib" 3.8.2+1.21.11-fabric
   https://cdn.modrinth.com/data/1eAoo2KR/versions/pHWDw3Vc/yet_another_config_lib_v3-3.8.2%2B1.21.11-fabric.jar
   SHA-512：392db7d471030cca27483ecf58c626a14cd73d71a18afe6d4173c6b030948b8a925b36e708d4cc2c897dfa3f20a7f23b999fc18aa6d36c156da29037601153ac
   许可证：LGPL-3.0-or-later
   作用：配置系统（YACL v3）。
   是否进入本项目发布 jar：否；玩家需要单独安装 YetAnotherConfigLib。

缺少这两个 jar 时构建会失败。请不要替换为同名但不同哈希的文件；校验失败应更新 manifest、许可证说明和测试记录后再合入。

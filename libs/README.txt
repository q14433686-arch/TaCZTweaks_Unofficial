本目录存放两个已固定版本和 SHA-256 的二进制依赖（构建时经 flatDir / files 引用，见 build.gradle.kts）。
**这两个 jar 不进 Git**（见 .gitignore：`libs/*.jar`）——TaCZ 本体约 58MB，每条发行线各存一份会把仓库撑大。
完整来源、许可证、用途、是否进入发布 jar 请以仓库根目录的 RESOURCE_IMPORT_MANIFEST.tsv 和 LICENSES.md 为准。

重建/校验方式（CI 每次运行都会执行第二条）：

  python3 scripts/download_dependencies.py            # 缺失就按 manifest 下载并校验
  python3 scripts/download_dependencies.py --check-only # 只校验，不下载

1) TACZ-Refabricated-26.3-1.1.8+fabric.26.3.R1.jar  (compileOnly, testRuntimeOnly)
   来源：https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial/releases/tag/26.3_R1
   SHA-256：faa1ce770c184ee2a69ef901a9402f83ee81382372ede07222270fde3530620f
   许可证：其 fabric.mod.json 声明为 GPL3 / CC BY-NC-ND 4.0
   作用：提供 com.tacz.guns.* 与 cn.sh1rocu.tacz.* 的编译类（mixin 目标）。
   是否进入本项目发布 jar：否；玩家需要单独安装 TaCZ Refabricated。

2) yacl-fabric.jar  (implementation)
   来源：Modrinth "YetAnotherConfigLib" 3.9.7+26.3-fabric
   https://cdn.modrinth.com/data/1eAoo2KR/versions/s9SjoFu1/yet_another_config_lib_v3-3.9.7%2B26.3-fabric.jar
   SHA-256：manifest 里仍是 UNVERIFIED_PENDING_CI —— Modrinth 只公布 sha1/sha512，
            本轮沙箱无法访问 CDN，因此由第一次 CI 运行用
            `python3 scripts/download_dependencies.py --print-sha256` 打印后回填 manifest。
   许可证：LGPL-3.0-or-later
   作用：配置系统（YACL v3）。
   是否进入本项目发布 jar：否；玩家需要单独安装 YetAnotherConfigLib。

缺少这两个 jar 时构建会失败。请不要替换为同名但不同哈希的文件；校验失败应更新 manifest、许可证说明和测试记录后再合入。

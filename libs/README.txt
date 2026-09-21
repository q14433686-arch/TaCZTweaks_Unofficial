本目录存放构建所需的二进制依赖（构建时经 files 引用，见 build.gradle.kts）。
完整来源、许可证、用途、是否进入发布 jar 请以仓库根目录的 RESOURCE_IMPORT_MANIFEST.tsv 和 LICENSES.md 为准。

重建/校验方式：

  python3 scripts/download_dependencies.py --check-only
  python3 scripts/download_dependencies.py

1) TACZ-Refabricated-26.3-1.1.8+fabric.26.3.R1.jar  (compileOnly, testRuntimeOnly)
   来源：https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial/releases/tag/26.3_R1
   SHA-256：faa1ce770c184ee2a69ef901a9402f83ee81382372ede07222270fde3530620f（GitHub release asset digest）
   许可证：其 fabric.mod.json 声明为 GPL3 / CC BY-NC-ND 4.0
   作用：提供 com.tacz.guns.* 与 cn.sh1rocu.tacz.* 的编译类（mixin 目标）。
   是否进入本项目发布 jar：否；玩家需要单独安装 TaCZ Refabricated。
   说明：本 jar 不入库（见 .gitignore），由 download_dependencies.py 按 manifest 拉取并校验。
   26.3 系列运行时门禁接受同一发布家族的 R1 及之后 revision（含 R1-hotfix 等后缀），见 TaCZTweaks.isSupportedTaczVersion。

2) yacl-fabric.jar  (implementation)
   来源：Modrinth "YetAnotherConfigLib" 3.9.6+26.2-fabric
   https://cdn.modrinth.com/data/1eAoo2KR/versions/cnfPzuFU/yet_another_config_lib_v3-3.9.6%2B26.2-fabric.jar
   SHA-256：829396c3b3e7d1801ae0e9e2921d0454c5a3078afdb6c6dda6b3d1819dfa0e3f
   许可证：LGPL-3.0-or-later
   作用：配置系统的编译期 API 桩（YACL v3，3.9.x API 面）。
   是否进入本项目发布 jar：否；玩家需要单独安装 YetAnotherConfigLib。
   说明：26.3 线运行期由 fabric.mod.json 钉在 =3.9.7+26.3-fabric。本桩文件是最后一份
   有校验记录的 3.9.x 副本（移植 26.3 的环境无法访问 Modrinth 更新它）；下次发布前请
   在可访问 Modrinth 的网络下替换为 3.9.7+26.3-fabric 并同步 manifest/许可证记录。

缺少这两个 jar 时构建会失败。请不要替换为同名但不同哈希的文件；校验失败应更新 manifest、许可证说明和测试记录后再合入。

本目录存放构建所需的二进制依赖（由 build.gradle 以 files("libs/...") 引用）。
完整来源、许可证、用途、是否进入发布 jar 以仓库根目录的 RESOURCE_IMPORT_MANIFEST.tsv 和 LICENSES.md 为准。
jar 本身不进入 Git（见 .gitignore 的 libs/*.jar）。

校验/下载：

  python3 scripts/download_dependencies.py --check-only
  python3 scripts/download_dependencies.py

文件名按关键词匹配，不要求与下面完全一致（CurseForge / 浏览器改名无所谓）；
但名字里含 "fabric" 的文件会被构建脚本故意忽略，防止误用 Fabric 版。

NeoForge 26.1.2 线需要的文件：

1) tacz-1.1.8+neoforge.26.1.2.R1.jar                      (必需, compileOnly + localRuntime)
   来源：https://github.com/q14433686-arch/TaCZ_Renovated/releases/tag/26.1.2_R1
   作用：提供 com.tacz.guns.* / me.xjqsh.lrtactical.* 的编译类，即全部 mixin 目标。
   是否进入本项目发布 jar：否；玩家需单独安装 TaCZ: Renovated。

2) yet-another-config-lib-v3-3.9.6+26.1-neoforge.jar      (必需, compileOnly + localRuntime)
   来源：CurseForge YACL «3.9.6 for neoforge 26.1»（Fabric 分支用的是同版本 fabric 构建）。
   作用：配置屏（IConfigScreenFactory 扩展点）。

3) sound-physics-remastered-neoforge-1.5.1+26.1.2.jar     (编译必需, compileOnly)
   运行期是可选 mod，但 SoundPhysicsMixin / SoundPhysicsCompat 直接引用
   com.sonicether.soundphysics.*，没有它编译不过。

4) firstaid-1.2.8+neoforge26.1.jar                        (真·可选, compileOnly)
5) pillagers_gun-3.2.2-neoforge-26.1.2.jar                (真·可选, compileOnly)
   这两个走反射 + 字符串目标 mixin，编译不需要；放进来只是方便核对类名，
   构建脚本检测到才会加进 classpath。

6) commons-math3-3.6.1.jar                                (compileOnly)
   TaCZ jar 内已内嵌，本项目只在编译期需要其类型。

注意：manifest 中这些条目的 sha256 目前是 pending —— 移植沙箱无法访问 CurseForge/Maven，
也无法下载 GitHub release 附件。发布构建前必须把实际下载文件的 SHA-256 写回
RESOURCE_IMPORT_MANIFEST.tsv，`--check-only` 才具备防篡改意义。

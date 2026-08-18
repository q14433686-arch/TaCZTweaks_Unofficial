本目录存放两个编译期依赖（构建时经 flatDir 引用，见 build.gradle.kts）。

1) TACZ-Refabricated-26.1.2-1.1.8+fabric.26.1.2.R2.jar  (compileOnly)
   来源：https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial/releases/tag/26.1.2_R2
   作用：提供 com.tacz.guns.* 与 cn.sh1rocu.tacz.* 的编译类（mixin 目标）。

2) yacl-fabric.jar  (implementation)
   来源：Modrinth "YetAnotherConfigLib" 3.9.6+26.1-fabric
   https://cdn.modrinth.com/data/1eAoo2KR/versions/svTkvBec/yet_another_config_lib_v3-3.9.6%2B26.1-fabric.jar
   作用：配置系统（YACL v3）。

缺少这两个 jar 时构建会失败。若 jar 丢失，按上述 URL 重新下载即可。

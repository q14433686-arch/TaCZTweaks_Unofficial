本目录只需要手动放入 TaCZ 编译期依赖（构建时经 build.gradle.kts 引用）：

TACZ-Refabricated-26.1.2-1.1.8+fabric.26.1.2.R2.jar  (compileOnly / testRuntimeOnly)
来源：https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial/releases/tag/26.1.2_R2
作用：提供 com.tacz.guns.* 与 cn.sh1rocu.tacz.* 的编译类及精确 Mixin 目标。

YACL 不再需要手动放入 libs/；Gradle 会从 Modrinth Maven 自动解析精确版本
3.9.6+26.1-fabric（version id svTkvBec）。

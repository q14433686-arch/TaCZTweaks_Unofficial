本目录需要手动放入两个编译期依赖（文件名必须完全一致），见 BUILD.md 第 3 步：

1. TACZ-Refabricated-26.1.2-1.1.8+fabric.26.1.2.R2.jar  （compileOnly，约 58MB）
   https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial/releases/download/26.1.2_R2/TACZ-Refabricated-26.1.2-1.1.8%2Bfabric.26.1.2.R2.jar

2. yacl-fabric.jar  （implementation，约 1MB）
   https://cdn.modrinth.com/data/1eAoo2KR/versions/svTkvBec/yet_another_config_lib_v3-3.9.6%2B26.1-fabric.jar

放好后本目录应为：
  libs/
  ├── README.txt
  ├── TACZ-Refabricated-26.1.2-1.1.8+fabric.26.1.2.R2.jar
  └── yacl-fabric.jar

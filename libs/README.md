# libs/ — local mod jars

This directory is used by the build via `flatDir` (see `buildSrc/src/main/kotlin/multiloader-common.gradle.kts`).

## tacz (unofficial 26.2)

TaCZ Tweaks for **26.2-fabric** depends on
[q14433686-arch/TaCZ_Refabricated_Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)
(`1.1.8+fabric.26.2.R2`, tag `26.2_R2`), which is **not published to Modrinth maven**.

Place the jar here:

```bash
bash scripts/fetch-tacz-26.2.sh
# -> libs/TACZ-Refabricated-26.2-1.1.8+fabric.26.2.R2.jar
```

The file is git-ignored (see `.gitignore`); each build machine must fetch it once.

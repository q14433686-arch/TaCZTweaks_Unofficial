# 维护分支与发行线

本仓库按 **Minecraft 版本 × 加载器** 分线维护。默认分支是 `26.2(main)`（Fabric 26.2）。
每条线有独立源码、依赖、构建脚本和产物版本号；不要把某一条线的 jar、配置或 issue 结论套到另一条线上。

## 当前分支

| Git 分支 | 加载器 | Minecraft | 目标 TaCZ 移植 | 典型产物版本前缀 | 本文件所在线 |
|---|---|---|---|---|---|
| [`26.2(main)`](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/tree/26.2(main)) | Fabric | 26.2 | [TaCZ_Refabricated_Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial) | `2.14.2+fabric.26.2.` | **是（当前检出）** |
| [`26.2-neoforge`](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/tree/26.2-neoforge) | NeoForge | 26.2 | [TaCZ_Renovated](https://github.com/q14433686-arch/TaCZ_Renovated) | `2.14.2+neoforge.26.2.` | 否 |
| [`26.1.2`](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/tree/26.1.2) | Fabric | 26.1.2 | TaCZ_Refabricated_Unofficial | `2.14.2+fabric.26.1.2.` | 否 |
| [`26.1.2-neoforge`](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/tree/26.1.2-neoforge) | NeoForge | 26.1.2 | TaCZ_Renovated | `2.14.2+neoforge.26.1.2.` | 否 |
| [`1.21.11`](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/tree/1.21.11) | Fabric | 1.21.11 | TaCZ_Refabricated_Unofficial | `2.14.2+fabric.1.21.11.` | 否 |
| [`1.21.11-neoforge`](https://github.com/q14433686-arch/TaCZTweaks_Unofficial/tree/1.21.11-neoforge) | NeoForge | 1.21.11 | TaCZ-Renovated `1.21.11` | `2.14.2+neoforge.1.21.11.` | 否 |

工作/PR 临时分支（例如 Arena 会话分支）不属于发行线。向仓库提改动时，请把 PR 打到对应发行分支，而不是默认把所有改动都合进 `26.2(main)`。

## 怎么选分支

1. 先确定你的 **Minecraft 版本** 和 **Fabric / NeoForge**。
2. 打开上表对应分支，只阅读该分支的 README、`BUILD.md` 和兼容矩阵。
3. 安装该分支发布文件 metadata 里列出的 TaCZ、YACL 和加载器依赖。
4. 提交 issue 时必须勾选同一条发行线；跨线混用日志或 jar 无法排查。

## 文档阅读约定

- 本分支（`26.2(main)`）正文里的「本线」「当前线」只指 **Fabric 26.2**。
- 功能表、mixin 细节、Java 版本（本线为 JDK 25）和可选兼容模组范围都以**当前分支源码**为准。
- 发布页标题保持稳定项目名；游戏版本与加载器写在**每个上传文件**的平台字段里，不写进长期项目名。

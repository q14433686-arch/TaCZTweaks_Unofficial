# CI 工作流稿（26.3 线，待上线）

这四个文件是 `.github/workflows/` 的正式内容稿，布局对齐 `q14433686-arch/TaCZ_Renovated`
移植仓的 compile-check / build / consistency / release 四条线。沙箱 Agent 的 token 无
`workflows` 权限，无法把 `.github/workflows/*.yml` push 进仓库（远端会拒收），因此以
本目录形式提交保存。

## 上线步骤（每个文件重复一次）

1. GitHub 网页打开本仓库 → **Add file → Create new file**；
2. 文件名输入 `.github/workflows/<name>.yml`（`compile-check` / `build` / `consistency` / `release`）；
3. 粘贴本目录对应文件的全文；
4. commit 目标选 `26.3-neoforge` 分支（或先开分支再合并）。

四个文件全部上线后：`arena/**`、`26.3*` 分支与 PR 的 push 会自动触发 compile-check /
build / consistency；release 由 `gh workflow run release.yml -f tag='(NEO)26.3...'` 手动触发。

## 各流程职责

| 文件 | 触发 | 做什么 |
|---|---|---|
| `compile-check.yml` | push `arena/**`/`26.3*`、PR | fetch libs/ → `compileJava`，arena 分支回推编译日志 |
| `build.yml` | 同上 | mixin/注册性/语言键/版本/图标静态门禁 → `gradlew build`（含单测）→ 上传 jar 与示例包 |
| `consistency.yml` | 所有分支 push、PR | `check_release_consistency.py` + 图标门禁 + 文档内链 |
| `release.yml` | `workflow_dispatch` | tag 校验 → 构建 → L0 发布门 → 建 GitHub Release |

runner 上的 `libs/` 由 `scripts/fetch_ci_dependencies.py` 重建：TaCZ R1 走 GitHub release
直链、commons-math3 走 Maven Central、YACL / Sound Physics 走 Modrinth API（26.3 + neoforge）。

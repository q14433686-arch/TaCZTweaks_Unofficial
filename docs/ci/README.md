# CI 工作流草稿（待维护者启用）

本目录收录三条 GitHub Actions 工作流草稿，仿照
[`TaCZ_Refabricated_Unofficial`](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial)
26.3 分支当前在用的 `build.yml` / `compile-check.yml` / `consistency.yml` 三件套设计，
并按本仓库的自身情况做了适配。

## 为什么它们不在 `.github/workflows/`

Arena 机器人 token 被 GitHub 拒绝写过 workflow（错误：`refusing to allow a GitHub App to
create or update workflow ... without workflows permission`）。所以这些草稿由维护者在有
`workflows` 权限的账号下入库，一行命令即可启用：

```bash
git mv docs/ci/workflows-draft/ci.yml \
       docs/ci/workflows-draft/compile-log.yml \
       docs/ci/workflows-draft/consistency.yml .github/workflows/
git commit -m "ci: enable three workflows drafted in docs/ci"
git push
```

挪走后记得把本 README 一并删掉（或改为指向已启用工作流的状态说明）。

## 三条工作流的分工

| 文件 | 干什么 | 触发 |
|---|---|---|
| `ci.yml` | 质量门：`download_dependencies.py` → 发布一致性 → 图标门 → `audit_port.py --strict` → audit 自测 → 完整 `./gradlew build` → 上传 jar 与示例包 artifact | push 到任一发行线（`arena/**`、`26.3`、`26.2(main)`、`26.2-neoforge`、`26.1.2`、`26.1.2-neoforge`、`1.21.11`、`1.21.11-neoforge`）+ PR + 手动 |
| `consistency.yml` | 独立的文档/元数据一致性检查（`ci.yml` 为省算力 paths-ignore 了纯文档改动，这条补上；TaCZ 仓库当年也踩过这个坑） | 仅 README/BUILD/CHANGELOG/manifest/fabric.mod.json/docs/LICENSES 等路径变化时 |
| `compile-log.yml` | **沙箱协作专线**：只在 `arena/**` 分支 push 时跑完整构建，把截断后的 `build.log` 通过 **Contents API** 写回该分支的 `build-reports/build.log`，让没有外网的 Arena 会话也能读到真实编译结果；并把 `libs/yacl-fabric.jar` 的 SHA-256 记入日志，给 manifest 的桩换新留取数通道 | push `arena/**` + 手动 |

## 维护注意

- 所有工作流都对 `build-reports/**` 做 paths-ignore，否则 compile-log 的日志回写会
  自我触发、无限循环（这是 TaCZ 仓库用一次事故换来的教训）。
- `ci.yml` 的运行环境：temurin JDK 25 + gradle/actions/setup-gradle@v4，与 TaCZ 侧一致。
- 首次启用前请先跑通 `scripts/download_dependencies.py`（它按
  `RESOURCE_IMPORT_MANIFEST.tsv` 拉取并校验 TaCZ R1 jar；YACL 走同样通道）。
- 若 `consistency` job 因 manifest 缺 jar 报错，那不是 CI 坏了：是维护者该按
  `BUILD.md` 提示把 libs 的 YACL 桩换成 3.9.7+26.3-fabric 并更新 manifest。

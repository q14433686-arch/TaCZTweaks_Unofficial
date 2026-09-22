# `ci/workflows/` — 待安装的 GitHub Actions 流程

这四个文件**本该放在 `.github/workflows/`**，但准备它们的机器人账号（GitHub App）没有
`workflows` 权限，push 会被 GitHub 直接拒绝：

```
refusing to allow a GitHub App to create or update workflow
`.github/workflows/audit.yml` without `workflows` permission
```

所以先放在这里（普通目录不受该限制），由你用一条命令挪到位。

## 安装

在仓库根目录执行：

```bash
bash ci/install-workflows.sh
```

脚本会把四个 yml 复制到 `.github/workflows/`、删掉 `ci/workflows/`，然后告诉你怎么提交。
**必须由有 `workflows` 权限的身份 push**（仓库维护者本人，或给对应 GitHub App 勾上
Workflows 权限后重试）。

手动做也一样：

```bash
mkdir -p .github/workflows
git mv ci/workflows/*.yml .github/workflows/
git rm -r --cached ci 2>/dev/null; rm -rf ci
git commit -m "ci: install workflows"
git push
```

## 四条流程是干什么的

| 文件 | 跑什么 | 何时红 |
|---|---|---|
| `consistency.yml` | `check_release_consistency.py` | 改了 `gradle.properties` 版本却忘了同步 README / BUILD / CHANGELOG / `fabric.mod.json` |
| `audit.yml` | `audit_port.py --strict` + 脚本自测 | mixin 漏登记、mixin 指向 TaCZ jar 里不存在的方法、配置项死开关、语言键漂移、图标漂移 |
| `compile-check.yml` | `compileJava` + `compileKotlin` | 编译错误；`arena/**` 分支还会把日志回推到 `build-reports/compile-java.log` |
| `build.yml` | `./gradlew build` + 上传 jar | 测试失败、jar 门禁失败；成功时产出可下载的 artifact（留 14 天） |

四条都以 `python3 scripts/download_dependencies.py` 开头重建 `libs/`（jar 不在 Git 里）。

详细说明见 [`BUILD.md` 第 7 节](../BUILD.md)。

#!/usr/bin/env bash
# 把 ci/workflows/*.yml 安装到 .github/workflows/。
#
# 为什么需要这一步：GitHub 不允许没有 `workflows` 权限的 GitHub App 创建或修改
# .github/workflows/ 下的文件，所以准备这些流程的机器人只能先把它们放在普通目录里。
# 本脚本由**有该权限的身份**（仓库维护者本人）执行一次即可。
set -euo pipefail

cd "$(dirname "$0")/.."

if [ ! -d ci/workflows ]; then
    echo "ci/workflows/ 不存在 —— 可能已经安装过了。" >&2
    echo "检查一下 .github/workflows/：" >&2
    ls -1 .github/workflows/ 2>/dev/null || echo "  (空)" >&2
    exit 1
fi

mkdir -p .github/workflows

installed=0
for f in ci/workflows/*.yml; do
    name=$(basename "$f")
    if [ -e ".github/workflows/$name" ] && ! cmp -s "$f" ".github/workflows/$name"; then
        echo "跳过 $name：.github/workflows/ 下已有不同内容的同名文件，请手动合并。" >&2
        continue
    fi
    cp "$f" ".github/workflows/$name"
    echo "安装 .github/workflows/$name"
    installed=$((installed + 1))
done

if [ "$installed" -eq 0 ]; then
    echo "没有安装任何文件。" >&2
    exit 1
fi

rm -rf ci
echo
echo "已安装 $installed 个流程，并删除了 ci/ 目录。接下来："
echo
echo "  git add -A .github/workflows ci"
echo "  git commit -m 'ci: install GitHub Actions workflows'"
echo "  git push"
echo
echo "如果 push 仍被拒绝（refusing to allow a GitHub App ... without \`workflows\` permission），"
echo "说明当前 push 身份仍是没有该权限的 App —— 需要用你自己的账号 push，"
echo "或在 GitHub 上给对应 App 勾上 Workflows 权限。"

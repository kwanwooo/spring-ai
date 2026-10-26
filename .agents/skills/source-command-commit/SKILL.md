---
name: "source-command-commit"
description: "Migrated source command `commit`"
---

# source-command-commit

Use this skill when the user asks to run the migrated source command `commit`.

## Command Template

description: 按约定式格式生成提交信息
分析当前 staged 变更，生成符合约定式格式的提交信息：
`feat|fix|refactor|docs|chore(scope): 简述`
只输出提交信息，不要执行 git commit。

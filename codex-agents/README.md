# Codex Agent Configs

这两个文件是可落地的自定义 agent 配置样板：

- `reviewer.toml`
- `frontend_implementer.toml`

## 推荐放置位置

全局可用：

- `~/.codex/agents/reviewer.toml`
- `~/.codex/agents/frontend_implementer.toml`

仅当前项目可用：

- `.codex/agents/reviewer.toml`
- `.codex/agents/frontend_implementer.toml`

## 使用建议

1. 确认 Codex 已开启多 agent 能力
2. 把这两个 TOML 文件复制到上面的目标目录之一
3. 重启 Codex
4. 在任务里明确要求使用对应 sub-agent

## 典型提示词

### reviewer

```text
请启动 reviewer sub-agent，对当前分支相对 main 的改动做审查。
重点检查正确性、回归风险、安全问题和缺失测试。
等 reviewer 完成后，再统一汇总结果。
```

### frontend_implementer

```text
请启动 frontend_implementer sub-agent，先分析当前前端结构，再根据需求做最小实现。
优先复用现有组件和样式模式。
完成后告诉我改了哪些文件，以及如何验证。
```

## 说明

- 这两个文件本身不是 prompt 模板，而是 agent 角色定义
- 它们适合和你之前那份 `sub-agent-prompt-templates.md` 配合使用
- 如果后续你要，我可以继续补 `android_data_integrator.toml`

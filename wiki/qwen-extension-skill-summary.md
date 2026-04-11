# Qwen Code 中 Extension 与 Skill 的关系总结

本文总结了当前关于 Qwen Code 中 **extension、skill、command、agent、MCP server** 之间关系的讨论结果，并结合 `iCinema` 项目给出实用建议。

## 一句话理解

- **Skill = 能力模块**
- **Extension = 能力包 / 插件包**
- **Extension 可以包含 Skill，Skill 也可以独立存在**

最核心的理解是：

> **Extension 是“打包容器”，Skill 是“容器里的一个部件”。**

也就是说：

- Skill 不是 Extension 的对立物
- Skill 可以单独存在
- Extension 也可以打包多个 Skills

---

## 基本概念

### Skill 是什么

Skill 是一种 **model-invoked capability**，即模型可根据用户请求自动决定是否使用。

Skill 的核心通常是一个 `SKILL.md` 文件，用来定义：

- 这项能力叫什么
- 什么时候适合使用
- 应该如何执行
- 可附带哪些参考文件、脚本、模板

Skill 也可以手动触发：

```bash
/skills <skill-name>
```

可以把 Skill 理解为：

> 一段可复用的专业做事方法 / 能力说明书

例如：

- `code-review`
- `android-compose-checklist`
- `pdf-helper`
- `media3-player-debug`

### Extension 是什么

Extension 更像一个 **插件包 / 分发单元**。

Extension 可以打包和发布一整套能力，包括：

- prompts / commands
- skills
- subagents / agents
- MCP servers
- `QWEN.md`
- settings

它主要解决的是：

- 如何安装
- 如何启用 / 禁用
- 如何共享给别人
- 如何把多个能力组合成一个统一插件

可以把 Extension 理解为：

> 一个可安装、可分发、可复用的能力包

---

## 二者之间的关系

可以用下面这个结构理解：

```text
Extension
├── commands
├── skills
│   ├── skill-a
│   │   └── SKILL.md
│   └── skill-b
│       └── SKILL.md
├── agents
├── mcpServers
├── QWEN.md
└── qwen-extension.json
```

也就是说：

- **Skill 是能力单元**
- **Extension 是发布和安装这些能力的容器**

---

## Skill 是否必须放在 Extension 里

**不必须。** Skill 可以有三种来源：

1. **Personal Skills**
   - 路径：`~/.qwen/skills/`
2. **Project Skills**
   - 路径：`.qwen/skills/`
3. **Extension Skills**
   - 路径：已安装 extension 内部的 `skills/`

因此：

- 你可以直接创建一个 Skill，不依赖 Extension
- 也可以把 Skill 打包进 Extension 里

两种方式都成立。

---

## 最关键的区别

### Skill 关注“能力定义”

它解决的是：

- 这项能力是什么
- 什么时候触发
- 如何执行
- 有哪些辅助资源

### Extension 关注“安装与分发”

它解决的是：

- 这一整套能力如何安装
- 如何共享给团队或其他项目
- 如何统一启用 / 禁用 / 更新
- 如何把 Skill、Command、Agent、MCP 组合起来

---

## 类比帮助理解

### 类比一：VS Code 生态

- **Extension** = VS Code 插件
- **Skill** = 插件里的一项具体能力

### 类比二：工具箱

- **Extension** = 工具箱
- **Skill** = 工具箱里的一把工具

### 类比三：App 与功能模块

- **Extension** = 一个 App
- **Skill** = App 中的某个功能模块

---

## 在 iCinema 项目里的实战对照

下表是更实用的区分方式：

| 机制 | 作用 | 谁触发 | 适合 iCinema 的例子 | 什么时候用 |
|---|---|---|---|---|
| **Skill** | 给模型一套专业做事方法 | 模型自动触发，也可 `/skills` 手动触发 | Compose UI review、Media3 播放排障、Gradle 构建排障 | 希望 Qwen 在相关任务中自动更专业 |
| **Command** | 把固定 prompt / 流程做成快捷命令 | 用户手动触发 | `/android:check-build`、`/review:home-screen` | 想显式运行某个固定流程 |
| **Agent / Subagent** | 专门负责某类任务的子助手 | 模型调用 | `refactoring-expert`、`android-architecture-reviewer` | 任务复杂，适合分工 |
| **MCP Server** | 提供新的工具能力 | 模型通过工具调用 | Android 文档查询、GitHub、Jira、内部 API | 需要访问外部系统或新增工具能力 |
| **Extension** | 把以上内容整体打包安装 / 分发 | 用户安装、启用 | `icinema-android-toolkit` | 需要长期复用、团队共享、可安装 |

---

## 每种机制在 iCinema 中如何理解

### 1. Skill：最适合先做

Skill 最适合用于定义一类高频工作流，让 Qwen 在遇到相关任务时自动更专业。

例如：

```text
.qwen/skills/compose-screen-review/SKILL.md
```

其中可以定义：

- 什么时候用：审查 Compose 页面
- 检查哪些点：状态提升、重组、remember 使用、滚动性能、命名、MVI 一致性
- 输出格式：先列问题，再给建议

适合 iCinema 的 Skill 示例：

- `compose-screen-review`
- `media3-player-debug`
- `mvi-state-check`
- `retrofit-repository-review`
- `gradle-build-troubleshoot`

### 2. Command：适合固定动作

当你希望“显式地执行某个固定流程”时，更适合用 Command。

例如：

```text
commands/android/review-screen.md
```

然后手动运行：

```bash
/android:review-screen HomeScreen
```

适合 iCinema 的 Command 示例：

- `/android:review-screen <ScreenName>`
- `/android:trace-state <ViewModel>`
- `/android:summarize-crash <stacktrace>`
- `/gradle:diagnose-build`

### 3. Agent：适合专职子助手

当任务复杂度提升，需要跨文件分析、专项重构或专项审查时，适合使用 Agent / Subagent。

适合 iCinema 的 Agent 示例：

- `compose-refactor-expert`
- `android-architecture-reviewer`
- `player-bug-investigator`

### 4. MCP Server：适合补外部能力

如果只是写 prompt 和流程，通常不需要 MCP。

但如果你需要：

- 访问 GitHub / Jira / 文档站 / 内部 API
- 调用外部服务
- 增加当前 CLI 没有的新工具能力

那么就需要 MCP server。

适合 iCinema 的 MCP 方向：

- GitHub MCP
- Android / Kotlin 文档检索 MCP
- 内部 API / CMS 调试 MCP

### 5. Extension：适合最后统一打包

当你已经拥有：

- 多个 skills
- 多个 commands
- 一个或多个 agents
- 一个或多个 MCP servers
- 一个 `QWEN.md`

这时就很适合做成一个 extension。

例如：

```text
icinema-android-toolkit/
├── qwen-extension.json
├── QWEN.md
├── skills/
│   ├── compose-screen-review/
│   │   └── SKILL.md
│   ├── media3-player-debug/
│   │   └── SKILL.md
│   └── gradle-build-troubleshoot/
│       └── SKILL.md
├── commands/
│   └── android/
│       ├── review-screen.md
│       └── summarize-crash.md
├── agents/
│   └── architecture-reviewer.md
└── ...
```

---

## 最推荐的落地顺序

针对当前项目，建议按以下顺序落地：

### 第一阶段：先写 Project Skills

先在项目中创建最有价值、成本最低的 Skills。

建议优先做：

1. `compose-screen-review`
2. `media3-player-debug`
3. `gradle-build-troubleshoot`

### 第二阶段：补充 Commands

把高频、固定的操作做成命令。

例如：

- `/android:review-screen`
- `/android:diagnose-build`

### 第三阶段：再考虑 Agents

如果你发现需要复杂专项分析、跨文件重构、架构审查，再补充 Agent。

### 第四阶段：最后再做 Extension

只有在需要团队共享、多项目复用、统一安装时，再把前面的内容打包成 Extension。

---

## 最简单的判断公式

### 用 Skill，如果你想要：

> 遇到这类任务时，Qwen 自动更专业

### 用 Command，如果你想要：

> 我手动执行一个固定流程

### 用 Agent，如果你想要：

> 让一个专职子助手处理专项复杂任务

### 用 MCP，如果你想要：

> 为 Qwen 增加新工具 / 接入外部系统

### 用 Extension，如果你想要：

> 把以上这些内容打包成插件统一安装和分发

---

## 针对 iCinema 当前阶段的建议

对于当前 `iCinema` 项目，最合适的策略是：

**先不要急着做 Extension，先做 Project Skills。**

推荐优先创建：

```text
.qwen/skills/compose-screen-review/SKILL.md
.qwen/skills/media3-player-debug/SKILL.md
.qwen/skills/gradle-build-troubleshoot/SKILL.md
```

原因：

- 收益最大
- 成本最低
- 最容易快速见效
- 不需要先处理安装、发布、共享这些额外问题

当这些 Skill 稳定后，再视需要补充 command、agent、MCP，最后再打包成 extension。

---

## 最终结论

最短总结如下：

- **Skill = 能力模块**
- **Extension = 能力包 / 插件包**
- **Skill 可以独立存在，也可以被打包进 Extension**
- 对当前 iCinema 项目来说，最合理的路径是：
  - 先做 **project skills**
  - 再补 **commands**
  - 再考虑 **agents / MCP**
  - 最后再做 **extension**

这也是当前讨论形成的推荐方案。

# Superpowers-ZH 中文增强版
本项目已安装 superpowers-zh 技能框架（21 个 skills，位于 `.claude/skills/`）。

## 核心规则
1. **收到任务时，先检查是否有匹配的 skill** — 哪怕只有 1% 的可能性也要检查
2. **设计先于编码** — 功能需求先用 brainstorming skill 做需求分析
3. **测试先于实现** — 写代码前先写测试（TDD）
4. **验证先于完成** — 声称完成前必须运行验证命令

## 如何使用
当任务匹配某个 skill 时，使用 `Skill` 工具加载对应 skill 并严格遵循其流程。绝不要用 Read 工具读取 SKILL.md 文件。

常用 skills 速查：`brainstorming`（需求分析）、`writing-plans`（多步任务规划）、`test-driven-development`（TDD）、`systematic-debugging`（bug 排查）、`verification-before-completion`（完成前验证）、`requesting-code-review`（合并前审查）、`using-git-worktrees`（隔离开发）、`dispatching-parallel-agents`（独立任务并行）。

完整列表见 `.claude/skills/` 目录。
<!-- superpowers-zh:end -->

# Spring AI 多模型对话服务
Spring Boot 4.0.8 + Spring AI 2.0.0 + JDK 17。DeepSeek/GLM 双模型对话，含 SSE 流式、会话记忆持久化（MySQL）、RAG 向量检索、降级重试、Token 计量。

## Commands

```bash
./gradlew build -x test        # 快速构建（跳过测试和 checkstyle）
./gradlew test                 # 全部测试
./gradlew checkstyleMain checkstyleTest   # 风格检查
./gradlew clean check          # 提交前完整门禁（test + checkstyle）
./gradlew bootRun              # 启动，默认 8080 端口
```

## 架构
请求链：`ChatController` / `RagController` → `ChatService`（按 `ModelProvider` 枚举路由 deepseek/glm）→ `ChatClient`。

- **模型手动构建**：`ChatModelConfig` 用 `OpenAiChatModel.builder()` 各建一个 DeepSeek/GLM（都走 OpenAI 兼容接口），再包一层 `ResilientChatModel`（重试+降级）。`application.properties` 里 `spring.ai.model.*=none` 是禁用自动装配、避免多模型 bean 冲突。
- **重试/降级在模型层**（`ResilientChatModel`，advisor 链之下），这样记忆 advisor 只执行一次，不会因重试重复写 user 消息。
- **会话记忆**：`JdbcChatMemoryRepository`（MySQL）+ `MessageWindowChatMemory`（10 条滑动窗口）。
- **RAG**：`RagConfig`（GLM `embedding-3` + `SimpleVectorStore` 文件持久化到 `data/vectorstore.json` + `TokenTextSplitter`），检索增强经 `QuestionAnswerAdvisor`。
- **用量/成本**：`UsageService` 落库 `AI_TOKEN_USAGE`（对话 + 嵌入都记），单价在 `PricingProperties`（`spring.ai.pricing.models.*`）。
- **横切**：`LoggingAdvisors`（日志 + 用量记录）、`GlobalExceptionHandler`（统一 `ErrorResponse`）、`WebSearchTool`（DuckDuckGo）。

## 关键坑
- `ResilientChatModel` **必须重写 `getOptions()` 透传主模型**，并在委托前用 `promptFor()` 把 prompt 的 options 换成对应模型的——否则 `OpenAiChatModel` 把 `DefaultChatOptions` 强转 `OpenAiChatOptions` 抛 `ClassCastException`，且降级时会把主模型的 model 名带到备用模型。
- 模型手动构建，**`spring.ai.retry.*` 自动配置对它们无效**（所以重试是自己在 `ResilientChatModel` 实现的）。
- 会话记忆持久化的是**滑动窗口（10 条）而非全量历史**；`JdbcChatMemoryRepository` 会**过滤 tool 消息**（`ToolResponseMessage` / 带 tool call 的 `AssistantMessage`）。
- 嵌入模型只有 **GLM**（DeepSeek 无公开 embedding 接口），需 GLM key 开通 `embedding-3` 权限。

## 约定
- 分层：Controller 只做校验 + 路由，编排在 Service，模型/基础设施包装在 `config/`。
- 新增 provider：同步改 `ModelProvider` + `ChatModelConfig` + `ChatService`。
- DTO 放 `dto/`，用 `@NotBlank` 校验；业务异常由 `GlobalExceptionHandler` 映射为 `ErrorResponse`。
- 不要绕过 `ResilientChatModel` 直接调底层模型；不要改 `SPRING_AI_CHAT_MEMORY` 表结构；不要改 `initialize-schema=never`。

详细功能与 API 文档见 `README.md`。

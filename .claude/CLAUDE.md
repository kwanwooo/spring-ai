# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Spring AI 多模型对话服务

Spring Boot 4.0.8 + Spring AI 2.0.0 + JDK 17。DeepSeek/GLM 双模型对话，含 SSE 流式、会话记忆持久化（MySQL）、RAG 向量检索、降级重试、Token 计量。

## 常用命令
```bash
./gradlew build                    # 构建（含测试；跳过测试用 ./gradlew build -x test）
./gradlew bootRun                  # 运行，默认 8080 端口
```

- 需要 `JAVA_HOME` 指向 JDK 17；报「JAVA_HOME is not set」时用 Gradle 工具链自动下载的 `~/.gradle/jdks/eclipse_adoptium-17-*/` 即可。
- 运行/测试前需 `export DEEPSEEK_API_KEY / GLM_API_KEY / MYSQL_PASSWORD`。
- 建表（`initialize-schema=never`，两张表都手动建）：
  ```bash
  mysql -u root -p spring_ai < db/chat-memory-mysql.sql
  mysql -u root -p spring_ai < db/token-usage-mysql.sql
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
- `.env` **不会被 Spring Boot 自动加载**（项目未引入 dotenv），配置走系统环境变量。

## 约定

- 分层：Controller 只做校验 + 路由，编排在 Service，模型/基础设施包装在 `config/`。
- 新增 provider：同步改 `ModelProvider` + `ChatModelConfig` + `ChatService`。
- DTO 放 `dto/`，用 `@NotBlank` 校验；业务异常由 `GlobalExceptionHandler` 映射为 `ErrorResponse`。
- 不要绕过 `ResilientChatModel` 直接调底层模型；不要改 `SPRING_AI_CHAT_MEMORY` 表结构；不要改 `initialize-schema=never`。

详细功能与 API 文档见 `README.md`。

```# Spring AI 多模型对话服务

基于 **Spring Boot 4.0.8** 和 **Spring AI 2.0.0** 构建的多模型 AI 对话服务。该项目提供统一的 RESTful API 接口，支持 **DeepSeek** 和 **GLM（智谱）** 两种大语言模型，具备同步/流式对话、上下文记忆、请求日志记录及函数调用扩展能力。

## 🚀 核心特性

*   **多模型接入**：统一封装 DeepSeek 和 GLM 模型，支持通过配置动态切换。
*   **流式输出 (SSE)**：支持 Server-Sent Events 流式响应，提升长文本生成体验。
*   **上下文记忆**：内置 `MessageWindowChatMemory`，自动维护最近 10 条对话历史。
*   **日志监控**：自定义 `LoggingAdvisors`，自动拦截并记录请求输入、输出及耗时。
*   **函数调用 (Function Calling)**：预留 `WebSearchTool` 工具接口，支持扩展联网搜索等外部能力。
*   **架构轻量**：基于 Gradle 构建，代码结构清晰，易于二次开发。

## 🛠 技术栈

| 组件 | 版本 | 说明 |
| :--- | :--- | :--- |
| **Java** | 17 | LTS 版本，支持现代 Java 特性 |
| **Spring Boot** | 4.0.8 | 核心框架 |
| **Spring AI** | 2.0.0 | AI 集成框架 |
| **Gradle** | 9.7.1 | 构建工具 |
| **Lombok** | - | 简化 Java Bean 代码 |

## 📁 项目结构

text
spring
```-ai/
├── src/main/java/com/example/spring_ai/
│   ├── SpringAiApplication.java          # 应用启动类
│   ├── config/
│   │   ├── ChatMemoryConfig.java         # 对话记忆配置 (10 条窗口)
│   │   ├── ChatModelConfig.java          # 多模型 ChatClient 初始化
│   │   ├── LoggingAdvisors.java          # 请求/响应日志拦截器
│   │   └── WebSearchTool.java            # 联网搜索工具定义
│   ├── controller/
│   │   └── ChatController.java           # REST API 控制器
│   ├── dto/
│   │   ├── ChatRequest.java              # 请求数据传输对象
│   │   └── ChatResponse.java             # 响应数据传输对象
│   ├── model/
│   │   └── ModelProvider.java            # 模型提供商枚举 (DEEPSEEK/GLM)
│   └── service/
│       └── ChatService.java              # 核心业务逻辑与模型路由
├── build.gradle                          # Gradle 构建配置
├── settings.gradle
├── .gitignore
├── gradlew / gradlew.bat                 # Gradle Wrapper
└── README.md                             # 项目说明文档
```

## ⚙️ 环境配置

在 `src/main/resources/application.yml` 中配置模型 API 密钥：yaml

```spring:
  ai:
    deepseek:
      base-url: https://api.deepseek.com/v1
      api-key: YOUR_DEEPSEEK_API_KEY
      model: deepseek-chat
    glm:
      base-url: https://open.bigmodel.cn/api/paas/v4
      api-key: YOUR_GLM_API_KEY
      model: glm
```

## 🔌 API 接口说明

### 1. 同步对话 (POST)

**Endpoint**: `/api/chat`

**请求示例**:

bash
curl
``` -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "deepseek",
    "conversationId": "user-123",
    "message": "你好，请介绍一下 Spring AI"
 
```

**响应示例
```json
{
  "content": "Spring AI 是一个用于在 Java 应用程序中集成 AI 模型的框架..."
}
```
 2. 流式对话 (GET)

**Endpoint**: `/api/chat/stream`

**请求示例**:bash

```curl http://localhost:8080/api/chat/stream?conversationId=user-123&message=写一首关于春天的诗
```

**说明**: 返回 `text/event-stream` 格式数据，客户端需按 SSE 协议解析。

## 🏗 快速开始

### 前置要求
*   JDK 17+
*   Git

### 构建与bash
```
# 1. 克隆项目
git clone <repository-url>
cd spring-ai

# 2. 构建项目
./gradlew build

# 3. 运行应用
./gradlew
```

## 🏛 架构设计

mermaid

```graph TD
    Client[客户端] --> Controller[ChatController]
    Controller --> Service[ChatService]
    Service -->|路由 | Config{ChatModelConfig}
    
    Config -->|DeepSeek| DS_Client[DeepSeek ChatClient]
    Config -->|GLM| GLM_Client[GLM ChatClient]
    
    DS_Client --> OpenAI_API[OpenAI 兼容接口]
    GLM_Client --> OpenAI_API
    
    Service <--> Memory[ChatMemory (10 条上下文)]
    Service --> Tool[WebSearchTool]
```
package com.example.spring_ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 对话请求体。
 *
 * @param message        用户输入
 * @param system         可选的自定义系统提示词，为空时使用默认系统提示词
 * @param provider       模型提供方（deepseek / glm），为空时默认 deepseek
 * @param conversationId 会话 ID，用于区分不同会话的记忆，为空时使用默认会话
 */
public record ChatRequest(@NotBlank(message = "message 不能为空") String message, String system, String provider,
		String conversationId) {
}

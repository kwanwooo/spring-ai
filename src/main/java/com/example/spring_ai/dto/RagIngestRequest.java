package com.example.spring_ai.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;

/**
 * RAG 文本摄入请求。
 *
 * @param text     要摄入的文本内容
 * @param metadata 可选元数据（如来源、标题），随分块一并存储
 */
public record RagIngestRequest(@NotBlank(message = "text 不能为空") String text, Map<String, Object> metadata) {
}

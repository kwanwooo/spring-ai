package com.example.spring_ai.dto;

/**
 * 统一错误响应体。
 *
 * @param code    HTTP 状态码
 * @param message 错误信息
 */
public record ErrorResponse(int code, String message) {
}

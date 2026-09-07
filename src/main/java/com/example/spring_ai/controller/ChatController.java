package com.example.spring_ai.controller;

import com.example.spring_ai.dto.ChatRequest;
import com.example.spring_ai.dto.ChatResponse;
import com.example.spring_ai.model.ModelProvider;
import com.example.spring_ai.service.ChatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 多模型对话接口。
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

	private final ChatService chatService;

	public ChatController(ChatService chatService) {
		this.chatService = chatService;
	}

	/**
	 * 单轮对话。
	 *
	 * <pre>
	 * POST /api/chat
	 * {"provider": "glm", "conversationId": "u-123", "message": "用一句话介绍 Spring AI", "system": "你是一名 Java 架构师"}
	 * </pre>
	 */
	@PostMapping
	public ChatResponse chat(@RequestBody ChatRequest request) {
		ModelProvider provider = resolveProvider(request.provider());
		String content = isBlank(request.system())
				? chatService.chat(provider, request.conversationId(), request.message())
				: chatService.chat(provider, request.conversationId(), request.system(), request.message());
		return new ChatResponse(content);
	}

	/**
	 * 流式对话（SSE）。
	 *
	 * <pre>
	 * GET /api/chat/stream?provider=glm&conversationId=u-123&message=讲个笑话&system=...
	 * </pre>
	 */
	@GetMapping(value = "/stream", produces = "text/event-stream;charset=UTF-8")
	public Flux<String> stream(@RequestParam String message, @RequestParam(required = false) String system,
			@RequestParam(required = false) String conversationId,
			@RequestParam(defaultValue = "deepseek") String provider) {
		ModelProvider resolved = resolveProvider(provider);
		return isBlank(system) ? chatService.chatStream(resolved, conversationId, message)
				: chatService.chatStream(resolved, conversationId, system, message);
	}

	private ModelProvider resolveProvider(String provider) {
		if (provider == null || provider.isBlank() || "deepseek".equalsIgnoreCase(provider)) {
			return ModelProvider.DEEPSEEK;
		}
		if ("glm".equalsIgnoreCase(provider)) {
			return ModelProvider.GLM;
		}
		throw new IllegalArgumentException("不支持的模型提供方: " + provider);
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

}

package com.example.spring_ai.controller;

import java.util.Map;

import com.example.spring_ai.dto.ChatRequest;
import com.example.spring_ai.dto.ChatResponse;
import com.example.spring_ai.dto.RagIngestRequest;
import com.example.spring_ai.model.ModelProvider;
import com.example.spring_ai.service.RagService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RAG（检索增强生成）接口。
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {

	private final RagService ragService;

	public RagController(RagService ragService) {
		this.ragService = ragService;
	}

	/**
	 * 摄入文档文本，用于构建知识库。
	 *
	 * <pre>
	 * POST /api/rag/ingest
	 * {"text": "...", "metadata": {"source": "README.md"}}
	 * </pre>
	 */
	@PostMapping("/ingest")
	public Map<String, Integer> ingest(@RequestBody @Valid RagIngestRequest request) {
		return Map.of("chunks", this.ragService.ingest(request.text(), request.metadata()));
	}

	/**
	 * 基于知识库的问答。
	 *
	 * <pre>
	 * POST /api/rag/chat
	 * {"provider": "deepseek", "conversationId": "u-123", "message": "Spring AI 是什么？"}
	 * </pre>
	 */
	@PostMapping("/chat")
	public ChatResponse chat(@RequestBody @Valid ChatRequest request) {
		ModelProvider provider = ModelProvider.resolve(request.provider());
		String content = this.ragService.ask(provider, request.conversationId(), request.message());
		return new ChatResponse(content);
	}

}

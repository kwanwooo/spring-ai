package com.example.spring_ai.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import com.example.spring_ai.dto.ChatRequest;
import com.example.spring_ai.dto.ChatResponse;
import com.example.spring_ai.dto.RagIngestRequest;
import com.example.spring_ai.model.ModelProvider;
import com.example.spring_ai.service.RagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link RagController} 摄入与问答编排的单元测试。
 */
class RagControllerTest {

	private RagService ragService;

	private RagController controller;

	@BeforeEach
	void setUp() {
		this.ragService = mock(RagService.class);
		this.controller = new RagController(this.ragService);
	}

	@Test
	void ingestReturnsChunkCount() {
		when(this.ragService.ingest("text", null)).thenReturn(3);

		Map<String, Integer> result = this.controller.ingest(new RagIngestRequest("text", null));

		assertThat(result).containsEntry("chunks", 3);
		verify(this.ragService).ingest("text", null);
	}

	@Test
	void chatRoutesToAsk() {
		when(this.ragService.ask(ModelProvider.DEEPSEEK, "c1", "hi")).thenReturn("answer");

		ChatResponse resp = this.controller.chat(new ChatRequest("hi", null, "deepseek", "c1"));

		assertThat(resp.content()).isEqualTo("answer");
		verify(this.ragService).ask(ModelProvider.DEEPSEEK, "c1", "hi");
	}

}

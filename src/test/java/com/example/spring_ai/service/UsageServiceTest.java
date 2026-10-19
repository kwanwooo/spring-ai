package com.example.spring_ai.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import com.example.spring_ai.config.PricingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * {@link UsageService} 用量落库逻辑的单元测试。
 */
class UsageServiceTest {

	private JdbcTemplate jdbcTemplate;

	private PricingProperties pricing;

	private UsageService usageService;

	@BeforeEach
	void setUp() {
		this.jdbcTemplate = mock(JdbcTemplate.class);
		this.pricing = mock(PricingProperties.class);
		this.usageService = new UsageService(this.jdbcTemplate, this.pricing);
	}

	@Test
	void recordInsertsRowWithCost() {
		ChatResponse response = mock(ChatResponse.class);
		ChatResponseMetadata metadata = mock(ChatResponseMetadata.class);
		Usage usage = mock(Usage.class);
		when(response.getMetadata()).thenReturn(metadata);
		when(metadata.getModel()).thenReturn("deepseek-chat");
		when(metadata.getUsage()).thenReturn(usage);
		when(usage.getPromptTokens()).thenReturn(100);
		when(usage.getCompletionTokens()).thenReturn(50);
		when(usage.getTotalTokens()).thenReturn(150);
		when(this.pricing.cost("deepseek-chat", 100, 50)).thenReturn(0.5);

		this.usageService.record(Map.of(ChatMemory.CONVERSATION_ID, "conv-1"), response);

		verify(this.jdbcTemplate)
			.update(anyString(), eq("conv-1"), eq("deepseek-chat"), eq(100), eq(50), eq(150), eq(0.5));
	}

	@Test
	void recordSkipsWhenMetadataNull() {
		ChatResponse response = mock(ChatResponse.class);
		when(response.getMetadata()).thenReturn(null);

		this.usageService.record(null, response);

		verifyNoInteractions(this.jdbcTemplate);
	}

	@Test
	void recordSkipsWhenUsageNull() {
		ChatResponse response = mock(ChatResponse.class);
		ChatResponseMetadata metadata = mock(ChatResponseMetadata.class);
		when(response.getMetadata()).thenReturn(metadata);
		when(metadata.getUsage()).thenReturn(null);

		this.usageService.record(null, response);

		verifyNoInteractions(this.jdbcTemplate);
	}

	@Test
	void recordSwallowsDbException() {
		ChatResponse response = mock(ChatResponse.class);
		ChatResponseMetadata metadata = mock(ChatResponseMetadata.class);
		Usage usage = mock(Usage.class);
		when(response.getMetadata()).thenReturn(metadata);
		when(metadata.getModel()).thenReturn("deepseek-chat");
		when(metadata.getUsage()).thenReturn(usage);
		when(usage.getPromptTokens()).thenReturn(100);
		when(usage.getCompletionTokens()).thenReturn(50);
		when(usage.getTotalTokens()).thenReturn(150);
		when(this.jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any()))
			.thenThrow(new RuntimeException("db down"));

		assertThatCode(() -> this.usageService.record(null, response)).doesNotThrowAnyException();
	}

	@Test
	void recordEmbeddingInsertsRowWithNullConversation() {
		EmbeddingResponse response = mock(EmbeddingResponse.class);
		EmbeddingResponseMetadata metadata = mock(EmbeddingResponseMetadata.class);
		Usage usage = mock(Usage.class);
		when(response.getMetadata()).thenReturn(metadata);
		when(metadata.getModel()).thenReturn("embedding-3");
		when(metadata.getUsage()).thenReturn(usage);
		when(usage.getPromptTokens()).thenReturn(10);
		when(usage.getCompletionTokens()).thenReturn(null);
		when(usage.getTotalTokens()).thenReturn(10);
		when(this.pricing.cost("embedding-3", 10, 0)).thenReturn(0.000005);

		this.usageService.recordEmbedding(response);

		verify(this.jdbcTemplate).update(anyString(), isNull(), eq("embedding-3"), eq(10), eq(0), eq(10), eq(0.000005));
	}

}

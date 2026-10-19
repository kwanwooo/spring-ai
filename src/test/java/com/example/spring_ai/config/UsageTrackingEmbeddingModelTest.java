package com.example.spring_ai.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.spring_ai.service.UsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

/**
 * {@link UsageTrackingEmbeddingModel} 委托 + 用量记录的单元测试。
 */
class UsageTrackingEmbeddingModelTest {

	private EmbeddingModel delegate;

	private UsageService usageService;

	private UsageTrackingEmbeddingModel model;

	@BeforeEach
	void setUp() {
		this.delegate = mock(EmbeddingModel.class);
		this.usageService = mock(UsageService.class);
		this.model = new UsageTrackingEmbeddingModel(this.delegate, this.usageService);
	}

	@Test
	void callDelegatesAndRecordsUsage() {
		EmbeddingResponse response = mock(EmbeddingResponse.class);
		EmbeddingRequest request = mock(EmbeddingRequest.class);
		when(this.delegate.call(any(EmbeddingRequest.class))).thenReturn(response);

		assertThat(this.model.call(request)).isSameAs(response);
		verify(this.usageService).recordEmbedding(response);
	}

	@Test
	void dimensionsDelegates() {
		when(this.delegate.dimensions()).thenReturn(1024);

		assertThat(this.model.dimensions()).isEqualTo(1024);
	}

}

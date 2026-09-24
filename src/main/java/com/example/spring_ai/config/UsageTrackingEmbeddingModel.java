package com.example.spring_ai.config;

import java.util.List;

import com.example.spring_ai.service.UsageService;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

/**
 * 记录嵌入（embedding）用量的装饰器，把 RAG 的向量化成本也纳入统计。
 */
public class UsageTrackingEmbeddingModel implements EmbeddingModel {

	private final EmbeddingModel delegate;

	private final UsageService usageService;

	public UsageTrackingEmbeddingModel(EmbeddingModel delegate, UsageService usageService) {
		this.delegate = delegate;
		this.usageService = usageService;
	}

	@Override
	public EmbeddingResponse call(EmbeddingRequest request) {
		EmbeddingResponse response = this.delegate.call(request);
		this.usageService.recordEmbedding(response);
		return response;
	}

	@Override
	public float[] embed(Document document) {
		// 经 call() 走一遍以记录用量
		return this.embed(List.of(document.getText())).get(0);
	}

	@Override
	public int dimensions() {
		return this.delegate.dimensions();
	}

}

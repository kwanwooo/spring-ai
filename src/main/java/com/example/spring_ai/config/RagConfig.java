package com.example.spring_ai.config;

import java.io.File;

import com.example.spring_ai.service.UsageService;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 相关配置：嵌入模型、向量库、文本切分器。
 *
 * <p>嵌入模型复用 GLM 的 OpenAI 兼容接口（DeepSeek 暂无公开嵌入接口），
 * 并包一层 {@link UsageTrackingEmbeddingModel} 统计向量化成本；
 * 向量库使用 {@link SimpleVectorStore} 持久化到本地文件，零外部依赖。
 */
@Configuration
public class RagConfig {

	@Bean
	public EmbeddingModel embeddingModel(@Value("${spring.ai.glm.base-url}") String baseUrl,
			@Value("${spring.ai.glm.api-key}") String apiKey,
			@Value("${spring.ai.glm.embedding.model}") String model) {
		return OpenAiEmbeddingModel.builder()
			.options(OpenAiEmbeddingOptions.builder().baseUrl(baseUrl).apiKey(apiKey).model(model).build())
			.build();
	}

	@Bean
	public SimpleVectorStore vectorStore(EmbeddingModel embeddingModel, UsageService usageService,
			@Value("${spring.ai.rag.vector-store-file:data/vectorstore.json}") String filePath) {
		EmbeddingModel tracked = new UsageTrackingEmbeddingModel(embeddingModel, usageService);
		SimpleVectorStore store = SimpleVectorStore.builder(tracked).build();
		File file = new File(filePath);
		// 确保父目录存在：SimpleVectorStore.save 不会自动建目录，否则首次摄入会因目录缺失而失败
		File parent = file.getParentFile();
		if (parent != null && !parent.exists() && !parent.mkdirs()) {
			throw new IllegalStateException("无法创建向量库目录: " + parent.getAbsolutePath());
		}
		if (file.exists()) {
			store.load(file);
		}
		return store;
	}

	@Bean
	public TokenTextSplitter tokenTextSplitter() {
		return TokenTextSplitter.builder().build();
	}

}

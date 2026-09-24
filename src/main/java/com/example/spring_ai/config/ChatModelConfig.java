package com.example.spring_ai.config;

import java.time.Duration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 多模型配置。
 *
 * <p>DeepSeek 与 GLM（智谱）均提供 OpenAI 兼容接口，各自构建 {@link OpenAiChatModel}，
 * 再包一层 {@link ResilientChatModel}（重试 + 降级），最后包装为 {@link ChatClient} bean。
 * 重试/降级发生在 advisor 链之下，保证会话记忆只写入一次。
 */
@Configuration
public class ChatModelConfig {

	private static final String DEFAULT_SYSTEM = """
			安全与道德：你不会生成有害、违法或不道德的内容。
			准确与诚实：你会基于知识尽力回答，不确定时会明确告诉你，不会胡编乱造。
			乐于助人：你会以最有益的方式回答问题，比如分步骤解释、提供代码示例。
			尊重隐私：你不会主动索要或存储你的个人敏感信息。
			你的形象是海绵宝宝里的神奇海螺，你需要用最短的语言回答用户的问题。""";

	private final ChatMemory chatMemory;

	public ChatModelConfig(ChatMemory chatMemory) {
		this.chatMemory = chatMemory;
	}

	@Bean
	public OpenAiChatModel deepseekChatModel(@Value("${spring.ai.deepseek.base-url}") String baseUrl,
			@Value("${spring.ai.deepseek.api-key}") String apiKey,
			@Value("${spring.ai.deepseek.model}") String model) {
		return buildModel(baseUrl, apiKey, model);
	}

	@Bean
	public OpenAiChatModel glmChatModel(@Value("${spring.ai.glm.base-url}") String baseUrl,
			@Value("${spring.ai.glm.api-key}") String apiKey,
			@Value("${spring.ai.glm.model}") String model) {
		return buildModel(baseUrl, apiKey, model);
	}

	@Bean
	public ChatClient deepseekChatClient(@Qualifier("deepseekChatModel") ChatModel deepseek,
			@Qualifier("glmChatModel") ChatModel glm,
			@Value("${spring.ai.fallback.enabled:true}") boolean fallbackEnabled,
			@Value("${spring.ai.fallback.max-attempts:2}") int maxAttempts,
			@Value("${spring.ai.fallback.backoff-ms:1000}") long backoffMs) {
		return buildChatClient(deepseek, glm, fallbackEnabled, maxAttempts, backoffMs);
	}

	@Bean
	public ChatClient glmChatClient(@Qualifier("glmChatModel") ChatModel glm,
			@Qualifier("deepseekChatModel") ChatModel deepseek,
			@Value("${spring.ai.fallback.enabled:true}") boolean fallbackEnabled,
			@Value("${spring.ai.fallback.max-attempts:2}") int maxAttempts,
			@Value("${spring.ai.fallback.backoff-ms:1000}") long backoffMs) {
		return buildChatClient(glm, deepseek, fallbackEnabled, maxAttempts, backoffMs);
	}

	private OpenAiChatModel buildModel(String baseUrl, String apiKey, String model) {
		return OpenAiChatModel.builder()
			.options(OpenAiChatOptions.builder()
				.baseUrl(baseUrl)
				.apiKey(apiKey)
				.model(model)
				.temperature(0.7)
				.build())
			.build();
	}

	private ChatClient buildChatClient(ChatModel primary, ChatModel fallback, boolean fallbackEnabled, int maxAttempts,
			long backoffMs) {
		ChatModel model = new ResilientChatModel(primary, fallbackEnabled ? fallback : null, maxAttempts,
				Duration.ofMillis(backoffMs));
		return ChatClient.builder(model)
			.defaultSystem(DEFAULT_SYSTEM)
			.defaultAdvisors(MessageChatMemoryAdvisor.builder(this.chatMemory).build())
			.build();
	}

}

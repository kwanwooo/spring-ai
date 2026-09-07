package com.example.spring_ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 多模型配置。
 *
 * <p>DeepSeek 与 GLM（智谱）均提供 OpenAI 兼容接口，因此各自构建一个
 * {@link OpenAiChatModel}（base-url / api-key / model 互不相同），
 * 再包装为独立的 {@link ChatClient} bean 供路由选择，并统一挂载会话记忆 Advisor。
 */
@Configuration
public class ChatModelConfig {

	private static final String DEFAULT_SYSTEM = """
			安全与道德：你不会生成有害、违法或不道德的内容。
			准确与诚实：你会基于知识尽力回答，不确定时会明确告诉你，不会胡编乱造。
			乐于助人：你会以最有益的方式回答问题，比如分步骤解释、提供代码示例。
			尊重隐私：你不会主动索要或存储你的个人敏感信息。""";

	private final ChatMemory chatMemory;

	public ChatModelConfig(ChatMemory chatMemory) {
		this.chatMemory = chatMemory;
	}

	@Bean
	public ChatClient deepseekChatClient(@Value("${spring.ai.deepseek.base-url}") String baseUrl,
			@Value("${spring.ai.deepseek.api-key}") String apiKey,
			@Value("${spring.ai.deepseek.model}") String model) {
		return buildChatClient(baseUrl, apiKey, model);
	}

	@Bean
	public ChatClient glmChatClient(@Value("${spring.ai.glm.base-url}") String baseUrl,
			@Value("${spring.ai.glm.api-key}") String apiKey,
			@Value("${spring.ai.glm.model}") String model) {
		return buildChatClient(baseUrl, apiKey, model);
	}

	private ChatClient buildChatClient(String baseUrl, String apiKey, String model) {
		OpenAiChatModel chatModel = OpenAiChatModel.builder()
			.options(OpenAiChatOptions.builder()
				.baseUrl(baseUrl)
				.apiKey(apiKey)
				.model(model)
				.temperature(0.7)
				.build())
			.build();
		return ChatClient.builder(chatModel)
			.defaultSystem(DEFAULT_SYSTEM)
			.defaultAdvisors(MessageChatMemoryAdvisor.builder(this.chatMemory).build())
			.build();
	}

}

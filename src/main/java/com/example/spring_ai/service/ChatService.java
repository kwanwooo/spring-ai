package com.example.spring_ai.service;

import java.util.Map;

import com.example.spring_ai.config.LoggingAdvisors;
import com.example.spring_ai.config.WebSearchTool;
import com.example.spring_ai.model.ModelProvider;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 多模型对话服务，按 {@link ModelProvider} 路由到对应的 {@link ChatClient}。
 */
@Service
public class ChatService {

	private final Map<ModelProvider, ChatClient> chatClients;

	public ChatService(@Qualifier("deepseekChatClient") ChatClient deepseekChatClient,
			@Qualifier("glmChatClient") ChatClient glmChatClient) {
		this.chatClients = Map.of(ModelProvider.DEEPSEEK, deepseekChatClient, ModelProvider.GLM, glmChatClient);
	}

	/**
	 * 单轮对话，返回模型回复文本。
	 *
	 * @param conversationId 会话 ID，用于区分不同会话的记忆；为空则使用默认会话
	 */
	public String chat(ModelProvider provider, String conversationId, String message) {

		return client(provider).prompt()
			.user(message)
			.advisors(new LoggingAdvisors())
			.advisors(spec -> setConversationId(spec, conversationId))
				.tools(new WebSearchTool())
			.call()
			.content();
	}

	/**
	 * 单轮对话，支持为本次调用自定义系统提示词。
	 */
	public String chat(ModelProvider provider, String conversationId, String system, String message) {
		return client(provider).prompt()
			.system(system)
			.user(message)
			.advisors(new LoggingAdvisors())
			.advisors(spec -> setConversationId(spec, conversationId))
				.tools(new WebSearchTool())
				.call()
			.content();
	}

	/**
	 * 流式对话，逐 token 返回回复内容。
	 */
	public Flux<String> chatStream(ModelProvider provider, String conversationId, String message) {
		return client(provider).prompt()
			.user(message)
			.advisors(new LoggingAdvisors())
			.advisors(spec -> setConversationId(spec, conversationId))
				.tools(new WebSearchTool())
				.stream()
			.content();
	}

	/**
	 * 流式对话，支持为本次调用自定义系统提示词。
	 */
	public Flux<String> chatStream(ModelProvider provider, String conversationId, String system, String message) {
		return client(provider).prompt()
			.system(system)
			.user(message)
			.advisors(new LoggingAdvisors())
			.advisors(spec -> setConversationId(spec, conversationId))
				.tools(new WebSearchTool())
				.stream()
			.content();
	}

	/**
	 * 结构化输出：让模型返回可反序列化为指定类型的实体。
	 */
	public <T> T chatForEntity(ModelProvider provider, String message, Class<T> type) {
		return client(provider).prompt().user(message).call().entity(type);
	}

	private void setConversationId(ChatClient.AdvisorSpec spec, String conversationId) {
		if (conversationId != null && !conversationId.isBlank()) {
			spec.param(ChatMemory.CONVERSATION_ID, conversationId);
		}
	}

	private ChatClient client(ModelProvider provider) {
		ChatClient chatClient = this.chatClients.get(provider);
		if (chatClient == null) {
			throw new IllegalArgumentException("不支持的模型提供方: " + provider);
		}
		return chatClient;
	}

}

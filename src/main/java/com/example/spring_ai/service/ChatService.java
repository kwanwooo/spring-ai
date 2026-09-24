package com.example.spring_ai.service;

import java.util.Map;

import com.example.spring_ai.config.LoggingAdvisors;
import com.example.spring_ai.config.WebSearchTool;
import com.example.spring_ai.model.ModelProvider;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 多模型对话服务，按 {@link ModelProvider} 路由到对应的 {@link ChatClient}。
 * 重试与降级由 {@code ResilientChatModel} 在模型层处理，本服务只负责编排。
 */
@Service
public class ChatService {

	private final Map<ModelProvider, ChatClient> chatClients;

	private final LoggingAdvisors loggingAdvisors;

	private final WebSearchTool webSearchTool;

	public ChatService(@Qualifier("deepseekChatClient") ChatClient deepseekChatClient,
			@Qualifier("glmChatClient") ChatClient glmChatClient,
			LoggingAdvisors loggingAdvisors,
			WebSearchTool webSearchTool) {
		this.chatClients = Map.of(ModelProvider.DEEPSEEK, deepseekChatClient, ModelProvider.GLM, glmChatClient);
		this.loggingAdvisors = loggingAdvisors;
		this.webSearchTool = webSearchTool;
	}

	/**
	 * 单轮对话，返回模型回复文本。
	 */
	public String chat(ModelProvider provider, String conversationId, String message) {
		return chat(provider, conversationId, null, message);
	}

	/**
	 * 单轮对话，支持自定义系统提示词。
	 */
	public String chat(ModelProvider provider, String conversationId, String system, String message) {
		ChatClient.ChatClientRequestSpec spec = client(provider).prompt().user(message);
		if (system != null && !system.isBlank()) {
			spec = spec.system(system);
		}
		return spec.advisors(this.loggingAdvisors)
			.advisors(s -> setConversationId(s, conversationId))
			.tools(this.webSearchTool)
			.call()
			.content();
	}

	/**
	 * 流式对话，逐 token 返回回复内容。
	 */
	public Flux<String> chatStream(ModelProvider provider, String conversationId, String message) {
		return chatStream(provider, conversationId, null, message);
	}

	/**
	 * 流式对话，支持自定义系统提示词。
	 */
	public Flux<String> chatStream(ModelProvider provider, String conversationId, String system, String message) {
		ChatClient.ChatClientRequestSpec spec = client(provider).prompt().user(message);
		if (system != null && !system.isBlank()) {
			spec = spec.system(system);
		}
		return spec.advisors(this.loggingAdvisors)
			.advisors(s -> setConversationId(s, conversationId))
			.tools(this.webSearchTool)
			.stream()
			.content();
	}

	/**
	 * 检索增强（RAG）对话：额外挂载 {@code ragAdvisor} 进行向量检索并注入上下文。
	 */
	public String ragChat(ModelProvider provider, String conversationId, String message, Advisor ragAdvisor) {
		return client(provider).prompt()
			.user(message)
			.advisors(this.loggingAdvisors)
			.advisors(spec -> setConversationId(spec, conversationId))
			.advisors(ragAdvisor)
			.call()
			.content();
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

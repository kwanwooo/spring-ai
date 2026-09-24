package com.example.spring_ai.service;

import java.util.Map;

import com.example.spring_ai.config.PricingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Token 用量与成本记录服务，把模型（对话 + 嵌入）的用量落库到 {@code AI_TOKEN_USAGE}。
 */
@Service
public class UsageService {

	private static final Logger log = LoggerFactory.getLogger(UsageService.class);

	private final JdbcTemplate jdbcTemplate;

	private final PricingProperties pricing;

	public UsageService(JdbcTemplate jdbcTemplate, PricingProperties pricing) {
		this.jdbcTemplate = jdbcTemplate;
		this.pricing = pricing;
	}

	/**
	 * 记录一次对话模型的用量。
	 */
	public void record(Map<String, Object> context, ChatResponse response) {
		if (response.getMetadata() == null) {
			return;
		}
		String conversationId = context != null ? (String) context.get(ChatMemory.CONVERSATION_ID) : null;
		doRecord(response.getMetadata().getModel(), conversationId, response.getMetadata().getUsage());
	}

	/**
	 * 记录一次嵌入模型的用量（无 conversation、输出 token 为 0）。
	 */
	public void recordEmbedding(EmbeddingResponse response) {
		if (response.getMetadata() == null) {
			return;
		}
		doRecord(response.getMetadata().getModel(), null, response.getMetadata().getUsage());
	}

	private void doRecord(String model, String conversationId, Usage usage) {
		try {
			if (usage == null || (usage.getPromptTokens() == null && usage.getCompletionTokens() == null)) {
				return;
			}
			int prompt = usage.getPromptTokens() == null ? 0 : usage.getPromptTokens();
			int completion = usage.getCompletionTokens() == null ? 0 : usage.getCompletionTokens();
			int total = usage.getTotalTokens() == null ? prompt + completion : usage.getTotalTokens();
			double cost = this.pricing.cost(model, prompt, completion);

			this.jdbcTemplate.update(
					"INSERT INTO AI_TOKEN_USAGE(conversation_id, model, prompt_tokens, completion_tokens, total_tokens, cost)"
							+ " VALUES(?,?,?,?,?,?)",
					conversationId, model, prompt, completion, total, cost);
		}
		catch (Exception e) {
			// 用量记录失败不应影响正常链路
			log.warn("记录 token 用量失败: {}", e.getMessage());
		}
	}

}

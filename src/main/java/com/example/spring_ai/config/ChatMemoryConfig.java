package com.example.spring_ai.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatMemoryConfig {

	// 将会话记忆持久化到数据库（JDBC），外层用 MessageWindowChatMemory 保留滑动窗口，
	// 仅保留最近 10 条消息；JdbcChatMemoryRepository 由 Spring AI 自动配置提供。
	// 注意：持久化的是「滑动窗口」（最近 10 条），而非全量历史；需要完整审计历史需另用全量存储方案
	@Bean
	public ChatMemory chatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepository) {
		return MessageWindowChatMemory.builder()
			.chatMemoryRepository(jdbcChatMemoryRepository)
			.maxMessages(10)
			.build();
	}

}

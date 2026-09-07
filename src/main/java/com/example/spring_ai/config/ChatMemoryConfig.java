package com.example.spring_ai.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatMemoryConfig {

	// 配置带滑动窗口的 ChatMemory，由 ChatModelConfig 中的各 ChatClient 通过
	// MessageChatMemoryAdvisor 引用，无需在此单独构建 ChatClient
	@Bean
	public ChatMemory chatMemory() {
		return MessageWindowChatMemory.builder()
			.maxMessages(10) // 限制记忆窗口大小为10条消息
			.build();
	}

}

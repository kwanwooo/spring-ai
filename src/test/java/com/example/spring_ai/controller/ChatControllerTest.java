package com.example.spring_ai.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.spring_ai.dto.ChatRequest;
import com.example.spring_ai.dto.ChatResponse;
import com.example.spring_ai.model.ModelProvider;
import com.example.spring_ai.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

/**
 * {@link ChatController} 路由与编排的单元测试。
 */
class ChatControllerTest {

	private ChatService chatService;

	private ChatController controller;

	@BeforeEach
	void setUp() {
		this.chatService = mock(ChatService.class);
		this.controller = new ChatController(this.chatService);
	}

	@Test
	void chatDefaultsToDeepseekWithoutSystem() {
		when(this.chatService.chat(ModelProvider.DEEPSEEK, null, "hi")).thenReturn("hello");

		ChatResponse resp = this.controller.chat(new ChatRequest("hi", null, null, null));

		assertThat(resp.content()).isEqualTo("hello");
		verify(this.chatService).chat(ModelProvider.DEEPSEEK, null, "hi");
	}

	@Test
	void chatRoutesToGlmWithSystem() {
		when(this.chatService.chat(ModelProvider.GLM, "c1", "sys", "hi")).thenReturn("hello");

		ChatResponse resp = this.controller.chat(new ChatRequest("hi", "sys", "glm", "c1"));

		assertThat(resp.content()).isEqualTo("hello");
		verify(this.chatService).chat(ModelProvider.GLM, "c1", "sys", "hi");
	}

	@Test
	void chatRejectsUnknownProvider() {
		assertThatThrownBy(() -> this.controller.chat(new ChatRequest("hi", null, "openai", null)))
			.isInstanceOf(IllegalArgumentException.class);
		verifyNoInteractions(this.chatService);
	}

	@Test
	void streamDefaultsToDeepseek() {
		when(this.chatService.chatStream(ModelProvider.DEEPSEEK, null, "hi")).thenReturn(Flux.just("a"));

		Flux<String> flux = this.controller.stream("hi", null, null, "deepseek");

		assertThat(flux.collectList().block()).containsExactly("a");
		verify(this.chatService).chatStream(ModelProvider.DEEPSEEK, null, "hi");
	}

	@Test
	void streamRoutesToGlmWithSystem() {
		when(this.chatService.chatStream(ModelProvider.GLM, "c1", "sys", "hi")).thenReturn(Flux.just("a", "b"));

		Flux<String> flux = this.controller.stream("hi", "sys", "c1", "GLM");

		assertThat(flux.collectList().block()).containsExactly("a", "b");
		verify(this.chatService).chatStream(ModelProvider.GLM, "c1", "sys", "hi");
	}

}

package com.example.spring_ai.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * {@link ResilientChatModel} 的重试 / 降级 / transient 判定单元测试。
 */
class ResilientChatModelTest {

	@Test
	void getOptionsDelegatesToPrimary() {
		ChatModel primary = mock(ChatModel.class);
		ChatOptions options = mock(ChatOptions.class);
		when(primary.getOptions()).thenReturn(options);

		ResilientChatModel model = new ResilientChatModel(primary, null, 1, Duration.ZERO);

		assertThat(model.getOptions()).isSameAs(options);
	}

	@Test
	void callSucceedsOnFirstAttempt() {
		ChatModel primary = mock(ChatModel.class);
		ChatResponse ok = mock(ChatResponse.class);
		when(primary.call(any(Prompt.class))).thenReturn(ok);
		ResilientChatModel model = new ResilientChatModel(primary, null, 2, Duration.ZERO);

		assertThat(model.call(new Prompt(new UserMessage("hi")))).isSameAs(ok);
		verify(primary, times(1)).call(any(Prompt.class));
	}

	@Test
	void callRetriesOnTransientThenSucceeds() {
		ChatModel primary = mock(ChatModel.class);
		ChatResponse ok = mock(ChatResponse.class);
		when(primary.call(any(Prompt.class)))
			.thenThrow(new RuntimeException("request timeout"))
			.thenReturn(ok);
		ResilientChatModel model = new ResilientChatModel(primary, null, 2, Duration.ZERO);

		assertThat(model.call(new Prompt(new UserMessage("hi")))).isSameAs(ok);
		verify(primary, times(2)).call(any(Prompt.class));
	}

	@Test
	void callFallsBackOnNonTransient() {
		ChatModel primary = mock(ChatModel.class);
		ChatModel fallback = mock(ChatModel.class);
		ChatResponse ok = mock(ChatResponse.class);
		when(primary.call(any(Prompt.class))).thenThrow(new RuntimeException("400 bad request"));
		when(fallback.call(any(Prompt.class))).thenReturn(ok);
		ResilientChatModel model = new ResilientChatModel(primary, fallback, 2, Duration.ZERO);

		assertThat(model.call(new Prompt(new UserMessage("hi")))).isSameAs(ok);
		verify(primary, times(1)).call(any(Prompt.class));
		verify(fallback, times(1)).call(any(Prompt.class));
	}

	@Test
	void callFallsBackAfterRetriesExhausted() {
		ChatModel primary = mock(ChatModel.class);
		ChatModel fallback = mock(ChatModel.class);
		ChatResponse ok = mock(ChatResponse.class);
		when(primary.call(any(Prompt.class))).thenThrow(new RuntimeException("timeout"));
		when(fallback.call(any(Prompt.class))).thenReturn(ok);
		ResilientChatModel model = new ResilientChatModel(primary, fallback, 3, Duration.ZERO);

		assertThat(model.call(new Prompt(new UserMessage("hi")))).isSameAs(ok);
		verify(primary, times(3)).call(any(Prompt.class));
		verify(fallback, times(1)).call(any(Prompt.class));
	}

	@Test
	void callThrowsLastWhenNoFallbackAndExhausted() {
		ChatModel primary = mock(ChatModel.class);
		RuntimeException last = new RuntimeException("400 bad request");
		when(primary.call(any(Prompt.class))).thenThrow(last);
		ResilientChatModel model = new ResilientChatModel(primary, null, 2, Duration.ZERO);

		assertThatThrownBy(() -> model.call(new Prompt(new UserMessage("hi")))).isSameAs(last);
	}

	@Test
	void streamSucceedsOnPrimary() {
		ChatModel primary = mock(ChatModel.class);
		ChatModel fallback = mock(ChatModel.class);
		ChatResponse ok = mock(ChatResponse.class);
		when(primary.stream(any(Prompt.class))).thenReturn(Flux.just(ok));
		ResilientChatModel model = new ResilientChatModel(primary, fallback, 1, Duration.ZERO);

		assertThat(model.stream(new Prompt(new UserMessage("hi"))).blockFirst()).isSameAs(ok);
		verify(fallback, never()).stream(any(Prompt.class));
	}

	@Test
	void streamFallsBackOnError() {
		ChatModel primary = mock(ChatModel.class);
		ChatModel fallback = mock(ChatModel.class);
		ChatResponse ok = mock(ChatResponse.class);
		when(primary.stream(any(Prompt.class))).thenReturn(Flux.error(new RuntimeException("timeout")));
		when(fallback.stream(any(Prompt.class))).thenReturn(Flux.just(ok));
		ResilientChatModel model = new ResilientChatModel(primary, fallback, 1, Duration.ZERO);

		assertThat(model.stream(new Prompt(new UserMessage("hi"))).blockFirst()).isSameAs(ok);
	}

	@Test
	void isTransientForIoAndTimeout() {
		assertThat(ResilientChatModel.isTransient(new IOException())).isTrue();
		assertThat(ResilientChatModel.isTransient(new TimeoutException())).isTrue();
	}

	@Test
	void isTransientForMessagePatterns() {
		assertThat(ResilientChatModel.isTransient(new RuntimeException("429 Too Many Requests"))).isTrue();
		assertThat(ResilientChatModel.isTransient(new RuntimeException("gateway timeout"))).isTrue();
		assertThat(ResilientChatModel.isTransient(new RuntimeException("HTTP 503"))).isTrue();
		assertThat(ResilientChatModel.isTransient(new RuntimeException("HTTP 408"))).isTrue();
	}

	@Test
	void isNotTransientForClientErrors() {
		assertThat(ResilientChatModel.isTransient(new RuntimeException("400 bad request"))).isFalse();
		assertThat(ResilientChatModel.isTransient(new RuntimeException("401 unauthorized"))).isFalse();
		assertThat(ResilientChatModel.isTransient(new RuntimeException("plain error"))).isFalse();
	}

	@Test
	void isTransientWalksCauseChain() {
		assertThat(ResilientChatModel.isTransient(new RuntimeException(new IOException()))).isTrue();
	}

}

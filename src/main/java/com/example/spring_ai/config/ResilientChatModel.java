package com.example.spring_ai.config;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

/**
 * 带「重试 + 降级」的 {@link ChatModel} 装饰器。
 *
 * <p>工作在 advisor 链之下：记忆 / 日志 / 用量 advisor 只执行一次，失败重试不会重复写记忆。
 * 仅对 transient 错误（网络、超时、429、5xx）重试；非 transient 错误（4xx）直接跳过当前模型。
 */
public class ResilientChatModel implements ChatModel {

	private static final Logger log = LoggerFactory.getLogger(ResilientChatModel.class);

	private final ChatModel primary;

	private final ChatModel fallback;

	private final int maxAttempts;

	private final Duration backoff;

	public ResilientChatModel(ChatModel primary, ChatModel fallback, int maxAttempts, Duration backoff) {
		this.primary = primary;
		this.fallback = fallback;
		this.maxAttempts = Math.max(1, maxAttempts);
		this.backoff = backoff;
	}

	@Override
	public ChatOptions getOptions() {
		return this.primary.getOptions();
	}

	@Override
	public ChatResponse call(Prompt prompt) {
		ChatModel[] models = this.fallback != null
				? new ChatModel[] { this.primary, this.fallback }
				: new ChatModel[] { this.primary };
		RuntimeException last = null;
		for (ChatModel model : models) {
			Prompt modelPrompt = promptFor(model, prompt);
			for (int attempt = 1; attempt <= this.maxAttempts; attempt++) {
				try {
					return model.call(modelPrompt);
				}
				catch (RuntimeException e) {
					last = e;
					if (isTransient(e) && attempt < this.maxAttempts) {
						log.warn("模型调用失败，{}ms 后重试（第 {}/{} 次）: {}", this.backoff.toMillis(), attempt,
								this.maxAttempts, e.getMessage());
						sleep(this.backoff.toMillis());
					}
					else {
						// 非 transient 或重试耗尽 → 放弃当前模型
						break;
					}
				}
			}
		}
		throw last;
	}

	@Override
	public Flux<ChatResponse> stream(Prompt prompt) {
		Flux<ChatResponse> flux = doStream(this.primary, promptFor(this.primary, prompt));
		if (this.fallback != null) {
			flux = flux.onErrorResume(e -> doStream(this.fallback, promptFor(this.fallback, prompt)));
		}
		return flux;
	}

	private Prompt promptFor(ChatModel model, Prompt prompt) {
		return prompt.mutate().chatOptions(model.getOptions()).build();
	}

	private Flux<ChatResponse> doStream(ChatModel model, Prompt prompt) {
		Flux<ChatResponse> flux = model.stream(prompt);
		if (this.maxAttempts > 1) {
			flux = flux.retryWhen(
					Retry.backoff(this.maxAttempts - 1, this.backoff).filter(ResilientChatModel::isTransient));
		}
		return flux;
	}

	/**
	 * 判断异常是否 transient（可重试）：网络/超时异常、429、5xx、408。
	 */
	static boolean isTransient(Throwable t) {
		Throwable e = t;
		while (e != null) {
			if (e instanceof IOException || e instanceof TimeoutException) {
				return true;
			}
			String msg = e.getMessage();
			if (msg != null) {
				String m = msg.toLowerCase();
				if (m.contains("429") || m.contains("too many requests") || m.contains("timeout")
						|| m.contains("timed out") || m.matches(".*\\b5[0-9]{2}\\b.*") || m.matches(".*\\b408\\b.*")) {
					return true;
				}
			}
			e = e.getCause();
		}
		return false;
	}

	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}

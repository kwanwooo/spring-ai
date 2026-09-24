package com.example.spring_ai.config;

import com.example.spring_ai.service.UsageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 日志 + Token 用量记录 Advisor。
 *
 * <p>同步与流式两条链路都会经过这里：记录请求、响应与耗时，并把模型的
 * token 用量（含成本）交给 {@link UsageService} 落库。
 */
@Component
public class LoggingAdvisors implements CallAdvisor, StreamAdvisor {

	private static final Logger log = LoggerFactory.getLogger(LoggingAdvisors.class);

	private final UsageService usageService;

	public LoggingAdvisors(UsageService usageService) {
		this.usageService = usageService;
	}

	@Override
	public String getName() {
		return "loggingAdvisors";
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain chain) {
		printPrompt(chatClientRequest);

		long start = System.currentTimeMillis();
		ChatClientResponse response = chain.nextCall(chatClientRequest);
		long end = System.currentTimeMillis();

		String responseText = response.chatResponse().getResult().getOutput().getText();
		printTimeAndText(start, end, responseText);
		usageService.record(chatClientRequest.context(), response.chatResponse());
		return response;
	}

	@Override
	public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain chain) {
		printPrompt(chatClientRequest);

		long start = System.currentTimeMillis();
		Flux<ChatClientResponse> flux = chain.nextStream(chatClientRequest);

		return new ChatClientMessageAggregator().aggregateChatClientResponse(flux, response -> {
			long end = System.currentTimeMillis();
			String responseText = response.chatResponse().getResult().getOutput().getText();
			printTimeAndText(start, end, responseText);
			usageService.record(chatClientRequest.context(), response.chatResponse());
		});
	}

	private void printPrompt(ChatClientRequest chatClientRequest) {
		log.info("用户提示词: {}", chatClientRequest.prompt().getUserMessage());
		log.info("系统提示词: {}", chatClientRequest.prompt().getSystemMessages());
	}

	private void printTimeAndText(long start, long end, String responseText) {
		log.info("AI回复 (耗时{}ms): {}", (end - start), responseText);
	}

}

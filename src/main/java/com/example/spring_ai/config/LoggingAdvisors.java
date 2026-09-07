package com.example.spring_ai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;

/**
 *  添加日志 Advisor
 */
public class LoggingAdvisors implements CallAdvisor, StreamAdvisor {
    private static final Logger log = LoggerFactory.getLogger(LoggingAdvisors.class);

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
        // 1. 前置处理：记录请求
        printPrompt(chatClientRequest);

        long start = System.currentTimeMillis();
        // 2. 执行调用链（调用AI模型）
        ChatClientResponse chatClientResponse = chain.nextCall(chatClientRequest);
        long end = System.currentTimeMillis();

        // 3. 后置处理：记录响应和耗时
        String responseText = chatClientResponse.chatResponse().getResult().getOutput().getText();
        printTimeAndText(start, end, responseText);
        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain chain) {
        // 1. 前置处理：记录请求
        printPrompt(chatClientRequest);

        long start = System.currentTimeMillis();
        // 2. 执行调用链（调用AI模型）
        Flux<ChatClientResponse> flux = chain.nextStream(chatClientRequest);
        long end = System.currentTimeMillis();

        // 3. 后置处理：记录响应和耗时
        return (new ChatClientMessageAggregator()).aggregateChatClientResponse(flux, advisedResponse -> {
            String responseText = advisedResponse.chatResponse().getResult().getOutput().getText();
            printTimeAndText(start, end, responseText);
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

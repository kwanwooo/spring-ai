package com.example.spring_ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;

import com.example.spring_ai.config.LoggingAdvisors;
import com.example.spring_ai.config.WebSearchTool;
import com.example.spring_ai.model.ModelProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import reactor.core.publisher.Flux;

/**
 * {@link ChatService} 编排逻辑的单元测试：验证 provider 路由、系统提示词、会话 ID、工具挂载与 RAG advisor。
 */
class ChatServiceTest {

    private ChatClient deepseekClient;

    private ChatClient glmClient;

    private LoggingAdvisors loggingAdvisors;

    private WebSearchTool webSearchTool;

    private ChatService chatService;

    private ChatClient.ChatClientRequestSpec deepseekSpec;

    private ChatClient.CallResponseSpec deepseekCallSpec;

    private ChatClient.StreamResponseSpec deepseekStreamSpec;

    private ChatClient.ChatClientRequestSpec glmSpec;

    private ChatClient.CallResponseSpec glmCallSpec;

    @BeforeEach
    void setUp() {
        this.deepseekClient = mock(ChatClient.class);
        this.glmClient = mock(ChatClient.class);
        this.loggingAdvisors = mock(LoggingAdvisors.class);
        this.webSearchTool = mock(WebSearchTool.class);

        this.deepseekSpec = mock(ChatClient.ChatClientRequestSpec.class, RETURNS_SELF);
        this.deepseekCallSpec = mock(ChatClient.CallResponseSpec.class);
        this.deepseekStreamSpec = mock(ChatClient.StreamResponseSpec.class);

        this.glmSpec = mock(ChatClient.ChatClientRequestSpec.class, RETURNS_SELF);
        this.glmCallSpec = mock(ChatClient.CallResponseSpec.class);

        when(this.deepseekClient.prompt()).thenReturn(this.deepseekSpec);
        when(this.deepseekSpec.call()).thenReturn(this.deepseekCallSpec);
        when(this.deepseekSpec.stream()).thenReturn(this.deepseekStreamSpec);

        when(this.glmClient.prompt()).thenReturn(this.glmSpec);
        when(this.glmSpec.call()).thenReturn(this.glmCallSpec);

        this.chatService = new ChatService(this.deepseekClient, this.glmClient, this.loggingAdvisors,
                this.webSearchTool);
    }

    @Test
    void chatRoutesToDeepseekAndReturnsContent() {
        when(this.deepseekCallSpec.content()).thenReturn("hello");

        String result = this.chatService.chat(ModelProvider.DEEPSEEK, null, "hi");

        assertThat(result).isEqualTo("hello");
        verify(this.deepseekClient).prompt();
        verify(this.glmClient, never()).prompt();
        verify(this.deepseekSpec).tools(this.webSearchTool);
    }

    @Test
    void chatRoutesToGlm() {
        when(this.glmCallSpec.content()).thenReturn("hello");

        String result = this.chatService.chat(ModelProvider.GLM, "c1", "sys", "hi");

        assertThat(result).isEqualTo("hello");
        verify(this.glmClient).prompt();
        verify(this.deepseekClient, never()).prompt();
    }

    @Test
    void chatAppliesSystemPromptWhenPresent() {
        when(this.deepseekCallSpec.content()).thenReturn("hello");

        this.chatService.chat(ModelProvider.DEEPSEEK, null, "sys", "hi");

        verify(this.deepseekSpec).system("sys");
    }

    @Test
    void chatSkipsBlankSystemPrompt() {
        when(this.deepseekCallSpec.content()).thenReturn("hello");

        this.chatService.chat(ModelProvider.DEEPSEEK, null, "   ", "hi");

        verify(this.deepseekSpec, never()).system(anyString());
    }

    @Test
    void chatSetsConversationIdWhenPresent() {
        when(this.deepseekCallSpec.content()).thenReturn("hello");

        this.chatService.chat(ModelProvider.DEEPSEEK, "conv-1", null, "hi");

        ChatClient.AdvisorSpec advisorSpec = mock(ChatClient.AdvisorSpec.class);
        conversationIdAdvisor().accept(advisorSpec);
        verify(advisorSpec).param(ChatMemory.CONVERSATION_ID, "conv-1");
    }

    @Test
    void chatSkipsConversationIdWhenBlank() {
        when(this.deepseekCallSpec.content()).thenReturn("hello");

        this.chatService.chat(ModelProvider.DEEPSEEK, "   ", null, "hi");

        ChatClient.AdvisorSpec advisorSpec = mock(ChatClient.AdvisorSpec.class);
        conversationIdAdvisor().accept(advisorSpec);
        verify(advisorSpec, never()).param(anyString(), any());
    }

    @Test
    void chatRejectsNullProvider() {
        assertThatThrownBy(() -> this.chatService.chat(null, null, "hi"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("不支持的模型提供方");
    }

    @Test
    void chatStreamReturnsContentFlux() {
        when(this.deepseekStreamSpec.content()).thenReturn(Flux.just("a", "b"));

        Flux<String> flux = this.chatService.chatStream(ModelProvider.DEEPSEEK, null, "hi");

        assertThat(flux.collectList().block()).containsExactly("a", "b");
        verify(this.deepseekClient).prompt();
        verify(this.glmClient, never()).prompt();
    }

    @Test
    void ragChatAppliesRagAdvisor() {
        Advisor ragAdvisor = mock(Advisor.class);
        when(this.deepseekCallSpec.content()).thenReturn("answer");

        String result = this.chatService.ragChat(ModelProvider.DEEPSEEK, null, "hi", ragAdvisor);

        assertThat(result).isEqualTo("answer");
        verify(this.deepseekSpec).advisors(ragAdvisor);
    }

    @SuppressWarnings("unchecked")
    private Consumer<ChatClient.AdvisorSpec> conversationIdAdvisor() {
        ArgumentCaptor<Consumer<ChatClient.AdvisorSpec>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(this.deepseekSpec).advisors(captor.capture());
        return captor.getValue();
    }

}

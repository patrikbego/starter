package com.starter.application;

import com.starter.ports.AiChatPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private AiChatPort aiChatPort;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(aiChatPort);
    }

    @Test
    void sendMessage_usesProvidedSessionId() {
        when(aiChatPort.complete(eq("hello"), eq(Optional.of("session-1"))))
                .thenReturn(new AiChatPort.ChatResult("Echo: hello", "session-1"));

        var result = chatService.sendMessage("hello", Optional.of("session-1"));

        assertThat(result.reply()).isEqualTo("Echo: hello");
        assertThat(result.sessionId()).isEqualTo("session-1");
        verify(aiChatPort).complete("hello", Optional.of("session-1"));
    }

    @Test
    void sendMessage_generatesSessionIdWhenMissing() {
        when(aiChatPort.complete(eq("hello"), any()))
                .thenAnswer(invocation -> {
                    Optional<String> sessionId = invocation.getArgument(1);
                    return new AiChatPort.ChatResult("Echo: hello", sessionId.orElseThrow());
                });

        var result = chatService.sendMessage("hello", Optional.empty());

        assertThat(result.reply()).isEqualTo("Echo: hello");
        assertThat(result.sessionId()).isNotBlank();
    }
}

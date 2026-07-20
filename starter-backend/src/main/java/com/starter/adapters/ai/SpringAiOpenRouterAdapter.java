package com.starter.adapters.ai;

import com.starter.application.AiProviderException;
import com.starter.ports.AiChatPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Profile("!local")
public class SpringAiOpenRouterAdapter implements AiChatPort {

    private final ChatClient chatClient;

    @Override
    public ChatResult complete(String userMessage, Optional<String> sessionId) {
        try {
            String reply = chatClient.prompt()
                    .user(userMessage)
                    .call()
                    .content();
            String resolvedSessionId = sessionId.orElse("");
            return new ChatResult(reply, resolvedSessionId);
        } catch (Exception e) {
            log.error("AI provider request failed", e);
            throw new AiProviderException("AI provider request failed", e);
        }
    }
}

package com.starter.application;

import com.starter.ports.AiChatPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final AiChatPort aiChatPort;

    public AiChatPort.ChatResult sendMessage(String message, Optional<String> sessionId) {
        String resolvedSessionId = sessionId.filter(id -> !id.isBlank()).orElse(UUID.randomUUID().toString());
        return aiChatPort.complete(message, Optional.of(resolvedSessionId));
    }
}

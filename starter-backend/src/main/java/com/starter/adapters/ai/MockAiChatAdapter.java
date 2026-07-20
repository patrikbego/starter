package com.starter.adapters.ai;

import com.starter.ports.AiChatPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@Profile("local")
public class MockAiChatAdapter implements AiChatPort {

    @Override
    public ChatResult complete(String userMessage, Optional<String> sessionId) {
        log.debug("MockAiChatAdapter: returning echo response");
        String resolvedSessionId = sessionId.orElse("mock-session");
        return new ChatResult("Echo: " + userMessage, resolvedSessionId);
    }
}

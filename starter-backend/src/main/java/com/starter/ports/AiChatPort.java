package com.starter.ports;

import java.util.Optional;

public interface AiChatPort {

    ChatResult complete(String userMessage, Optional<String> sessionId);

    record ChatResult(String reply, String sessionId) {}
}

package com.starter.api;

import com.starter.api.dto.ChatRequest;
import com.starter.api.dto.ChatResponse;
import com.starter.application.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        var result = chatService.sendMessage(
                request.getMessage(),
                Optional.ofNullable(request.getSessionId())
        );
        return ChatResponse.builder()
                .reply(result.reply())
                .sessionId(result.sessionId())
                .build();
    }
}

package com.starter.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank(message = "Message must not be empty")
    @Size(max = 4000, message = "Message must be at most 4000 characters")
    private String message;

    private String sessionId;
}

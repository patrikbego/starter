package com.starter.api.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChatResponse {
    String reply;
    String sessionId;
}

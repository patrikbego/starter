package com.starter.api;

import com.starter.ports.AiChatPort;
import com.starter.security.FirebaseAuthService;
import com.starter.security.FirebaseUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "firebase.enabled=false",
        "spring.cloud.gcp.firestore.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ChatControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FirebaseAuthService firebaseAuthService;

    @MockBean
    private AiChatPort aiChatPort;

    @Test
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn400ForEmptyMessage() throws Exception {
        when(firebaseAuthService.verifyIdToken("valid-token"))
                .thenReturn(FirebaseUser.builder().uid("uid").authorities(List.of()).build());

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturnChatReply() throws Exception {
        when(firebaseAuthService.verifyIdToken("valid-token"))
                .thenReturn(FirebaseUser.builder().uid("uid").authorities(List.of()).build());
        when(aiChatPort.complete(eq("hello"), any()))
                .thenReturn(new AiChatPort.ChatResult("Echo: hello", "session-123"));

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Echo: hello"))
                .andExpect(jsonPath("$.sessionId").value("session-123"));
    }
}

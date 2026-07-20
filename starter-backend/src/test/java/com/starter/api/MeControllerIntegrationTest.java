package com.starter.api;

import com.starter.domain.User;
import com.starter.ports.UserRepositoryPort;
import com.starter.security.FirebaseAuthService;
import com.starter.security.FirebaseUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "firebase.enabled=false",
        "spring.cloud.gcp.firestore.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class MeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FirebaseAuthService firebaseAuthService;

    @MockBean
    private UserRepositoryPort userRepository;

    @Test
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCreateUserOnFirstCall() throws Exception {
        String uid = "test-uid";
        String email = "test@example.com";
        String name = "Test User";

        FirebaseUser firebaseUser = FirebaseUser.builder()
                .uid(uid)
                .email(email)
                .name(name)
                .authorities(List.of())
                .build();

        User createdUser = User.builder()
                .id(uid)
                .email(email)
                .displayName(name)
                .createdAt(Instant.parse("2026-01-15T10:00:00Z"))
                .updatedAt(Instant.parse("2026-01-15T10:00:00Z"))
                .build();

        when(firebaseAuthService.verifyIdToken("valid-token")).thenReturn(firebaseUser);
        when(userRepository.findById(uid)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(createdUser);

        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(uid))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.displayName").value(name))
                .andExpect(jsonPath("$.createdAt").value("2026-01-15T10:00:00Z"));
    }

    @Test
    void shouldReturnExistingUser() throws Exception {
        String uid = "existing-uid";
        FirebaseUser firebaseUser = FirebaseUser.builder()
                .uid(uid)
                .email("existing@example.com")
                .name("Existing User")
                .authorities(List.of())
                .build();

        User existingUser = User.builder()
                .id(uid)
                .email("existing@example.com")
                .displayName("Existing User")
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();

        when(firebaseAuthService.verifyIdToken("valid-token")).thenReturn(firebaseUser);
        when(userRepository.findById(uid)).thenReturn(Optional.of(existingUser));

        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(uid))
                .andExpect(jsonPath("$.email").value("existing@example.com"));
    }
}

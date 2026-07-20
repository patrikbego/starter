package com.starter.application;

import com.starter.domain.User;
import com.starter.ports.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepositoryPort userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void getOrCreateUser_returnsExistingUser() {
        User existing = User.builder()
                .id("uid-1")
                .email("user@example.com")
                .displayName("User")
                .build();

        when(userRepository.findById("uid-1")).thenReturn(Optional.of(existing));

        User result = userService.getOrCreateUser("uid-1", "user@example.com", "User");

        assertThat(result).isEqualTo(existing);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getOrCreateUser_createsUserWhenMissing() {
        when(userRepository.findById("uid-2")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.getOrCreateUser("uid-2", "new@example.com", "New User");

        assertThat(result.getId()).isEqualTo("uid-2");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getDisplayName()).isEqualTo("New User");
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo("uid-2");
    }
}

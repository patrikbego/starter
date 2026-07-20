package com.starter.application;

import com.starter.domain.User;
import com.starter.ports.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepositoryPort userRepository;

    public User getOrCreateUser(String firebaseUid, String email, String displayName) {
        return userRepository.findById(firebaseUid)
                .orElseGet(() -> createUser(firebaseUid, email, displayName));
    }

    private User createUser(String firebaseUid, String email, String displayName) {
        log.info("Creating new user: {} ({})", firebaseUid, email);
        Instant now = Instant.now();
        User user = User.builder()
                .id(firebaseUid)
                .email(email)
                .displayName(displayName)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return userRepository.save(user);
    }

    public Optional<User> getUser(String id) {
        return userRepository.findById(id);
    }
}

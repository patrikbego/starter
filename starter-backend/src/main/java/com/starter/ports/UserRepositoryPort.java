package com.starter.ports;

import com.starter.domain.User;

import java.util.Optional;

public interface UserRepositoryPort {
    Optional<User> findById(String id);
    User save(User user);
}

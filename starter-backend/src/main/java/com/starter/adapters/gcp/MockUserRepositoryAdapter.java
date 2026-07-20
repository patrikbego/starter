package com.starter.adapters.gcp;

import com.starter.domain.User;
import com.starter.ports.UserRepositoryPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("local")
public class MockUserRepositoryAdapter implements UserRepositoryPort {

    private final Map<String, User> users = new ConcurrentHashMap<>();

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public User save(User user) {
        users.put(user.getId(), user);
        return user;
    }
}

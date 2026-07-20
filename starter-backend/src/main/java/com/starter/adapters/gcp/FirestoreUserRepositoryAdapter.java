package com.starter.adapters.gcp;

import com.starter.domain.User;
import com.starter.ports.UserRepositoryPort;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Slf4j
@Repository
@RequiredArgsConstructor
@Profile("!local")
public class FirestoreUserRepositoryAdapter implements UserRepositoryPort {

    private final Firestore firestore;
    private static final String COLLECTION = "users";

    @Override
    public Optional<User> findById(String id) {
        try {
            var documentSnapshot = firestore.collection(COLLECTION).document(id).get().get();
            if (documentSnapshot.exists()) {
                return Optional.ofNullable(documentSnapshot.toObject(User.class));
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error fetching user from Firestore: {}", id, e);
            throw new RuntimeException("Failed to fetch user", e);
        }
        return Optional.empty();
    }

    @Override
    public User save(User user) {
        try {
            firestore.collection(COLLECTION).document(user.getId()).set(user).get();
            return user;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error saving user to Firestore: {}", user.getId(), e);
            throw new RuntimeException("Failed to save user", e);
        }
    }
}

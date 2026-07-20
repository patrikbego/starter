package com.starter.adapters.firebase;

import com.starter.security.FirebaseAuthService;
import com.starter.security.FirebaseUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Service
@Profile("local")
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class MockFirebaseAuthService implements FirebaseAuthService {

    private static final String MOCK_UID_PREFIX = "local-user-";

    @Override
    public FirebaseUser verifyIdToken(String idToken) {
        log.debug("MockFirebaseAuthService: Verifying token (local development mode)");

        String uid = generateMockUid(idToken);
        String email = generateMockEmail(uid);

        log.info("MockFirebaseAuthService: Authenticated local user {} with email {}", uid, email);

        return FirebaseUser.builder()
                .uid(uid)
                .email(email)
                .name("Local Developer")
                .picture(null)
                .emailVerified(true)
                .claims(Map.of("role", "USER"))
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    private String generateMockUid(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            return MOCK_UID_PREFIX + UUID.randomUUID().toString().substring(0, 8);
        }
        int hash = Math.abs(idToken.hashCode());
        return MOCK_UID_PREFIX + Integer.toHexString(hash);
    }

    private String generateMockEmail(String uid) {
        return uid + "@local.dev";
    }
}

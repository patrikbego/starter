package com.starter.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Profile("!local")
@ConditionalOnBean(FirebaseAuth.class)
public class FirebaseAuthServiceImpl implements FirebaseAuthService {

    private final FirebaseAuth firebaseAuth;

    @Override
    public FirebaseUser verifyIdToken(String idToken) {
        try {
            FirebaseToken token = firebaseAuth.verifyIdToken(idToken);
            return FirebaseUser.builder()
                    .uid(token.getUid())
                    .email(token.getEmail())
                    .name(token.getName())
                    .picture(token.getPicture())
                    .emailVerified(token.isEmailVerified())
                    .claims(token.getClaims())
                    .authorities(extractAuthorities(token))
                    .build();
        } catch (FirebaseAuthException e) {
            throw new AuthException(e.getMessage(), e);
        }
    }

    private Collection<? extends GrantedAuthority> extractAuthorities(FirebaseToken token) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        Object role = token.getClaims().get("role");
        if (role != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toString().toUpperCase()));
        }
        return authorities;
    }
}

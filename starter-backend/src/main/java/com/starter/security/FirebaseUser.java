package com.starter.security;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Map;

@Getter
@Builder
public class FirebaseUser {
    private final String uid;
    private final String email;
    private final String name;
    private final String picture;
    private final boolean emailVerified;
    private final Map<String, Object> claims;
    private final Collection<? extends GrantedAuthority> authorities;
}

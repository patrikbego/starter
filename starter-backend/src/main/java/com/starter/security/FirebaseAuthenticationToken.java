package com.starter.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class FirebaseAuthenticationToken extends AbstractAuthenticationToken {

    private final FirebaseUser user;

    public FirebaseAuthenticationToken(FirebaseUser user) {
        super(user.getAuthorities());
        this.user = user;
        setAuthenticated(true);
    }

    public FirebaseAuthenticationToken(FirebaseUser user, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.user = user;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return user;
    }

    public FirebaseUser getUser() {
        return user;
    }

    public String getUid() {
        return user.getUid();
    }

    public String getEmail() {
        return user.getEmail();
    }
}

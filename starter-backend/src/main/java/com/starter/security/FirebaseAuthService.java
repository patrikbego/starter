package com.starter.security;

public interface FirebaseAuthService {
    FirebaseUser verifyIdToken(String idToken);
}

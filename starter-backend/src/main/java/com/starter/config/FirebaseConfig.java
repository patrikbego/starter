package com.starter.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

@Configuration
@Slf4j
@Profile("!local")
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
public class FirebaseConfig {

    @Value("${firebase.emulator.host:}")
    private String emulatorHost;

    @Value("${spring.cloud.gcp.project-id:}")
    private String projectId;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            if (emulatorHost != null && !emulatorHost.isBlank()) {
                log.info("Firebase Auth Emulator mode enabled: {}", emulatorHost);
                System.setProperty("FIREBASE_AUTH_EMULATOR_HOST", emulatorHost);
            } else {
                log.info("Using production Firebase Auth");
            }

            log.info("Initializing Firebase App with project ID: {}", projectId);

            GoogleCredentials credentials;
            try {
                credentials = GoogleCredentials.getApplicationDefault();
                log.info("Using Application Default Credentials");
            } catch (IOException e) {
                if (emulatorHost != null && !emulatorHost.isBlank()) {
                    log.info("Using NO-OP credentials for Firebase Auth Emulator");
                    credentials = GoogleCredentials.newBuilder().build();
                } else {
                    throw e;
                }
            }

            FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                    .setCredentials(credentials);

            if (projectId != null && !projectId.isBlank()) {
                optionsBuilder.setProjectId(projectId);
            }

            FirebaseApp app = FirebaseApp.initializeApp(optionsBuilder.build());
            log.info("Firebase App initialized: {}", app.getName());
            return app;
        }
        return FirebaseApp.getInstance();
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }
}

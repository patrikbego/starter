package com.starter;

import com.starter.security.FirebaseAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "firebase.enabled=false",
        "spring.cloud.gcp.firestore.enabled=false"
})
@ActiveProfiles("local")
class StarterApplicationTests {

    @MockBean
    private FirebaseAuthService firebaseAuthService;

    @Test
    void contextLoads() {
    }
}

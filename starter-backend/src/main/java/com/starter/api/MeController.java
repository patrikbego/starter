package com.starter.api;

import com.starter.api.dto.UserResponse;
import com.starter.application.UserService;
import com.starter.security.FirebaseUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;

    @GetMapping
    public UserResponse getMe(@AuthenticationPrincipal FirebaseUser firebaseUser) {
        var user = userService.getOrCreateUser(
                firebaseUser.getUid(),
                firebaseUser.getEmail(),
                firebaseUser.getName()
        );
        return UserResponse.from(user);
    }
}

package com.anverraglobal.insurance.controller;

import com.anverraglobal.insurance.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        // Mocked response for now, ideally fetch full UserDTO from UserService
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", Map.of(
                "id", userPrincipal.getId(),
                "email", userPrincipal.getUsername()
            )
        ));
    }
}

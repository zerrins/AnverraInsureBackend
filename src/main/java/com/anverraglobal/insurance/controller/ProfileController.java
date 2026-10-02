package com.anverraglobal.insurance.controller;

import com.anverraglobal.insurance.security.UserPrincipal;
import com.anverraglobal.insurance.auth.service.UserService;
import com.anverraglobal.insurance.model.dto.auth.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        UserDTO userDTO = userService.getUserProfile(userPrincipal.getId());
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", userDTO
        ));
    }

    @PutMapping("/me")
    public ResponseEntity<Map<String, Object>> updateProfile(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody UserDTO userDTO) {
        UserDTO updatedUser = userService.updateUserProfile(userPrincipal.getId(), userDTO);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", updatedUser
        ));
    }
}

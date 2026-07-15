package com.anverraglobal.insurance.auth.controller;

import com.anverraglobal.insurance.auth.dto.*;
import com.anverraglobal.insurance.model.enums.RoleName;
import com.anverraglobal.insurance.auth.service.AuthenticationService;
import com.anverraglobal.insurance.auth.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final OtpService otpService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(authenticationService.signup(request));
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authenticationService.registerUser(request));
    }

    @PostMapping("/register/subagent")
    @PreAuthorize("hasAnyRole('AGENT', 'BROKER')")
    public ResponseEntity<AuthResponse> registerSubAgent(@Valid @RequestBody RegisterRequest request) {
        request.setRole(RoleName.SUB_AGENT);
        return ResponseEntity.ok(authenticationService.registerUser(request));
    }

    @PostMapping("/otp/send")
    public ResponseEntity<OtpResponse> sendOtp(@Valid @RequestBody OtpRequest request) {
        return ResponseEntity.ok(otpService.sendOtp(request.getPhone(), request.getPurpose()));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        boolean verified = otpService.verifyOtp(request.getPhone(), request.getOtp(), request.getPurpose());
        return ResponseEntity.ok(ApiResponse.builder()
                .success(verified)
                .message("OTP verified successfully")
                .verified(verified)
                .build());
    }

    @PostMapping("/login/otp")
    public ResponseEntity<AuthResponse> loginWithOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return ResponseEntity.ok(authenticationService.loginWithOtp(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authenticationService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@RequestHeader(value = "Refresh-Token", required = false) String refreshToken) {
        authenticationService.logout(refreshToken);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Logged out successfully")
                .build());
    }
}

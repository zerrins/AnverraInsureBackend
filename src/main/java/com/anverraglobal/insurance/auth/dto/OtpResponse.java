package com.anverraglobal.insurance.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtpResponse {
    private String message;
    private String phone;
    private LocalDateTime expiresAt;
    private long expiresInSeconds;
    
    // Only populated in dev/mock mode
    private String otp;
}

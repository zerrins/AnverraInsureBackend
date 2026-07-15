package com.anverraglobal.insurance.auth.dto;

import com.anverraglobal.insurance.model.enums.RoleName;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private long expiresIn;
    private UserDto user;

    @Data
    @Builder
    public static class UserDto {
        private Long id;
        private String email;
        private String name;
        private String phone;
        private Set<RoleName> roles;
    }
}

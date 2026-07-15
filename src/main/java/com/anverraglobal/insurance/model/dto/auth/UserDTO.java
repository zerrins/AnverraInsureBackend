package com.anverraglobal.insurance.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String uuid;
    private String email;
    private String name;
    private String phone;
    private List<String> roles;
    private String status;
    private boolean emailVerified;
    private boolean phoneVerified;
    private UserProfileDTO profile;
}

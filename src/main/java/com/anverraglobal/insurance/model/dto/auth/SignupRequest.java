package com.anverraglobal.insurance.model.dto.auth;

import com.anverraglobal.insurance.model.enums.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class SignupRequest {
    private String firstName;
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    private String email;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "OTP is required")
    private String otp;

    @NotEmpty(message = "At least one role is required")
    private Set<RoleName> roles;

    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;
    private String country;
    private String agentCode;
    private String brokerCode;
}

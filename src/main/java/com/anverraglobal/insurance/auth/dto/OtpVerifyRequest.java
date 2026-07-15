package com.anverraglobal.insurance.auth.dto;

import com.anverraglobal.insurance.model.enums.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OtpVerifyRequest {
    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "OTP is required")
    private String otp;

    @NotNull(message = "Purpose is required")
    private OtpPurpose purpose;
}

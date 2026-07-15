package com.anverraglobal.insurance.model.dto.auth;

import com.anverraglobal.insurance.model.enums.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OtpRequest {
    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotNull(message = "Purpose is required")
    private OtpPurpose purpose;
}

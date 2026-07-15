package com.anverraglobal.insurance.auth.service;

import com.anverraglobal.insurance.auth.dto.OtpResponse;
import com.anverraglobal.insurance.auth.entity.MobileOtp;
import com.anverraglobal.insurance.auth.repository.MobileOtpRepository;
import com.anverraglobal.insurance.model.enums.OtpPurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OtpServiceTest {

    @Mock
    private MobileOtpRepository mobileOtpRepository;

    @InjectMocks
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(otpService, "exposeOtpInResponse", true);
    }

    @Test
    void sendOtp_TestNumber_Returns123456() {
        String phone = "+919876543211";
        
        when(mobileOtpRepository.countByPhoneNumberAndCreatedAtAfter(anyString(), any())).thenReturn(0L);

        OtpResponse response = otpService.sendOtp(phone, OtpPurpose.LOGIN);

        assertEquals("123456", response.getOtp());
        verify(mobileOtpRepository).save(any(MobileOtp.class));
    }
}

package com.anverraglobal.insurance.auth.service;

import com.anverraglobal.insurance.auth.dto.OtpResponse;
import com.anverraglobal.insurance.auth.entity.MobileOtp;
import com.anverraglobal.insurance.auth.entity.User;
import com.anverraglobal.insurance.auth.repository.MobileOtpRepository;
import com.anverraglobal.insurance.auth.repository.UserRepository;
import com.anverraglobal.insurance.exception.BadRequestException;
import com.anverraglobal.insurance.model.enums.OtpPurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OtpServiceTest {

    @Mock
    private MobileOtpRepository mobileOtpRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(otpService, "exposeOtpInResponse", true);
        ReflectionTestUtils.setField(otpService, "testModeEnabled", true);
    }

    @Test
    void sendOtp_TestNumber_Returns123456() {
        String phone = "+919876543211";
        
        when(mobileOtpRepository.countByPhoneNumberAndCreatedAtAfter(anyString(), any())).thenReturn(0L);
        when(userRepository.findByPhoneAndDeletedFalse(phone)).thenReturn(Optional.of(new User())); // For LOGIN

        OtpResponse response = otpService.sendOtp(phone, OtpPurpose.LOGIN);

        assertEquals("123456", response.getOtp());
        verify(mobileOtpRepository).acquireAdvisoryLock(phone + "_LOGIN");
        verify(mobileOtpRepository).invalidatePreviousOtps(phone, OtpPurpose.LOGIN);
        verify(mobileOtpRepository).save(any(MobileOtp.class));
    }

    @Test
    void sendOtp_RateLimitExceeded_ThrowsException() {
        String phone = "+919876543210";
        
        when(mobileOtpRepository.countByPhoneNumberAndCreatedAtAfter(anyString(), any())).thenReturn(5L);
        when(userRepository.findByPhoneAndDeletedFalse(phone)).thenReturn(Optional.of(new User())); 

        assertThrows(BadRequestException.class, () -> otpService.sendOtp(phone, OtpPurpose.LOGIN));
    }

    @Test
    void sendOtp_LoginPurposeButUserNotFound_ThrowsException() {
        String phone = "+919876543210";
        when(userRepository.findByPhoneAndDeletedFalse(phone)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> otpService.sendOtp(phone, OtpPurpose.LOGIN));
    }

    @Test
    void sendOtp_RegistrationPurposeButUserExists_ThrowsException() {
        String phone = "+919876543210";
        when(userRepository.findByPhoneAndDeletedFalse(phone)).thenReturn(Optional.of(new User()));

        assertThrows(BadRequestException.class, () -> otpService.sendOtp(phone, OtpPurpose.REGISTRATION));
    }

    @Test
    void verifyOtp_InvalidOtpExceedsLimit_ThrowsException() {
        String phone = "+919876543210";
        MobileOtp otp = MobileOtp.builder()
                .phoneNumber(phone)
                .otpCode("111111")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        otp.setId(1L);

        when(mobileOtpRepository.findLatestValidOtp(eq(phone), eq(OtpPurpose.LOGIN), any())).thenReturn(Optional.of(otp));
        when(mobileOtpRepository.incrementAttemptCountIfUnderLimit(1L, 3)).thenReturn(0);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> otpService.verifyOtp(phone, "999999", OtpPurpose.LOGIN));
        assertEquals("Maximum attempts exceeded. Please request a new one.", ex.getMessage());
    }

    @Test
    void verifyOtp_AttemptsExhausted_ThrowsException() {
        String phone = "+919876543210";
        MobileOtp otp = MobileOtp.builder()
                .phoneNumber(phone)
                .otpCode("111111")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attemptCount(3)
                .build();
        otp.setId(1L);

        when(mobileOtpRepository.findLatestValidOtp(eq(phone), eq(OtpPurpose.LOGIN), any())).thenReturn(Optional.of(otp));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> otpService.verifyOtp(phone, "111111", OtpPurpose.LOGIN));
        assertEquals("Maximum attempts exceeded. Please request a new one.", ex.getMessage());
    }

    @Test
    void normalizePhone_ValidIndianNumber_ReturnsWithPrefix() {
        assertEquals("+919876543210", otpService.normalizePhone("9876543210"));
        assertEquals("+919876543210", otpService.normalizePhone("919876543210"));
        assertEquals("+919876543210", otpService.normalizePhone("+91-9876-543210"));
    }

    @Test
    void normalizePhone_InvalidNumber_ThrowsException() {
        assertThrows(BadRequestException.class, () -> otpService.normalizePhone("987654321")); // 9 digits
        assertThrows(BadRequestException.class, () -> otpService.normalizePhone("929876543210")); // 12 digits, doesn't start with 91
    }
}

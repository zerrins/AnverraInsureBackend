package com.anverraglobal.insurance.auth.service;

import com.anverraglobal.insurance.auth.dto.OtpResponse;
import com.anverraglobal.insurance.auth.entity.MobileOtp;
import com.anverraglobal.insurance.auth.repository.MobileOtpRepository;
import com.anverraglobal.insurance.auth.repository.UserRepository;
import com.anverraglobal.insurance.exception.BadRequestException;
import com.anverraglobal.insurance.model.enums.OtpPurpose;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final MobileOtpRepository mobileOtpRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.expose-in-response:false}")
    private boolean exposeOtpInResponse;

    @Value("${app.otp.test-mode.enabled:false}")
    private boolean testModeEnabled;

    private static final List<String> TEST_NUMBERS = List.of(
            "+919876543211", "+919876543212", "+919876543213", "+918050950295"
    );

    @Transactional
    public OtpResponse sendOtp(String phone, OtpPurpose purpose) {
        String normalizedPhone = normalizePhone(phone);
        
        mobileOtpRepository.acquireAdvisoryLock(normalizedPhone + "_" + purpose.name());
        
        if (purpose == OtpPurpose.LOGIN) {
            if (userRepository.findByPhoneAndDeletedFalse(normalizedPhone).isEmpty()) {
                throw new BadRequestException("No account found with this phone number");
            }
        } else if (purpose == OtpPurpose.REGISTRATION) {
            if (userRepository.findByPhoneAndDeletedFalse(normalizedPhone).isPresent()) {
                throw new BadRequestException("An account with this phone number already exists");
            }
        }

        mobileOtpRepository.invalidatePreviousOtps(normalizedPhone, purpose);

        // Rate limiting: max 5 OTPs per hour
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentOtps = mobileOtpRepository.countByPhoneNumberAndCreatedAtAfter(normalizedPhone, oneHourAgo);
        if (recentOtps >= 5) {
            throw new BadRequestException("Too many OTP requests. Please try again after 1 hour.");
        }

        // Generate OTP
        String otpCode = generateOtp(normalizedPhone);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        MobileOtp otp = MobileOtp.builder()
                .phoneNumber(normalizedPhone)
                .otpCode(otpCode)
                .purpose(purpose)
                .expiresAt(expiresAt)
                .verified(false)
                .attemptCount(0)
                .build();

        mobileOtpRepository.save(otp);
        log.info("Saved OTP for phone: {}, purpose: {}", normalizedPhone, purpose);

        OtpResponse response = OtpResponse.builder()
                .message("OTP sent successfully")
                .phone(maskPhone(normalizedPhone))
                .expiresAt(expiresAt)
                .expiresInSeconds(300)
                .build();

        if (testModeEnabled && (exposeOtpInResponse || TEST_NUMBERS.contains(normalizedPhone))) {
            response.setOtp(otpCode);
        }

        return response;
    }

    @Transactional
    public boolean verifyOtp(String phone, String otpCode, OtpPurpose purpose) {
        String normalizedPhone = normalizePhone(phone);
        
        MobileOtp otp = mobileOtpRepository.findLatestValidOtp(normalizedPhone, purpose, LocalDateTime.now())
                .orElseThrow(() -> new BadRequestException("No valid OTP found for this phone number. Or it has expired."));

        if (!otp.isValid()) {
            throw new BadRequestException("Maximum attempts exceeded. Please request a new one.");
        }

        if (otp.getOtpCode().equals(otpCode)) {
            otp.markVerified();
            mobileOtpRepository.save(otp);
            return true;
        } else {
            int updated = mobileOtpRepository.incrementAttemptCountIfUnderLimit(otp.getId(), 3);
            if (updated == 0) {
                throw new BadRequestException("Maximum attempts exceeded. Please request a new one.");
            }
            throw new BadRequestException("Invalid OTP. Please try again.");
        }
    }
    
    public void validateVerifiedOtpExists(String phone, OtpPurpose purpose) {
        String normalizedPhone = normalizePhone(phone);
        
        MobileOtp otp = mobileOtpRepository.findFirstByPhoneNumberAndPurposeAndVerifiedOrderByCreatedAtDesc(normalizedPhone, purpose, true)
                .orElseThrow(() -> new BadRequestException("No verified OTP found. Please verify OTP first."));
                
        // ensure it was verified recently (e.g. within last 15 minutes as per business rule 1)
        if (otp.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(15))) {
            throw new BadRequestException("OTP verification session expired. Please start over.");
        }
    }

    public String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new BadRequestException("Phone number is required");
        }
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 10) {
            return "+91" + digits;
        } else if (digits.length() == 12 && digits.startsWith("91")) {
            return "+" + digits;
        }
        throw new BadRequestException("Invalid phone number format. Must be a 10-digit Indian number.");
    }

    private String generateOtp(String phone) {
        if (testModeEnabled && TEST_NUMBERS.contains(phone)) {
            return "123456";
        }
        int number = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(number);
    }
    
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 10) return phone;
        String prefix = phone.substring(0, 3); // +91
        String suffix = phone.substring(phone.length() - 2); // 10
        return prefix + "****" + suffix;
    }
}

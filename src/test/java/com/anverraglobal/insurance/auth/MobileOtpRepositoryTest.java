package com.anverraglobal.insurance.auth;

import com.anverraglobal.insurance.auth.entity.MobileOtp;
import com.anverraglobal.insurance.auth.repository.MobileOtpRepository;
import com.anverraglobal.insurance.model.enums.OtpPurpose;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Repository tests for {@link MobileOtpRepository}.
 *
 * <p>Verifies:
 * <ul>
 *   <li>OTP creation and basic persistence</li>
 *   <li>Lookup query: phone + purpose + unverified + unexpired</li>
 *   <li>Rate-limit count query (countByPhoneNumberAndCreatedAtAfter)</li>
 *   <li>Expired OTP cleanup (deleteByExpiresAtBefore)</li>
 *   <li>OTP invalidation (invalidatePreviousOtps)</li>
 *   <li>Domain helper: isValid()</li>
 * </ul>
 * </p>
 *
 * <p>Business rules tested (docs/05_business_rules.md §OTP Rules):
 * rules 10-15.</p>
 */
@DisplayName("MobileOtpRepository")
class MobileOtpRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private MobileOtpRepository mobileOtpRepository;

    private MobileOtp buildOtp(String phone, OtpPurpose purpose, boolean expired) {
        return MobileOtp.builder()
                .phoneNumber(phone)
                .otpCode("123456")
                .purpose(purpose)
                .verified(false)
                .expiresAt(expired
                        ? LocalDateTime.now().minusMinutes(1)   // already expired
                        : LocalDateTime.now().plusMinutes(5))   // 5-min window
                .build();
    }

    @Nested
    @DisplayName("persistence")
    class Persistence {

        @Test
        @DisplayName("should persist an OTP record")
        void shouldPersistOtp() {
            MobileOtp otp = buildOtp("+919876543210", OtpPurpose.REGISTRATION, false);
            MobileOtp saved = mobileOtpRepository.save(otp);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getOtpCode()).isEqualTo("123456");
            assertThat(saved.isVerified()).isFalse();
            assertThat(saved.getAttemptCount()).isZero();
        }
    }

    @Nested
    @DisplayName("findLatestValidOtp")
    class FindLatestValidOtp {

        @Test
        @DisplayName("should find valid unverified unexpired OTP")
        void shouldFindValidOtp() {
            mobileOtpRepository.save(buildOtp("+919876543001", OtpPurpose.LOGIN, false));
            mobileOtpRepository.flush();

            Optional<MobileOtp> found = mobileOtpRepository.findLatestValidOtp(
                    "+919876543001", OtpPurpose.LOGIN, LocalDateTime.now());

            assertThat(found).isPresent();
            assertThat(found.get().getPhoneNumber()).isEqualTo("+919876543001");
        }

        @Test
        @DisplayName("should NOT find expired OTP")
        void shouldNotFindExpiredOtp() {
            mobileOtpRepository.save(buildOtp("+919876543002", OtpPurpose.LOGIN, true));
            mobileOtpRepository.flush();

            Optional<MobileOtp> found = mobileOtpRepository.findLatestValidOtp(
                    "+919876543002", OtpPurpose.LOGIN, LocalDateTime.now());

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("should NOT find already verified OTP")
        void shouldNotFindVerifiedOtp() {
            MobileOtp otp = buildOtp("+919876543003", OtpPurpose.REGISTRATION, false);
            otp.setVerified(true);
            mobileOtpRepository.save(otp);
            mobileOtpRepository.flush();

            Optional<MobileOtp> found = mobileOtpRepository.findLatestValidOtp(
                    "+919876543003", OtpPurpose.REGISTRATION, LocalDateTime.now());

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("should NOT find OTP for different purpose")
        void shouldNotFindOtpForDifferentPurpose() {
            mobileOtpRepository.save(buildOtp("+919876543004", OtpPurpose.LOGIN, false));
            mobileOtpRepository.flush();

            Optional<MobileOtp> found = mobileOtpRepository.findLatestValidOtp(
                    "+919876543004", OtpPurpose.REGISTRATION, LocalDateTime.now());

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("rate limit count")
    class RateLimitCount {

        @Test
        @DisplayName("should count OTPs created in the last hour")
        void shouldCountOtpsInLastHour() {
            String phone = "+919876543100";
            mobileOtpRepository.save(buildOtp(phone, OtpPurpose.LOGIN, false));
            mobileOtpRepository.save(buildOtp(phone, OtpPurpose.LOGIN, false));
            mobileOtpRepository.save(buildOtp(phone, OtpPurpose.LOGIN, false));
            mobileOtpRepository.flush();

            long count = mobileOtpRepository.countByPhoneNumberAndCreatedAtAfter(
                    phone, LocalDateTime.now().minusHours(1));

            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("count should be 0 for phone with no OTPs")
        void shouldReturnZeroForPhoneWithNoOtps() {
            long count = mobileOtpRepository.countByPhoneNumberAndCreatedAtAfter(
                    "+919999999999", LocalDateTime.now().minusHours(1));
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("cleanup")
    class Cleanup {

        @Test
        @DisplayName("deleteByExpiresAtBefore should delete expired OTPs only")
        void shouldDeleteExpiredOnly() {
            MobileOtp active  = mobileOtpRepository.save(buildOtp("+919876543200", OtpPurpose.LOGIN, false));
            MobileOtp expired = mobileOtpRepository.save(buildOtp("+919876543201", OtpPurpose.LOGIN, true));
            mobileOtpRepository.flush();

            mobileOtpRepository.deleteByExpiresAtBefore(LocalDateTime.now());
            mobileOtpRepository.flush();

            assertThat(mobileOtpRepository.findById(active.getId())).isPresent();
            assertThat(mobileOtpRepository.findById(expired.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("invalidation")
    class Invalidation {

        @Test
        @DisplayName("invalidatePreviousOtps should mark old OTPs as verified")
        void shouldInvalidatePreviousOtps() {
            String phone = "+919876543300";
            MobileOtp old1 = mobileOtpRepository.save(buildOtp(phone, OtpPurpose.REGISTRATION, false));
            MobileOtp old2 = mobileOtpRepository.save(buildOtp(phone, OtpPurpose.REGISTRATION, false));
            mobileOtpRepository.flush();

            mobileOtpRepository.invalidatePreviousOtps(phone, OtpPurpose.REGISTRATION);
            mobileOtpRepository.flush();

            // Both should now be marked verified (invalidated)
            MobileOtp r1 = mobileOtpRepository.findById(old1.getId()).orElseThrow();
            MobileOtp r2 = mobileOtpRepository.findById(old2.getId()).orElseThrow();
            assertThat(r1.isVerified()).isTrue();
            assertThat(r2.isVerified()).isTrue();
        }

        @Test
        @DisplayName("invalidatePreviousOtps should NOT affect different purpose OTPs")
        void shouldNotAffectDifferentPurpose() {
            String phone = "+919876543301";
            MobileOtp loginOtp = mobileOtpRepository.save(buildOtp(phone, OtpPurpose.LOGIN, false));
            mobileOtpRepository.flush();

            mobileOtpRepository.invalidatePreviousOtps(phone, OtpPurpose.REGISTRATION);
            mobileOtpRepository.flush();

            MobileOtp reloaded = mobileOtpRepository.findById(loginOtp.getId()).orElseThrow();
            assertThat(reloaded.isVerified()).isFalse();
        }
    }

    @Nested
    @DisplayName("domain helper: isValid()")
    class DomainHelper {

        @Test
        @DisplayName("isValid should return true for unverified, unexpired, under attempt limit")
        void isValidShouldReturnTrue() {
            MobileOtp otp = buildOtp("+919876543400", OtpPurpose.LOGIN, false);
            assertThat(otp.isValid()).isTrue();
        }

        @Test
        @DisplayName("isValid should return false for expired OTP")
        void isValidShouldReturnFalseWhenExpired() {
            MobileOtp otp = buildOtp("+919876543401", OtpPurpose.LOGIN, true);
            assertThat(otp.isValid()).isFalse();
        }

        @Test
        @DisplayName("isValid should return false after 3 attempts")
        void isValidShouldReturnFalseAfterMaxAttempts() {
            MobileOtp otp = buildOtp("+919876543402", OtpPurpose.LOGIN, false);
            otp.incrementAttemptCount();
            otp.incrementAttemptCount();
            otp.incrementAttemptCount(); // attempt_count = 3

            assertThat(otp.isValid()).isFalse();
        }

        @Test
        @DisplayName("isValid should return false for verified OTP")
        void isValidShouldReturnFalseWhenVerified() {
            MobileOtp otp = buildOtp("+919876543403", OtpPurpose.LOGIN, false);
            otp.markVerified();
            assertThat(otp.isValid()).isFalse();
        }
    }
}

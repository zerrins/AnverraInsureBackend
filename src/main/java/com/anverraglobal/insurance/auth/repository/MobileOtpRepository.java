package com.anverraglobal.insurance.auth.repository;

import com.anverraglobal.insurance.auth.entity.MobileOtp;
import com.anverraglobal.insurance.model.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for {@link MobileOtp} persistence operations.
 *
 * <p>Business rules (docs/05_business_rules.md §OTP Rules):
 * <ul>
 *   <li>OTP expiry: 5 minutes from creation time</li>
 *   <li>Max 3 wrong attempts per OTP record</li>
 *   <li>Rate limit: max 5 OTPs per phone per hour (use {@link #countByPhoneNumberAndCreatedAtAfter})</li>
 *   <li>Cleanup: scheduled hourly job deletes expired records via {@link #deleteByExpiresAtBefore}</li>
 * </ul>
 * </p>
 */
@Repository
public interface MobileOtpRepository extends JpaRepository<MobileOtp, Long> {

    /**
     * Finds the most recent active (unverified, unexpired, under attempt limit) OTP
     * for a given phone and purpose.
     *
     * <p>This is the primary OTP verification query. The {@code attemptCount < 3}
     * guard is enforced at the entity level via {@link MobileOtp#isValid()};
     * the query fetches the candidate record and the service validates it.</p>
     *
     * @param phoneNumber the normalised phone number
     * @param purpose     the OTP purpose
     * @param now         current timestamp (must be before expiresAt)
     * @return the active OTP record, or empty if none found
     */
    @Query("""
            SELECT o FROM MobileOtp o
            WHERE o.phoneNumber = :phone
              AND o.purpose = :purpose
              AND o.verified = false
              AND o.expiresAt > :now
            ORDER BY o.createdAt DESC
            LIMIT 1
            """)
    Optional<MobileOtp> findLatestValidOtp(
            @Param("phone") String phoneNumber,
            @Param("purpose") OtpPurpose purpose,
            @Param("now") LocalDateTime now
    );

    /**
     * Counts OTPs generated for a phone number after a given timestamp.
     * Used to enforce the rate limit of max 5 OTPs per phone per hour.
     *
     * <pre>
     * Usage:
     *   long count = repo.countByPhoneNumberAndCreatedAtAfter(phone, LocalDateTime.now().minusHours(1));
     *   if (count >= 5) throw new RateLimitException(...);
     * </pre>
     *
     * @param phoneNumber phone number to check
     * @param since       timestamp to count OTPs from (now minus 1 hour)
     * @return number of OTPs generated since that timestamp
     */
    long countByPhoneNumberAndCreatedAtAfter(String phoneNumber, LocalDateTime since);

    /**
     * Deletes all OTP records that have expired.
     * Called by the scheduled cleanup job that runs every hour.
     *
     * @param now current timestamp — records with expiresAt before this are deleted
     */
    @Modifying
    @Query("DELETE FROM MobileOtp o WHERE o.expiresAt < :now")
    void deleteByExpiresAtBefore(@Param("now") LocalDateTime now);

    /**
     * Invalidates all unverified OTPs for a phone + purpose combination.
     * Called before issuing a new OTP to prevent stale records from being used.
     *
     * @param phoneNumber the phone number
     * @param purpose     the OTP purpose
     */
    @Modifying
    @Query("""
            UPDATE MobileOtp o
            SET o.verified = true
            WHERE o.phoneNumber = :phone
              AND o.purpose = :purpose
              AND o.verified = false
            """)
    void invalidatePreviousOtps(
            @Param("phone") String phoneNumber,
            @Param("purpose") OtpPurpose purpose
    );
            
    Optional<MobileOtp> findFirstByPhoneNumberAndPurposeAndVerifiedOrderByCreatedAtDesc(String phoneNumber, OtpPurpose purpose, boolean verified);
}

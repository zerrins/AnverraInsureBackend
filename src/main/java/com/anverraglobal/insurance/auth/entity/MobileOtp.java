package com.anverraglobal.insurance.auth.entity;

import com.anverraglobal.insurance.model.entity.BaseEntity;
import com.anverraglobal.insurance.model.enums.OtpPurpose;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Stores a generated OTP (One-Time Password) for phone-based authentication flows.
 *
 * <p>Source of truth: docs/04_data_model.md — Table: mobile_otps</p>
 *
 * <p>Business rules (docs/05_business_rules.md §OTP Rules):
 * <ul>
 *   <li>OTP is 6 digits, generated using {@code SecureRandom}</li>
 *   <li>Stored in <strong>plain text</strong> (acceptable given 5-minute TTL)</li>
 *   <li>Expires 5 minutes from creation ({@code expiresAt})</li>
 *   <li>Max 3 wrong attempts before OTP is considered invalidated</li>
 *   <li>Rate limit: max 5 OTPs per phone per hour (enforced at service layer
 *       using {@code countByPhoneNumberAndCreatedAtAfter})</li>
 *   <li>Scheduled cleanup job deletes expired records hourly</li>
 *   <li>Test numbers (+919876543211, +919876543212, +919876543213, +918050950295)
 *       always use OTP "123456" (dev/test mode only)</li>
 * </ul>
 * </p>
 *
 * <p>Phone number is stored in +91XXXXXXXXXX normalised format.
 * Normalisation is enforced at service layer before persistence.</p>
 */
@Entity
@Table(
        name = "mobile_otps",
        indexes = {
                /*
                 * Composite index for the primary OTP lookup query:
                 * WHERE phone_number = ? AND purpose = ? AND verified = false AND expires_at > now()
                 * (docs/11_target_architecture.md §Indexing Strategy)
                 */
                @Index(
                        name = "idx_mobile_otps_lookup",
                        columnList = "phone_number, purpose, verified, created_at"
                ),
                @Index(name = "idx_mobile_otps_phone",      columnList = "phone_number"),
                @Index(name = "idx_mobile_otps_expires_at", columnList = "expires_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MobileOtp extends BaseEntity {

    /**
     * Normalised phone number in +91XXXXXXXXXX format.
     */
    @NotBlank(message = "Phone number is required")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    /**
     * 6-digit OTP code. Stored in plain text (acceptable for 5-minute TTL).
     * Always generated via {@code SecureRandom} in the service layer.
     */
    @NotBlank(message = "OTP code is required")
    @Size(min = 6, max = 10, message = "OTP code must be 6-10 characters")
    @Column(name = "otp_code", nullable = false, length = 10)
    private String otpCode;

    /**
     * Absolute timestamp when this OTP expires (createdAt + 5 minutes).
     * After this time the OTP must be rejected.
     */
    @NotNull(message = "Expiry time is required")
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * True once the user has successfully submitted this OTP code.
     * Verified OTPs cannot be reused for re-verification.
     */
    @Column(name = "verified", nullable = false)
    @Builder.Default
    private boolean verified = false;

    /**
     * The purpose for which this OTP was generated.
     * Determines which user-existence checks are applied at the service layer.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", length = 30)
    private OtpPurpose purpose;

    /**
     * Number of incorrect verification attempts made against this OTP.
     * When this reaches 3, the OTP must be treated as invalidated.
     * (docs/05_business_rules.md rule 12)
     */
    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    /**
     * Timestamp when this OTP record was created.
     * Used for rate-limit checks (max 5 OTPs per phone per hour).
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // -----------------------------------------------------------------------
    // Domain helpers
    // -----------------------------------------------------------------------

    /**
     * Returns true if this OTP is still valid (not verified, not expired,
     * and under the max attempt limit).
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isValid() {
        return !verified
                && LocalDateTime.now().isBefore(expiresAt)
                && attemptCount < 3;
    }

    /**
     * Increments the incorrect attempt counter.
     */
    public void incrementAttemptCount() {
        this.attemptCount++;
    }

    /**
     * Marks this OTP as verified. Call only after code is confirmed correct.
     */
    public void markVerified() {
        this.verified = true;
    }
}

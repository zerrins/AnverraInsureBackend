package com.anverraglobal.insurance.model.enums;

/**
 * Lifecycle status of an OTP record.
 *
 * <p>Not directly stored as a column in the {@code mobile_otps} table
 * (the table uses a boolean {@code verified} flag plus the {@code expires_at}
 * timestamp). This enum is used for in-memory state representation and
 * service-layer logic.</p>
 *
 * <p>Business rules (docs/05_business_rules.md §OTP Rules):
 * <ul>
 *   <li>OTP expires after 5 minutes (expires_at &lt; now)</li>
 *   <li>Max 3 wrong attempts before OTP is INVALIDATED</li>
 *   <li>Cleanup job runs hourly to delete expired OTPs</li>
 * </ul>
 * </p>
 */
public enum OtpStatus {

    /**
     * OTP has been generated and sent. Not yet verified, not yet expired.
     */
    PENDING,

    /**
     * OTP has been successfully verified by the user.
     * The {@code verified} flag on the DB record is {@code true}.
     */
    VERIFIED,

    /**
     * OTP has passed its {@code expires_at} timestamp without being verified.
     */
    EXPIRED,

    /**
     * OTP has been invalidated due to too many incorrect attempts (max 3).
     */
    INVALIDATED
}

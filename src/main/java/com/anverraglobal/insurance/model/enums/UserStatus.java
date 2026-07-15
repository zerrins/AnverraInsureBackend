package com.anverraglobal.insurance.model.enums;

/**
 * Represents the lifecycle state of a user account.
 *
 * <p>Source of truth: docs/03_roles_and_permissions.md — User Status Values</p>
 *
 * <ul>
 *   <li>ACTIVE            — User can log in and use the system</li>
 *   <li>INACTIVE          — User has been deactivated by admin</li>
 *   <li>SUSPENDED         — User has been suspended (admin action)</li>
 *   <li>PENDING_VERIFICATION — User registered but KYC/email not yet verified</li>
 * </ul>
 *
 * <p>Business rule (docs/05_business_rules.md §5):
 * New user via signup starts as PENDING_VERIFICATION.
 * New user created by admin starts as ACTIVE.</p>
 */
public enum UserStatus {

    /**
     * User can log in and use the system normally.
     */
    ACTIVE,

    /**
     * User has been deactivated by an admin. Login is blocked.
     */
    INACTIVE,

    /**
     * User has been suspended by an admin (temporary block). Login is blocked.
     */
    SUSPENDED,

    /**
     * User registered via OTP signup but phone/KYC verification is still
     * pending. phoneVerified=true but emailVerified=false at this stage.
     */
    PENDING_VERIFICATION
}

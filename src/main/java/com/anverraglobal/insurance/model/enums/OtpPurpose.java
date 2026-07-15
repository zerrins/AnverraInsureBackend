package com.anverraglobal.insurance.model.enums;

/**
 * The purpose for which an OTP was requested.
 *
 * <p>Source of truth: docs/04_data_model.md — Table: mobile_otps (purpose column)
 * and docs/05_business_rules.md §OTP Rules (rule 14).</p>
 *
 * <p>Business rules per purpose:
 * <ul>
 *   <li>LOGIN              — User must EXIST with that phone number</li>
 *   <li>REGISTRATION       — User must NOT exist with that phone number</li>
 *   <li>PHONE_VERIFICATION — No existence check</li>
 *   <li>PASSWORD_RESET     — User must EXIST with that phone number</li>
 * </ul>
 * </p>
 */
public enum OtpPurpose {

    /**
     * OTP sent for OTP-based login flow.
     * Prerequisite: a registered user must exist for the given phone.
     */
    LOGIN,

    /**
     * OTP sent during the signup/registration flow (Step 1 of OTP-based signup).
     * Prerequisite: no user must exist for the given phone.
     */
    REGISTRATION,

    /**
     * OTP sent to verify/change a user's phone number on their profile.
     * No user-existence check is required.
     */
    PHONE_VERIFICATION,

    /**
     * OTP sent to initiate a password reset flow.
     * Prerequisite: a registered user must exist for the given phone.
     */
    PASSWORD_RESET
}

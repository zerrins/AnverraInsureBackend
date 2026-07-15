package com.anverraglobal.insurance.auth.entity;

import com.anverraglobal.insurance.model.entity.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * Extended profile information for a user. One-to-one with {@link User}.
 *
 * <p>Source of truth: docs/04_data_model.md — Table: user_profiles</p>
 *
 * <p>Design notes:
 * <ul>
 *   <li>Profile is an optional extension of the User aggregate.
 *       It is created separately (e.g., when user completes their profile).
 *       It is NOT created automatically on user registration.</li>
 *   <li>All fields are optional (nullable) unless noted.</li>
 *   <li>Indian-specific fields: PAN (10 chars), Aadhar (12 digits), GSTIN (15 chars).</li>
 *   <li>Validation on length constraints; no business-level format validation
 *       here — that belongs in the service layer.</li>
 * </ul>
 * </p>
 */
@Entity
@Table(
        name = "user_profiles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_profiles_user", columnNames = "user_id")
        },
        indexes = {
                @Index(name = "idx_user_profiles_user", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile extends AuditableEntity {

    // -----------------------------------------------------------------------
    // Owning relationship
    // -----------------------------------------------------------------------

    /**
     * The user this profile belongs to.
     * The unique constraint on user_id enforces the one-to-one cardinality at DB level.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_user_profiles_user")
    )
    private User user;

    // -----------------------------------------------------------------------
    // Personal details
    // -----------------------------------------------------------------------

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /**
     * MALE, FEMALE, or OTHER — stored as free text per spec.
     */
    @Size(max = 10)
    @Column(name = "gender", length = 10)
    private String gender;

    // -----------------------------------------------------------------------
    // Address
    // -----------------------------------------------------------------------

    @Size(max = 255)
    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Size(max = 255)
    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Size(max = 100)
    @Column(name = "city", length = 100)
    private String city;

    @Size(max = 100)
    @Column(name = "state", length = 100)
    private String state;

    /**
     * Indian postal pin code.
     */
    @Size(max = 20)
    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Size(max = 100)
    @Column(name = "country", length = 100)
    @Builder.Default
    private String country = "India";

    // -----------------------------------------------------------------------
    // Regulatory / KYC identifiers (Indian market)
    // -----------------------------------------------------------------------

    /**
     * PAN number — 10-character alphanumeric (e.g., ABCDE1234F).
     * Format validation enforced at service layer.
     */
    @Size(max = 10, message = "PAN number must not exceed 10 characters")
    @Column(name = "pan_number", length = 10)
    private String panNumber;

    /**
     * Aadhaar number — 12-digit numeric identifier.
     * Format validation enforced at service layer.
     */
    @Size(max = 12, message = "Aadhar number must not exceed 12 characters")
    @Column(name = "aadhar_number", length = 12)
    private String aadharNumber;

    /**
     * GSTIN — 15-character alphanumeric GST identification number.
     * Format validation enforced at service layer.
     */
    @Size(max = 15, message = "GSTIN must not exceed 15 characters")
    @Column(name = "gstin", length = 15)
    private String gstin;

    // -----------------------------------------------------------------------
    // Banking details
    // -----------------------------------------------------------------------

    @Size(max = 100)
    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Size(max = 30)
    @Column(name = "bank_account_number", length = 30)
    private String bankAccountNumber;

    /**
     * IFSC code — 11 characters standard.
     */
    @Size(max = 11)
    @Column(name = "bank_ifsc_code", length = 11)
    private String bankIfscCode;

    // -----------------------------------------------------------------------
    // Emergency contact
    // -----------------------------------------------------------------------

    @Size(max = 255)
    @Column(name = "emergency_contact_name", length = 255)
    private String emergencyContactName;

    @Size(max = 20)
    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    // -----------------------------------------------------------------------
    // Professional identifiers
    // -----------------------------------------------------------------------

    /**
     * Agent licence/registration code assigned by the insurance regulator (IRDAI).
     */
    @Size(max = 30)
    @Column(name = "agent_code", length = 30)
    private String agentCode;

    /**
     * Broker licence/registration code.
     */
    @Size(max = 30)
    @Column(name = "broker_code", length = 30)
    private String brokerCode;

    /**
     * Name of the agent's or broker's company/firm.
     */
    @Size(max = 255)
    @Column(name = "company", length = 255)
    private String company;
}

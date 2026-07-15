package com.anverraglobal.insurance.auth.entity;

import com.anverraglobal.insurance.model.entity.AuditableEntity;
import com.anverraglobal.insurance.model.enums.RoleName;
import com.anverraglobal.insurance.model.enums.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Primary identity entity for all system users.
 *
 * <p>Source of truth: docs/04_data_model.md — Table: users</p>
 *
 * <p>Key design decisions:
 * <ul>
 *   <li>Extends {@link AuditableEntity} for id, createdAt, updatedAt,
 *       createdBy (principal name), updatedBy (principal name).</li>
 *   <li>{@code createdByUserId} is a separate nullable Long storing the ID of
 *       the admin user who created this user (null = self-registered via OTP).</li>
 *   <li>Soft-delete is supported via {@code deleted} + {@code deletedAt}.
 *       All queries should filter {@code deleted = false}.</li>
 *   <li>{@code roles} is the owning side of the User–UserRole relationship.
 *       CascadeType.ALL with orphanRemoval ensures roles are managed
 *       through the User aggregate root.</li>
 *   <li>{@code profile} is lazily loaded. Use fetch join when profile is needed.</li>
 *   <li>UUID is auto-generated on pre-persist if not supplied.</li>
 * </ul>
 * </p>
 *
 * <p>Business rules:
 * <ul>
 *   <li>Email must be unique (DB constraint + unique index)</li>
 *   <li>Phone must be unique when not null (DB constraint)</li>
 *   <li>Phone stored as +91XXXXXXXXXX (normalised — enforced at service layer)</li>
 *   <li>rewardPoints never goes below zero (enforced at service layer)</li>
 *   <li>lockedUntil: if set and in the future, login is blocked</li>
 * </ul>
 * (docs/05_business_rules.md §Authentication Rules, docs/03_roles_and_permissions.md §Account Security)
 * </p>
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uq_users_uuid",  columnNames = "uuid"),
                @UniqueConstraint(name = "uq_users_phone", columnNames = "phone")
        },
        indexes = {
                @Index(name = "idx_users_email",  columnList = "email"),
                @Index(name = "idx_users_uuid",   columnList = "uuid"),
                @Index(name = "idx_users_phone",  columnList = "phone"),
                @Index(name = "idx_users_status", columnList = "status"),
                @Index(name = "idx_users_deleted", columnList = "deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends AuditableEntity {

    // -----------------------------------------------------------------------
    // External / business identity
    // -----------------------------------------------------------------------

    /**
     * Universally unique identifier used as an external reference.
     * Generated automatically on first persist; never changes after creation.
     */
    @Column(name = "uuid", nullable = false, updatable = false, length = 36)
    private String uuid;

    // -----------------------------------------------------------------------
    // Credentials
    // -----------------------------------------------------------------------

    /**
     * Login email address. Must be globally unique.
     * Validated as a proper email format.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /**
     * BCrypt-encoded password hash.
     * Never stored in plain text; never returned in API responses.
     */
    @NotBlank(message = "Password hash is required")
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    // -----------------------------------------------------------------------
    // Profile basics
    // -----------------------------------------------------------------------

    /**
     * Full display name of the user.
     */
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Mobile phone number. Stored in +91XXXXXXXXXX format (Indian numbers).
     * Normalisation is enforced at the service layer before persistence.
     */
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * URL pointing to the user's profile image in blob storage.
     */
    @Size(max = 500, message = "Profile image URL must not exceed 500 characters")
    @Column(name = "profile_image", length = 500)
    private String profileImage;

    // -----------------------------------------------------------------------
    // Account status & security
    // -----------------------------------------------------------------------

    /**
     * Current lifecycle status of the user account.
     * Defaults to PENDING_VERIFICATION for OTP-signup; ACTIVE for admin-created users.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    /**
     * Whether the user's email address has been verified.
     * Not currently a blocking condition but stored for future use.
     */
    @Column(name = "email_verified")
    @Builder.Default
    private boolean emailVerified = false;

    /**
     * Whether the user's phone number has been verified via OTP.
     * Set to true after a successful OTP verification.
     */
    @Column(name = "phone_verified")
    @Builder.Default
    private boolean phoneVerified = false;

    /**
     * Number of consecutive failed login attempts.
     * Reset to 0 on successful login.
     */
    @Min(value = 0, message = "Login attempts cannot be negative")
    @Column(name = "login_attempts")
    @Builder.Default
    private int loginAttempts = 0;

    /**
     * Timestamp until which the account is locked due to too many failed attempts.
     * NULL means the account is not locked.
     */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    /**
     * Timestamp of the last successful login.
     */
    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    // -----------------------------------------------------------------------
    // Reward points
    // -----------------------------------------------------------------------

    /**
     * Current reward point balance. Never goes below zero.
     *
     * <p>Business rules (docs/05_business_rules.md §Reward Points):
     * <ul>
     *   <li>Initial value: 100 points</li>
     *   <li>+100 on payment confirmation</li>
     *   <li>-5 on policy creation</li>
     *   <li>Floor: 0 (never negative)</li>
     * </ul>
     * </p>
     */
    @Min(value = 0, message = "Reward points cannot be negative")
    @Column(name = "reward_points")
    @Builder.Default
    private int rewardPoints = 100;

    // -----------------------------------------------------------------------
    // Provenance
    // -----------------------------------------------------------------------

    /**
     * ID of the user who created this user account.
     * NULL for self-registered users (OTP signup flow).
     * Set by admin when creating users programmatically.
     */
    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    // -----------------------------------------------------------------------
    // Soft delete
    // -----------------------------------------------------------------------

    /**
     * Soft-delete flag. True means this record is logically deleted.
     * All production queries must filter {@code deleted = false}.
     */
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    /**
     * Timestamp when the record was soft-deleted.
     * NULL for active records.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // -----------------------------------------------------------------------
    // Relationships
    // -----------------------------------------------------------------------

    /**
     * The set of roles assigned to this user.
     * A user may hold multiple roles simultaneously (e.g., AGENT + BROKER).
     * Managed through this collection — do NOT interact with UserRole directly.
     */
    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private Set<UserRole> roles = new HashSet<>();

    /**
     * Extended profile information for the user.
     * Lazily loaded; use a fetch join in queries that require profile data.
     */
    @OneToOne(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private UserProfile profile;

    // -----------------------------------------------------------------------
    // Lifecycle hooks
    // -----------------------------------------------------------------------

    /**
     * Generates the UUID if not already set before the entity is first persisted.
     */
    @PrePersist
    protected void onPrePersist() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
    }

    // -----------------------------------------------------------------------
    // Domain helpers
    // -----------------------------------------------------------------------

    /**
     * Adds a role to this user. Manages the bidirectional relationship.
     *
     * @param roleName the role to add
     */
    public void addRole(RoleName roleName) {
        UserRole userRole = new UserRole(this, roleName);
        this.roles.add(userRole);
    }

    /**
     * Removes a role from this user.
     *
     * @param roleName the role to remove
     */
    public void removeRole(RoleName roleName) {
        this.roles.removeIf(r -> r.getRole() == roleName);
    }

    /**
     * Returns true if this user has the given role.
     *
     * @param roleName the role to check
     * @return true if the user holds that role
     */
    public boolean hasRole(RoleName roleName) {
        return this.roles.stream().anyMatch(r -> r.getRole() == roleName);
    }

    /**
     * Marks the user as soft-deleted.
     */
    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.status = UserStatus.INACTIVE;
    }

    /**
     * Returns true if the account is currently locked.
     */
    public boolean isAccountLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }
}

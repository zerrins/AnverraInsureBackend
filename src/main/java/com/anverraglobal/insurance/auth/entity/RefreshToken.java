package com.anverraglobal.insurance.auth.entity;

import com.anverraglobal.insurance.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Persisted JWT refresh token for stateless token revocation.
 *
 * <p>Not defined as a table in the original data model spec (docs/04_data_model.md),
 * but is required by the build plan (docs/12_build_execution_plan.md Phase 1)
 * and included here as part of the auth domain persistence layer.</p>
 *
 * <p>Design notes:
 * <ul>
 *   <li>The {@code token} column holds the raw refresh token string (opaque token
 *       or JWT) and is indexed for fast lookup on every API call.</li>
 *   <li>{@code revoked} supports immediate invalidation (e.g., on logout or
 *       password change) without waiting for natural expiry.</li>
 *   <li>A scheduled cleanup job (Phase 1) deletes expired/revoked tokens.
 *       ({@link #expiresAt} + {@link #revoked} combination filters in repository)</li>
 *   <li>One user may have multiple active refresh tokens (multi-device support).
 *       If single-session is required, delete old tokens on new login in the service.</li>
 * </ul>
 * </p>
 */
@Entity
@Table(
        name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_refresh_tokens_token",   columnList = "token"),
                @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
                @Index(name = "idx_refresh_tokens_expires", columnList = "expires_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken extends BaseEntity {

    /**
     * The opaque refresh token string. Unique across the table.
     * Indexed for fast lookup on token validation calls.
     */
    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    /**
     * The user this refresh token was issued for.
     * Cascade is intentionally NONE — tokens are managed independently.
     * Use {@link com.anverraglobal.insurance.auth.repository.RefreshTokenRepository#deleteByUser}
     * to revoke all tokens for a user on logout or password change.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_refresh_tokens_user")
    )
    private User user;

    /**
     * The absolute expiry timestamp of this token.
     * After this timestamp the token must be rejected even if not explicitly revoked.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Whether this token has been explicitly revoked.
     * Revoked tokens must be rejected immediately regardless of expiry.
     * Set to true on: logout, password change, admin revoke action.
     */
    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private boolean revoked = false;

    /**
     * Timestamp when this token was issued.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // -----------------------------------------------------------------------
    // Domain helpers
    // -----------------------------------------------------------------------

    /**
     * Returns true if this token is currently valid (not revoked and not expired).
     */
    public boolean isValid() {
        return !revoked && LocalDateTime.now().isBefore(expiresAt);
    }

    /**
     * Revokes this token immediately.
     */
    public void revoke() {
        this.revoked = true;
    }
}

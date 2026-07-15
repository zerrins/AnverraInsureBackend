package com.anverraglobal.insurance.auth.repository;

import com.anverraglobal.insurance.auth.entity.RefreshToken;
import com.anverraglobal.insurance.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for {@link RefreshToken} persistence operations.
 *
 * <p>Refresh token lifecycle:
 * <ol>
 *   <li>Created on successful login (service layer)</li>
 *   <li>Looked up on token-refresh API call via {@link #findByToken(String)}</li>
 *   <li>Revoked on logout or password change via {@link #revokeAllByUser(User)}</li>
 *   <li>Cleaned up by scheduled job via {@link #deleteExpiredAndRevoked(LocalDateTime)}</li>
 * </ol>
 * </p>
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Finds a refresh token by its raw token string.
     * Used on every token-refresh API request.
     *
     * @param token the raw token string
     * @return the token record if it exists
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Marks all refresh tokens for a user as revoked.
     * Called on logout or password change to invalidate all sessions.
     *
     * @param user the user whose tokens to revoke
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user")
    void revokeAllByUser(@Param("user") User user);

    /**
     * Marks all refresh tokens for a user ID as revoked.
     * Use when the User entity is not loaded.
     *
     * @param userId the user's ID
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :userId")
    void revokeAllByUserId(@Param("userId") Long userId);

    /**
     * Physically deletes all expired or revoked tokens.
     * Called by the scheduled cleanup job (hourly).
     *
     * @param now current timestamp — tokens with expiresAt before this are deleted
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now OR rt.revoked = true")
    void deleteExpiredAndRevoked(@Param("now") LocalDateTime now);

    /**
     * Physically deletes all tokens for a user.
     * Used when a user is hard-deleted.
     *
     * @param user the user whose tokens to delete
     */
    @Modifying
    void deleteByUser(User user);

    /**
     * Counts active (valid) tokens for a user.
     * Useful for multi-device session count enforcement.
     *
     * @param user the user
     * @param now  current timestamp
     * @return count of non-revoked, non-expired tokens
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user = :user AND rt.revoked = false AND rt.expiresAt > :now")
    long countActiveTokensByUser(@Param("user") User user, @Param("now") LocalDateTime now);
}

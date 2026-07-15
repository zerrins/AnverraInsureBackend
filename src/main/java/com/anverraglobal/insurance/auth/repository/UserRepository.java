package com.anverraglobal.insurance.auth.repository;

import com.anverraglobal.insurance.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link User} persistence operations.
 *
 * <p>All query methods that return active users must filter {@code deleted = false}.
 * Soft-deleted users are retained in the database for audit/compliance purposes.</p>
 *
 * <p>Security note: this repository returns raw entities.
 * Callers must never expose the {@code password} field in API responses —
 * use DTOs and MapStruct mappers for all outbound data.</p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // -----------------------------------------------------------------------
    // Lookup by unique identifiers
    // -----------------------------------------------------------------------

    /**
     * Finds an active (non-deleted) user by their email address.
     * Used in email+password login and registration uniqueness checks.
     *
     * @param email the normalised email address
     * @return the user if found and not soft-deleted
     */
    Optional<User> findByEmailAndDeletedFalse(String email);

    /**
     * Finds an active user by their normalised phone number (+91XXXXXXXXXX).
     * Used in OTP login and registration uniqueness checks.
     *
     * @param phone the normalised phone number
     * @return the user if found and not soft-deleted
     */
    Optional<User> findByPhoneAndDeletedFalse(String phone);

    /**
     * Finds an active user by their internal ID.
     */
    Optional<User> findByIdAndDeletedFalse(Long id);

    /**
     * Finds an active user by their UUID (external reference ID).
     * Used in cross-service lookups where internal Long ID is not exposed.
     *
     * @param uuid the UUID string (36-char)
     * @return the user if found and not soft-deleted
     */
    Optional<User> findByUuidAndDeletedFalse(String uuid);

    // -----------------------------------------------------------------------
    // Existence checks (for registration validation)
    // -----------------------------------------------------------------------

    /**
     * Checks whether an email is already registered (including soft-deleted users).
     * Soft-deleted emails are NOT freed for re-registration (data integrity).
     *
     * @param email the email to check
     * @return true if any user record (active or deleted) exists with that email
     */
    boolean existsByEmail(String email);

    /**
     * Checks whether a phone number is already registered (including soft-deleted users).
     *
     * @param phone the normalised phone number
     * @return true if any user record (active or deleted) exists with that phone
     */
    boolean existsByPhone(String phone);

    // -----------------------------------------------------------------------
    // Soft-delete aware list queries
    // -----------------------------------------------------------------------

    /**
     * Returns all active (non-deleted) users.
     * Use this instead of {@code findAll()} to respect soft-delete.
     */
    List<User> findAllByDeletedFalse();

    // -----------------------------------------------------------------------
    // Soft-delete operation
    // -----------------------------------------------------------------------

    /**
     * Performs a soft-delete on a user by setting deleted=true and deletedAt.
     * This is the preferred delete mechanism — never call {@code delete(user)}.
     *
     * @param userId    the ID of the user to soft-delete
     * @param deletedAt the deletion timestamp
     */
    @Modifying
    @Query("UPDATE User u SET u.deleted = true, u.deletedAt = :deletedAt WHERE u.id = :userId")
    void softDeleteById(@Param("userId") Long userId, @Param("deletedAt") LocalDateTime deletedAt);

    // -----------------------------------------------------------------------
    // Login tracking
    // -----------------------------------------------------------------------

    /**
     * Updates lastLogin timestamp and resets loginAttempts to 0 after successful login.
     *
     * @param userId    the authenticated user's ID
     * @param lastLogin the login timestamp
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :lastLogin, u.loginAttempts = 0 WHERE u.id = :userId")
    void recordSuccessfulLogin(@Param("userId") Long userId, @Param("lastLogin") LocalDateTime lastLogin);

    /**
     * Increments the loginAttempts counter for a user after a failed login attempt.
     *
     * @param userId the user's ID
     */
    @Modifying
    @Query("UPDATE User u SET u.loginAttempts = u.loginAttempts + 1 WHERE u.id = :userId")
    void incrementLoginAttempts(@Param("userId") Long userId);

    /**
     * Locks a user's account until the given timestamp.
     *
     * @param userId       the user's ID
     * @param lockedUntil  timestamp until which login is blocked
     */
    @Modifying
    @Query("UPDATE User u SET u.lockedUntil = :lockedUntil WHERE u.id = :userId")
    void lockAccount(@Param("userId") Long userId, @Param("lockedUntil") LocalDateTime lockedUntil);
}

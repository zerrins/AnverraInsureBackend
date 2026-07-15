package com.anverraglobal.insurance.auth.repository;

import com.anverraglobal.insurance.auth.entity.User;
import com.anverraglobal.insurance.auth.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link UserProfile} persistence operations.
 *
 * <p>UserProfile is a one-to-one extension of {@link User}.
 * It is NOT created automatically on user registration — it is created
 * only when the user completes their extended profile.</p>
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    /**
     * Finds the profile for a given user entity.
     *
     * @param user the user whose profile to retrieve
     * @return the profile, or empty if not yet created
     */
    Optional<UserProfile> findByUser(User user);

    /**
     * Finds the profile for a given user ID.
     * Preferred when only the user ID is available (avoids loading the User entity).
     *
     * @param userId the user's internal ID
     * @return the profile, or empty if not yet created
     */
    Optional<UserProfile> findByUserId(Long userId);

    /**
     * Checks whether a profile exists for the given user.
     *
     * @param user the user to check
     * @return true if a profile record exists
     */
    boolean existsByUser(User user);

    /**
     * Deletes the profile for a given user.
     * Called when a user is hard-deleted (should not be needed with soft-delete).
     *
     * @param user the user whose profile to delete
     */
    void deleteByUser(User user);
}

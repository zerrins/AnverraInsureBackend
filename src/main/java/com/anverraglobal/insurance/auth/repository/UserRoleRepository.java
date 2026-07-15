package com.anverraglobal.insurance.auth.repository;

import com.anverraglobal.insurance.auth.entity.User;
import com.anverraglobal.insurance.auth.entity.UserRole;
import com.anverraglobal.insurance.model.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link UserRole} join-table operations.
 *
 * <p>In most cases, role management should be done via {@link User#addRole(RoleName)}
 * and {@link User#removeRole(RoleName)}, which manage the collection directly.
 * Use this repository for direct bulk queries or deletions.</p>
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    /**
     * Returns all role assignments for a given user.
     *
     * @param user the user entity
     * @return list of UserRole records
     */
    List<UserRole> findByUser(User user);

    /**
     * Returns all role assignments for a given user ID.
     *
     * @param userId the user's internal ID
     * @return list of UserRole records
     */
    List<UserRole> findByUserId(Long userId);

    /**
     * Checks if a user has a specific role assigned.
     *
     * @param user the user entity
     * @param role the role to check
     * @return true if the assignment exists
     */
    boolean existsByUserAndRole(User user, RoleName role);

    /**
     * Deletes a specific role assignment for a user.
     * Useful when revoking a single role without touching others.
     *
     * @param user the user entity
     * @param role the role to remove
     */
    @Modifying
    void deleteByUserAndRole(User user, RoleName role);

    /**
     * Deletes all role assignments for a user.
     * Used when hard-resetting a user's roles.
     *
     * @param user the user entity
     */
    @Modifying
    void deleteByUser(User user);

    /**
     * Returns all users that have a specific role.
     * Used for admin queries (e.g., "list all agents").
     *
     * @param role the role to filter by
     * @return list of UserRole records for that role
     */
    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.user u WHERE ur.role = :role AND u.deleted = false")
    List<UserRole> findActiveUsersByRole(@Param("role") RoleName role);
}

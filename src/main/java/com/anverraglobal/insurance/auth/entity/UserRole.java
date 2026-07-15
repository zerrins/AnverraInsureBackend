package com.anverraglobal.insurance.auth.entity;

import com.anverraglobal.insurance.model.entity.BaseEntity;
import com.anverraglobal.insurance.model.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;

/**
 * Join entity representing a role assigned to a user.
 *
 * <p>Source of truth: docs/04_data_model.md — Table: user_roles</p>
 *
 * <p>Design notes:
 * <ul>
 *   <li>The role is stored as a VARCHAR {@link RoleName} enum string rather than
 *       a FK into the {@code roles} reference table. This matches the spec
 *       column definition: {@code role VARCHAR(30)}.</li>
 *   <li>This entity is always accessed through the {@link User#getRoles()} collection.
 *       Direct repository interaction should be limited to deletion queries.</li>
 *   <li>Unique constraint on (user_id, role) prevents duplicate role assignments.</li>
 * </ul>
 * </p>
 *
 * <p>A user may hold multiple roles simultaneously — there will be one
 * {@code user_roles} row per role per user.</p>
 */
@Entity
@Table(
        name = "user_roles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_roles_user_role", columnNames = {"user_id", "role"})
        },
        indexes = {
                @Index(name = "idx_user_roles_user_id", columnList = "user_id"),
                @Index(name = "idx_user_roles_role",    columnList = "role")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class UserRole extends BaseEntity {

    /**
     * The user this role assignment belongs to.
     * Always non-null; set on construction.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_roles_user")
    )
    private User user;

    /**
     * The assigned role, stored as its enum string name.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private RoleName role;

    /**
     * Convenience constructor used by {@link User#addRole(RoleName)}.
     *
     * @param user     the owning user
     * @param roleName the role to assign
     */
    public UserRole(User user, RoleName roleName) {
        this.user = user;
        this.role = roleName;
    }
}

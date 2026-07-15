package com.anverraglobal.insurance.auth.entity;

import com.anverraglobal.insurance.model.entity.BaseEntity;
import com.anverraglobal.insurance.model.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;

/**
 * Reference data entity representing an authority/role in the system.
 *
 * <p>This is a lightweight reference table holding one record per {@link RoleName}
 * enum value. It is seeded at application startup via Liquibase.</p>
 *
 * <p>Source of truth: docs/03_roles_and_permissions.md — Role Definitions.
 * Five roles: ADMIN, BROKER, AGENT, SUB_AGENT, DE_AGENT.</p>
 *
 * <p>The role-to-user assignment is stored in the {@code user_roles} join table
 * via the {@link UserRole} entity — not as a direct FK on the {@code users} table.
 * A user can hold multiple roles simultaneously.</p>
 */
@Entity
@Table(
        name = "roles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_roles_name", columnNames = "name")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {

    /**
     * The canonical role name. Stored as a VARCHAR string in the DB.
     * Unique across the table — exactly one row per RoleName value.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, length = 30)
    private RoleName name;

    /**
     * Human-readable description of what this role can do.
     */
    @Column(name = "description", length = 500)
    private String description;
}

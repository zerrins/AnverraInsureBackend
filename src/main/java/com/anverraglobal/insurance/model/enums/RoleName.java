package com.anverraglobal.insurance.model.enums;

/**
 * Canonical role names for the AnverraGlobal platform.
 *
 * <p>Source of truth: docs/03_roles_and_permissions.md — Role Definitions</p>
 *
 * <p>A user can hold <strong>multiple roles simultaneously</strong>
 * (stored as a {@code Set<UserRole>} join records).
 * Do NOT store a single role on the user entity — always use the join table.</p>
 *
 * <p>Role hierarchy (informational, not enforced by this enum):
 * <pre>
 *   ADMIN > BROKER > AGENT > SUB_AGENT
 *                  > DE_AGENT
 * </pre>
 * </p>
 */
public enum RoleName {

    /**
     * System administrator with full access to all entities.
     */
    ADMIN,

    /**
     * Organisation-level principal. Policies are associated with a broker.
     * Can manage agents under the broker account.
     */
    BROKER,

    /**
     * Individual insurance intermediary. Creates and manages policies.
     * Earns incoming commission from insurance company.
     * May have sub-agents working under them.
     */
    AGENT,

    /**
     * Works under one or more agents. Earns outgoing commission from the agent
     * when they source a policy. Relationship is implicit via the commissions table.
     */
    SUB_AGENT,

    /**
     * Data Entry Agent. Enters policies on behalf of agents.
     * Has limited access compared to a full agent.
     */
    DE_AGENT
}

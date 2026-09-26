package org.vrz.relay.api.event;

/**
 * Reason explaining why a member left a party.
 */
public enum LeaveReason {
    /**
     * The player left voluntarily.
     */
    VOLUNTARY,

    /**
     * The player was kicked by the leader or admin.
     */
    KICKED,

    /**
     * The party was disbanded.
     */
    DISBANDED
}

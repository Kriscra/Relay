package org.vrz.relay.api.notification;

/**
 * Priority tiers for notification queuing and preemption.
 * Higher priority notifications will preempt and pause lower priority notifications.
 */
public enum NotificationPriority {

    /**
     * Lowest priority background feed.
     * Used for continuous persistent status bars (e.g., RPG mana/health, compass).
     * Shown only when no higher priority notification is in the queue.
     */
    CONTINUOUS(0),

    /**
     * Standard notification priority.
     * Used for general gameplay updates, quest steps, and economy alerts.
     */
    NORMAL(1),

    /**
     * High notification priority.
     * Preempts and pauses NORMAL and CONTINUOUS notifications.
     * Used for region transitions, trade requests, or important events.
     */
    HIGH(2),

    /**
     * Critical notification priority.
     * Overrides and locks out all lower priority notifications.
     * Used for combat warnings, low health alarms, and server restart countdowns.
     */
    CRITICAL(3);

    private final int level;

    NotificationPriority(int level) {
        this.level = level;
    }

    /**
     * Gets the numeric weight of this priority.
     *
     * @return numeric weight
     */
    public int getLevel() {
        return level;
    }

    /**
     * Checks if this priority is higher than the other priority.
     *
     * @param other priority to compare
     * @return true if higher
     */
    public boolean isHigherThan(NotificationPriority other) {
        return this.level > other.level;
    }
}

package org.vrz.relay.api.cooldown;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Snapshot descriptor representing an active or expired cooldown entry.
 */
public record CooldownEntry(
        @Nullable UUID targetId,
        @NotNull String key,
        @NotNull Instant startTime,
        @NotNull Duration totalDuration,
        @NotNull Instant expiryTime
) {

    /**
     * Checks if this cooldown is a global server-wide cooldown (not bound to a specific player UUID).
     *
     * @return true if global
     */
    public boolean isGlobal() {
        return targetId == null;
    }

    /**
     * Checks if the cooldown has reached or surpassed its expiry timestamp.
     *
     * @return true if expired
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiryTime);
    }

    /**
     * Calculates the remaining duration until this cooldown expires.
     *
     * @return remaining duration, or {@link Duration#ZERO} if already expired
     */
    @NotNull
    public Duration getRemaining() {
        Instant now = Instant.now();
        if (now.isAfter(expiryTime)) {
            return Duration.ZERO;
        }
        return Duration.between(now, expiryTime);
    }

    /**
     * Gets the remaining time in milliseconds.
     *
     * @return remaining milliseconds, or 0 if expired
     */
    public long getRemainingMillis() {
        return Math.max(0, getRemaining().toMillis());
    }

    /**
     * Calculates the progress of this cooldown from 0.0 (just applied) to 1.0 (fully expired/ready).
     * <p>
     * Useful for UI elements like BossBars, ActionBars, and cooldown progress indicators.
     *
     * @return progress value between 0.0 and 1.0
     */
    public double getProgress() {
        long total = totalDuration.toMillis();
        if (total <= 0) {
            return 1.0;
        }
        long elapsed = Duration.between(startTime, Instant.now()).toMillis();
        if (elapsed >= total) {
            return 1.0;
        }
        if (elapsed <= 0) {
            return 0.0;
        }
        return (double) elapsed / total;
    }
}

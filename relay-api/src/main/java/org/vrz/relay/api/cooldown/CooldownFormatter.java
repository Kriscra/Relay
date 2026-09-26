package org.vrz.relay.api.cooldown;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Utility helper for formatting cooldown durations into human-readable strings.
 */
public final class CooldownFormatter {

    private CooldownFormatter() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Formats a duration into a compact human-readable string.
     * <ul>
     *   <li>Over 1 day: {@code "2d 5h"}</li>
     *   <li>Over 1 hour: {@code "3h 24m"}</li>
     *   <li>Over 1 minute: {@code "4m 12s"}</li>
     *   <li>Over 5 seconds: {@code "8s"}</li>
     *   <li>Under 5 seconds: {@code "3.4s"}</li>
     * </ul>
     *
     * @param duration remaining duration
     * @return compact formatted string
     */
    @NotNull
    public static String formatCompact(@NotNull Duration duration) {
        long millis = Math.max(0, duration.toMillis());
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            long remHours = hours % 24;
            return remHours > 0 ? days + "d " + remHours + "h" : days + "d";
        }
        if (hours > 0) {
            long remMinutes = minutes % 60;
            return remMinutes > 0 ? hours + "h " + remMinutes + "m" : hours + "h";
        }
        if (minutes > 0) {
            long remSeconds = seconds % 60;
            return remSeconds > 0 ? minutes + "m " + remSeconds + "s" : minutes + "m";
        }
        if (seconds >= 5) {
            return seconds + "s";
        }

        double decSeconds = millis / 1000.0;
        return String.format(java.util.Locale.US, "%.1fs", decSeconds);
    }

    /**
     * Formats a duration into standard digital clock format (MM:SS or HH:MM:SS).
     *
     * @param duration duration to format
     * @return clock string (e.g. {@code "04:12"} or {@code "01:25:30"})
     */
    @NotNull
    public static String formatClock(@NotNull Duration duration) {
        long totalSeconds = Math.max(0, duration.toSeconds());
        long s = totalSeconds % 60;
        long m = (totalSeconds / 60) % 60;
        long h = totalSeconds / 3600;

        if (h > 0) {
            return String.format(java.util.Locale.US, "%02d:%02d:%02d", h, m, s);
        }
        return String.format(java.util.Locale.US, "%02d:%02d", m, s);
    }
}

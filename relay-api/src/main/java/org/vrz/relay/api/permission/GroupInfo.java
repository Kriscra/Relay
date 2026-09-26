package org.vrz.relay.api.permission;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Descriptor representing a permission rank or group.
 */
public record GroupInfo(
        @NotNull String name,
        @NotNull String displayName,
        int weight,
        @Nullable String prefix,
        @Nullable String suffix
) {

    public GroupInfo {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Group name cannot be null or blank");
        }
    }
}

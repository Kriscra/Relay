package org.vrz.relay.api.permission;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Result data transfer object returned by permission and group mutation operations.
 */
public record PermissionResult(
        @NotNull Status status,
        @Nullable String message
) {

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    @NotNull
    public static PermissionResult success() {
        return new PermissionResult(Status.SUCCESS, null);
    }

    @NotNull
    public static PermissionResult success(@NotNull String message) {
        return new PermissionResult(Status.SUCCESS, message);
    }

    @NotNull
    public static PermissionResult failure(@NotNull Status status, @NotNull String message) {
        return new PermissionResult(status, message);
    }

    public enum Status {
        /**
         * Permission or group change applied successfully.
         */
        SUCCESS,

        /**
         * The player already possesses this permission or group.
         */
        ALREADY_GRANTED,

        /**
         * The player does not hold this permission or is not a member of the group.
         */
        NOT_FOUND,

        /**
         * The underlying permission provider does not support mutating this property.
         */
        NOT_SUPPORTED,

        /**
         * An unexpected error occurred while persisting the permission change.
         */
        ERROR
    }
}

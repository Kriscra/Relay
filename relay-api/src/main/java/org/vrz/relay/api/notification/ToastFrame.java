package org.vrz.relay.api.notification;

import org.jetbrains.annotations.NotNull;

/**
 * Visual frame styles for toast/advancement popup notifications in the top-right corner.
 */
public enum ToastFrame {

    /**
     * Regular advancement frame with rounded borders.
     */
    TASK("task"),

    /**
     * Goal advancement frame with pointed borders.
     */
    GOAL("goal"),

    /**
     * Challenge advancement frame with ornate spiked borders.
     */
    CHALLENGE("challenge");

    private final String key;

    ToastFrame(@NotNull String key) {
        this.key = key;
    }

    /**
     * Gets the Minecraft advancement frame key string.
     *
     * @return frame key name
     */
    @NotNull
    public String getKey() {
        return key;
    }
}

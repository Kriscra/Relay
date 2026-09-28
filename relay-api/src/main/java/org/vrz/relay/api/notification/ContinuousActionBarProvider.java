package org.vrz.relay.api.notification;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Functional interface for providing continuous background status bar text (e.g. RPG HP/Mana).
 */
@FunctionalInterface
public interface ContinuousActionBarProvider {

    /**
     * Supplies the current component to display when no higher-priority notification is active.
     *
     * @param player target player
     * @return component to display, or null to display nothing
     */
    @Nullable
    Component provide(@NotNull Player player);
}

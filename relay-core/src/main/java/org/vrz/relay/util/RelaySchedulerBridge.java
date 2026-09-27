package org.vrz.relay.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Internal bridge providing multi-platform scheduling abstraction across Folia, Paper, and Spigot.
 */
public final class RelaySchedulerBridge {

    private RelaySchedulerBridge() {}

    /**
     * Executes a task on the thread owning the specified player.
     *
     * @param plugin plugin
     * @param player player
     * @param task   runnable task
     */
    public static void runOnPlayer(@NotNull Plugin plugin, @NotNull Player player, @NotNull Runnable task) {
        if (Bukkit.getServer() == null) {
            task.run();
            return;
        }

        try {
            // Paper 1.20+ / Folia Entity Scheduler
            player.getScheduler().execute(plugin, task, null, 1L);
        } catch (Throwable e) {
            // Standard Bukkit / Spigot fallback
            if (Bukkit.isPrimaryThread()) {
                task.run();
            } else {
                Bukkit.getScheduler().runTask(plugin, task);
            }
        }
    }

    /**
     * Executes a task on the thread owning the specified location.
     *
     * @param plugin   plugin
     * @param location location
     * @param task     runnable task
     */
    public static void runAtLocation(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable task) {
        if (Bukkit.getServer() == null || location.getWorld() == null) {
            task.run();
            return;
        }

        try {
            // Folia Region Scheduler
            Bukkit.getRegionScheduler().execute(plugin, location, task);
        } catch (Throwable e) {
            // Standard Bukkit / Spigot fallback
            if (Bukkit.isPrimaryThread()) {
                task.run();
            } else {
                Bukkit.getScheduler().runTask(plugin, task);
            }
        }
    }

    /**
     * Schedules a recurring global timer task.
     *
     * @param plugin      plugin
     * @param task        task
     * @param delayTicks  delay
     * @param periodTicks period
     * @return runnable to cancel the timer
     */
    @NotNull
    public static Runnable runTimer(@NotNull Plugin plugin, @NotNull Runnable task, long delayTicks, long periodTicks) {
        if (Bukkit.getServer() == null) {
            return () -> {};
        }

        try {
            // Folia GlobalRegionScheduler
            Object scheduledTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> task.run(), Math.max(1, delayTicks), Math.max(1, periodTicks));
            return () -> {
                try {
                    scheduledTask.getClass().getMethod("cancel").invoke(scheduledTask);
                } catch (Throwable ignored) {}
            };
        } catch (Throwable e) {
            // Standard Bukkit scheduler
            int taskId = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks).getTaskId();
            return () -> Bukkit.getScheduler().cancelTask(taskId);
        }
    }
}

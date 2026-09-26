package org.vrz.relay.cooldown;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.cooldown.CooldownEntry;
import org.vrz.relay.api.cooldown.CooldownService;
import org.vrz.relay.api.event.CooldownExpiredEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * High-performance, concurrent implementation of {@link CooldownService}.
 */
public class RelayCooldownServiceImpl implements CooldownService {

    private static final Logger LOGGER = Logger.getLogger("RelayCooldownService");

    private static record InternalCooldown(
            @NotNull CooldownEntry entry,
            @Nullable Plugin plugin,
            @Nullable Consumer<CooldownEntry> onExpire,
            @Nullable ScheduledFuture<?> scheduledTask
    ) {}

    private final Map<UUID, Map<String, InternalCooldown>> playerCooldowns = new ConcurrentHashMap<>();
    private final Map<String, InternalCooldown> globalCooldowns = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor;

    public RelayCooldownServiceImpl() {
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Relay-CooldownScheduler");
            t.setDaemon(true);
            return t;
        });

        // Periodic sweep every 30 seconds to purge expired entries from memory
        this.cleanupExecutor.scheduleAtFixedRate(this::cleanExpired, 30, 30, TimeUnit.SECONDS);
    }

    @Override
    @NotNull
    public CooldownEntry setCooldown(@NotNull UUID targetId,
                                    @NotNull String key,
                                    @NotNull Duration duration,
                                    @Nullable Plugin plugin,
                                    @Nullable Consumer<CooldownEntry> onExpire) {
        Objects.requireNonNull(targetId, "targetId cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(duration, "duration cannot be null");

        String normalizedKey = key.toLowerCase(Locale.ROOT);
        Instant now = Instant.now();
        Instant expiry = now.plus(duration);

        CooldownEntry entry = new CooldownEntry(targetId, normalizedKey, now, duration, expiry);

        ScheduledFuture<?> scheduledTask = null;
        if (duration.toMillis() > 0) {
            scheduledTask = cleanupExecutor.schedule(() -> {
                handleExpiration(entry, onExpire);
            }, duration.toMillis(), TimeUnit.MILLISECONDS);
        }

        InternalCooldown internal = new InternalCooldown(entry, plugin, onExpire, scheduledTask);

        Map<String, InternalCooldown> userMap = playerCooldowns.computeIfAbsent(targetId, k -> new ConcurrentHashMap<>());
        InternalCooldown previous = userMap.put(normalizedKey, internal);
        if (previous != null && previous.scheduledTask() != null) {
            previous.scheduledTask().cancel(false);
        }

        return entry;
    }

    @Override
    public boolean hasCooldown(@NotNull UUID targetId, @NotNull String key) {
        return getCooldown(targetId, key).isPresent();
    }

    @Override
    @NotNull
    public Duration getRemaining(@NotNull UUID targetId, @NotNull String key) {
        return getCooldown(targetId, key)
                .map(CooldownEntry::getRemaining)
                .orElse(Duration.ZERO);
    }

    @Override
    @NotNull
    public Optional<CooldownEntry> getCooldown(@NotNull UUID targetId, @NotNull String key) {
        Objects.requireNonNull(targetId, "targetId cannot be null");
        Objects.requireNonNull(key, "key cannot be null");

        Map<String, InternalCooldown> userMap = playerCooldowns.get(targetId);
        if (userMap == null) return Optional.empty();

        InternalCooldown internal = userMap.get(key.toLowerCase(Locale.ROOT));
        if (internal == null) return Optional.empty();

        if (internal.entry().isExpired()) {
            userMap.remove(key.toLowerCase(Locale.ROOT));
            return Optional.empty();
        }

        return Optional.of(internal.entry());
    }

    @Override
    public boolean clearCooldown(@NotNull UUID targetId, @NotNull String key) {
        Objects.requireNonNull(targetId, "targetId cannot be null");
        Objects.requireNonNull(key, "key cannot be null");

        Map<String, InternalCooldown> userMap = playerCooldowns.get(targetId);
        if (userMap == null) return false;

        InternalCooldown removed = userMap.remove(key.toLowerCase(Locale.ROOT));
        if (removed != null) {
            if (removed.scheduledTask() != null) {
                removed.scheduledTask().cancel(false);
            }
            return true;
        }
        return false;
    }

    @Override
    public void clearAllCooldowns(@NotNull UUID targetId) {
        Objects.requireNonNull(targetId, "targetId cannot be null");
        Map<String, InternalCooldown> userMap = playerCooldowns.remove(targetId);
        if (userMap != null) {
            for (InternalCooldown internal : userMap.values()) {
                if (internal.scheduledTask() != null) {
                    internal.scheduledTask().cancel(false);
                }
            }
        }
    }

    @Override
    @NotNull
    public Map<String, CooldownEntry> getActiveCooldowns(@NotNull UUID targetId) {
        Objects.requireNonNull(targetId, "targetId cannot be null");
        Map<String, InternalCooldown> userMap = playerCooldowns.get(targetId);
        if (userMap == null) return Collections.emptyMap();

        Map<String, CooldownEntry> active = new HashMap<>();
        for (Map.Entry<String, InternalCooldown> entry : userMap.entrySet()) {
            if (!entry.getValue().entry().isExpired()) {
                active.put(entry.getKey(), entry.getValue().entry());
            }
        }
        return Collections.unmodifiableMap(active);
    }

    @Override
    @NotNull
    public CooldownEntry setGlobalCooldown(@NotNull String key,
                                          @NotNull Duration duration,
                                          @Nullable Plugin plugin) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(duration, "duration cannot be null");

        String normalizedKey = key.toLowerCase(Locale.ROOT);
        Instant now = Instant.now();
        Instant expiry = now.plus(duration);

        CooldownEntry entry = new CooldownEntry(null, normalizedKey, now, duration, expiry);

        ScheduledFuture<?> scheduledTask = null;
        if (duration.toMillis() > 0) {
            scheduledTask = cleanupExecutor.schedule(() -> {
                handleExpiration(entry, null);
            }, duration.toMillis(), TimeUnit.MILLISECONDS);
        }

        InternalCooldown internal = new InternalCooldown(entry, plugin, null, scheduledTask);
        InternalPreviousCheck(normalizedKey, internal);

        return entry;
    }

    private void InternalPreviousCheck(String key, InternalCooldown internal) {
        InternalCooldown previous = globalCooldowns.put(key, internal);
        if (previous != null && previous.scheduledTask() != null) {
            previous.scheduledTask().cancel(false);
        }
    }

    @Override
    public boolean hasGlobalCooldown(@NotNull String key) {
        Objects.requireNonNull(key, "key cannot be null");
        InternalCooldown internal = globalCooldowns.get(key.toLowerCase(Locale.ROOT));
        if (internal == null) return false;

        if (internal.entry().isExpired()) {
            globalCooldowns.remove(key.toLowerCase(Locale.ROOT));
            return false;
        }
        return true;
    }

    @Override
    @NotNull
    public Duration getGlobalRemaining(@NotNull String key) {
        Objects.requireNonNull(key, "key cannot be null");
        InternalCooldown internal = globalCooldowns.get(key.toLowerCase(Locale.ROOT));
        if (internal == null || internal.entry().isExpired()) {
            return Duration.ZERO;
        }
        return internal.entry().getRemaining();
    }

    @Override
    public boolean clearGlobalCooldown(@NotNull String key) {
        Objects.requireNonNull(key, "key cannot be null");
        InternalCooldown removed = globalCooldowns.remove(key.toLowerCase(Locale.ROOT));
        if (removed != null) {
            if (removed.scheduledTask() != null) {
                removed.scheduledTask().cancel(false);
            }
            return true;
        }
        return false;
    }

    @Override
    public void clearAllCooldowns(@NotNull Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");

        // Player cooldowns
        for (Map<String, InternalCooldown> userMap : playerCooldowns.values()) {
            userMap.values().removeIf(internal -> {
                if (plugin.equals(internal.plugin())) {
                    if (internal.scheduledTask() != null) {
                        internal.scheduledTask().cancel(false);
                    }
                    return true;
                }
                return false;
            });
        }

        // Global cooldowns
        globalCooldowns.values().removeIf(internal -> {
            if (plugin.equals(internal.plugin())) {
                if (internal.scheduledTask() != null) {
                    internal.scheduledTask().cancel(false);
                }
                return true;
            }
            return false;
        });
    }

    private void handleExpiration(CooldownEntry entry, Consumer<CooldownEntry> callback) {
        try {
            if (callback != null) {
                callback.accept(entry);
            }
            if (Bukkit.getServer() != null && Bukkit.getPluginManager() != null) {
                Bukkit.getPluginManager().callEvent(new CooldownExpiredEvent(entry));
            }
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "Exception handling cooldown expiration for key: " + entry.key(), t);
        }
    }

    private void cleanExpired() {
        for (Map.Entry<UUID, Map<String, InternalCooldown>> entry : playerCooldowns.entrySet()) {
            Map<String, InternalCooldown> userMap = entry.getValue();
            userMap.values().removeIf(cooldown -> cooldown.entry().isExpired());
            if (userMap.isEmpty()) {
                playerCooldowns.remove(entry.getKey());
            }
        }
        globalCooldowns.values().removeIf(cooldown -> cooldown.entry().isExpired());
    }

    public void shutdown() {
        cleanupExecutor.shutdownNow();
        playerCooldowns.clear();
        globalCooldowns.clear();
    }
}

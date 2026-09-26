package org.vrz.relay.data;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.data.DataChangeListener;
import org.vrz.relay.api.data.DataKey;
import org.vrz.relay.api.data.SharedDataService;
import org.vrz.relay.api.event.DataChangeEvent;
import org.vrz.relay.api.messenger.Subscription;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Concrete high-performance implementation of {@link SharedDataService}.
 */
public class RelaySharedDataServiceImpl implements SharedDataService {

    private static final Logger LOGGER = Logger.getLogger("RelaySharedData");

    private static final class StoredValue<T> {
        private final DataKey<T> key;
        private final T value;
        private final Plugin plugin;
        private final Instant expiry;
        private final ScheduledFuture<?> task;

        private StoredValue(@NotNull DataKey<T> key,
                            @NotNull T value,
                            @Nullable Plugin plugin,
                            @Nullable Instant expiry,
                            @Nullable ScheduledFuture<?> task) {
            this.key = key;
            this.value = value;
            this.plugin = plugin;
            this.expiry = expiry;
            this.task = task;
        }

        private boolean isExpired() {
            return expiry != null && Instant.now().isAfter(expiry);
        }
    }

    private final Map<UUID, Map<DataKey<?>, StoredValue<?>>> playerStore = new ConcurrentHashMap<>();
    private final Map<DataKey<?>, StoredValue<?>> globalStore = new ConcurrentHashMap<>();
    private final Map<DataKey<?>, List<DataObserverEntry<?>>> observers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService ttlScheduler;

    public RelaySharedDataServiceImpl() {
        this.ttlScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Relay-SharedDataTTLScheduler");
            t.setDaemon(true);
            return t;
        });

        // Periodic purge of expired TTL entries every 60 seconds
        this.ttlScheduler.scheduleAtFixedRate(this::cleanExpired, 60, 60, TimeUnit.SECONDS);
    }

    @Override
    public <T> void set(@NotNull UUID targetId,
                        @NotNull DataKey<T> key,
                        @NotNull T value,
                        @Nullable Plugin plugin) {
        setInternal(targetId, key, value, null, plugin);
    }

    @Override
    public <T> void setTemporary(@NotNull UUID targetId,
                                 @NotNull DataKey<T> key,
                                 @NotNull T value,
                                 @NotNull Duration ttl,
                                 @Nullable Plugin plugin) {
        Objects.requireNonNull(ttl, "ttl duration cannot be null");
        Instant expiry = Instant.now().plus(ttl);
        setInternal(targetId, key, value, expiry, plugin);
    }

    @SuppressWarnings("unchecked")
    private <T> void setInternal(@NotNull UUID targetId,
                                 @NotNull DataKey<T> key,
                                 @NotNull T value,
                                 @Nullable Instant expiry,
                                 @Nullable Plugin plugin) {
        Objects.requireNonNull(targetId, "targetId cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");

        ScheduledFuture<?> task = null;
        if (expiry != null) {
            long delay = Math.max(0, Duration.between(Instant.now(), expiry).toMillis());
            task = ttlScheduler.schedule(() -> {
                remove(targetId, key);
            }, delay, TimeUnit.MILLISECONDS);
        }

        StoredValue<T> stored = new StoredValue<>(key, value, plugin, expiry, task);
        Map<DataKey<?>, StoredValue<?>> userMap = playerStore.computeIfAbsent(targetId, k -> new ConcurrentHashMap<>());

        StoredValue<?> previous = userMap.put(key, stored);
        T oldValue = null;
        if (previous != null) {
            if (previous.task != null) {
                previous.task.cancel(false);
            }
            if (!previous.isExpired()) {
                oldValue = (T) previous.value;
            }
        }

        notifyChange(targetId, key, oldValue, value);
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(@NotNull UUID targetId, @NotNull DataKey<T> key) {
        Objects.requireNonNull(targetId, "targetId cannot be null");
        Objects.requireNonNull(key, "key cannot be null");

        Map<DataKey<?>, StoredValue<?>> userMap = playerStore.get(targetId);
        if (userMap == null) return Optional.empty();

        StoredValue<?> stored = userMap.get(key);
        if (stored == null) return Optional.empty();

        if (stored.isExpired()) {
            userMap.remove(key);
            notifyChange(targetId, key, (T) stored.value, null);
            return Optional.empty();
        }

        return Optional.of((T) stored.value);
    }

    @Override
    public boolean has(@NotNull UUID targetId, @NotNull DataKey<?> key) {
        return get(targetId, (DataKey<Object>) key).isPresent();
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> Optional<T> remove(@NotNull UUID targetId, @NotNull DataKey<T> key) {
        Objects.requireNonNull(targetId, "targetId cannot be null");
        Objects.requireNonNull(key, "key cannot be null");

        Map<DataKey<?>, StoredValue<?>> userMap = playerStore.get(targetId);
        if (userMap == null) return Optional.empty();

        StoredValue<?> removed = userMap.remove(key);
        if (removed == null) return Optional.empty();

        if (removed.task != null) {
            removed.task.cancel(false);
        }

        T oldVal = (T) removed.value;
        notifyChange(targetId, key, oldVal, null);

        if (userMap.isEmpty()) {
            playerStore.remove(targetId);
        }

        return removed.isExpired() ? Optional.empty() : Optional.of(oldVal);
    }

    @Override
    public void clearAll(@NotNull UUID targetId) {
        Objects.requireNonNull(targetId, "targetId cannot be null");
        Map<DataKey<?>, StoredValue<?>> userMap = playerStore.remove(targetId);
        if (userMap != null) {
            for (StoredValue<?> val : userMap.values()) {
                if (val.task != null) {
                    val.task.cancel(false);
                }
            }
        }
    }

    @Override
    @NotNull
    public Map<DataKey<?>, Object> getAll(@NotNull UUID targetId) {
        Objects.requireNonNull(targetId, "targetId cannot be null");
        Map<DataKey<?>, StoredValue<?>> userMap = playerStore.get(targetId);
        if (userMap == null) return Collections.emptyMap();

        Map<DataKey<?>, Object> snapshot = new HashMap<>();
        for (Map.Entry<DataKey<?>, StoredValue<?>> entry : userMap.entrySet()) {
            if (!entry.getValue().isExpired()) {
                snapshot.put(entry.getKey(), entry.getValue().value);
            }
        }
        return Collections.unmodifiableMap(snapshot);
    }

    @Override
    public <T> void setGlobal(@NotNull DataKey<T> key, @NotNull T value, @Nullable Plugin plugin) {
        setGlobalInternal(key, value, null, plugin);
    }

    @Override
    public <T> void setGlobalTemporary(@NotNull DataKey<T> key,
                                       @NotNull T value,
                                       @NotNull Duration ttl,
                                       @Nullable Plugin plugin) {
        Objects.requireNonNull(ttl, "ttl cannot be null");
        setGlobalInternal(key, value, Instant.now().plus(ttl), plugin);
    }

    @SuppressWarnings("unchecked")
    private <T> void setGlobalInternal(@NotNull DataKey<T> key,
                                       @NotNull T value,
                                       @Nullable Instant expiry,
                                       @Nullable Plugin plugin) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");

        ScheduledFuture<?> task = null;
        if (expiry != null) {
            long delay = Math.max(0, Duration.between(Instant.now(), expiry).toMillis());
            task = ttlScheduler.schedule(() -> removeGlobal(key), delay, TimeUnit.MILLISECONDS);
        }

        StoredValue<T> stored = new StoredValue<>(key, value, plugin, expiry, task);
        StoredValue<?> previous = globalStore.put(key, stored);

        T oldValue = null;
        if (previous != null) {
            if (previous.task != null) previous.task.cancel(false);
            if (!previous.isExpired()) oldValue = (T) previous.value;
        }

        notifyChange(null, key, oldValue, value);
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getGlobal(@NotNull DataKey<T> key) {
        Objects.requireNonNull(key, "key cannot be null");
        StoredValue<?> stored = globalStore.get(key);
        if (stored == null) return Optional.empty();

        if (stored.isExpired()) {
            globalStore.remove(key);
            notifyChange(null, key, (T) stored.value, null);
            return Optional.empty();
        }

        return Optional.of((T) stored.value);
    }

    @Override
    public boolean hasGlobal(@NotNull DataKey<?> key) {
        return getGlobal((DataKey<Object>) key).isPresent();
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> Optional<T> removeGlobal(@NotNull DataKey<T> key) {
        Objects.requireNonNull(key, "key cannot be null");
        StoredValue<?> removed = globalStore.remove(key);
        if (removed == null) return Optional.empty();

        if (removed.task != null) removed.task.cancel(false);

        T oldVal = (T) removed.value;
        notifyChange(null, key, oldVal, null);

        return removed.isExpired() ? Optional.empty() : Optional.of(oldVal);
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> Subscription observe(@NotNull DataKey<T> key,
                                    @NotNull Plugin observer,
                                    @NotNull DataChangeListener<T> listener) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(observer, "observer plugin cannot be null");
        Objects.requireNonNull(listener, "listener cannot be null");

        DataObserverEntry<T> entry = new DataObserverEntry<>(key, observer, listener, this::removeObserver);
        observers.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(entry);
        return entry;
    }

    private void removeObserver(@NotNull DataObserverEntry<?> entry) {
        List<DataObserverEntry<?>> list = observers.get(entry.getKey());
        if (list != null) {
            list.remove(entry);
            if (list.isEmpty()) {
                observers.remove(entry.getKey());
            }
        }
    }

    @Override
    public void clearAll(@NotNull Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");

        // Clear player store entries by this plugin
        for (Map<DataKey<?>, StoredValue<?>> userMap : playerStore.values()) {
            userMap.values().removeIf(stored -> {
                if (plugin.equals(stored.plugin)) {
                    if (stored.task != null) stored.task.cancel(false);
                    return true;
                }
                return false;
            });
        }

        // Clear global entries by this plugin
        globalStore.values().removeIf(stored -> {
            if (plugin.equals(stored.plugin)) {
                if (stored.task != null) stored.task.cancel(false);
                return true;
            }
            return false;
        });

        // Clear observers registered by this plugin
        for (List<DataObserverEntry<?>> list : observers.values()) {
            list.removeIf(obs -> {
                if (plugin.equals(obs.getPlugin())) {
                    obs.unsubscribe();
                    return true;
                }
                return false;
            });
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void notifyChange(@Nullable UUID targetId, @NotNull DataKey<T> key, @Nullable T oldValue, @Nullable T newValue) {
        List<DataObserverEntry<?>> list = observers.get(key);
        if (list != null) {
            for (DataObserverEntry<?> entry : list) {
                try {
                    ((DataObserverEntry<T>) entry).dispatch(targetId, oldValue, newValue);
                } catch (Throwable t) {
                    LOGGER.log(Level.SEVERE, "Exception in data change observer: " + key.getFullKey(), t);
                }
            }
        }

        try {
            if (Bukkit.getServer() != null && Bukkit.getPluginManager() != null) {
                Bukkit.getPluginManager().callEvent(new DataChangeEvent<>(targetId, key, oldValue, newValue));
            }
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "Ignored event dispatch failure (expected in standalone tests)", t);
        }
    }

    private void cleanExpired() {
        for (Map.Entry<UUID, Map<DataKey<?>, StoredValue<?>>> entry : playerStore.entrySet()) {
            Map<DataKey<?>, StoredValue<?>> userMap = entry.getValue();
            userMap.values().removeIf(stored -> stored.isExpired());
            if (userMap.isEmpty()) {
                playerStore.remove(entry.getKey());
            }
        }
        globalStore.values().removeIf(stored -> stored.isExpired());
    }

    public void shutdown() {
        ttlScheduler.shutdownNow();
        playerStore.clear();
        globalStore.clear();
        observers.clear();
    }
}

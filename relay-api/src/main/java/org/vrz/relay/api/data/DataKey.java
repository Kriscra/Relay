package org.vrz.relay.api.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Type-safe descriptor representing a shared data key.
 *
 * @param <T> data type stored under this key
 */
public final class DataKey<T> {

    private final String namespace;
    private final String key;
    private final Class<T> type;
    private final T defaultValue;
    private final String fullKey;

    private DataKey(@NotNull String namespace,
                    @NotNull String key,
                    @NotNull Class<T> type,
                    @Nullable T defaultValue) {
        this.namespace = Objects.requireNonNull(namespace, "namespace cannot be null").toLowerCase(Locale.ROOT);
        this.key = Objects.requireNonNull(key, "key cannot be null").toLowerCase(Locale.ROOT);
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.defaultValue = defaultValue;
        this.fullKey = this.namespace + ":" + this.key;
    }

    /**
     * Creates a type-safe data key.
     *
     * @param namespace owning namespace (e.g. "combat", "party")
     * @param key       property key (e.g. "tagged", "leader")
     * @param type      data type token
     * @param <T>       data type
     * @return new DataKey
     */
    @NotNull
    public static <T> DataKey<T> of(@NotNull String namespace, @NotNull String key, @NotNull Class<T> type) {
        return new DataKey<>(namespace, key, type, null);
    }

    /**
     * Creates a type-safe data key with a default fallback value.
     *
     * @param namespace    owning namespace
     * @param key          property key
     * @param type         data type token
     * @param defaultValue default value when key is absent
     * @param <T>          data type
     * @return new DataKey
     */
    @NotNull
    public static <T> DataKey<T> of(@NotNull String namespace, @NotNull String key, @NotNull Class<T> type, @NotNull T defaultValue) {
        return new DataKey<>(namespace, key, type, Objects.requireNonNull(defaultValue, "defaultValue cannot be null"));
    }

    /**
     * Creates a type-safe data key from a colon-separated string (e.g. "clan:level").
     */
    @NotNull
    public static <T> DataKey<T> of(@NotNull String namespacedKey, @NotNull Class<T> type) {
        int idx = namespacedKey.indexOf(':');
        if (idx == -1) {
            return of("relay", namespacedKey, type);
        }
        return of(namespacedKey.substring(0, idx), namespacedKey.substring(idx + 1), type);
    }

    @NotNull
    public String getNamespace() {
        return namespace;
    }

    @NotNull
    public String getKey() {
        return key;
    }

    @NotNull
    public Class<T> getType() {
        return type;
    }

    @NotNull
    public Optional<T> getDefaultValue() {
        return Optional.ofNullable(defaultValue);
    }

    @NotNull
    public String getFullKey() {
        return fullKey;
    }

    /**
     * Safely casts an arbitrary object to this key's type.
     *
     * @param value raw value
     * @return typed value
     */
    public T cast(@NotNull Object value) {
        return type.cast(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataKey<?> that)) return false;
        return Objects.equals(fullKey, that.fullKey);
    }

    @Override
    public int hashCode() {
        return fullKey.hashCode();
    }

    @Override
    public String toString() {
        return fullKey + " (" + type.getSimpleName() + ")";
    }
}

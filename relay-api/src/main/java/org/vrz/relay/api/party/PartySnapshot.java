package org.vrz.relay.api.party;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable snapshot record representing a party state at a point in time.
 * Thread-safe and safely passable across threads and plugins.
 */
public record PartySnapshot(
        @NotNull UUID id,
        @NotNull String name,
        @NotNull UUID leaderId,
        @NotNull Set<UUID> members,
        int maxSize,
        boolean friendlyFireEnabled,
        @NotNull Instant createdAt,
        @NotNull Map<String, Object> metadata
) implements Party {

    public PartySnapshot {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(leaderId, "leaderId cannot be null");
        Objects.requireNonNull(members, "members cannot be null");
        Objects.requireNonNull(createdAt, "createdAt cannot be null");
        Objects.requireNonNull(metadata, "metadata cannot be null");

        // Guarantee immutable copies
        members = Collections.unmodifiableSet(new HashSet<>(members));
        metadata = Collections.unmodifiableMap(new HashMap<>(metadata));
    }

    @Override
    @NotNull
    public UUID getId() {
        return id;
    }

    @Override
    @NotNull
    public String getName() {
        return name;
    }

    @Override
    @NotNull
    public UUID getLeaderId() {
        return leaderId;
    }

    @Override
    @NotNull
    public Set<UUID> getMembers() {
        return members;
    }

    @Override
    public int getMaxSize() {
        return maxSize;
    }

    @Override
    public boolean isFriendlyFireEnabled() {
        return friendlyFireEnabled;
    }

    @Override
    @NotNull
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    @NotNull
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Creates a new builder for constructing a {@link PartySnapshot}.
     *
     * @param id party ID
     * @param leaderId leader UUID
     * @return new builder instance
     */
    public static Builder builder(@NotNull UUID id, @NotNull UUID leaderId) {
        return new Builder(id, leaderId);
    }

    /**
     * Creates a builder pre-populated with data from an existing {@link Party}.
     *
     * @param party template party
     * @return builder instance
     */
    public static Builder from(@NotNull Party party) {
        return new Builder(party.getId(), party.getLeaderId())
                .name(party.getName())
                .members(party.getMembers())
                .maxSize(party.getMaxSize())
                .friendlyFireEnabled(party.isFriendlyFireEnabled())
                .createdAt(party.getCreatedAt())
                .metadata(party.getMetadata());
    }

    /**
     * Builder for fluent construction of {@link PartySnapshot}.
     */
    public static final class Builder {
        private final UUID id;
        private UUID leaderId;
        private String name = "Party";
        private final Set<UUID> members = new HashSet<>();
        private int maxSize = 8;
        private boolean friendlyFireEnabled = false;
        private Instant createdAt = Instant.now();
        private final Map<String, Object> metadata = new HashMap<>();

        private Builder(@NotNull UUID id, @NotNull UUID leaderId) {
            this.id = Objects.requireNonNull(id, "id cannot be null");
            this.leaderId = Objects.requireNonNull(leaderId, "leaderId cannot be null");
            this.members.add(leaderId);
        }

        public Builder leaderId(@NotNull UUID leaderId) {
            this.leaderId = Objects.requireNonNull(leaderId, "leaderId cannot be null");
            this.members.add(leaderId);
            return this;
        }

        public Builder name(@NotNull String name) {
            this.name = Objects.requireNonNull(name, "name cannot be null");
            return this;
        }

        public Builder members(@NotNull Set<UUID> members) {
            this.members.clear();
            this.members.addAll(members);
            this.members.add(leaderId);
            return this;
        }

        public Builder addMember(@NotNull UUID memberId) {
            this.members.add(Objects.requireNonNull(memberId, "memberId cannot be null"));
            return this;
        }

        public Builder maxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder friendlyFireEnabled(boolean friendlyFireEnabled) {
            this.friendlyFireEnabled = friendlyFireEnabled;
            return this;
        }

        public Builder createdAt(@NotNull Instant createdAt) {
            this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
            return this;
        }

        public Builder metadata(@NotNull Map<String, Object> metadata) {
            this.metadata.clear();
            this.metadata.putAll(metadata);
            return this;
        }

        public Builder putMetadata(@NotNull String key, @Nullable Object value) {
            if (value == null) {
                this.metadata.remove(key);
            } else {
                this.metadata.put(key, value);
            }
            return this;
        }

        public PartySnapshot build() {
            return new PartySnapshot(id, name, leaderId, members, maxSize, friendlyFireEnabled, createdAt, metadata);
        }
    }
}

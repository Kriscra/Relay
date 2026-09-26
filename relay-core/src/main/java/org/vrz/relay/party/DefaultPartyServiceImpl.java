package org.vrz.relay.party;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.event.LeaveReason;
import org.vrz.relay.api.event.PartyCreatedEvent;
import org.vrz.relay.api.event.PartyDisbandedEvent;
import org.vrz.relay.api.event.PartyFriendlyFireToggleEvent;
import org.vrz.relay.api.event.PartyLeaderChangedEvent;
import org.vrz.relay.api.event.PartyMemberJoinedEvent;
import org.vrz.relay.api.event.PartyMemberLeaveEvent;
import org.vrz.relay.api.party.Party;
import org.vrz.relay.api.party.PartyService;
import org.vrz.relay.api.party.PartySnapshot;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Standard thread-safe, high-performance in-memory implementation of {@link PartyService}.
 * <p>
 * Registered by Relay at {@link org.vrz.relay.api.service.ServicePriority#FALLBACK}
 * to ensure that servers without third-party party plugins have full party and friendly-fire
 * functionality out of the box. Third-party party plugins can override this provider at any time.
 */
public class DefaultPartyServiceImpl implements PartyService {

    private final ConcurrentHashMap<UUID, PartyEntity> partiesById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, UUID> playerToParty = new ConcurrentHashMap<>();

    @Override
    @NotNull
    public CompletableFuture<Optional<Party>> getParty(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        UUID partyId = playerToParty.get(playerId);
        if (partyId == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        PartyEntity party = partiesById.get(partyId);
        if (party == null) {
            playerToParty.remove(playerId, partyId);
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return CompletableFuture.completedFuture(Optional.of(party.toSnapshot()));
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<Party>> getPartyById(@NotNull UUID partyId) {
        Objects.requireNonNull(partyId, "partyId cannot be null");
        PartyEntity party = partiesById.get(partyId);
        return CompletableFuture.completedFuture(
                party != null ? Optional.of(party.toSnapshot()) : Optional.empty()
        );
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> inParty(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        return CompletableFuture.completedFuture(playerToParty.containsKey(playerId));
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> inSameParty(@NotNull UUID playerA, @NotNull UUID playerB) {
        Objects.requireNonNull(playerA, "playerA cannot be null");
        Objects.requireNonNull(playerB, "playerB cannot be null");

        UUID partyA = playerToParty.get(playerA);
        UUID partyB = playerToParty.get(playerB);
        boolean same = partyA != null && partyA.equals(partyB);
        return CompletableFuture.completedFuture(same);
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> isLeader(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        UUID partyId = playerToParty.get(playerId);
        if (partyId == null) {
            return CompletableFuture.completedFuture(false);
        }

        PartyEntity party = partiesById.get(partyId);
        return CompletableFuture.completedFuture(party != null && party.leaderId.equals(playerId));
    }

    @Override
    @NotNull
    public CompletableFuture<Set<UUID>> getPartyMembers(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        UUID partyId = playerToParty.get(playerId);
        if (partyId == null) {
            return CompletableFuture.completedFuture(Collections.emptySet());
        }

        PartyEntity party = partiesById.get(partyId);
        if (party == null) {
            return CompletableFuture.completedFuture(Collections.emptySet());
        }

        return CompletableFuture.completedFuture(Set.copyOf(party.members));
    }

    @Override
    @NotNull
    public CompletableFuture<Collection<Party>> getActiveParties() {
        List<Party> list = partiesById.values().stream()
                .map(PartyEntity::toSnapshot)
                .collect(Collectors.toList());
        return CompletableFuture.completedFuture(list);
    }

    @Override
    public boolean isFriendlySync(@NotNull UUID damager, @NotNull UUID victim) {
        Objects.requireNonNull(damager, "damager cannot be null");
        Objects.requireNonNull(victim, "victim cannot be null");

        if (damager.equals(victim)) {
            return false;
        }

        UUID partyA = playerToParty.get(damager);
        if (partyA == null) {
            return false;
        }

        UUID partyB = playerToParty.get(victim);
        if (!partyA.equals(partyB)) {
            return false;
        }

        PartyEntity party = partiesById.get(partyA);
        if (party == null) {
            return false;
        }

        return !party.friendlyFireEnabled;
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> isFriendly(@NotNull UUID damager, @NotNull UUID victim) {
        return CompletableFuture.completedFuture(isFriendlySync(damager, victim));
    }

    @Override
    @NotNull
    public CompletableFuture<Party> createParty(@NotNull UUID leaderId, @NotNull String name, int maxSize) {
        Objects.requireNonNull(leaderId, "leaderId cannot be null");
        Objects.requireNonNull(name, "name cannot be null");

        if (playerToParty.containsKey(leaderId)) {
            CompletableFuture<Party> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Player " + leaderId + " is already in a party"));
            return future;
        }

        UUID partyId = UUID.randomUUID();
        PartyEntity party = new PartyEntity(partyId, name, leaderId, maxSize);
        partiesById.put(partyId, party);
        playerToParty.put(leaderId, partyId);

        PartySnapshot snapshot = party.toSnapshot();
        callEvent(new PartyCreatedEvent(snapshot));
        return CompletableFuture.completedFuture(snapshot);
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> disbandParty(@NotNull UUID partyId) {
        Objects.requireNonNull(partyId, "partyId cannot be null");
        PartyEntity party = partiesById.remove(partyId);
        if (party == null) {
            return CompletableFuture.completedFuture(false);
        }

        for (UUID memberId : party.members) {
            playerToParty.remove(memberId, partyId);
        }

        PartySnapshot snapshot = party.toSnapshot();
        callEvent(new PartyDisbandedEvent(snapshot, party.leaderId));
        return CompletableFuture.completedFuture(true);
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> addMember(@NotNull UUID partyId, @NotNull UUID memberId) {
        Objects.requireNonNull(partyId, "partyId cannot be null");
        Objects.requireNonNull(memberId, "memberId cannot be null");

        PartyEntity party = partiesById.get(partyId);
        if (party == null) {
            return CompletableFuture.completedFuture(false);
        }

        if (playerToParty.containsKey(memberId)) {
            return CompletableFuture.completedFuture(false);
        }

        if (party.maxSize > 0 && party.members.size() >= party.maxSize) {
            return CompletableFuture.completedFuture(false);
        }

        party.members.add(memberId);
        playerToParty.put(memberId, partyId);

        PartySnapshot snapshot = party.toSnapshot();
        callEvent(new PartyMemberJoinedEvent(snapshot, memberId));
        return CompletableFuture.completedFuture(true);
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> removeMember(@NotNull UUID partyId, @NotNull UUID memberId) {
        Objects.requireNonNull(partyId, "partyId cannot be null");
        Objects.requireNonNull(memberId, "memberId cannot be null");

        PartyEntity party = partiesById.get(partyId);
        if (party == null || !party.members.contains(memberId)) {
            return CompletableFuture.completedFuture(false);
        }

        party.members.remove(memberId);
        playerToParty.remove(memberId, partyId);

        PartySnapshot snapshot = party.toSnapshot();
        callEvent(new PartyMemberLeaveEvent(snapshot, memberId, LeaveReason.VOLUNTARY));

        if (party.members.isEmpty()) {
            partiesById.remove(partyId);
            callEvent(new PartyDisbandedEvent(snapshot, memberId));
        } else if (party.leaderId.equals(memberId)) {
            // Transfer leadership to remaining member
            UUID nextLeader = party.members.iterator().next();
            party.leaderId = nextLeader;
            callEvent(new PartyLeaderChangedEvent(party.toSnapshot(), memberId, nextLeader));
        }

        return CompletableFuture.completedFuture(true);
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> setLeader(@NotNull UUID partyId, @NotNull UUID newLeaderId) {
        Objects.requireNonNull(partyId, "partyId cannot be null");
        Objects.requireNonNull(newLeaderId, "newLeaderId cannot be null");

        PartyEntity party = partiesById.get(partyId);
        if (party == null || !party.members.contains(newLeaderId)) {
            return CompletableFuture.completedFuture(false);
        }

        UUID oldLeader = party.leaderId;
        if (oldLeader.equals(newLeaderId)) {
            return CompletableFuture.completedFuture(true);
        }

        party.leaderId = newLeaderId;
        callEvent(new PartyLeaderChangedEvent(party.toSnapshot(), oldLeader, newLeaderId));
        return CompletableFuture.completedFuture(true);
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> setFriendlyFire(@NotNull UUID partyId, boolean enabled) {
        Objects.requireNonNull(partyId, "partyId cannot be null");

        PartyEntity party = partiesById.get(partyId);
        if (party == null) {
            return CompletableFuture.completedFuture(false);
        }

        party.friendlyFireEnabled = enabled;
        callEvent(new PartyFriendlyFireToggleEvent(party.toSnapshot(), enabled));
        return CompletableFuture.completedFuture(true);
    }

    /**
     * Clears all parties from memory.
     */
    public void clearAll() {
        partiesById.clear();
        playerToParty.clear();
    }

    private void callEvent(Event event) {
        try {
            if (Bukkit.getServer() != null && Bukkit.getPluginManager() != null) {
                Bukkit.getPluginManager().callEvent(event);
            }
        } catch (Throwable ignored) {
            // Safely ignored in headless unit test contexts
        }
    }

    /**
     * Internal mutable party representation.
     */
    private static final class PartyEntity {
        private final UUID id;
        private volatile String name;
        private volatile UUID leaderId;
        private final Set<UUID> members = ConcurrentHashMap.newKeySet();
        private volatile int maxSize;
        private volatile boolean friendlyFireEnabled;
        private final Instant createdAt;
        private final Map<String, Object> metadata = new ConcurrentHashMap<>();

        private PartyEntity(@NotNull UUID id, @NotNull String name, @NotNull UUID leaderId, int maxSize) {
            this.id = id;
            this.name = name;
            this.leaderId = leaderId;
            this.members.add(leaderId);
            this.maxSize = maxSize;
            this.friendlyFireEnabled = false;
            this.createdAt = Instant.now();
        }

        private PartySnapshot toSnapshot() {
            return new PartySnapshot(
                    id,
                    name,
                    leaderId,
                    members,
                    maxSize,
                    friendlyFireEnabled,
                    createdAt,
                    metadata
            );
        }
    }
}

package org.vrz.relay;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.vrz.relay.api.party.Party;
import org.vrz.relay.api.party.PartyService;
import org.vrz.relay.party.DefaultPartyServiceImpl;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class RelayPartyServiceTest {

    private DefaultPartyServiceImpl partyService;

    @BeforeEach
    void setUp() {
        this.partyService = new DefaultPartyServiceImpl();
    }

    @Test
    @DisplayName("Create party successfully registers leader and members")
    void testCreateParty() throws Exception {
        UUID leader = UUID.randomUUID();
        Party party = partyService.createParty(leader, "AlphaTeam").get();

        assertNotNull(party);
        assertEquals("AlphaTeam", party.getName());
        assertEquals(leader, party.getLeaderId());
        assertTrue(party.getMembers().contains(leader));
        assertEquals(1, party.getSize());

        assertTrue(partyService.inParty(leader).get());
        assertTrue(partyService.isLeader(leader).get());

        Optional<Party> fetched = partyService.getParty(leader).get();
        assertTrue(fetched.isPresent());
        assertEquals(party.getId(), fetched.get().getId());
    }

    @Test
    @DisplayName("Player cannot create a second party if already in one")
    void testDuplicatePartyCreationFails() throws Exception {
        UUID leader = UUID.randomUUID();
        partyService.createParty(leader, "Team1").get();

        ExecutionException exception = assertThrows(ExecutionException.class, () -> {
            partyService.createParty(leader, "Team2").get();
        });
        assertTrue(exception.getCause() instanceof IllegalStateException);
    }

    @Test
    @DisplayName("Adding members respects maximum party capacity")
    void testAddMemberAndCapacity() throws Exception {
        UUID leader = UUID.randomUUID();
        UUID member1 = UUID.randomUUID();
        UUID member2 = UUID.randomUUID();

        // Max size 2 (leader + 1 member)
        Party party = partyService.createParty(leader, "Duo", 2).get();

        assertTrue(partyService.addMember(party.getId(), member1).get());
        assertTrue(partyService.inParty(member1).get());
        assertTrue(partyService.inSameParty(leader, member1).get());

        Set<UUID> members = partyService.getPartyMembers(leader).get();
        assertEquals(2, members.size());
        assertTrue(members.contains(leader));
        assertTrue(members.contains(member1));

        // Third member should be rejected due to capacity limit
        assertFalse(partyService.addMember(party.getId(), member2).get());
        assertFalse(partyService.inParty(member2).get());
    }

    @Test
    @DisplayName("Friendly fire logic functions correctly for sync combat checks")
    void testFriendlyFireProtection() throws Exception {
        UUID playerA = UUID.randomUUID();
        UUID playerB = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();

        Party party = partyService.createParty(playerA, "Knights").get();
        partyService.addMember(party.getId(), playerB).get();

        // Self-attack should not be treated as friendly-fire protection
        assertFalse(partyService.isFriendlySync(playerA, playerA));

        // Teammates with friendly fire disabled (default)
        assertTrue(partyService.isFriendlySync(playerA, playerB));
        assertTrue(partyService.isFriendly(playerA, playerB).get());
        assertFalse(partyService.canDamageSync(playerA, playerB));
        assertFalse(partyService.canDamage(playerA, playerB).get());

        // Stranger (not in party)
        assertFalse(partyService.isFriendlySync(playerA, stranger));
        assertTrue(partyService.canDamageSync(playerA, stranger));

        // Toggle friendly fire enabled
        assertTrue(partyService.setFriendlyFire(party.getId(), true).get());
        assertFalse(partyService.isFriendlySync(playerA, playerB));
        assertTrue(partyService.canDamageSync(playerA, playerB));
    }

    @Test
    @DisplayName("Removing leader transfers leadership; removing last member disbands party")
    void testLeadershipSuccessionAndAutoDisband() throws Exception {
        UUID leader = UUID.randomUUID();
        UUID member = UUID.randomUUID();

        Party party = partyService.createParty(leader, "Squad").get();
        partyService.addMember(party.getId(), member).get();

        // Leader leaves
        assertTrue(partyService.removeMember(party.getId(), leader).get());
        assertFalse(partyService.inParty(leader).get());

        // Remaining member should become new leader
        assertTrue(partyService.isLeader(member).get());
        Party updated = partyService.getParty(member).get().orElseThrow();
        assertEquals(member, updated.getLeaderId());
        assertEquals(1, updated.getSize());

        // Last member leaves -> party auto-disbands
        assertTrue(partyService.removeMember(party.getId(), member).get());
        assertFalse(partyService.inParty(member).get());
        assertFalse(partyService.getPartyById(party.getId()).get().isPresent());
    }

    @Test
    @DisplayName("Disbanding party clears all member associations")
    void testDisbandParty() throws Exception {
        UUID leader = UUID.randomUUID();
        UUID member = UUID.randomUUID();

        Party party = partyService.createParty(leader, "ToDisband").get();
        partyService.addMember(party.getId(), member).get();

        assertTrue(partyService.disbandParty(party.getId()).get());

        assertFalse(partyService.inParty(leader).get());
        assertFalse(partyService.inParty(member).get());
        assertFalse(partyService.getPartyById(party.getId()).get().isPresent());
    }
}

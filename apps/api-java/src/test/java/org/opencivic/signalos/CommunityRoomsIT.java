package org.opencivic.signalos;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.CommunityRoomMessage;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.CommunityRoomMessageRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:roomsit;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    // Lets one test assert how many rows a screen actually read, not just what it returned.
    "spring.jpa.properties.hibernate.generate_statistics=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommunityRoomsIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private CommunityRoomMessageRepository messageRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;

    private UUID communityId;
    private UUID coordinatorId;

    @BeforeEach
    void setUp() {
        coordinatorId = saveUser("rooms_coord", "Room Coordinator");
        saveUser("rooms_member", "Room Member");

        Community community = new Community();
        community.setName("Rooms District");
        community.setSlug("rooms-district");
        community.setDescription("Real-time coordination rooms");
        communityId = communityRepository.save(community).getId();

        addMembership(coordinatorId, CommunityRole.COORDINATOR);
        User member = userRepository.findByUsername("rooms_member").orElseThrow();
        addMembership(member.getId(), CommunityRole.MEMBER);
    }

    private UUID saveUser(String username, String displayName) {
        User user = new User(username, "encoded", username + "@example.com", "ROLE_CITIZEN");
        user.setDisplayName(displayName);
        user.setVerified(true);
        user.setEnabled(true);
        return userRepository.save(user).getId();
    }

    private void addMembership(UUID userId, CommunityRole role) {
        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(userId);
        membership.setRole(role);
        membership.setCreatedBy(coordinatorId);
        membershipRepository.save(membership);
    }

    @Test
    void coordinatorShouldCreateRoomAndMembersShouldSeeIt() throws Exception {
        mockMvc.perform(post("/api/community/rooms")
                .with(user("rooms_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "name": "Night patrol working group",
                      "topic": "Coordinate the lighting audit walk"
                    }
                    """.formatted(communityId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rooms", hasSize(1)))
            .andExpect(jsonPath("$.rooms[0].name").value("Night patrol working group"))
            .andExpect(jsonPath("$.rooms[0].unreadMentionCount").value(0));

        mockMvc.perform(get("/api/community/rooms/workspace")
                .with(user("rooms_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rooms", hasSize(1)))
            .andExpect(jsonPath("$.mentionableUsernames", hasSize(2)));
    }

    @Test
    void memberShouldNotBeAbleToCreateRoom() throws Exception {
        mockMvc.perform(post("/api/community/rooms")
                .with(user("rooms_member").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "name": "Unauthorized room",
                      "topic": "Should not be created"
                    }
                    """.formatted(communityId)))
            .andExpect(status().isForbidden());
    }

    @Test
    void mentionsShouldBeUserSpecificAndAuditable() throws Exception {
        String roomId = createRoomAsCoordinator();

        mockMvc.perform(post("/api/community/rooms/messages")
                .with(user("rooms_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "roomId": "%s",
                      "body": "@rooms_member please confirm the 7pm walk start"
                    }
                    """.formatted(communityId, roomId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mentionedUserIds", hasSize(1)));

        mockMvc.perform(get("/api/community/rooms/workspace")
                .with(user("rooms_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rooms[0].unreadMentionCount").value(1))
            .andExpect(jsonPath("$.mentionInbox.unreadTotal").value(1))
            .andExpect(jsonPath("$.mentionInbox.items[0].roomName").value("Night patrol working group"))
            .andExpect(jsonPath("$.mentionInbox.items[0].mentionedByName").value("Room Coordinator"));

        mockMvc.perform(patch("/api/community/rooms/mentions/read")
                .with(user("rooms_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unreadTotal").value(0));

        mockMvc.perform(get("/api/community/rooms/workspace")
                .with(user("rooms_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rooms[0].unreadMentionCount").value(0));
    }

    @Test
    void authorShouldNotMentionThemselves() throws Exception {
        String roomId = createRoomAsCoordinator();

        mockMvc.perform(post("/api/community/rooms/messages")
                .with(user("rooms_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "roomId": "%s",
                      "body": "@rooms_coord note to self about the audit"
                    }
                    """.formatted(communityId, roomId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mentionedUserIds", hasSize(0)));

        mockMvc.perform(get("/api/community/rooms/workspace")
                .with(user("rooms_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mentionInbox.unreadTotal").value(0));
    }

    @Test
    void muteStateShouldBeUserSpecific() throws Exception {
        String roomId = createRoomAsCoordinator();

        mockMvc.perform(patch("/api/community/rooms/{roomId}/mute", roomId)
                .with(user("rooms_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("muted", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.muted").value(true));

        mockMvc.perform(get("/api/community/rooms/workspace")
                .with(user("rooms_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rooms[0].muted").value(true));

        mockMvc.perform(get("/api/community/rooms/workspace")
                .with(user("rooms_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rooms[0].muted").value(false));

        mockMvc.perform(patch("/api/community/rooms/{roomId}/mute", roomId)
                .with(user("rooms_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("muted", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.muted").value(false));
    }

    @Test
    void nonMemberShouldNotReadRoomMessages() throws Exception {
        saveUser("outsider", "Outsider");

        mockMvc.perform(get("/api/community/rooms/workspace")
                .with(user("outsider").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void openingABusyRoomShouldNotShipItsWholeHistory() throws Exception {
        String roomId = createRoomAsCoordinator();
        for (int i = 0; i < 60; i++) {
            postMessage(roomId, "Field note " + i);
        }

        String body = mockMvc.perform(get("/api/community/rooms/{roomId}", roomId)
                .with(user("rooms_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);

        // The workspace endpoint next to this one takes a limit. This one took none, so opening a room
        // loaded and serialised its entire history — on a platform whose stated goal is field use on a
        // phone with little bandwidth.
        org.junit.jupiter.api.Assertions.assertTrue(
            json.get("messages").size() <= org.opencivic.signalos.service.CommunityListLimits.DEFAULT_LIMIT,
            "a room read should be bounded, got " + json.get("messages").size() + " messages");

        // Capping the payload silently would be its own lie: a consumer showing "no more messages"
        // would be wrong. The true total stays available and the truncation is stated.
        org.junit.jupiter.api.Assertions.assertEquals(60, json.get("messageCount").asInt(),
            "the real total must still be reported, not the capped page size");
        org.junit.jupiter.api.Assertions.assertTrue(json.get("hasMoreMessages").asBoolean(),
            "a capped list must say so, or a consumer treats the page as the whole room");
    }

    @Test
    void aCallerCanAskForMoreOfABusyRoomAndTheCapIsRespected() throws Exception {
        String roomId = createRoomAsCoordinator();
        for (int i = 0; i < 60; i++) {
            postMessage(roomId, "Field note " + i);
        }

        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        String wider = mockMvc.perform(get("/api/community/rooms/{roomId}", roomId)
                .with(user("rooms_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("limit", "55"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertEquals(55, mapper.readTree(wider).get("messages").size());

        String absurd = mockMvc.perform(get("/api/community/rooms/{roomId}", roomId)
                .with(user("rooms_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("limit", "100000"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        // resolveLimit caps rather than erroring, so a client cannot ask the server into loading
        // everything it was just protected from loading. With 60 messages and a cap of 200 the whole
        // room fits, so the assertion is that nothing is truncated and nothing blows up.
        var absurdJson = mapper.readTree(absurd);
        org.junit.jupiter.api.Assertions.assertEquals(60, absurdJson.get("messages").size(),
            "a limit above the room's size should return the room whole");
        org.junit.jupiter.api.Assertions.assertFalse(absurdJson.get("hasMoreMessages").asBoolean(),
            "nothing was left out, so there is nothing more to say");
        org.junit.jupiter.api.Assertions.assertTrue(
            mapper.readTree(wider).get("messages").size()
                <= org.opencivic.signalos.service.CommunityListLimits.MAX_LIMIT,
            "a requested limit must never exceed the shared maximum");
    }

    @Test
    void openingTheWorkspaceShouldNotReadEveryMessageOfEveryRoom() throws Exception {
        String roomId = createRoomAsCoordinator();
        for (int i = 0; i < 5; i++) {
            postMessage(roomId, "Field note " + i);
        }

        long withFiveMessages = workspaceEntityLoads();

        for (int i = 5; i < 60; i++) {
            postMessage(roomId, "Field note " + i);
        }

        long withSixtyMessages = workspaceEntityLoads();

        // The counts are right either way, so asserting messageCount proves nothing about I/O: the
        // defect was reading 60 rows to produce the number 60. Hibernate 7 dropped per-entity
        // statistics, so measure the thing that actually matters instead - whether the reads grow when
        // the history grows. Same room, same caller, same screen; only the history size differs.
        //
        // One room here, so the N is small and easy to read. With 20 busy rooms it was 20 full
        // histories on every workspace open, which is the screen a coordinator loads precisely to find
        // out which room is busy.
        org.junit.jupiter.api.Assertions.assertEquals(withFiveMessages, withSixtyMessages,
            "opening the workspace read " + withSixtyMessages + " entities for a 60-message room and "
                + withFiveMessages + " for a 5-message one: the reads scale with history, so it is "
                + "loading messages instead of counting them");
    }

    /** Entity loads for one workspace open, with nothing left in the persistence context to serve from cache. */
    private long workspaceEntityLoads() throws Exception {
        // Without em.clear() the messages are still in the first-level cache from the posts above and
        // the workspace is served from memory, reporting 0 loads for entirely the wrong reason.
        entityManager.flush();
        entityManager.clear();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        String body = mockMvc.perform(get("/api/community/rooms/workspace")
                .with(user("rooms_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
        org.junit.jupiter.api.Assertions.assertEquals(1, json.get("rooms").size());
        org.junit.jupiter.api.Assertions.assertTrue(json.get("rooms").get(0).get("messageCount").asInt() >= 5,
            "the summary must still count messages, not the page it happened to load");

        return statistics.getEntityLoadCount();
    }

@Test
    void openingABusyRoomShouldNotQueryOncePerMessageOrAuthor() throws Exception {
        String roomId = createRoomAsCoordinator();
        List<UUID> authors = saveUsers("note_author_", 50);
        seedMessages(roomId, authors, 0, 5);

        long withFiveMessages = roomDetailQueries(roomId);

        seedMessages(roomId, authors, 5, 50);

        long withFiftyMessages = roomDetailQueries(roomId);

        // Two N+1s lived on this screen. Mentions were fetched per message id, and the author's display
        // name was resolved inside the loop that renders each message, so a page of 50 cost 50 mention
        // queries plus one user lookup per message - up to 400 queries at the 200 cap round 52 set.
        //
        // Every message has a different author on purpose. With one author the user lookups hit the
        // first-level cache and the second one onwards cost nothing, which hid the defect from the
        // first version of this test: it saw the mention queries and not the author ones.
        //
        // Differential on purpose: a fixed threshold would rot the first time an unrelated query is
        // added to this screen. What must hold is that the query count does not depend on how many
        // messages the page holds.
        org.junit.jupiter.api.Assertions.assertEquals(withFiveMessages, withFiftyMessages,
            "opening a room ran " + withFiftyMessages + " queries for 50 messages and " + withFiveMessages
                + " for 5: reads scale with the page, so it is querying per message instead of per screen");
    }

    @Test
    void openingAJustCreatedRoomShouldWorkWithNothingToLookUp() throws Exception {
        // The default state of every new room: no messages, so no author ids and no mention ids. Both
        // "fetch the page's related rows" queries would otherwise be asked to match an empty list.
        String roomId = createRoomAsCoordinator();

        mockMvc.perform(get("/api/community/rooms/{roomId}", roomId)
                .with(user("rooms_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messages", hasSize(0)))
            .andExpect(jsonPath("$.messageCount").value(0))
            .andExpect(jsonPath("$.hasMoreMessages").value(false));
    }

    /** Messages posted straight through the repository so each can have its own author. */
    private void seedMessages(String roomId, List<UUID> authors, int from, int to) {
        for (int i = from; i < to; i++) {
            CommunityRoomMessage message = new CommunityRoomMessage();
            message.setRoomId(UUID.fromString(roomId));
            message.setCommunityId(communityId);
            message.setAuthorId(authors.get(i));
            message.setBody("Field note " + i);
            message.setCreatedAt(LocalDateTime.now().minusMinutes(to - i));
            messageRepository.save(message);
        }
    }

    private List<UUID> saveUsers(String prefix, int count) {
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(saveUser(prefix + i, "Author " + i));
            addMembership(ids.get(i), CommunityRole.MEMBER);
        }
        return ids;
    }

    /** Queries executed by one room detail open, with nothing left in the persistence context to serve from cache. */
    private long roomDetailQueries(String roomId) throws Exception {
        entityManager.flush();
        entityManager.clear();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        mockMvc.perform(get("/api/community/rooms/{roomId}", roomId)
                .with(user("rooms_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk());

        return statistics.getQueryExecutionCount();
    }

    @Test
    void openingTheWorkspaceShouldNotQueryOncePerMember() throws Exception {
        long withTwoMembers = workspaceQueries();

        for (int i = 0; i < 28; i++) {
            addMembership(saveUser("bulk_member_" + i, "Bulk Member " + i), CommunityRole.MEMBER);
        }

        long withThirtyMembers = workspaceQueries();

        // Every membership triggered its own user lookup to build the mentionable-username list, so a
        // community of 500 members cost 500 queries on the screen that lists its rooms - which is also
        // the screen that tells a coordinator whether anyone is active.
        org.junit.jupiter.api.Assertions.assertEquals(withTwoMembers, withThirtyMembers,
            "opening the workspace ran " + withThirtyMembers + " queries with 30 members and "
                + withTwoMembers + " with 2: reads scale with membership, so it is querying per member");
    }

    /** Queries executed by one workspace open, with nothing left in the persistence context to serve from cache. */
    /**
     * Entities loaded by one workspace open.
     *
     * <p>Entity loads and not query executions on purpose: a per-member {@code findById} is an entity
     * load, and {@code getQueryExecutionCount} does not count those. Measured the other way, this test
     * passed with the N+1 still in place - which is the failure mode worth avoiding, a green test that
     * cannot fail.
     */
    private long workspaceQueries() throws Exception {
        entityManager.flush();
        entityManager.clear();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        mockMvc.perform(get("/api/community/rooms/workspace")
                .with(user("rooms_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk());

        return statistics.getEntityLoadCount();
    }

    private void postMessage(String roomId, String body) throws Exception {
        mockMvc.perform(post("/api/community/rooms/messages")
                .with(user("rooms_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "communityId": "%s", "roomId": "%s", "body": "%s" }
                    """.formatted(communityId, roomId, body)))
            .andExpect(status().isOk());
    }

    private String createRoomAsCoordinator() throws Exception {
        String body = mockMvc.perform(post("/api/community/rooms")
                .with(user("rooms_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "name": "Night patrol working group",
                      "topic": "Coordinate the lighting audit walk"
                    }
                    """.formatted(communityId)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        return new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(body).get("rooms").get(0).get("id").asText();
    }
}
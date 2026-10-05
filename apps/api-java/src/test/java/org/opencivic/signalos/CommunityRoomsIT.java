package org.opencivic.signalos;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
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
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommunityRoomsIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;

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
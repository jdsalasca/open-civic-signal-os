package org.opencivic.signalos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SignalGeoMapIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private CommunityMembershipRepository membershipRepository;

    @Autowired
    private SignalRepository signalRepository;

    private UUID communityOneId;
    private UUID communityTwoId;
    private UUID outsiderCommunityId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        User user = new User("geo_map_user", "{noop}pw", "geo-map@test.dev", "ROLE_CITIZEN");
        user.setEnabled(true);
        user.setVerified(true);
        user = userRepository.save(user);
        userId = user.getId();

        Community communityOne = new Community();
        communityOne.setName("Santa Elena");
        communityOne.setSlug("santa-elena");
        communityOne.setDescription("Community one");
        communityOne = communityRepository.save(communityOne);
        communityOneId = communityOne.getId();

        Community communityTwo = new Community();
        communityTwo.setName("San Javier");
        communityTwo.setSlug("san-javier");
        communityTwo.setDescription("Community two");
        communityTwo = communityRepository.save(communityTwo);
        communityTwoId = communityTwo.getId();

        Community outsiderCommunity = new Community();
        outsiderCommunity.setName("Outsider");
        outsiderCommunity.setSlug("outsider");
        outsiderCommunity = communityRepository.save(outsiderCommunity);
        outsiderCommunityId = outsiderCommunity.getId();

        membershipRepository.save(membership(communityOneId));
        membershipRepository.save(membership(communityTwoId));

        signalRepository.save(signal(communityOneId, "Broken water pipe", "INFRASTRUCTURE", "NEW", 4.6102, -74.0811, 280.0, "Block A"));
        signalRepository.save(signal(communityOneId, "Unsafe crossing", "SAFETY", "IN_REVIEW", 4.6110, -74.0805, 190.0, "Main avenue"));
        signalRepository.save(signal(communityOneId, "No GPS issue", "SERVICES", "NEW", null, null, 120.0, "Indoor hall"));
        signalRepository.save(signal(communityTwoId, "Street light outage", "SAFETY", "NEW", 4.7031, -74.0321, 230.0, "North gate"));
        signalRepository.save(signal(outsiderCommunityId, "Should not be visible", "SAFETY", "NEW", 4.75, -74.01, 999.0, "Hidden"));
    }

    @Test
    @WithMockUser(username = "geo_map_user", roles = {"CITIZEN"})
    void returnsCommunityMapWithPointsClustersAndCoverage() throws Exception {
        mockMvc.perform(
                get("/api/signals/map")
                    .param("communityId", communityOneId.toString())
                    .param("status", "NEW,IN_REVIEW")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.communityId").value(communityOneId.toString()))
            .andExpect(jsonPath("$.communityName").value("Santa Elena"))
            .andExpect(jsonPath("$.mappedSignalsCount").value(2))
            .andExpect(jsonPath("$.unmappedSignalsCount").value(1))
            .andExpect(jsonPath("$.points.length()").value(2))
            .andExpect(jsonPath("$.clusters.length()").value(1))
            .andExpect(jsonPath("$.availableCategories.length()").value(3));
    }

    @Test
    @WithMockUser(username = "geo_map_user", roles = {"CITIZEN"})
    void returnsHeatMapAcrossMemberCommunitiesOnly() throws Exception {
        mockMvc.perform(get("/api/signals/map/heat"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visibleCommunitiesCount").value(2))
            .andExpect(jsonPath("$.totalMappedSignalsCount").value(3))
            .andExpect(jsonPath("$.communities.length()").value(2))
            .andExpect(jsonPath("$.communities[0].communityName").exists())
            .andExpect(jsonPath("$.communities[?(@.communityId=='" + outsiderCommunityId + "')]").isEmpty());
    }

    private CommunityMembership membership(UUID communityId) {
        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(userId);
        membership.setRole(CommunityRole.MEMBER);
        membership.setCreatedBy(userId);
        return membership;
    }

    private Signal signal(
        UUID communityId,
        String title,
        String category,
        String status,
        Double latitude,
        Double longitude,
        double priorityScore,
        String locationLabel
    ) {
        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setCommunityId(communityId);
        signal.setAuthorId(userId);
        signal.setTitle(title);
        signal.setDescription(title + " description");
        signal.setCategory(category);
        signal.setStatus(status);
        signal.setUrgency(4);
        signal.setImpact(4);
        signal.setAffectedPeople(12);
        signal.setPriorityScore(priorityScore);
        signal.setLocationLabel(locationLabel);
        signal.setLatitude(latitude);
        signal.setLongitude(longitude);
        signal.setCreatedAt(LocalDateTime.now().minusHours(2));
        return signal;
    }
}

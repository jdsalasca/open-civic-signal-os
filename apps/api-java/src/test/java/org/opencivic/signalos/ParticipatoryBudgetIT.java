package org.opencivic.signalos;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityProposal;
import org.opencivic.signalos.domain.CommunityProposalVoteMode;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityProposalRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * The proposal cost field is free text, so a budget built on it is only as good as the reading.
 *
 * <p>The property that matters: a selection that looks within budget while some proposals have no
 * readable amount is not within budget, it is unknown. The response has to say that rather than
 * reporting a total that quietly omits them.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:budget;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ParticipatoryBudgetIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private CommunityProposalRepository proposalRepository;

    private UUID communityId;
    private UUID coordinatorId;
    private UUID readableProposal;
    private UUID unreadableProposal;

    @BeforeEach
    void setUp() {
        User coordinator = new User("budget_coord", "encoded", "budget@example.com", "ROLE_CITIZEN");
        coordinator.setVerified(true);
        coordinator.setEnabled(true);
        coordinatorId = userRepository.save(coordinator).getId();

        Community community = new Community();
        community.setName("Riverside District");
        community.setSlug("riverside-district");
        community.setDescription("Participatory budget");
        communityId = communityRepository.save(community).getId();

        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(coordinatorId);
        membership.setRole(CommunityRole.COORDINATOR);
        membership.setCreatedBy(coordinatorId);
        membershipRepository.save(membership);

        readableProposal = proposal("Repair the water main", "USD 4000");
        unreadableProposal = proposal("Lighting study", "about 3k");
    }

    @Test
    void creatingABudgetShouldReadEveryProposalCost() throws Exception {
        String created = createBudget(1_000_000L, "USD");
        String budgetId = created.replaceAll("(?s).*\"budgetId\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/community/participatory-budgets/{id}", budgetId)
                .with(user("budget_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.allocations").value(2))
            // One cost is readable, one is not, and the count says so.
            .andExpect(jsonPath("$.unreadableCosts").value(1))
            .andExpect(jsonPath("$.totalFormatted").value("USD 10000.00"))
            .andExpect(jsonPath("$.allocationViews", hasSize(2)))
            // The unreadable one is first, because it is what needs attention.
            .andExpect(jsonPath("$.allocationViews[0].costReadable").value(false))
            .andExpect(jsonPath("$.allocationViews[0].rawCostText").value("about 3k"))
            .andExpect(jsonPath("$.allocationViews[0].amountMinor").doesNotExist())
            .andExpect(jsonPath("$.allocationViews[1].costReadable").value(true))
            .andExpect(jsonPath("$.allocationViews[1].amountFormatted").value("USD 4000.00"));
    }

    @Test
    void aSelectionThatFitsShouldReportTheRemainder() throws Exception {
        String budgetId = createBudgetId(1_000_000L, "USD");

        mockMvc.perform(post("/api/community/participatory-budgets/{id}/selection", budgetId)
                .with(user("budget_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"proposalIds": ["%s"]}
                    """.formatted(readableProposal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.withinBudget").value(true))
            .andExpect(jsonPath("$.includedTotalFormatted").value("USD 4000.00"))
            .andExpect(jsonPath("$.remainingFormatted").value("USD 6000.00"))
            .andExpect(jsonPath("$.interpretation").value(containsString("fits")));
    }

    @Test
    void aSelectionOverBudgetShouldReportTheOverspend() throws Exception {
        String budgetId = createBudgetId(100_000L, "USD");

        mockMvc.perform(post("/api/community/participatory-budgets/{id}/selection", budgetId)
                .with(user("budget_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"proposalIds": ["%s"]}
                    """.formatted(readableProposal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.withinBudget").value(false))
            .andExpect(jsonPath("$.remainingFormatted").value("USD -3000.00"))
            .andExpect(jsonPath("$.interpretation").value(containsString("exceeds the envelope")));
    }

    @Test
    void anUnreadableCostShouldMakeTheSelectionUnknownRatherThanConfirmed() throws Exception {
        String budgetId = createBudgetId(1_000_000L, "USD");

        // Selecting only the readable one still leaves an unreadable cost in the envelope, and the
        // response must not present the total as complete.
        mockMvc.perform(post("/api/community/participatory-budgets/{id}/selection", budgetId)
                .with(user("budget_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"proposalIds": ["%s"]}
                    """.formatted(readableProposal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unreadableCosts").value(1))
            .andExpect(jsonPath("$.interpretation").value(
                containsString("not fully costed")))
            .andExpect(jsonPath("$.interpretation").value(
                containsString("unknown rather than confirmed")))
            // And it shows what the parser was given.
            .andExpect(jsonPath("$.interpretation").value(
                containsString("shows the text the parser was given")));
    }

    @Test
    void theSimulationShouldRefuseToChoose() throws Exception {
        String budgetId = createBudgetId(1_000_000L, "USD");

        mockMvc.perform(get("/api/community/participatory-budgets/{id}", budgetId)
                .with(user("budget_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            // Which proposals to fund is the community's decision.
            .andExpect(jsonPath("$.interpretation").value(
                containsString("does NOT choose which proposals to fund")))
            .andExpect(jsonPath("$.interpretation").value(
                containsString("no standing to make it")));
    }

    @Test
    void selectingAgainShouldReplaceTheWholeSelection() throws Exception {
        String budgetId = createBudgetId(1_000_000L, "USD");

        mockMvc.perform(post("/api/community/participatory-budgets/{id}/selection", budgetId)
                .with(user("budget_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"proposalIds": ["%s"]}
                    """.formatted(readableProposal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.includedTotalFormatted").value("USD 4000.00"));

        // Replacing rather than toggling, so a caller cannot end up with a half-applied change.
        mockMvc.perform(post("/api/community/participatory-budgets/{id}/selection", budgetId)
                .with(user("budget_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"proposalIds": []}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.includedTotalFormatted").value("USD 0.00"));
    }

    @Test
    void aBudgetWithoutANameShouldBeRejected() throws Exception {
        mockMvc.perform(post("/api/community/participatory-budgets")
                .with(user("budget_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "name": "   ",
                      "totalBudgetMinor": 1000000,
                      "currency": "USD"
                    }
                    """.formatted(communityId)))
            .andExpect(status().isBadRequest())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .content().string(containsString("budget name is required")));
    }

    @Test
    void aNonPositiveEnvelopeShouldBeRejected() throws Exception {
        mockMvc.perform(post("/api/community/participatory-budgets")
                .with(user("budget_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "name": "Empty envelope",
                      "totalBudgetMinor": 0,
                      "currency": "USD"
                    }
                    """.formatted(communityId)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void anInvalidCurrencyShouldBeRejected() throws Exception {
        mockMvc.perform(post("/api/community/participatory-budgets")
                .with(user("budget_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "name": "Bad currency",
                      "totalBudgetMinor": 1000000,
                      "currency": "DOLLARS"
                    }
                    """.formatted(communityId)))
            .andExpect(status().isBadRequest())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .content().string(containsString("three-letter ISO code")));
    }

    @Test
    void aResidentWithoutTheScopeShouldNotCreateOrRead() throws Exception {
        User resident = new User("budget_resident", "encoded", "res@example.com", "ROLE_CITIZEN");
        resident.setVerified(true);
        resident.setEnabled(true);
        UUID residentId = userRepository.save(resident).getId();
        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(residentId);
        membership.setRole(CommunityRole.MEMBER);
        membership.setCreatedBy(coordinatorId);
        membershipRepository.save(membership);

        mockMvc.perform(post("/api/community/participatory-budgets")
                .with(user("budget_resident").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "name": "Not mine",
                      "totalBudgetMinor": 1000000,
                      "currency": "USD"
                    }
                    """.formatted(communityId)))
            .andExpect(status().isForbidden());
    }

    private String createBudget(long totalMinor, String currency) throws Exception {
        return mockMvc.perform(post("/api/community/participatory-budgets")
                .with(user("budget_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "name": "2026 neighbourhood fund",
                      "totalBudgetMinor": %d,
                      "currency": "%s"
                    }
                    """.formatted(communityId, totalMinor, currency)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
    }

    private String createBudgetId(long totalMinor, String currency) throws Exception {
        return createBudget(totalMinor, currency)
            .replaceAll("(?s).*\"budgetId\":\"([^\"]+)\".*", "$1");
    }

    private UUID proposal(String title, String estimatedCost) {
        CommunityProposal proposal = new CommunityProposal();
        proposal.setCommunityId(communityId);
        proposal.setAuthorId(coordinatorId);
        proposal.setTitle(title);
        proposal.setStatus("PROPOSED");
        proposal.setProblemStatement("Recorded for the budget test.");
        proposal.setProposedSolution("Recorded for the budget test.");
        proposal.setEstimatedCost(estimatedCost);
        proposal.setBeneficiariesSummary("Residents");
        proposal.setVoteMode(CommunityProposalVoteMode.YES_NO);
        proposal.setCreatedAt(LocalDateTime.now().minusDays(3));
        proposal.setUpdatedAt(LocalDateTime.now().minusDays(3));
        return proposalRepository.save(proposal).getId();
    }
}
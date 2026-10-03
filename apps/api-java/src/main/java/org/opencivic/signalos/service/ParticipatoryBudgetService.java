package org.opencivic.signalos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityProposal;
import org.opencivic.signalos.domain.ParticipatoryBudget;
import org.opencivic.signalos.domain.ParticipatoryBudgetAllocation;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityProposalRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.ParticipatoryBudgetAllocationRepository;
import org.opencivic.signalos.repository.ParticipatoryBudgetRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Participatory budgeting: allocate a fixed envelope across proposals and see whether it fits.
 *
 * <p>The platform does not hold money. It records what a community decided to allocate and reports
 * whether the arithmetic closes, which is a different and more useful thing than a payment system.
 *
 * <p>The hard part is that {@code estimatedCost} is free text. "USD 4000" and "about 3k" are both
 * plausible things a resident writes, and only one of them is an amount. So:
 *
 * <ul>
 *   <li><b>An unreadable cost is reported, never guessed.</b> A budget that silently invented a
 *       number would produce an allocation that looks decided and is not. The raw text travels with
 *       the finding so a reader can see what the parser was given.
 *   <li><b>Money is minor units in a long.</b> A double would round, and the rounding would surface
 *       in a council meeting rather than in a test.
 *   <li><b>The simulation does not choose.</b> It reports what a selection costs and whether it
 *       fits. Which proposals to fund is the community's decision, and the platform has no standing
 *       to make it.
 * </ul>
 */
@Service
public class ParticipatoryBudgetService {

    public static final String VERSION = "v1";

    private static final int MAX_ALLOCATIONS = 500;

    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;
    private final CommunityProposalRepository proposalRepository;
    private final ParticipatoryBudgetRepository budgetRepository;
    private final ParticipatoryBudgetAllocationRepository allocationRepository;

    public ParticipatoryBudgetService(
        CommunityRepository communityRepository,
        UserRepository userRepository,
        CommunityProposalRepository proposalRepository,
        ParticipatoryBudgetRepository budgetRepository,
        ParticipatoryBudgetAllocationRepository allocationRepository
    ) {
        this.communityRepository = communityRepository;
        this.userRepository = userRepository;
        this.proposalRepository = proposalRepository;
        this.budgetRepository = budgetRepository;
        this.allocationRepository = allocationRepository;
    }

    public record CreateBudgetRequest(
        UUID communityId,
        String name,
        long totalBudgetMinor,
        String currency
    ) {}

    /** One proposal's place in the envelope, with the cost as read and as written. */
    public record AllocationView(
        UUID proposalId,
        String title,
        String rawCostText,
        Long amountMinor,
        String amountFormatted,
        boolean costReadable,
        boolean included
    ) {}

    public record BudgetView(
        UUID budgetId,
        UUID communityId,
        String name,
        long totalBudgetMinor,
        String totalFormatted,
        String currency,
        int allocations,
        int unreadableCosts,
        long includedTotalMinor,
        String includedTotalFormatted,
        long remainingMinor,
        String remainingFormatted,
        boolean withinBudget,
        List<AllocationView> allocationViews,
        String interpretation,
        LocalDateTime createdAt
    ) {}

    @Transactional
    public BudgetView createBudget(CreateBudgetRequest request, String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        Community community = communityRepository.findById(request.communityId())
            .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + request.communityId()));

        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException(
                "A budget name is required. An unnamed envelope is unfindable when a community runs "
                    + "more than one exercise.");
        }
        if (request.totalBudgetMinor() <= 0) {
            throw new IllegalArgumentException(
                "totalBudgetMinor must be greater than 0, but was: " + request.totalBudgetMinor());
        }
        String currency = normaliseCurrency(request.currency());

        ParticipatoryBudget budget = new ParticipatoryBudget();
        budget.setId(UUID.randomUUID());
        budget.setCommunityId(community.getId());
        budget.setName(request.name().trim());
        budget.setTotalBudgetMinor(request.totalBudgetMinor());
        budget.setCurrency(currency);
        budget.setCreatedBy(user.getId());
        budget.setCreatedAt(LocalDateTime.now());
        budget = budgetRepository.save(budget);

        // Every proposal in the community gets an allocation row, with its cost read once. Reading it
        // lazily per request would let a proposal edited later change a past simulation.
        List<CommunityProposal> proposals =
            proposalRepository.findByCommunityIdOrderByUpdatedAtDescCreatedAtDesc(community.getId());
        if (proposals.size() > MAX_ALLOCATIONS) {
            throw new IllegalArgumentException(
                "This community has " + proposals.size() + " proposals, above the "
                    + MAX_ALLOCATIONS + " this envelope supports. Split the exercise.");
        }
        for (CommunityProposal proposal : proposals) {
            ParticipatoryBudgetAllocation allocation = new ParticipatoryBudgetAllocation();
            allocation.setId(UUID.randomUUID());
            allocation.setBudgetId(budget.getId());
            allocation.setProposalId(proposal.getId());
            allocation.setRawCostText(proposal.getEstimatedCost() == null ? "" : proposal.getEstimatedCost());
            allocation.setAmountMinor(
                CostTextParser.parseMinorUnits(proposal.getEstimatedCost(), currency).orElse(null));
            allocation.setIncluded(false);
            allocation.setCreatedAt(LocalDateTime.now());
            allocationRepository.save(allocation);
        }

        return view(budget);
    }

    /**
     * Marks which proposals are funded and reports whether the arithmetic closes.
     *
     * <p>Replaces the whole selection rather than toggling one at a time, so a caller cannot end up
     * with a half-applied change if something fails partway.
     */
    @Transactional
    public BudgetView select(UUID budgetId, UUID communityId, List<UUID> proposalIds, String username) {
        userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        ParticipatoryBudget budget = budgetRepository.findByIdAndCommunityId(budgetId, communityId)
            .orElseThrow(() -> new ResourceNotFoundException("Budget not found: " + budgetId));

        List<UUID> selected = proposalIds == null ? List.of() : proposalIds;
        List<ParticipatoryBudgetAllocation> allocations =
            allocationRepository.findByBudgetIdOrderByCreatedAtAsc(budgetId);

        for (ParticipatoryBudgetAllocation allocation : allocations) {
            allocation.setIncluded(selected.contains(allocation.getProposalId()));
            allocationRepository.save(allocation);
        }

        return view(budget);
    }

    @Transactional(readOnly = true)
    public BudgetView getBudget(UUID budgetId, UUID communityId, String username) {
        userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        ParticipatoryBudget budget = budgetRepository.findByIdAndCommunityId(budgetId, communityId)
            .orElseThrow(() -> new ResourceNotFoundException("Budget not found: " + budgetId));
        return view(budget);
    }

    @Transactional(readOnly = true)
    public List<BudgetView> listBudgets(UUID communityId, String username) {
        userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        return budgetRepository.findByCommunityIdOrderByCreatedAtDesc(communityId).stream()
            .map(this::view)
            .toList();
    }

    private BudgetView view(ParticipatoryBudget budget) {
        List<ParticipatoryBudgetAllocation> allocations =
            allocationRepository.findByBudgetIdOrderByCreatedAtAsc(budget.getId());

        List<AllocationView> views = new ArrayList<>();
        long includedTotal = 0;
        int unreadable = 0;

        for (ParticipatoryBudgetAllocation allocation : allocations) {
            CommunityProposal proposal = proposalRepository.findById(allocation.getProposalId()).orElse(null);
            if (proposal == null) {
                continue;
            }
            boolean readable = allocation.getAmountMinor() != null;
            if (!readable) {
                unreadable++;
            }
            if (allocation.isIncluded() && readable) {
                includedTotal += allocation.getAmountMinor();
            }
            views.add(new AllocationView(
                proposal.getId(),
                proposal.getTitle(),
                allocation.getRawCostText(),
                allocation.getAmountMinor(),
                readable ? format(allocation.getAmountMinor(), budget.getCurrency()) : null,
                readable,
                allocation.isIncluded()
            ));
        }

        // Unreadable costs first, then the largest, so the things needing attention are read first.
        views.sort(Comparator
            .comparing(AllocationView::costReadable)
            .thenComparing(view -> view.amountMinor() == null ? 0L : -view.amountMinor()));

        long remaining = budget.getTotalBudgetMinor() - includedTotal;
        boolean within = remaining >= 0;

        return new BudgetView(
            budget.getId(),
            budget.getCommunityId(),
            budget.getName(),
            budget.getTotalBudgetMinor(),
            format(budget.getTotalBudgetMinor(), budget.getCurrency()),
            budget.getCurrency(),
            views.size(),
            unreadable,
            includedTotal,
            format(includedTotal, budget.getCurrency()),
            remaining,
            format(remaining, budget.getCurrency()),
            within,
            views,
            interpretation(views.size(), unreadable, within, remaining, budget.getCurrency()),
            budget.getCreatedAt()
        );
    }

    /**
     * Says what the arithmetic means and what it does not.
     *
     * <p>An unreadable cost is the finding that matters: a selection that looks within budget while
     * three proposals have no readable amount is not within budget, it is unknown.
     */
    private String interpretation(
        int allocations,
        int unreadable,
        boolean within,
        long remaining,
        String currency
    ) {
        StringBuilder text = new StringBuilder();
        text.append("This reports what the selected proposals cost and whether the envelope covers them. ")
            .append("It does NOT choose which proposals to fund: that is the community's decision, and ")
            .append("the platform has no standing to make it. ");
        if (allocations == 0) {
            text.append("No proposals are in this envelope yet.");
            return text.toString();
        }
        if (unreadable > 0) {
            text.append(unreadable).append(" of ").append(allocations)
                .append(" proposal(s) have a cost that could not be read as an amount, so they are ")
                .append("excluded from the total. The selection is therefore not fully costed, and ")
                .append("whether it fits is unknown rather than confirmed. Each one shows the text the ")
                .append("parser was given.");
        }
        if (within) {
            text.append(" The selection fits, with ").append(format(remaining, currency))
                .append(" unallocated.");
        } else {
            text.append(" The selection exceeds the envelope by ")
                .append(format(-remaining, currency)).append(".");
        }
        return text.toString();
    }

    private String normaliseCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "USD";
        }
        String code = currency.trim().toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(code);
            return code;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                "currency must be a three-letter ISO code, but was: " + currency);
        }
    }

    /** Formats minor units for display, using the currency's own fraction digits. */
    private String format(long minorUnits, String currency) {
        int fractionDigits;
        try {
            fractionDigits = Currency.getInstance(currency).getDefaultFractionDigits();
        } catch (IllegalArgumentException ex) {
            fractionDigits = 2;
        }
        BigDecimal amount = BigDecimal.valueOf(minorUnits)
            .movePointLeft(fractionDigits)
            .setScale(Math.max(fractionDigits, 0), RoundingMode.UNNECESSARY);
        return currency + " " + amount.toPlainString();
    }

    /** Exposed so a caller can see the parser's contract without reading the source. */
    public Optional<Long> parseCost(String text, String currency) {
        return CostTextParser.parseMinorUnits(text, currency);
    }
}
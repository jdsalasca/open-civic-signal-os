package org.opencivic.signalos.web;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.ProposedWeights;
import org.opencivic.signalos.service.FormulaChangeProposalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Formula change proposals.
 *
 * <p>The preview is public and stores nothing: it is a pure computation over the current backlog, so
 * anyone can ask what a set of weights would do before it is even proposed. That is the transparency
 * half. Proposing and deciding are restricted to the platform owner, because the formula is
 * platform-wide and a scoring change is not a community-scoped act.
 */
@RestController
@RequestMapping("/api/formula-change-proposals")
public class FormulaChangeProposalController {

    private final FormulaChangeProposalService proposalService;

    public FormulaChangeProposalController(FormulaChangeProposalService proposalService) {
        this.proposalService = proposalService;
    }

    public record PreviewRequest(
        double urgencyMultiplier,
        double impactMultiplier,
        double affectedPeopleDivisor,
        double affectedPeopleCap,
        double communityVotesDivisor,
        double communityVotesCap,
        Integer limit
    ) {
        ProposedWeights toWeights() {
            return new ProposedWeights(
                urgencyMultiplier, impactMultiplier,
                affectedPeopleDivisor, affectedPeopleCap,
                communityVotesDivisor, communityVotesCap);
        }
    }

    public record ProposeRequest(
        String title,
        String rationale,
        double urgencyMultiplier,
        double impactMultiplier,
        double affectedPeopleDivisor,
        double affectedPeopleCap,
        double communityVotesDivisor,
        double communityVotesCap
    ) {
        ProposedWeights toWeights() {
            return new ProposedWeights(
                urgencyMultiplier, impactMultiplier,
                affectedPeopleDivisor, affectedPeopleCap,
                communityVotesDivisor, communityVotesCap);
        }
    }

    public record DecisionRequest(boolean approve, String note) {}

    /** Public and storing nothing: what would these weights do to the backlog that exists now. */
    @PostMapping("/preview")
    public FormulaChangeProposalService.ImpactPreview preview(@RequestBody PreviewRequest request) {
        return proposalService.preview(request.toWeights(), request.limit());
    }

    @PostMapping
    public FormulaChangeProposalService.ProposalView propose(
        @RequestBody ProposeRequest request,
        Principal principal
    ) {
        return proposalService.propose(
            request.title(), request.rationale(), request.toWeights(), principal.getName());
    }

    @GetMapping
    public List<FormulaChangeProposalService.ProposalView> list(
        @RequestParam(defaultValue = "false") boolean onlyUndecided
    ) {
        return proposalService.list(onlyUndecided);
    }

    @GetMapping("/{proposalId}")
    public FormulaChangeProposalService.ProposalView get(@PathVariable UUID proposalId) {
        return proposalService.get(proposalId);
    }

    @PostMapping("/{proposalId}/decision")
    public FormulaChangeProposalService.ProposalView decide(
        @PathVariable UUID proposalId,
        @RequestBody DecisionRequest request,
        Principal principal
    ) {
        return proposalService.decide(proposalId, request.approve(), request.note(), principal.getName());
    }
}
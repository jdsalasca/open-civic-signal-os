package org.opencivic.signalos.web;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.service.CommunityAccessService;
import org.opencivic.signalos.service.ParticipatoryBudgetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Participatory budgeting.
 *
 * <p>Gated on the open-data management scope, because creating an envelope and marking proposals as
 * funded is a governance act rather than a read.
 */
@RestController
@RequestMapping("/api/community/participatory-budgets")
public class ParticipatoryBudgetController {

    private final ParticipatoryBudgetService budgetService;
    private final CommunityAccessService communityAccessService;

    public ParticipatoryBudgetController(
        ParticipatoryBudgetService budgetService,
        CommunityAccessService communityAccessService
    ) {
        this.budgetService = budgetService;
        this.communityAccessService = communityAccessService;
    }

    public record SelectRequest(List<UUID> proposalIds) {}

    @PostMapping
    public ParticipatoryBudgetService.BudgetView createBudget(
        @RequestBody ParticipatoryBudgetService.CreateBudgetRequest request,
        Principal principal
    ) {
        requireScope(request.communityId(), principal);
        return budgetService.createBudget(request, principal.getName());
    }

    @PostMapping("/{budgetId}/selection")
    public ParticipatoryBudgetService.BudgetView select(
        @PathVariable UUID budgetId,
        @RequestParam UUID communityId,
        @RequestBody SelectRequest request,
        Principal principal
    ) {
        requireScope(communityId, principal);
        return budgetService.select(budgetId, communityId, request.proposalIds(), principal.getName());
    }

    @GetMapping("/{budgetId}")
    public ParticipatoryBudgetService.BudgetView getBudget(
        @PathVariable UUID budgetId,
        @RequestParam UUID communityId,
        Principal principal
    ) {
        requireScope(communityId, principal);
        return budgetService.getBudget(budgetId, communityId, principal.getName());
    }

    @GetMapping
    public List<ParticipatoryBudgetService.BudgetView> listBudgets(
        @RequestParam UUID communityId,
        Principal principal
    ) {
        requireScope(communityId, principal);
        return budgetService.listBudgets(communityId, principal.getName());
    }

    private void requireScope(UUID communityId, Principal principal) {
        var user = communityAccessService.getCurrentUser(principal.getName());
        communityAccessService.requireScope(
            user.getId(), communityId, CommunityPermissionScope.MANAGE_OPEN_DATA_EXPORTS);
    }
}
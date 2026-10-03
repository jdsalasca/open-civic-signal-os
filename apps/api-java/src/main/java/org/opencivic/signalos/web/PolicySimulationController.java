package org.opencivic.signalos.web;

import java.util.List;
import org.opencivic.signalos.domain.ProposedWeights;
import org.opencivic.signalos.service.PolicySimulationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Policy simulation sandbox.
 *
 * <p>Public and storing nothing, like the single-change preview it extends. A community arguing
 * about weights should be able to run the comparison itself rather than ask the platform owner to
 * run it for them, and a simulation that persisted would start to look like a decision.
 */
@RestController
@RequestMapping("/api/policy-simulation")
public class PolicySimulationController {

    private final PolicySimulationService simulationService;

    public PolicySimulationController(PolicySimulationService simulationService) {
        this.simulationService = simulationService;
    }

    public record ScenarioRequest(
        String name,
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

    public record SimulationRequest(List<ScenarioRequest> scenarios, Integer limit) {}

    @PostMapping
    public PolicySimulationService.SimulationReport simulate(@RequestBody SimulationRequest request) {
        List<PolicySimulationService.Scenario> scenarios = request.scenarios() == null
            ? List.of()
            : request.scenarios().stream()
                .map(scenario -> new PolicySimulationService.Scenario(
                    scenario.name(), scenario.toWeights()))
                .toList();
        return simulationService.simulate(scenarios, request.limit());
    }
}
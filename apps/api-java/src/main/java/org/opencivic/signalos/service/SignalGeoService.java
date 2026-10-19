package org.opencivic.signalos.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.web.dto.CommunitySignalClusterResponse;
import org.opencivic.signalos.web.dto.CommunitySignalHeatCellResponse;
import org.opencivic.signalos.web.dto.CommunitySignalMapPointResponse;
import org.opencivic.signalos.web.dto.CommunitySignalMapResponse;
import org.opencivic.signalos.web.dto.CommunitySignalsHeatMapResponse;
import org.opencivic.signalos.web.dto.SignalMapFiltersResponse;
import org.springframework.stereotype.Service;

@Service
public class SignalGeoService {
    private static final double CLUSTER_GRID_SIZE = 0.02d;

    private final SignalRepository signalRepository;
    private final CommunityRepository communityRepository;
    private final CommunityMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public SignalGeoService(
        SignalRepository signalRepository,
        CommunityRepository communityRepository,
        CommunityMembershipRepository membershipRepository,
        UserRepository userRepository
    ) {
        this.signalRepository = signalRepository;
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    public CommunitySignalMapResponse getCommunityMap(
        UUID communityId,
        String category,
        List<String> statuses,
        LocalDate fromDate,
        LocalDate toDate
    ) {
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));

        List<Signal> allSignals = signalRepository.findByCommunityId(communityId);
        List<Signal> filteredSignals = filterSignals(allSignals, category, statuses, fromDate, toDate);
        List<Signal> mappedSignals = filteredSignals.stream()
            .filter(this::hasCoordinates)
            .toList();

        SignalMapFiltersResponse filters = new SignalMapFiltersResponse(category, statuses, fromDate, toDate);
        LocalDateTime generatedAt = LocalDateTime.now();
        LocalDateTime freshnessAnchor = filteredSignals.stream()
            .map(Signal::getCreatedAt)
            .max(LocalDateTime::compareTo)
            .orElse(generatedAt);

        return new CommunitySignalMapResponse(
            community.getId(),
            community.getName(),
            community.getSlug(),
            generatedAt,
            freshness(freshnessAnchor),
            filters,
            collectCategories(allSignals),
            collectStatuses(allSignals),
            mappedSignals.size(),
            filteredSignals.size() - mappedSignals.size(),
            mappedSignals.stream().mapToDouble(this::heatWeight).sum(),
            mappedSignals.stream()
                .map(signal -> toPoint(signal, community.getName()))
                .sorted(Comparator.comparingDouble(CommunitySignalMapPointResponse::heatWeight).reversed())
                .toList(),
            buildClusters(mappedSignals, community)
        );
    }

    public CommunitySignalsHeatMapResponse getCommunityHeatMap(
        String username,
        boolean allowGlobal,
        String category,
        List<String> statuses,
        LocalDate fromDate,
        LocalDate toDate
    ) {
        List<Community> visibleCommunities = resolveVisibleCommunities(username, allowGlobal);
        if (visibleCommunities.isEmpty()) {
            LocalDateTime generatedAt = LocalDateTime.now();
            return new CommunitySignalsHeatMapResponse(
                generatedAt,
                freshness(generatedAt),
                new SignalMapFiltersResponse(category, statuses, fromDate, toDate),
                List.of(),
                List.of(),
                0,
                0,
                0,
                List.of()
            );
        }

        Map<UUID, Community> communitiesById = visibleCommunities.stream()
            .collect(Collectors.toMap(Community::getId, community -> community));
        List<Signal> visibleSignals = signalRepository.findByCommunityIdIn(communitiesById.keySet());
        List<Signal> filteredSignals = filterSignals(visibleSignals, category, statuses, fromDate, toDate);
        List<Signal> mappedSignals = filteredSignals.stream()
            .filter(this::hasCoordinates)
            .toList();
        Map<UUID, List<Signal>> signalsByCommunity = filteredSignals.stream()
            .filter(signal -> signal.getCommunityId() != null && communitiesById.containsKey(signal.getCommunityId()))
            .collect(Collectors.groupingBy(Signal::getCommunityId));

        LocalDateTime generatedAt = LocalDateTime.now();
        List<CommunitySignalHeatCellResponse> heatCells = new ArrayList<>();

        for (Community community : visibleCommunities) {
            List<Signal> communitySignals = signalsByCommunity.getOrDefault(community.getId(), List.of());
            List<Signal> communityMappedSignals = communitySignals.stream()
                .filter(this::hasCoordinates)
                .toList();
            if (communitySignals.isEmpty()) {
                continue;
            }
            Optional<Signal> topSignal = communityMappedSignals.stream()
                .max(Comparator.comparingDouble(this::heatWeight));
            heatCells.add(new CommunitySignalHeatCellResponse(
                community.getId(),
                community.getName(),
                community.getSlug(),
                averageLatitude(communityMappedSignals),
                averageLongitude(communityMappedSignals),
                communityMappedSignals.size(),
                communitySignals.size() - communityMappedSignals.size(),
                communityMappedSignals.stream().mapToDouble(this::heatWeight).sum(),
                communityMappedSignals.stream().mapToDouble(Signal::getPriorityScore).average().orElse(0),
                dominantCategory(communitySignals),
                topSignal.map(Signal::getId).orElse(null),
                topSignal.map(Signal::getTitle).orElse(null)
            ));
        }

        return new CommunitySignalsHeatMapResponse(
            generatedAt,
            freshness(
                filteredSignals.stream()
                    .map(Signal::getCreatedAt)
                    .max(LocalDateTime::compareTo)
                    .orElse(generatedAt)
            ),
            new SignalMapFiltersResponse(category, statuses, fromDate, toDate),
            collectCategories(visibleSignals),
            collectStatuses(visibleSignals),
            visibleCommunities.size(),
            mappedSignals.size(),
            mappedSignals.stream().mapToDouble(this::heatWeight).sum(),
            heatCells.stream()
                .sorted(Comparator.comparingDouble(CommunitySignalHeatCellResponse::cumulativeHeatScore).reversed())
                .toList()
        );
    }

    private List<Community> resolveVisibleCommunities(String username, boolean allowGlobal) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        List<CommunityMembership> memberships = membershipRepository.findByUserId(user.getId());
        if (!memberships.isEmpty()) {
            Map<UUID, Community> communitiesById = communityRepository.findAllById(
                memberships.stream().map(CommunityMembership::getCommunityId).toList()
            ).stream().collect(Collectors.toMap(Community::getId, community -> community));
            return memberships.stream()
                .map(CommunityMembership::getCommunityId)
                .distinct()
                .map(communitiesById::get)
                .filter(Objects::nonNull)
                .toList();
        }
        if (allowGlobal) {
            return communityRepository.findAll();
        }
        return List.of();
    }

    private List<Signal> filterSignals(
        Collection<Signal> signals,
        String category,
        List<String> statuses,
        LocalDate fromDate,
        LocalDate toDate
    ) {
        return signals.stream()
            .filter(signal -> category == null || category.isBlank() || category.equalsIgnoreCase(signal.getCategory()))
            .filter(signal -> statuses == null || statuses.isEmpty() || statuses.contains(signal.getStatus().toUpperCase()))
            .filter(signal -> fromDate == null || !signal.getCreatedAt().toLocalDate().isBefore(fromDate))
            .filter(signal -> toDate == null || !signal.getCreatedAt().toLocalDate().isAfter(toDate))
            .toList();
    }

    private boolean hasCoordinates(Signal signal) {
        return signal.getLatitude() != null && signal.getLongitude() != null;
    }

    private CommunitySignalMapPointResponse toPoint(Signal signal, String communityName) {
        return new CommunitySignalMapPointResponse(
            signal.getId(),
            signal.getCommunityId(),
            communityName,
            signal.getTitle(),
            signal.getCategory(),
            signal.getStatus(),
            signal.getLocationLabel(),
            signal.getLatitude(),
            signal.getLongitude(),
            signal.getPriorityScore(),
            heatWeight(signal),
            signal.getCreatedAt()
        );
    }

    private List<CommunitySignalClusterResponse> buildClusters(List<Signal> mappedSignals, Community community) {
        Map<String, List<Signal>> groups = mappedSignals.stream()
            .collect(Collectors.groupingBy(signal -> clusterKey(signal.getLatitude(), signal.getLongitude())));

        return groups.entrySet().stream()
            .map(entry -> {
                List<Signal> signals = entry.getValue();
                Signal topSignal = signals.stream()
                    .max(Comparator.comparingDouble(this::heatWeight))
                    .orElseThrow();
                return new CommunitySignalClusterResponse(
                    entry.getKey(),
                    community.getId(),
                    community.getName(),
                    averageLatitude(signals),
                    averageLongitude(signals),
                    signals.size(),
                    signals.stream().mapToDouble(Signal::getPriorityScore).sum(),
                    dominantCategory(signals),
                    topSignal.getId(),
                    topSignal.getTitle()
                );
            })
            .sorted(Comparator.comparingInt(CommunitySignalClusterResponse::signalCount).reversed()
                .thenComparing(Comparator.comparingDouble(CommunitySignalClusterResponse::cumulativePriorityScore).reversed()))
            .toList();
    }

    private String clusterKey(double latitude, double longitude) {
        long latCell = Math.round(Math.floor(latitude / CLUSTER_GRID_SIZE));
        long lngCell = Math.round(Math.floor(longitude / CLUSTER_GRID_SIZE));
        return latCell + ":" + lngCell;
    }

    private String dominantCategory(List<Signal> signals) {
        return signals.stream()
            .collect(Collectors.groupingBy(Signal::getCategory, Collectors.counting()))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("UNCATEGORIZED");
    }

    private List<String> collectCategories(Collection<Signal> signals) {
        return signals.stream()
            .map(Signal::getCategory)
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .toList();
    }

    private List<String> collectStatuses(Collection<Signal> signals) {
        return signals.stream()
            .map(Signal::getStatus)
            .filter(Objects::nonNull)
            .map(String::toUpperCase)
            .distinct()
            .sorted()
            .toList();
    }

    private double averageLatitude(List<Signal> signals) {
        return signals.stream().map(Signal::getLatitude).filter(Objects::nonNull).mapToDouble(Double::doubleValue).average().orElse(4.71099d);
    }

    private double averageLongitude(List<Signal> signals) {
        return signals.stream().map(Signal::getLongitude).filter(Objects::nonNull).mapToDouble(Double::doubleValue).average().orElse(-74.07209d);
    }

    private double heatWeight(Signal signal) {
        return signal.getPriorityScore() + Math.max(signal.getImpact(), signal.getUrgency()) * 10.0;
    }

    private String freshness(LocalDateTime timestamp) {
        long minutes = ChronoUnit.MINUTES.between(timestamp, LocalDateTime.now());
        if (minutes < 1) {
            return "updated just now";
        }
        if (minutes < 60) {
            return "updated " + minutes + "m ago";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return "updated " + hours + "h ago";
        }
        return "updated " + (hours / 24) + "d ago";
    }
}

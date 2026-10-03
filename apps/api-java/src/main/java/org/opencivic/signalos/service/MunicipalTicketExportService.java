package org.opencivic.signalos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.web.dto.MunicipalTicketExportResponse;
import org.opencivic.signalos.web.dto.MunicipalTicketFieldResponse;
import org.opencivic.signalos.web.dto.MunicipalTicketRecordResponse;
import org.opencivic.signalos.web.dto.MunicipalTicketUnmappedCategoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Municipal ticket export: the shape a city helpdesk can actually ingest.
 *
 * <p>The generic CSV export already exists and the municipal adapter is a translation problem,
 * not a new pipeline. Three things make it worth doing rather than telling a municipality to
 * write a parser:
 *
 * <ul>
 *   <li>The field mapping is <b>published</b>, not implied. A municipality can read
 *       {@code /municipal-tickets/field-map} and know what every column means before
 *       ingesting anything.
 *   <li>Category translation is <b>community-owned and explicit</b>. Cities run fixed service
 *       codes. Guessing "this looks like a pothole" from a free-text category would quietly
 *       misroute a citizen's complaint, so an unmapped category is reported and excluded
 *       rather than forced into a default bucket.
 *   <li>Every row carries a <b>stable citation</b> back to the platform signal, so a resident
 *       can point at the ticket and a city officer can point back at the evidence.
 * </ul>
 *
 * <p>Redaction runs here for the same reason it runs on open data: a municipal export leaves
 * the platform and lands in a procurement system with its own access rules.
 */
@Service
public class MunicipalTicketExportService {

    /**
     * The published field map. Order matters: it is the CSV column order, and a municipality
     * pins their importer to it. Adding a column is a breaking change to their parser, so it
     * goes in an ADR rather than a convenience edit.
     */
    private static final List<MunicipalTicketFieldResponse> FIELD_MAP = List.of(
        new MunicipalTicketFieldResponse("ticket_ref", "string",
            "Stable citation for this ticket. Quote it back to the platform to retrieve the source signal."),
        new MunicipalTicketFieldResponse("external_category_code", "string",
            "City service code this report was mapped to. Prefixed UNMAPPED. when the community has not mapped the category yet."),
        new MunicipalTicketFieldResponse("title", "string",
            "Report title, with personal contact details redacted."),
        new MunicipalTicketFieldResponse("description", "string",
            "Report body, with personal contact details redacted."),
        new MunicipalTicketFieldResponse("location", "string",
            "Free-text location label. Not coordinates: the precise location of a private home is a safety risk."),
        new MunicipalTicketFieldResponse("status", "string",
            "Lifecycle status in the platform."),
        new MunicipalTicketFieldResponse("priority_score", "decimal(5,2)",
            "Platform prioritisation score. NOT the city's own priority: provided as context only."),
        new MunicipalTicketFieldResponse("reported_at", "iso8601",
            "When the resident submitted the report."),
        new MunicipalTicketFieldResponse("source_channel", "string",
            "How the report entered the platform, for provenance."),
        new MunicipalTicketFieldResponse("source_ref", "string",
            "File and line when the report came from an import, for provenance."),
        new MunicipalTicketFieldResponse("platform_url", "string",
            "Link back to the public signal page.")
    );

    private static final String DEFAULT_CATEGORY_PREFIX = "UNMAPPED.";

    private final CommunityAccessService communityAccessService;
    private final CommunityRepository communityRepository;
    private final SignalRepository signalRepository;
    private final PublicDataAnonymizer anonymizer;
    private final String platformBaseUrl;

    public MunicipalTicketExportService(
        CommunityAccessService communityAccessService,
        CommunityRepository communityRepository,
        SignalRepository signalRepository,
        PublicDataAnonymizer anonymizer,
        @org.springframework.beans.factory.annotation.Value("${app.public-base-url:http://localhost:5173}") String platformBaseUrl
    ) {
        this.communityAccessService = communityAccessService;
        this.communityRepository = communityRepository;
        this.signalRepository = signalRepository;
        this.anonymizer = anonymizer;
        this.platformBaseUrl = platformBaseUrl.endsWith("/")
            ? platformBaseUrl.substring(0, platformBaseUrl.length() - 1)
            : platformBaseUrl;
    }

    @Transactional(readOnly = true)
    public List<MunicipalTicketFieldResponse> fieldMap() {
        return FIELD_MAP;
    }

    @Transactional(readOnly = true)
    public MunicipalTicketExportResponse export(
        UUID communityId,
        String username,
        Map<String, String> categoryMap,
        boolean includeUnmapped
    ) {
        User user = communityAccessService.getCurrentUser(username);
        communityAccessService.requireScope(user.getId(), communityId,
            org.opencivic.signalos.domain.CommunityPermissionScope.MANAGE_OPEN_DATA_EXPORTS);
        if (!communityRepository.existsById(communityId)) {
            throw new ResourceNotFoundException("Community not found for municipal export: " + communityId);
        }

        Map<String, String> normalisedMap = normaliseCategoryMap(categoryMap);

        List<Signal> signals = signalRepository.findByCommunityId(communityId).stream()
            .sorted(java.util.Comparator.comparing(Signal::getCreatedAt,
                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
            .toList();

        List<MunicipalTicketRecordResponse> tickets = new ArrayList<>();
        Map<String, Integer> unmappedCounts = new java.util.LinkedHashMap<>();
        Map<String, String> unmappedLabels = new java.util.LinkedHashMap<>();

        for (Signal signal : signals) {
            String category = signal.getCategory() == null
                ? "unspecified"
                : signal.getCategory().trim().toLowerCase(Locale.ROOT);
            String externalCode = normalisedMap.get(category);

            if (externalCode == null || externalCode.isBlank()) {
                unmappedCounts.merge(category, 1, Integer::sum);
                unmappedLabels.putIfAbsent(category,
                    signal.getCategory() == null ? "unspecified" : signal.getCategory());
                if (!includeUnmapped) {
                    // Excluded rather than coerced. A report filed against the wrong city service
                    // code reaches the wrong department and stops being chased. Callers who want
                    // everything anyway ask for includeUnmapped and get an explicit prefix.
                    continue;
                }
                externalCode = DEFAULT_CATEGORY_PREFIX + category.toUpperCase(Locale.ROOT).replace(' ', '_');
            }

            tickets.add(toTicket(signal, externalCode));
        }

        tickets.sort(java.util.Comparator.comparing(MunicipalTicketRecordResponse::ticketRef));

        List<MunicipalTicketUnmappedCategoryResponse> unmapped = new ArrayList<>();
        unmappedCounts.forEach((category, count) -> unmapped.add(new MunicipalTicketUnmappedCategoryResponse(
            category,
            unmappedLabels.get(category),
            count,
            "Set a code for this category in categoryMap to give it a real service code, or request "
                + "includeUnmapped to export it under the " + DEFAULT_CATEGORY_PREFIX + " prefix."
        )));

        return new MunicipalTicketExportResponse(
            communityId,
            fieldMap(),
            tickets,
            unmapped,
            tickets.size(),
            signals.size() - tickets.size(),
            normalisedMap
        );
    }

    private MunicipalTicketRecordResponse toTicket(Signal signal, String externalCode) {
        return new MunicipalTicketRecordResponse(
            ticketRef(signal),
            externalCode,
            anonymizer.redact(signal.getTitle()),
            anonymizer.redact(signal.getDescription()),
            anonymizer.redact(signal.getLocationLabel()),
            signal.getStatus(),
            score(signal),
            signal.getCreatedAt(),
            signal.getSourceChannel() == null ? null : signal.getSourceChannel().name(),
            signal.getSourceRef(),
            platformBaseUrl + "/signals/" + signal.getId()
        );
    }

/**
     * Stable citation, derived from the signal id. Deterministic on purpose: regenerating an
     * export must not invent new ticket references, or the city loses their history.
     */
    /**
     * Stable citation, derived from the signal id.
     *
     * <p>Delegates to {@link InstitutionalTicketReference} so the export and the handoff record
     * cannot disagree about the reference a community will quote back to a city.
     */
    private String ticketRef(Signal signal) {
        return InstitutionalTicketReference.forSignal(signal.getId());
    }

    private BigDecimal score(Signal signal) {
        return BigDecimal.valueOf(signal.getPriorityScore()).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, String> normaliseCategoryMap(Map<String, String> categoryMap) {
        if (categoryMap == null || categoryMap.isEmpty()) {
            return Map.of();
        }
        Map<String, String> normalised = new java.util.LinkedHashMap<>();
        categoryMap.forEach((key, value) -> {
            if (key != null && !key.isBlank()) {
                normalised.put(key.trim().toLowerCase(Locale.ROOT), value == null ? "" : value.trim());
            }
        });
        return Map.copyOf(normalised);
    }

    public String unmappedPrefix() {
        return DEFAULT_CATEGORY_PREFIX;
    }
}
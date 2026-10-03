package org.opencivic.signalos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.domain.IngestSource;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.SignalSourceChannel;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ConflictException;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.web.dto.IngestFieldError;
import org.opencivic.signalos.web.dto.IngestReportResponse;
import org.opencivic.signalos.web.dto.IngestRequest;
import org.opencivic.signalos.web.dto.IngestRowOutcome;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Validates an external export row by row and, on commit, persists the accepted rows as
 * signals carrying their ingest provenance.
 *
 * The contract is that a malformed row never fails the upload: every row gets an outcome and
 * a list of field errors, so a data steward fixes the source file instead of bisecting a
 * rejected batch. Nothing is persisted unless {@code commit} is true.
 */
@Service
public class IngestService {
    static final int MAX_ROWS = 2000;
    static final int PREVIEW_LENGTH = 120;
    static final int MIN_TITLE_LENGTH = 5;
    static final int MAX_TITLE_LENGTH = 150;
    static final int MIN_DESCRIPTION_LENGTH = 10;
    static final int MAX_DESCRIPTION_LENGTH = 2000;

    private static final String DEFAULT_CATEGORY = "uncategorized";

    private final CommunityAccessService communityAccessService;
    private final PrioritizationService prioritizationService;
    private final ObjectMapper objectMapper;

    public IngestService(
        CommunityAccessService communityAccessService,
        PrioritizationService prioritizationService,
        ObjectMapper objectMapper
    ) {
        this.communityAccessService = communityAccessService;
        this.prioritizationService = prioritizationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public IngestReportResponse ingest(IngestRequest request, String username) {
        if (request.communityId() == null) {
            throw new IllegalArgumentException("communityId is required to ingest an export.");
        }
        User user = communityAccessService.getCurrentUser(username);
        communityAccessService.requireScope(
            user.getId(), request.communityId(), CommunityPermissionScope.IMPORT_SIGNALS
        );

        IngestSource source = resolveSource(request.source());
        if (request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("content is required; send the export body as text.");
        }

        List<RawRow> rows = readRows(source, request.content());
        if (rows.size() > MAX_ROWS) {
            throw new ConflictException(
                "This export has %d rows, above the %d row limit. Split it and retry."
                    .formatted(rows.size(), MAX_ROWS)
            );
        }

        List<IngestRowOutcome> outcomes = new ArrayList<>();
        List<IngestFieldError> allErrors = new ArrayList<>();
        int accepted = 0;

        for (RawRow row : rows) {
            RowValidation validation = validateRow(row);
            if (!validation.errors().isEmpty()) {
                allErrors.addAll(validation.errors());
                outcomes.add(new IngestRowOutcome(
                    row.lineNumber(), false, preview(row.text()), null,
                    sourceRef(source, request.fileName(), row.lineNumber()), validation.errors()
                ));
                continue;
            }
            accepted++;
            outcomes.add(new IngestRowOutcome(
                row.lineNumber(), true, preview(row.text()), null,
                sourceRef(source, request.fileName(), row.lineNumber()), List.of()
            ));
        }

        List<IngestRowOutcome> committedOutcomes = outcomes;
        if (request.commit()) {
            committedOutcomes = commitAccepted(request, source, rows, outcomes, username);
        }

        return new IngestReportResponse(
            source.name(),
            request.fileName() == null ? "" : request.fileName(),
            sha256(request.content()),
            rows.size(),
            accepted,
            rows.size() - accepted,
            request.commit(),
            detectedColumns(source, request.content()),
            committedOutcomes,
            allErrors
        );
    }

    /**
     * Only clean rows are persisted. A row that failed validation is never written, so a
     * partially bad upload produces the good subset plus a full error report.
     */
    private List<IngestRowOutcome> commitAccepted(
        IngestRequest request,
        IngestSource source,
        List<RawRow> rows,
        List<IngestRowOutcome> outcomes,
        String username
    ) {
        List<IngestRowOutcome> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            RawRow row = rows.get(i);
            IngestRowOutcome outcome = outcomes.get(i);
            if (!outcome.valid()) {
                result.add(outcome);
                continue;
            }

            RowValidation validation = validateRow(row);
            Signal signal = prioritizationService.createSignal(
                buildTitle(validation),
                validation.description(),
                validation.category(),
                validation.urgency(),
                validation.impact(),
                validation.affectedPeople(),
                null,
                null,
                List.of(),
                null,
                null,
                sourceChannel(source).name(),
                outcome.sourceRef(),
                username,
                request.communityId()
            );
            result.add(new IngestRowOutcome(
                outcome.rowIndex(), true, outcome.preview(), signal.getId(),
                outcome.sourceRef(), List.of()
            ));
        }
        return result;
    }

    private List<RawRow> readRows(IngestSource source, String content) {
        return switch (source) {
            case CSV_EXPORT -> {
                IngestCsvReader.Parsed parsed = IngestCsvReader.parse(content);
                List<RawRow> out = new ArrayList<>();
                for (IngestCsvReader.Row row : parsed.rows()) {
                    out.add(new RawRow(
                        row.lineNumber(),
                        csvToMap(parsed.header(), row.values()),
                        // A short or long row would otherwise shift every later value into
                        // the wrong field, so the mismatch is itself a validation error.
                        row.values().size() == parsed.header().size() ? null : parsed.header().size()
                    ));
                }
                yield out;
            }
            case WHATSAPP_EXPORT, TELEGRAM_EXPORT -> {
                List<IngestChatExportReader.Message> messages;
                if (looksLikeJson(content)) {
                    messages = IngestChatExportReader.readJson(content, objectMapper);
                } else {
                    messages = IngestChatExportReader.readCsvText(content);
                }
                List<RawRow> out = new ArrayList<>();
                for (IngestChatExportReader.Message message : messages) {
                    Map<String, String> map = new LinkedHashMap<>();
                    map.put("text", message.text());
                    map.put("sender", message.sender());
                    map.put("timestamp", message.timestamp());
                    out.add(new RawRow(message.lineNumber(), map, null));
                }
                yield out;
            }
        };
    }

    private RowValidation validateRow(RawRow row) {
        List<IngestFieldError> errors = new ArrayList<>();
        Map<String, String> map = row.values();

        if (row.expectedColumnCount() != null) {
            errors.add(error(row.lineNumber(), "*", "COLUMN_COUNT_MISMATCH",
                "Row has a different number of columns than the header. Values would land in the wrong "
                    + "fields, so this row is rejected rather than shifted."));
        }

        String text = firstNonBlank(map, "title", "text", "message", "body", "subject", "description");
        String description = firstNonBlank(map, "description", "body", "message", "text");
        String category = firstNonBlank(map, "category");
        String sender = firstNonBlank(map, "sender", "from", "author");

        if (text == null || text.length() < MIN_TITLE_LENGTH) {
            errors.add(error(row.lineNumber(), "title", "REQUIRED",
                "A report needs at least %d characters of text.".formatted(MIN_TITLE_LENGTH)));
        } else if (text.length() > MAX_TITLE_LENGTH) {
            errors.add(error(row.lineNumber(), "title", "TOO_LONG",
                "Text is %d characters, above the %d limit.".formatted(text.length(), MAX_TITLE_LENGTH)));
        }

        String effectiveDescription = description == null ? text : description;
        if (effectiveDescription == null || effectiveDescription.length() < MIN_DESCRIPTION_LENGTH) {
            errors.add(error(row.lineNumber(), "description", "REQUIRED",
                "A report needs at least %d characters of context."
                    .formatted(MIN_DESCRIPTION_LENGTH)));
        } else if (effectiveDescription.length() > MAX_DESCRIPTION_LENGTH) {
            errors.add(error(row.lineNumber(), "description", "TOO_LONG",
                "Context is %d characters, above the %d limit."
                    .formatted(effectiveDescription.length(), MAX_DESCRIPTION_LENGTH)));
        }

        int urgency = readBoundedInt(row, map, "urgency", 1, 5, 1, errors);
        int impact = readBoundedInt(row, map, "impact", 1, 5, 1, errors);
        int affectedPeople = readBoundedInt(row, map, "affectedPeople", 0, 100_000_000, 0, errors);

        String contextSuffix = sender == null ? "" : " Reported by " + sender + ".";
        String finalDescription = effectiveDescription == null
            ? null
            : effectiveDescription + contextSuffix;

        return new RowValidation(
            errors, text, finalDescription,
            category == null ? DEFAULT_CATEGORY : category.trim().toLowerCase(Locale.ROOT),
            urgency, impact, affectedPeople
        );
    }

    private int readBoundedInt(
        RawRow row,
        Map<String, String> map,
        String field,
        int min,
        int max,
        int fallback,
        List<IngestFieldError> errors
    ) {
        String raw = firstNonBlank(map, field);
        if (raw == null) {
            return fallback;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            errors.add(error(row.lineNumber(), field, "NOT_A_NUMBER",
                "'" + raw + "' is not a whole number."));
            return fallback;
        }
        if (parsed < min || parsed > max) {
            errors.add(error(row.lineNumber(), field, "OUT_OF_RANGE",
                "%d is outside the allowed range %d-%d.".formatted(parsed, min, max)));
            return fallback;
        }
        return parsed;
    }

    private String buildTitle(RowValidation validation) {
        String title = validation.title() == null ? "Untitled report" : validation.title();
        return title.length() > MAX_TITLE_LENGTH ? title.substring(0, MAX_TITLE_LENGTH) : title;
    }

    private List<String> detectedColumns(IngestSource source, String content) {
        if (source != IngestSource.CSV_EXPORT) {
            return List.of("text", "sender", "timestamp");
        }
        try {
            return IngestCsvReader.parse(content).header();
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    private Map<String, String> csvToMap(List<String> header, List<String> values) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < header.size(); i++) {
            String key = header.get(i).trim();
            if (key.isEmpty()) {
                continue;
            }
            map.put(key, i < values.size() ? values.get(i) : null);
        }
        return map;
    }

    private IngestSource resolveSource(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("source is required.");
        }
        try {
            return IngestSource.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown ingest source: " + raw);
        }
    }

    private SignalSourceChannel sourceChannel(IngestSource source) {
        return switch (source) {
            case CSV_EXPORT -> SignalSourceChannel.CSV_IMPORT;
            case WHATSAPP_EXPORT, TELEGRAM_EXPORT -> SignalSourceChannel.CHAT_EXPORT;
        };
    }

    private String sourceRef(IngestSource source, String fileName, int lineNumber) {
        String base = fileName == null || fileName.isBlank() ? "upload" : fileName.trim();
        return base + "#" + lineNumber;
    }

    private static boolean looksLikeJson(String content) {
        String trimmed = content.trim();
        return trimmed.startsWith("{") || trimmed.startsWith("[");
    }

    private String preview(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= PREVIEW_LENGTH ? text : text.substring(0, PREVIEW_LENGTH - 3) + "...";
    }

    private static String firstNonBlank(Map<String, String> map, String... keys) {
        for (String key : keys) {
            String value = map.get(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static IngestFieldError error(int rowIndex, String field, String code, String message) {
        return new IngestFieldError(rowIndex, field, code, message);
    }

    static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash ingest content", e);
        }
    }

    private record RawRow(int lineNumber, Map<String, String> values, Integer expectedColumnCount) {
        String text() {
            return firstNonBlank(values, "title", "text", "message", "body", "subject", "description");
        }
    }

    private record RowValidation(
        List<IngestFieldError> errors,
        String title,
        String description,
        String category,
        int urgency,
        int impact,
        int affectedPeople
    ) {}
}
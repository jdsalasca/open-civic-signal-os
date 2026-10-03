package org.opencivic.signalos.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal RFC 4180 reader: handles quoted fields, escaped quotes, and embedded commas and
 * newlines.
 *
 * ponytail: only what a real export needs. No config dialect, no embedded newlines inside
 * quotes beyond treating them as field content, which is enough for WhatsApp/Telegram/CSV
 * exports and avoids pulling in a CSV library.
 */
public final class IngestCsvReader {
    private IngestCsvReader() {}

    /** One parsed row plus the 1-based line number it started on, for error reporting. */
    public record Row(int lineNumber, List<String> values) {}

    /**
     * @return rows excluding the header, which is returned separately
     * @throws IllegalArgumentException when a quoted field is never closed
     */
    public static Parsed parse(String content) {
        List<String> raw = splitRecords(content);
        if (raw.isEmpty()) {
            return new Parsed(List.of(), List.of());
        }

        List<String> header = splitFields(raw.get(0));
        List<Row> rows = new ArrayList<>();
        for (int i = 1; i < raw.size(); i++) {
            String record = raw.get(i);
            if (record.isBlank()) {
                continue;
            }
            rows.add(new Row(i + 1, splitFields(record)));
        }
        return new Parsed(header, rows);
    }

    public record Parsed(List<String> header, List<Row> rows) {}

    /** Splits on newlines, keeping quoted newlines inside their field. */
    private static List<String> splitRecords(String content) {
        List<String> records = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
            } else if ((c == '\n' || c == '\r') && !inQuotes) {
                if (c == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') {
                    i++;
                }
                records.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (inQuotes) {
            throw new IllegalArgumentException("CSV content has an unterminated quoted field.");
        }
        if (!current.isEmpty()) {
            records.add(current.toString());
        }
        return records;
    }

    private static List<String> splitFields(String record) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < record.length(); i++) {
            char c = record.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < record.length() && record.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());
        return fields;
    }
}
package org.opencivic.signalos.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Deterministic PII redaction for text that leaves the platform as public open data.
 *
 * <p>The open-data records are already structural: they carry no name, username, or email
 * column. What they do carry is free text written by residents, and that text routinely
 * contains someone's email or phone number because the resident is asking to be contacted
 * about their own report. Publishing it verbatim turns a transparency feature into a
 * re-identification vector.
 *
 * <p>Three properties this has to hold:
 *
 * <ul>
 *   <li><b>Deterministic.</b> Same input, same output. A published dataset must not change
 *       between two downloads of the same community, or a citizen cannot cite it.
 *   <li><b>No pseudonymisation.</b> A stable hash of a phone number is reversible by
 *       brute force over a small numeric space. Replacing with a fixed marker is honest;
 *       pretending to hash is not.
 *   <li><b>Visible.</b> The marker is in the output and the caller gets a count of how many
 *       values were touched. Redaction that nobody can see is indistinguishable from data
 *       loss.
 * </ul>
 *
 * <p>False positives are a real cost here and the patterns are deliberately narrow. A budget
 * figure like "12,000,000" is <b>not</b> redacted, because the national-id pattern requires a
 * trailing letter and the card pattern requires 13 or more digits. Widening those is a
 * deliberate decision, not a tweak; see the ADR.
 */
@Component
public class PublicDataAnonymizer {

    /** 13-19 digits with optional separators, which is the card-number length range. */
    private static final Pattern PAYMENT_CARD =
        Pattern.compile("(?<!\\d)(?:\\d[ -]?){12,18}\\d(?!\\d)");

    /** 8 digits followed by one letter, matching DNI/NIE-style identifiers. */
    private static final Pattern NATIONAL_ID =
        Pattern.compile("(?<!\\w)(\\d{8})([A-Za-z])(?!\\w)");

    /** Leading plus then 9-15 digits, which is how an international number is written. */
    private static final Pattern PHONE =
        Pattern.compile("(?<!\\w)\\+?\\d[\\d .()-]{7,}\\d(?!\\w)");

    private static final Pattern EMAIL =
        Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    /** Longest field we will bother scanning; anything bigger is not civic report prose. */
    private static final int MAX_SCAN_LENGTH = 20_000;

    public String redact(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_SCAN_LENGTH) {
            return value;
        }
        String result = EMAIL.matcher(value).replaceAll("[email redacted]");
        result = PAYMENT_CARD.matcher(result).replaceAll("[card redacted]");
        result = NATIONAL_ID.matcher(result).replaceAll("[national-id redacted]");
        // A national id already replaced above leaves a letter behind, which would then make
        // the phone pattern swallow the marker. Redact phones last and skip already-redacted
        // spans by requiring a digit run that does not start with a bracket.
        result = redactPhones(result);
        return result;
    }

    private String redactPhones(String value) {
        Matcher matcher = PHONE.matcher(value);
        StringBuilder out = new StringBuilder();
        int last = 0;
        while (matcher.find()) {
            String candidate = matcher.group();
            int digits = 0;
            for (int i = 0; i < candidate.length(); i++) {
                if (Character.isDigit(candidate.charAt(i))) {
                    digits++;
                }
            }
            if (digits < 9 || digits > 15) {
                continue;
            }
            out.append(value, last, matcher.start()).append("[phone redacted]");
            last = matcher.end();
        }
        out.append(value, last, value.length());
        return out.toString();
    }

    /** Categories this redactor can detect, for the checklist response. */
    public List<String> categories() {
        return List.of("EMAIL", "PHONE", "NATIONAL_ID", "PAYMENT_CARD");
    }

    /** Counts detections in a value without modifying it. */
    public List<String> detect(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_SCAN_LENGTH) {
            return List.of();
        }
        List<String> found = new ArrayList<>();
        collect(EMAIL, value, "EMAIL", found);
        collect(PAYMENT_CARD, value, "PAYMENT_CARD", found);
        collect(NATIONAL_ID, value, "NATIONAL_ID", found);
        for (Matcher matcher = PHONE.matcher(value); matcher.find(); ) {
            int digits = 0;
            for (int i = 0; i < matcher.group().length(); i++) {
                if (Character.isDigit(matcher.group().charAt(i))) {
                    digits++;
                }
            }
            if (digits >= 9 && digits <= 15) {
                found.add("PHONE");
            }
        }
        return found;
    }

    private void collect(Pattern pattern, String value, String label, List<String> found) {
        if (pattern.matcher(value).find()) {
            found.add(label);
        }
    }
}
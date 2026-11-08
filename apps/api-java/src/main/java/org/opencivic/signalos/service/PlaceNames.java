package org.opencivic.signalos.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/**
 * One place to normalize a city, locality or neighborhood before it is stored.
 *
 * <p>Two people writing "Bogotá", "BOGOTA" and "  bogota " mean the same place. Display keeps the
 * readable form ("Bogotá"); the key strips accents, case and punctuation so the duplicate check
 * compares meaning, not spelling.
 */
public final class PlaceNames {

    private static final Set<String> CONNECTORS = Set.of(
        "de", "del", "la", "las", "los", "y", "e", "da", "das", "do", "dos"
    );

    private PlaceNames() {}

    /** Trimmed, single-spaced, Title Case, or null when the input is blank. */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().replaceAll("\\s+", " ");
        if (value.isEmpty()) {
            return null;
        }
        if (value.length() > 120) {
            throw new IllegalArgumentException("Place name is too long (max 120 characters).");
        }
        if (!value.matches("[\\p{L}\\p{N} .,'’()#&-]+")) {
            throw new IllegalArgumentException("Place name contains unsupported characters.");
        }
        if (!value.matches(".*\\p{L}.*")) {
            throw new IllegalArgumentException("Place name must contain at least one letter.");
        }
        return titleCase(value);
    }

    /** Stable, accent-insensitive key; null when there is no place at all. */
    public static String key(String city, String locality, String neighborhood) {
        String cityPart = slug(city);
        String localityPart = slug(locality);
        String neighborhoodPart = slug(neighborhood);
        if (cityPart.isEmpty() && localityPart.isEmpty() && neighborhoodPart.isEmpty()) {
            return null;
        }
        return cityPart + "|" + localityPart + "|" + neighborhoodPart;
    }

    public static String slug(String raw) {
        if (raw == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(raw, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
        return withoutAccents.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-+|-+$", "");
    }

    private static String titleCase(String value) {
        String[] words = value.split(" ");
        StringBuilder out = new StringBuilder();
        for (int index = 0; index < words.length; index++) {
            if (index > 0) {
                out.append(' ');
            }
            String word = words[index];
            String lower = word.toLowerCase(Locale.ROOT);
            if (index > 0 && CONNECTORS.contains(lower)) {
                out.append(lower);
            } else {
                out.append(capitalizeSegments(lower));
            }
        }
        return out.toString();
    }

    // Capitalizes the start of each dot/hyphen segment so "d.c." becomes "D.C." and
    // "villa-nueva" becomes "Villa-Nueva".
    private static String capitalizeSegments(String word) {
        StringBuilder out = new StringBuilder();
        boolean capitalizeNext = true;
        for (char character : word.toCharArray()) {
            if (character == '.' || character == '-') {
                out.append(character);
                capitalizeNext = true;
            } else if (Character.isLetter(character)) {
                out.append(capitalizeNext ? Character.toUpperCase(character) : character);
                capitalizeNext = false;
            } else {
                out.append(character);
                capitalizeNext = false;
            }
        }
        return out.toString();
    }
}

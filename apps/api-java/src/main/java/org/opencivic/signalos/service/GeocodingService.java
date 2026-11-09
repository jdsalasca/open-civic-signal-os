package org.opencivic.signalos.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Coordinates to a human place, using OpenStreetMap's Nominatim. Nothing is hosted here and no key
 * is required; the public service asks for an identifiable User-Agent and at most one request per
 * second, so results are cached by rounded coordinates and a second caller is asked to retry.
 */
@Service
public class GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);
    private static final String ENDPOINT = "https://nominatim.openstreetmap.org/reverse";
    private static final String USER_AGENT =
        "OpenCivicSignalOS/1.0 (https://open-comunity.eridu.top)";
    private static final long MIN_INTERVAL_MS = 1000;
    private static final int CACHE_LIMIT = 500;

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, PlaceSuggestion> cache = new LinkedHashMap<>(CACHE_LIMIT, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, PlaceSuggestion> eldest) {
            return size() > CACHE_LIMIT;
        }
    };
    private long nextAllowedAt = 0;

    public Optional<PlaceSuggestion> reverse(double latitude, double longitude) {
        String key = String.format(java.util.Locale.ROOT, "%.4f,%.4f", latitude, longitude);
        synchronized (cache) {
            PlaceSuggestion cached = cache.get(key);
            if (cached != null) {
                return Optional.of(cached);
            }
            long now = System.currentTimeMillis();
            if (now < nextAllowedAt) {
                return Optional.empty();
            }
            nextAllowedAt = now + MIN_INTERVAL_MS;
        }

        String url = ENDPOINT
            + "?format=jsonv2&addressdetails=1&accept-language=es"
            + "&lat=" + latitude + "&lon=" + longitude;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(6))
            .header("User-Agent", USER_AGENT)
            .GET()
            .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Nominatim answered {} for {}", response.statusCode(), key);
                return Optional.empty();
            }
            PlaceSuggestion place = parse(response.body());
            synchronized (cache) {
                cache.put(key, place);
            }
            return Optional.of(place);
        } catch (Exception ex) {
            log.warn("Reverse geocoding failed for {}: {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    PlaceSuggestion parse(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode address = root.path("address");
        return new PlaceSuggestion(
            clean(firstOf(address, "city", "town", "municipality", "county", "state")),
            clean(firstOf(address, "suburb", "city_district", "district", "county")),
            clean(firstOf(address, "neighbourhood", "quarter", "suburb", "city_district")),
            firstOf(address, "country"),
            firstOf(address, "state", "region"),
            text(root, "display_name")
        );
    }

    /** "Localidad Teusaquillo" -> "Teusaquillo"; "Bogotá ciudad" -> "Bogotá". */
    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value
            .replaceFirst("(?i)^(localidad|comuna|municipio de|upzs? de)\\s+", "")
            .replaceFirst("(?i)\\s+ciudad$", "")
            .trim();
        return cleaned.isEmpty() ? value : cleaned;
    }

    private String firstOf(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText().trim();
        return text.isEmpty() ? null : text;
    }

    public record PlaceSuggestion(
        String city,
        String locality,
        String neighborhood,
        String country,
        String region,
        String displayName
    ) {}
}

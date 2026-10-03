package org.opencivic.signalos.web.dto;

import java.util.List;

/**
 * One measured figure for the period, with its previous-period value attached.
 *
 * <p>A number in a transparency report is only meaningful next to the number before it.
 * "Median resolution: 19 days" invites "compared to when?". Carrying the delta on the figure
 * itself means a renderer cannot accidentally publish the trendless half.
 */
public record TransparencyMetricResponse(
    String key,
    String label,
    long value,
    String unit,
    Long previousValue,
    Long delta,
    String direction
) {
    public static TransparencyMetricResponse of(String key, String label, long value, String unit, long previousValue) {
        long delta = value - previousValue;
        String direction = delta > 0 ? "UP" : delta < 0 ? "DOWN" : "FLAT";
        return new TransparencyMetricResponse(key, label, value, unit, previousValue, delta, direction);
    }

    public static TransparencyMetricResponse of(String key, String label, long value, String unit) {
        return new TransparencyMetricResponse(key, label, value, unit, null, null, "BASELINE");
    }
}
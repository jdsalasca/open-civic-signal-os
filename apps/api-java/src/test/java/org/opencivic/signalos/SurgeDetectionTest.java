package org.opencivic.signalos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.service.SurgeDetectionService;

/**
 * The verdict logic, tested without a database. A surge badge is only trustworthy if a human
 * can check the arithmetic, so these tests pin the arithmetic rather than the plumbing.
 */
class SurgeDetectionTest {

    private static final SurgeDetectionService.Thresholds THRESHOLDS =
        new SurgeDetectionService.Thresholds(4, 5, 2.0);

    private static SurgeDetectionService.SurgeZone assess(long current, long baselineMedian) {
        return SurgeDetectionService.assess(
            "c1", "Riverside", "utilities", current, baselineMedian,
            List.of(new SurgeDetectionService.WindowCounts(
                LocalDate.parse("2026-02-01"), LocalDate.parse("2026-02-08"), baselineMedian)),
            THRESHOLDS);
    }

    @Test
    void shouldFlagADoublingAtOrAboveThreshold() {
        var zone = assess(12, 6);

        assertThat(zone.verdict()).isEqualTo("SURGE");
        assertThat(zone.ratio()).isEqualTo(2.0);
        assertThat(zone.reason()).contains("2.0x").contains("threshold");
    }

    @Test
    void shouldSuppressAQuietZoneTriplingFromNothing() {
        // One report to three is a 300% increase and not a surge. Without the floor, a quiet
        // neighbourhood's dashboard fills with badges and people learn to ignore them.
        var zone = assess(3, 1);

        assertThat(zone.verdict()).isEqualTo("SUPPRESSED_LOW_VOLUME");
        assertThat(zone.reason()).contains("below the minimum of 5");
    }

    @Test
    void shouldFlagALargeSurgeWithNoBaselineAtAll() {
        var zone = assess(8, 0);

        assertThat(zone.verdict()).isEqualTo("SURGE");
        // Infinity is right for the ratio but must never reach a rendered report.
        assertThat(zone.reason()).contains("no-baseline");
    }

    @Test
    void shouldNotFlagAStableZone() {
        var zone = assess(7, 6);

        assertThat(zone.verdict()).isEqualTo("STABLE");
        assertThat(zone.reason()).contains("below the 2.0x threshold");
    }

    @Test
    void medianShouldSurviveTheOutlierWindowThatAmeanWouldAbsorb() {
        // The spike is exactly what we are detecting. A mean baseline would be dragged up by it
        // and report a smaller surge the bigger the surge got.
        List<Long> windows = List.of(2L, 3L, 2L, 40L);

        assertThat(SurgeDetectionService.median(windows)).isEqualTo(3L);
    }

    @Test
    void medianShouldAverageTheMiddlePairForEvenWindowCounts() {
        assertThat(SurgeDetectionService.median(List.of(2L, 4L, 6L, 8L))).isEqualTo(5L);
        assertThat(SurgeDetectionService.median(List.of())).isEqualTo(0L);
    }

    @Test
    void baselineWindowsShouldExcludeTheCurrentWindowAndNotOverlap() throws Exception {
        var periods = SurgeDetectionService.baselinePeriods(
            LocalDate.parse("2026-03-08"), 7, 4);

        assertThat(periods).hasSize(4);
        // Oldest first: the window a month back, not the one being measured.
        assertThat(periods.get(0).start()).isEqualTo(LocalDate.parse("2026-02-01"));
        assertThat(periods.get(0).end()).isEqualTo(LocalDate.parse("2026-02-08"));
        assertThat(periods.get(3).start()).isEqualTo(LocalDate.parse("2026-02-22"));
        assertThat(periods.get(3).end()).isEqualTo(LocalDate.parse("2026-03-01"));

        // The current window is [2026-03-01, 2026-03-08). The baseline must stop at its start.
        // If the most recent baseline window were the current one, the baseline would already
        // contain the spike and a real surge would report a smaller ratio the bigger it got.
        var currentStart = LocalDate.parse("2026-03-08").minusDays(7);
        assertThat(periods.get(3).end()).isEqualTo(currentStart);

        // And consecutive baseline windows must not overlap each other either.
        for (int i = 1; i < periods.size(); i++) {
            assertThat(periods.get(i).start()).isEqualTo(periods.get(i - 1).end());
        }
    }
}
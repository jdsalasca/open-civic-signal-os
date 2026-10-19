package org.opencivic.signalos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.service.SurgeDetectionService;

/**
 * Property-based coverage of the surge formula over generated inputs.
 *
 * <p>Written because a real bug got through the example tests: the most recent baseline window
 * was the current window, so every surge vanished. The examples could not catch that class of
 * mistake; these properties can, because they assert on the shape of the window layout rather
 * than on one hand-picked date range.
 *
 * <p>Seeded {@link Random} on purpose: a test that fails once and then passes on rerun is worse
 * than no test, and AGENTS.md requires deterministic behaviour from processing scripts.
 */
class SurgeDetectionPropertiesTest {

    private static final long SEED = 20260321L;

    @Test
    void baselineWindowsShouldNeverOverlapEachOtherOrTheCurrentWindow() {
        Random random = new Random(SEED);

        for (int attempt = 0; attempt < 500; attempt++) {
            int windowDays = 1 + random.nextInt(30);
            int windows = 1 + random.nextInt(12);
            LocalDate windowEnd = LocalDate.parse("2026-01-01").plusDays(random.nextInt(730));

            List<SurgeDetectionService.WindowCounts> periods =
                SurgeDetectionService.baselinePeriods(windowEnd, windowDays, windows);

            assertThat(periods).hasSize(windows);
            LocalDate currentStart = windowEnd.minusDays(windowDays);

            for (int i = 0; i < periods.size(); i++) {
                var period = periods.get(i);

                // Every window is exactly windowDays long.
                assertThat(period.start().plusDays(windowDays))
                    .as("window %d length, windowDays=%d", i, windowDays)
                    .isEqualTo(period.end());

                // Nothing reaches into the current window.
                assertThat(period.end().isAfter(currentStart))
                    .as("window %d (%s..%s) reaches into the current window %s",
                        i, period.start(), period.end(), currentStart)
                    .isFalse();

                // Consecutive windows are contiguous and disjoint.
                if (i > 0) {
                    assertThat(period.start())
                        .as("window %d overlaps window %d", i, i - 1)
                        .isEqualTo(periods.get(i - 1).end());
                }
            }
        }
    }

    @Test
    void medianShouldAlwaysSitBetweenTheSmallestAndLargestInput() {
        Random random = new Random(SEED + 1);

        for (int attempt = 0; attempt < 500; attempt++) {
            int size = 1 + random.nextInt(40);
            List<Long> values = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                values.add((long) random.nextInt(1000));
            }

            long median = SurgeDetectionService.median(values);

            assertThat(median)
                .isBetween(values.stream().min(Long::compareTo).orElseThrow(),
                    values.stream().max(Long::compareTo).orElseThrow());
        }
    }

    @Test
    void medianShouldNotMoveWhenASingleWindowBlowsUp() {
        // The property that justifies using a median at all: one outlier must not move it.
        for (int baseline = 0; baseline <= 50; baseline++) {
            List<Long> withSpike = List.of((long) baseline, (long) baseline, (long) baseline, 10_000L);

            assertThat(SurgeDetectionService.median(withSpike))
                .as("baseline %d with one 10000 outlier", baseline)
                .isEqualTo((long) baseline);
        }
    }

    @Test
    void aSurgeShouldRequireBothVolumeAndRatio() {
        Random random = new Random(SEED + 2);
        var thresholds = new SurgeDetectionService.Thresholds(
            4, 5 + random.nextInt(20), 1.1 + random.nextDouble() * 4);

        for (int attempt = 0; attempt < 500; attempt++) {
            long current = random.nextInt(60);
            long baseline = random.nextInt(30);
            var zone = SurgeDetectionService.assess("c", "Riverside", "utilities",
                current, baseline, List.of(), thresholds);

            if (current < thresholds.minCurrentCount()) {
                // Below the floor, never a surge, however extreme the ratio.
                assertThat(zone.verdict())
                    .as("current=%d below floor %d", current, thresholds.minCurrentCount())
                    .isNotEqualTo("SURGE");
                assertThat(zone.verdict()).isEqualTo("SUPPRESSED_LOW_VOLUME");
                continue;
            }

            boolean shouldSurge = baseline == 0 || (double) current / baseline >= thresholds.surgeMultiplier();
            assertThat(zone.verdict())
                .as("current=%d baseline=%d multiplier=%f",
                    current, baseline, thresholds.surgeMultiplier())
                .isEqualTo(shouldSurge ? "SURGE" : "STABLE");
        }
    }

    @Test
    void moreReportsShouldNeverLowerTheRatio() {
        var thresholds = new SurgeDetectionService.Thresholds(4, 5, 2.0);

        for (long baseline = 1; baseline <= 30; baseline++) {
            double previous = Double.NEGATIVE_INFINITY;
            for (long current = 0; current <= 60; current++) {
                var zone = SurgeDetectionService.assess("c", "Riverside", "utilities",
                    current, baseline, List.of(), thresholds);

                // Ratio is a monotone function of current for a fixed baseline, so a dashboard
                // sorted by ratio cannot show a bigger number below a smaller one.
                assertThat(zone.ratio())
                    .as("current=%d baseline=%d", current, baseline)
                    .isGreaterThanOrEqualTo(previous);
                previous = zone.ratio();
            }
        }
    }

    @Test
    void aVerdictShouldNeverAppearWithoutAReason() {
        Random random = new Random(SEED + 3);
        var thresholds = new SurgeDetectionService.Thresholds(4, 5, 2.0);

        for (int attempt = 0; attempt < 300; attempt++) {
            long current = random.nextInt(40);
            long baseline = random.nextInt(40);
            var zone = SurgeDetectionService.assess("c", "Riverside", "utilities",
                current, baseline, List.of(), thresholds);

            assertThat(zone.reason()).isNotBlank();
            assertThat(zone.reason()).contains(String.valueOf(current));
            // A reason must never leak a raw Infinity into a rendered report.
            assertThat(zone.reason()).doesNotContain("Infinity");
            assertThat(zone.reason()).doesNotContain("NaN");
        }
    }
}
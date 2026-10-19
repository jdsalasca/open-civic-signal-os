package org.opencivic.signalos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.service.DataFreshnessService;

/**
 * Silence is ambiguous and this class exists to refuse to guess.
 *
 * <p>A "no new reports" alert has two causes that send a responder to opposite places: intake
 * broke, or the community has nothing to report. Every verdict here therefore carries a
 * {@code cause}, and {@code UNKNOWN} is the expected answer rather than a failure to answer.
 */
class DataFreshnessTest {

    /** Events spaced {@code gapDays} apart, ending {@code daysAgo} before today. */
    private static List<LocalDateTime> series(int count, int gapDays, int daysAgo) {
        List<LocalDateTime> events = new ArrayList<>();
        LocalDate last = LocalDate.now().minusDays(daysAgo);
        for (int i = 0; i < count; i++) {
            events.add(last.minusDays((long) (count - 1 - i) * gapDays).atStartOfDay());
        }
        return events;
    }

    @Test
    void shouldReportOnCadenceWhenSilenceIsNormalForThisCommunity() {
        var result = DataFreshnessService.assess("REPORTS", "Community reports", series(8, 7, 5));

        assertThat(result.verdict()).isEqualTo("FRESH");
        assertThat(result.cause()).isEqualTo("ON_CADENCE");
        assertThat(result.medianGapDays()).isEqualTo(7L);
        assertThat(result.daysSinceLast()).isEqualTo(5L);
    }

    @Test
    void shouldFlagACommunityThatNormallyReportsWeeklyAndHasBeenSilentForAMonth() {
        // 30 days against a 7 day cadence is about 4.3x: over the 3x stale line, under 10x.
        var result = DataFreshnessService.assess("REPORTS", "Community reports", series(8, 7, 30));

        assertThat(result.verdict()).isEqualTo("STALE");
        assertThat(result.multipleOfMedianGap()).isBetween(3.0, 10.0);
        assertThat(result.reason()).contains("over the 3.0x threshold");
    }

    @Test
    void shouldEscalateToDormantWhenSilenceFarExceedsTheUsualCadence() {
        var result = DataFreshnessService.assess("REPORTS", "Community reports", series(8, 7, 120));

        assertThat(result.verdict()).isEqualTo("DORMANT");
        assertThat(result.multipleOfMedianGap()).isGreaterThan(10.0);
    }

    @Test
    void shouldNotFlagACommunityThatFilesTwiceAYear() {
        // Same 30 days of silence, but this place normally goes quiet for six months. A global
        // threshold would page someone here for nothing.
        var result = DataFreshnessService.assess("REPORTS", "Community reports", series(6, 180, 30));

        assertThat(result.verdict()).isEqualTo("FRESH");
        assertThat(result.medianGapDays()).isEqualTo(180L);
    }

    @Test
    void shouldAdmitWhenItCannotTellWhyASurfaceIsQuiet() {
        var result = DataFreshnessService.assess("REPORTS", "Community reports", series(8, 7, 120));

        // The whole point: visible silence, honest cause.
        assertThat(result.cause()).isEqualTo("UNKNOWN");
        assertThat(result.reason()).contains("Either intake has broken or the community genuinely");
    }

    @Test
    void shouldRefuseToJudgeSilenceWithoutHistory() {
        var result = DataFreshnessService.assess("PROPOSALS", "Proposals opened", List.of());

        assertThat(result.verdict()).isEqualTo("NO_HISTORY");
        assertThat(result.cause()).isEqualTo("UNKNOWN");
        assertThat(result.reason()).contains("setup question, not a dropout");
        assertThat(result.daysSinceLast()).isNull();
        assertThat(result.lastEventOn()).isNull();
    }

    @Test
    void shouldRefuseToJudgeSilenceWithTooFewEventsToEstablishACadence() {
        var result = DataFreshnessService.assess("DECISIONS", "Decisions recorded", series(3, 7, 90));

        assertThat(result.verdict()).isEqualTo("NO_BASELINE");
        assertThat(result.cause()).isEqualTo("UNKNOWN");
        assertThat(result.reason()).contains("4 needed");
    }

    @Test
    void oneLongGapShouldNotBecomeTheNormalGapAndMaskFutureAlerts() {
        // Six weekly reports, then a six month freeze. A mean gap becomes ~40 days and every
        // future weekly silence looks fine. A median stays at 7.
        List<LocalDateTime> events = series(6, 7, 180);
        events.addAll(series(1, 7, 3));

        var result = DataFreshnessService.assess("REPORTS", "Community reports", events);

        assertThat(result.medianGapDays()).isEqualTo(7L);
        assertThat(result.daysSinceLast()).isEqualTo(3L);
        assertThat(result.verdict()).isEqualTo("FRESH");
    }

    @Test
    void aSignalReportedTodayIsNeverNegativeOrStale() {
        var result = DataFreshnessService.assess("REPORTS", "Community reports", series(8, 7, 0));

        assertThat(result.daysSinceLast()).isZero();
        assertThat(result.verdict()).isEqualTo("FRESH");
    }
}
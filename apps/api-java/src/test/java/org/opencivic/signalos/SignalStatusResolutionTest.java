package org.opencivic.signalos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.SignalStatus;

/**
 * One definition of "this issue is settled", because there were eight.
 *
 * <p>Seven services each carried {@code Set.of("RESOLVED", "CLOSED", "REJECTED")} as a private
 * constant. A federation export added an eighth, written against the {@code SignalStatus} enum — which
 * has no {@code CLOSED} member, because {@code CLOSED} is a legacy string that predates the enum.
 *
 * <p>So the new code disagreed with the old seven: a signal whose status is the legacy {@code "CLOSED"}
 * was excluded from every ranking in the platform and included in the federated backlog. One public
 * view showed it as pending while six others showed it resolved. Nothing failed; the two answers just
 * differed, which is the worst way for this to break.
 */
class SignalStatusResolutionTest {

    @Test
    void settledStatusesAreSettled() {
        assertThat(SignalStatus.RESOLVED.isSettled()).isTrue();
        assertThat(SignalStatus.REJECTED.isSettled()).isTrue();
    }

    @Test
    void anOpenIssueIsNotSettled() {
        assertThat(SignalStatus.NEW.isSettled()).isFalse();
        // Flagged is still open. A flagged issue that has been reviewed is not thereby resolved.
        assertThat(SignalStatus.FLAGGED.isSettled()).isFalse();
    }

    @Test
    void theLegacyClosedStringIsSettled() {
        // "CLOSED" predates the enum and still exists in stored rows. Every ranking in the platform
        // excluded it, so treating it as open would resurrect a settled issue in one view only.
        assertThat(SignalStatus.isSettled("CLOSED")).isTrue();
    }

    @Test
    void rawStatusParsingIsCaseAndSpaceTolerant() {
        assertThat(SignalStatus.isSettled("resolved")).isTrue();
        assertThat(SignalStatus.isSettled("  RESOLVED ")).isTrue();
        assertThat(SignalStatus.isSettled("rejected")).isTrue();
    }

    @Test
    void anUnknownOrMissingStatusIsNotSettled() {
        // A public backlog should not quietly drop a resident's report because its status string is
        // one this version has not heard of. Unknown means open, not closed.
        assertThat(SignalStatus.isSettled(null)).isFalse();
        assertThat(SignalStatus.isSettled("")).isFalse();
        assertThat(SignalStatus.isSettled("   ")).isFalse();
        assertThat(SignalStatus.isSettled("ARCHIVED_BY_MUNICIPALITY")).isFalse();
    }
}
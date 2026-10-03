package org.opencivic.signalos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opencivic.signalos.service.CommunityListLimits;

class CommunityListLimitsTest {
    @Test
    void nullShouldFallBackToTheDefault() {
        assertEquals(CommunityListLimits.DEFAULT_LIMIT, CommunityListLimits.resolveLimit(null));
    }

    @Test
    void nonPositiveShouldFallBackToTheDefault() {
        assertEquals(CommunityListLimits.DEFAULT_LIMIT, CommunityListLimits.resolveLimit(0));
        assertEquals(CommunityListLimits.DEFAULT_LIMIT, CommunityListLimits.resolveLimit(-5));
    }

    @Test
    void oversizedShouldClampToTheMaximum() {
        assertEquals(CommunityListLimits.MAX_LIMIT, CommunityListLimits.resolveLimit(10_000));
    }

    @Test
    void inRangeShouldPassThrough() {
        assertEquals(1, CommunityListLimits.resolveLimit(1));
        assertEquals(75, CommunityListLimits.resolveLimit(75));
        assertEquals(CommunityListLimits.MAX_LIMIT, CommunityListLimits.resolveLimit(CommunityListLimits.MAX_LIMIT));
    }

    @Test
    void everyResolvedLimitShouldBePositiveAndBounded() {
        for (Integer requested : new Integer[] { null, -1, 0, 1, 50, 200, 201, 100_000 }) {
            int resolved = CommunityListLimits.resolveLimit(requested);
            assertTrue(resolved >= 1 && resolved <= CommunityListLimits.MAX_LIMIT,
                "resolved limit out of range for requested=" + requested);
        }
    }
}
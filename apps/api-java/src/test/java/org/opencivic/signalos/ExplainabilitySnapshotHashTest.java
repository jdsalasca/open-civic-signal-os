package org.opencivic.signalos;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.service.ExplainabilitySnapshotService;

/**
 * The hash is the whole guarantee, so its properties are tested without a database.
 *
 * <p>Canonicalisation matters more than it looks: if the hash depended on JSON field ordering,
 * a snapshot could fail verification for reasons that have nothing to do with whether anyone
 * tampered with it, and readers would learn to ignore the check.
 */
class ExplainabilitySnapshotHashTest {

    // Only the ObjectMapper matters for canonicalisation; the rest are never touched by these tests.
private final ExplainabilitySnapshotService service = new ExplainabilitySnapshotService(
        null, null, null, null, null, null, null, new ObjectMapper());

    @Test
    void hashShouldNotDependOnKeyOrdering() throws Exception {
        String a = "{\"signalId\":\"s1\",\"position\":1,\"title\":\"Lamp\"}";
        String b = "{\"title\":\"Lamp\",\"position\":1,\"signalId\":\"s1\"}";

        assertThat(service.canonicalise(new ObjectMapper().readTree(a)))
            .isEqualTo(service.canonicalise(new ObjectMapper().readTree(b)));
        assertThat(ExplainabilitySnapshotService.sha256(
            service.canonicalise(new ObjectMapper().readTree(a))))
            .isEqualTo(ExplainabilitySnapshotService.sha256(
                service.canonicalise(new ObjectMapper().readTree(b))));
    }

    @Test
    void hashShouldPreserveEntryOrderInArrays() throws Exception {
        // Reordering the entries changes the ranking, which must change the hash. Sorting arrays
        // would make a reordered ranking look identical to the original.
        String first = "{\"entries\":[{\"position\":1},{\"position\":2}]}";
        String swapped = "{\"entries\":[{\"position\":2},{\"position\":1}]}";

        assertThat(ExplainabilitySnapshotService.sha256(
            service.canonicalise(new ObjectMapper().readTree(first))))
            .isNotEqualTo(ExplainabilitySnapshotService.sha256(
                service.canonicalise(new ObjectMapper().readTree(swapped))));
    }

    @Test
    void hashShouldChangeWhenAnyValueChanges() throws Exception {
        String original = "{\"title\":\"Lamp out\",\"score\":82.4}";
        String edited = "{\"title\":\"Lamp out\",\"score\":99.9}";

        assertThat(ExplainabilitySnapshotService.sha256(
            service.canonicalise(new ObjectMapper().readTree(original))))
            .isNotEqualTo(ExplainabilitySnapshotService.sha256(
                service.canonicalise(new ObjectMapper().readTree(edited))));
    }

    @Test
    void hashShouldBeStableAcrossCalls() {
        String value = "{\"communityId\":\"c1\",\"entries\":[1,2,3]}";

        assertThat(ExplainabilitySnapshotService.sha256(value))
            .isEqualTo(ExplainabilitySnapshotService.sha256(value));
        assertThat(ExplainabilitySnapshotService.sha256(value)).hasSize(64);
    }

    @Test
    void nestedObjectsShouldAlsoBeCanonicalised() throws Exception {
        String a = "{\"outer\":{\"z\":1,\"a\":{\"y\":2,\"b\":3}}}";
        String b = "{\"outer\":{\"a\":{\"b\":3,\"y\":2},\"z\":1}}";

        assertThat(service.canonicalise(new ObjectMapper().readTree(a)))
            .isEqualTo(service.canonicalise(new ObjectMapper().readTree(b)));
    }
}
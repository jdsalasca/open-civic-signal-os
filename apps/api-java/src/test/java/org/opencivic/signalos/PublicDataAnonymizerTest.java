package org.opencivic.signalos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.opencivic.signalos.service.PublicDataAnonymizer;

/**
 * The redactor runs on text a resident wrote, so a false positive destroys real civic
 * content. These tests pin both directions: what must be caught, and what must survive.
 */
class PublicDataAnonymizerTest {

    private final PublicDataAnonymizer anonymizer = new PublicDataAnonymizer();

    @Test
    void shouldRedactEmailAddresses() {
        String result = anonymizer.redact("Write to maria.lopez+barrio@example.org before Friday.");

        assertThat(result).isEqualTo("Write to [email redacted] before Friday.");
    }

    @Test
    void shouldRedactInternationalPhoneNumbers() {
        assertThat(anonymizer.redact("Call me on +34 600 11 22 33 after 6pm."))
            .isEqualTo("Call me on [phone redacted] after 6pm.");
        assertThat(anonymizer.redact("Mi número es +34600112233."))
            .isEqualTo("Mi número es [phone redacted].");
    }

    @Test
    void shouldRedactNationalIdStyleIdentifiers() {
        assertThat(anonymizer.redact("DNI 12345678Z"))
            .isEqualTo("DNI [national-id redacted]");
    }

    @Test
    void shouldRedactPaymentCardNumbers() {
        assertThat(anonymizer.redact("Card 4111 1111 1111 1111 expired."))
            .isEqualTo("Card [card redacted] expired.");
    }

    @Test
    void shouldLeaveCivicProseIntact() {
        // Budget figures and counts must not be mistaken for identifiers. If this test ever
        // needs relaxing, the damage is a community losing real content to over-redaction.
        String text = "The 2024 budget was 12,000,000 euros across 3 districts with 4500 residents.";

        assertThat(anonymizer.redact(text)).isEqualTo(text);
    }

    @Test
    void shouldLeavePlainSignalReportsIntact() {
        String text = "Streetlight out on the main corridor for three nights, near the school gate.";

        assertThat(anonymizer.redact(text)).isEqualTo(text);
    }

    @Test
    void shouldBeDeterministic() {
        String input = "Contact maria@example.org or +34 600 11 22 33.";

        assertThat(anonymizer.redact(input)).isEqualTo(anonymizer.redact(input));
    }

    @Test
    void shouldNotDoubleRedactAnAlreadyRedactedValue() {
        String once = anonymizer.redact("Mail maria@example.org");
        String twice = anonymizer.redact(once);

        assertThat(twice).isEqualTo(once);
    }

    @Test
    void shouldReportCategoriesItCanDetect() {
        assertThat(anonymizer.detect("a@b.co and +34600112233"))
            .contains("EMAIL", "PHONE");
        assertThat(anonymizer.detect("Nothing sensitive here at all")).isEmpty();
    }

    @Test
    void shouldPassNullAndBlankThrough() {
        assertThat(anonymizer.redact(null)).isNull();
        assertThat(anonymizer.redact("   ")).isEqualTo("   ");
    }
}
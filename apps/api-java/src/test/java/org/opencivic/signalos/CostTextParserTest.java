package org.opencivic.signalos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.opencivic.signalos.service.CostTextParser;

/**
 * The proposal cost field is free text, so something has to read it, and the honest thing is to be
 * explicit about what the parser can and cannot do.
 *
 * <p>The rule that matters: unreadable is empty, never a guess. "about 3k" is not 3000, and a budget
 * that silently invented a number would produce an allocation that looks decided and is not.
 */
class CostTextParserTest {

    @Test
    void shouldReadACurrencyCodeBeforeTheAmount() {
        assertThat(CostTextParser.parseMinorUnits("USD 4000", "USD")).contains(400_000L);
        assertThat(CostTextParser.parseMinorUnits("EUR 1200", "USD")).contains(120_000L);
    }

    @Test
    void shouldReadACurrencyCodeAfterTheAmount() {
        assertThat(CostTextParser.parseMinorUnits("4000 USD", "USD")).contains(400_000L);
    }

    @Test
    void shouldReadCurrencySymbols() {
        assertThat(CostTextParser.parseMinorUnits("$4000", "USD")).contains(400_000L);
        assertThat(CostTextParser.parseMinorUnits("€1200", "USD")).contains(120_000L);
        assertThat(CostTextParser.parseMinorUnits("£500", "USD")).contains(50_000L);
    }

    @Test
    void shouldReadABareNumberAsTheBudgetsCurrency() {
        assertThat(CostTextParser.parseMinorUnits("4000", "USD")).contains(400_000L);
    }

    @Test
    void shouldReadDecimalsInBothConventions() {
        // "1.200,50" and "1,200.50" are the same number written two ways, and the text does not say
        // which convention it uses.
        assertThat(CostTextParser.parseMinorUnits("USD 1.200,50", "USD")).contains(120_050L);
        assertThat(CostTextParser.parseMinorUnits("USD 1,200.50", "USD")).contains(120_050L);
    }

    @Test
    void shouldReadThousandsSeparatorsWithoutDecimals() {
        assertThat(CostTextParser.parseMinorUnits("USD 12,000", "USD")).contains(1_200_000L);
        assertThat(CostTextParser.parseMinorUnits("USD 12.000", "USD")).contains(1_200_000L);
    }

    @Test
    void shouldRespectTheCurrencysOwnFractionDigits() {
        // Yen has no minor unit, so 4000 JPY is 4000 minor units, not 400000.
        assertThat(CostTextParser.parseMinorUnits("JPY 4000", "USD")).contains(4_000L);
    }

    @Test
    void shouldRefuseAnApproximationRatherThanGuess() {
        // The whole point. "about 3k" is not 3000.
        assertThat(CostTextParser.parseMinorUnits("about 3k", "USD")).isEmpty();
        assertThat(CostTextParser.parseMinorUnits("roughly 4000", "USD")).isEmpty();
    }

    @Test
    void shouldRefuseARange() {
        assertThat(CostTextParser.parseMinorUnits("1000-2000", "USD")).isEmpty();
        assertThat(CostTextParser.parseMinorUnits("USD 1000 to 2000", "USD")).isEmpty();
    }

    @Test
    void shouldRefuseTextWithNoAmount() {
        assertThat(CostTextParser.parseMinorUnits("TBD", "USD")).isEmpty();
        assertThat(CostTextParser.parseMinorUnits("unknown", "USD")).isEmpty();
        assertThat(CostTextParser.parseMinorUnits("", "USD")).isEmpty();
        assertThat(CostTextParser.parseMinorUnits(null, "USD")).isEmpty();
    }

    @Test
    void shouldRefuseANegativeAmount() {
        assertThat(CostTextParser.parseMinorUnits("-4000", "USD")).isEmpty();
    }

    @Test
    void shouldRefuseAnAmountWithWordsAroundIt() {
        assertThat(CostTextParser.parseMinorUnits("USD 4000 for materials", "USD")).isEmpty();
    }

    @Test
    void shouldBeDeterministic() {
        assertThat(CostTextParser.parseMinorUnits("USD 1,200.50", "USD"))
            .isEqualTo(CostTextParser.parseMinorUnits("USD 1,200.50", "USD"));
    }
}
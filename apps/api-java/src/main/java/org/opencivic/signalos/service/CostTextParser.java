package org.opencivic.signalos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads an amount out of the free text a proposal carries in {@code estimatedCost}.
 *
 * <p>The field is free text: "USD 4000", "€1.200", "about 3k", "unknown". A participatory budget
 * needs comparable numbers, so something has to read them, and the honest thing is to be explicit
 * about what this can and cannot do.
 *
 * <p>Two rules:
 *
 * <ul>
 *   <li><b>Unreadable is null, never a guess.</b> "about 3k" is not 3000. A budget that silently
 *       invented a number would produce an allocation that looks decided and is not.
 *   <li><b>Minor units, never a double.</b> Money in floating point is a rounding bug waiting for a
 *       council meeting to expose it.
 * </ul>
 *
 * <p>Deliberately narrow. It reads a leading or trailing currency marker and a number with optional
 * thousands separators and decimals. It does not read words, ranges, or approximations, because
 * those need a human.
 */
public final class CostTextParser {

    /** A currency code or symbol, then a number. */
    private static final Pattern PREFIXED = Pattern.compile(
        "^\\s*(?<currency>[A-Za-z]{3}|[$€£¥])\\s*(?<amount>[0-9][0-9.,]*)\\s*$");

    /** A number, then a currency code or symbol. */
    private static final Pattern SUFFIXED = Pattern.compile(
        "^\\s*(?<amount>[0-9][0-9.,]*)\\s*(?<currency>[A-Za-z]{3}|[$€£¥])\\s*$");

    /** A bare number, which is read as the budget's own currency. */
    private static final Pattern BARE = Pattern.compile("^\\s*(?<amount>[0-9][0-9.,]*)\\s*$");

    private CostTextParser() {}

    /**
     * The parsed amount in minor units, or empty when the text is not a plain amount.
     *
     * @param text           the raw {@code estimatedCost}
     * @param defaultCurrency the budget's currency, used when the text carries none
     */
    public static Optional<Long> parseMinorUnits(String text, String defaultCurrency) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String trimmed = text.trim();

        Matcher prefixed = PREFIXED.matcher(trimmed);
        if (prefixed.matches()) {
            return toMinorUnits(prefixed.group("amount"), prefixed.group("currency"), defaultCurrency);
        }
        Matcher suffixed = SUFFIXED.matcher(trimmed);
        if (suffixed.matches()) {
            return toMinorUnits(suffixed.group("amount"), suffixed.group("currency"), defaultCurrency);
        }
        Matcher bare = BARE.matcher(trimmed);
        if (bare.matches()) {
            return toMinorUnits(bare.group("amount"), null, defaultCurrency);
        }
        // Anything else — "about 3k", "TBD", "1000-2000" — is a human's problem, not a parser's.
        return Optional.empty();
    }

    private static Optional<Long> toMinorUnits(String amountText, String currencyText, String defaultCurrency) {
        String normalised = normaliseNumber(amountText);
        if (normalised == null) {
            return Optional.empty();
        }
        try {
            BigDecimal amount = new BigDecimal(normalised);
            if (amount.signum() < 0) {
                return Optional.empty();
            }
            int fractionDigits = fractionDigitsFor(currencyText, defaultCurrency);
            return Optional.of(
                amount.setScale(fractionDigits, RoundingMode.HALF_UP)
                    .movePointRight(fractionDigits)
                    .longValueExact());
        } catch (ArithmeticException | NumberFormatException ex) {
            return Optional.empty();
        }
    }

    /**
     * Resolves "1.200,50" and "1,200.50" to the same number.
     *
     * <p>Both conventions are in use and the text does not say which. The rule: the last separator is
     * the decimal one when it is followed by one or two digits, otherwise every separator is a
     * thousands marker. That is a heuristic, and it is the reason this parser refuses anything it is
     * not sure about rather than guessing.
     */
    private static String normaliseNumber(String amountText) {
        String value = amountText.trim();
        int lastDot = value.lastIndexOf('.');
        int lastComma = value.lastIndexOf(',');
        int lastSeparator = Math.max(lastDot, lastComma);
        if (lastSeparator < 0) {
            return value;
        }
        int digitsAfter = value.length() - lastSeparator - 1;
        boolean decimalSeparator = digitsAfter == 1 || digitsAfter == 2;
        if (!decimalSeparator) {
            return value.replace(".", "").replace(",", "");
        }
        String integerPart = value.substring(0, lastSeparator).replace(".", "").replace(",", "");
        String fractionPart = value.substring(lastSeparator + 1);
        return integerPart + "." + fractionPart;
    }

    private static int fractionDigitsFor(String currencyText, String defaultCurrency) {
        String code = currencyCode(currencyText, defaultCurrency);
        try {
            return Currency.getInstance(code).getDefaultFractionDigits();
        } catch (IllegalArgumentException ex) {
            // An unknown code is not a reason to fail the parse; two decimals is the common case.
            return 2;
        }
    }

    private static String currencyCode(String currencyText, String defaultCurrency) {
        if (currencyText == null || currencyText.isBlank()) {
            return defaultCurrency == null ? "USD" : defaultCurrency.toUpperCase(Locale.ROOT);
        }
        String trimmed = currencyText.trim();
        return switch (trimmed) {
            case "$" -> "USD";
            case "€" -> "EUR";
            case "£" -> "GBP";
            case "¥" -> "JPY";
            default -> trimmed.toUpperCase(Locale.ROOT);
        };
    }
}
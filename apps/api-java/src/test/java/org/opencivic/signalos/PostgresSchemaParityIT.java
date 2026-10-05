package org.opencivic.signalos;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Round 56 made the suite's schema the one Flyway builds, on H2. That is only worth anything if H2 and
 * PostgreSQL come out the same, because the suite now stands in for production.
 *
 * <p>They do agree: 62 tables and 582 columns, and after three rounds of closing gaps, on names, types,
 * nullability and string lengths. That is not luck, it is the migrations being written portably (V46 and
 * V50 carry explicit H2 workarounds), and nothing was enforcing it. A migration that quietly did the
 * right thing on only one engine would pass the suite and fail on deploy.
 *
 * <p>Skipped unless a PostgreSQL instance is pointed at, because the suite's own database is H2 and
 * requiring Docker for every run would make this test the most skipped one in the repository. Run it
 * with {@code npm run verify:pg-schema}, which starts a throwaway PostgreSQL, migrates it with the real
 * migration files, and sets the variables below.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:pgparityitdb;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class PostgresSchemaParityIT {

    /** One column as each engine describes it. H2's and PostgreSQL's spellings differ; that is the point. */
    private record Column(String key, String type, boolean nullable, Integer maxLength) {}

    @Autowired
    private JdbcTemplate h2;

    private Map<String, Column> postgres;

    @BeforeEach
    void requirePostgres() {
        String url = System.getenv("POSTGRES_VERIFY_URL");
        Assumptions.assumeTrue(url != null && !url.isBlank(),
            "no POSTGRES_VERIFY_URL set; run npm run verify:pg-schema to exercise this");

        Map<String, Column> columns = new LinkedHashMap<>();
        try (Connection connection = DriverManager.getConnection(url, "verify", "verify_only_not_a_secret");
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                 "select table_name, column_name, data_type, is_nullable, character_maximum_length "
                     + "from information_schema.columns where table_schema = 'public' "
                     + "order by table_name, column_name")) {
            while (rows.next()) {
                String key = rows.getString(1).toUpperCase() + "|" + rows.getString(2).toUpperCase();
                columns.put(key, new Column(key, rows.getString(3),
                    "YES".equalsIgnoreCase(rows.getString(4)), asLength(rows.getObject(5))));
            }
        } catch (Exception e) {
            throw new IllegalStateException("could not read the PostgreSQL schema at " + url, e);
        }
        this.postgres = columns;
    }

    private Map<String, Column> suiteColumns() {
        Map<String, Column> columns = new LinkedHashMap<>();
        for (java.util.Map<String, Object> row : h2.queryForList(
            "select table_name, column_name, data_type, is_nullable, character_maximum_length "
                + "from information_schema.columns where table_schema = 'PUBLIC' "
                + "order by table_name, column_name")) {
            String key = row.get("TABLE_NAME").toString().toUpperCase()
                + "|" + row.get("COLUMN_NAME").toString().toUpperCase();
            columns.put(key, new Column(key, row.get("DATA_TYPE").toString(),
                "YES".equalsIgnoreCase(row.get("IS_NULLABLE").toString()),
                asLength(row.get("CHARACTER_MAXIMUM_LENGTH"))));
        }
        return columns;
    }

    /**
     * Column lengths come back as Integer from H2 and, depending on the driver, as Long from
     * PostgreSQL - whose {@code character_maximum_length} is a domain over {@code integer} rather than a
     * plain int. Casting straight to Integer threw on the PostgreSQL side and nowhere else, which reads
     * like a schema problem and is not one.
     */
    private static Integer asLength(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    @Test
    void theSuiteSchemaAndPostgresShouldHaveTheSameTablesAndColumns() {
        Map<String, Column> suite = suiteColumns();

        assertThat(postgres)
            .as("the PostgreSQL schema is empty or unreachable - did the migrations run against it?")
            .isNotEmpty();

        Set<String> onlyInSuite = new TreeSet<>(suite.keySet());
        onlyInSuite.removeAll(postgres.keySet());
        Set<String> onlyInPostgres = new TreeSet<>(postgres.keySet());
        onlyInPostgres.removeAll(suite.keySet());

        assertThat(onlyInSuite)
            .as("these exist in the suite's H2 schema but not in PostgreSQL, so a migration only works "
                + "on one engine and the suite would not have caught it")
            .isEmpty();
        assertThat(onlyInPostgres)
            .as("these exist in PostgreSQL but not in the suite's H2 schema, so the suite is not testing "
                + "what production runs")
            .isEmpty();
    }

    @Test
    void everyColumnShouldAlsoHaveTheSameTypeOnBothEngines() {
        Map<String, String> divergence = new LinkedHashMap<>();
        for (java.util.Map.Entry<String, Column> entry : suiteColumns().entrySet()) {
            Column suite = entry.getValue();
            Column other = postgres.get(entry.getKey());
            if (other == null) {
                continue; // Already reported by the name comparison.
            }
            String left = canonicalType("h2", suite.type());
            String right = canonicalType("pg", other.type());
            if (!left.equals(right)) {
                divergence.put(entry.getKey(), suite.type() + " vs " + other.type());
            }
        }

        assertThat(divergence)
            .as("these columns have the same name on both engines but a different type, so a migration "
                + "means one thing in the suite and another in production")
            .isEmpty();
    }

    @Test
    void everyColumnShouldAgreeOnWhetherItIsNullable() {
        Map<String, String> divergence = new LinkedHashMap<>();
        for (java.util.Map.Entry<String, Column> entry : suiteColumns().entrySet()) {
            Column suite = entry.getValue();
            Column other = postgres.get(entry.getKey());
            if (other == null) {
                continue;
            }
            if (suite.nullable() != other.nullable()) {
                divergence.put(entry.getKey(),
                    (suite.nullable() ? "NULLABLE" : "NOT NULL") + " vs "
                        + (other.nullable() ? "NULLABLE" : "NOT NULL"));
            }
        }

        // 441 NOT NULL and 141 nullable on both engines, measured. This is the cheapest parity check
        // there is and it was missing: a column that accepts nulls in the suite and rejects them in
        // production fails on write, not on read, so nothing in a read-heavy test would notice.
        assertThat(divergence)
            .as("these columns disagree on nullability, so the suite accepts writes production rejects")
            .isEmpty();
    }

    @Test
    void everyStringColumnShouldAgreeOnItsMaximumLength() {
        Map<String, String> divergence = new LinkedHashMap<>();
        for (java.util.Map.Entry<String, Column> entry : suiteColumns().entrySet()) {
            Column suite = entry.getValue();
            Column other = postgres.get(entry.getKey());
            if (other == null) {
                continue;
            }
            String left = lengthOf("h2", suite);
            String right = lengthOf("pg", other);
            if (!left.equals(right)) {
                divergence.put(entry.getKey(), left + " vs " + right);
            }
        }

        // This is the gap round 59 documented as invisible. With names and types alone, a column that is
        // VARCHAR(255) in PostgreSQL and TEXT in the suite looked identical, because H2 reports TEXT as
        // CHARACTER VARYING. The length is what tells them apart: H2 spells an unbounded string
        // 1000000000, PostgreSQL reports no length at all. Measured across the schema, all 136 bounded
        // string columns agree exactly, with 24 distinct lengths, and the other 63 are unbounded on both.
        assertThat(divergence)
            .as("these string columns have the same name and type but a different maximum length, so the "
                + "suite would accept a longer value than production")
            .isEmpty();
    }

    /**
     * The spellings that differ, and only those.
     *
     * <p>Measured across all 582 columns: 162 differ raw, in exactly two pairs - H2 reports TEXT columns
     * as {@code CHARACTER VARYING} (63 columns) and calls {@code TIMESTAMP} what PostgreSQL spells
     * {@code TIMESTAMP WITHOUT TIME ZONE} (99). The other 420 already agree exactly, including the
     * integer/bigint split, which is kept distinct on purpose.
     *
     * <p>Anything else is compared verbatim, so an unanticipated spelling difference surfaces instead of
     * being absorbed.
     */
    private static String canonicalType(String engine, String rawType) {
        String type = rawType.toUpperCase(Locale.ROOT);
        boolean isSuite = engine.equals("h2");
        if (isSuite && type.equals("CHARACTER VARYING")) {
            return "string";
        }
        if (!isSuite && (type.equals("CHARACTER VARYING") || type.equals("TEXT"))) {
            return "string";
        }
        if (isSuite && type.equals("TIMESTAMP")) {
            return "timestamp";
        }
        if (!isSuite && type.equals("TIMESTAMP WITHOUT TIME ZONE")) {
            return "timestamp";
        }
        return type;
    }

    /**
     * How H2 spells "this string has no length limit". Measured, not assumed: I first wrote
     * {@link Integer#MAX_VALUE} here because that is what it looks like, and the test immediately failed
     * on all 63 unbounded columns with {@code 1000000000 vs unbounded}. H2 uses its own one-billion
     * marker. PostgreSQL reports no length at all, which is the null case above.
     */
    private static final int H2_UNBOUNDED_LENGTH = 1_000_000_000;

    /** Maximum length as a comparable string, with both engines' spellings of "unbounded" folded together. */
    private static String lengthOf(String engine, Column column) {
        if (!canonicalType(engine, column.type()).equals("string")) {
            return "n/a";
        }
        Integer length = column.maxLength();
        if (length == null) {
            return "unbounded";
        }
        return length == H2_UNBOUNDED_LENGTH || length == Integer.MAX_VALUE
            ? "unbounded"
            : String.valueOf(length);
    }

    @Test
    void theSequenceColumnRound55FoughtForShouldExistOnBoth() {
        // The narrow case that started this chain. V50 declared the column and production got it; the
        // suite's schema did not have it until round 55 spelled it out in the entity mapping.
        assertThat(suiteColumns().keySet())
            .as("the audit trail's insertion sequence is missing from the suite's schema")
            .contains("SIGNAL_STATUS_HISTORY|SEQ");
        assertThat(postgres.keySet())
            .as("the audit trail's insertion sequence is missing from PostgreSQL")
            .contains("SIGNAL_STATUS_HISTORY|SEQ");
    }
}
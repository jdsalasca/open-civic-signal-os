package org.opencivic.signalos;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
 * <p>They do agree: 62 tables and 582 columns, identical on both engines. That is not luck, it is the
 * migrations being written portably (V46 and V50 carry explicit H2 workarounds), and nothing was
 * enforcing it. A migration that quietly did the right thing only on one engine would pass the suite and
 * fail on deploy.
 *
 * <p>Skipped unless a PostgreSQL instance is pointed at, because the suite's own database is H2 and
 * requiring Docker for every run would make this test the most skipped one in the repository. Run it
 * with {@code npm run verify:pg-schema}, which starts a throwaway PostgreSQL, migrates it with the real
 * migration files, and sets the variables below.
 *
 * <p>Compares table and column names, not types: the engines spell types differently on purpose and a
 * type comparison would drown in false positives. A missing or extra column is the failure that has
 * actually bitten - V50's {@code seq} existed in the migration and in production, and nowhere else.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:pgparityitdb;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class PostgresSchemaParityIT {

    @Autowired
    private JdbcTemplate h2;

    private Set<String> postgres;
    private Map<String, String> postgresTypes;

    @BeforeEach
    void requirePostgres() {
        String url = System.getenv("POSTGRES_VERIFY_URL");
        Assumptions.assumeTrue(url != null && !url.isBlank(),
            "no POSTGRES_VERIFY_URL set; run npm run verify:pg-schema to exercise this");

        Set<String> columns = new LinkedHashSet<>();
        Map<String, String> types = new LinkedHashMap<>();
        try (Connection connection = DriverManager.getConnection(url, "verify", "verify_only_not_a_secret");
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                 "select table_name, column_name, data_type from information_schema.columns "
                     + "where table_schema = 'public' order by table_name, column_name")) {
            while (rows.next()) {
                String key = rows.getString(1).toUpperCase() + "|" + rows.getString(2).toUpperCase();
                columns.add(key);
                types.put(key, rows.getString(3));
            }
        } catch (Exception e) {
            throw new IllegalStateException("could not read the PostgreSQL schema at " + url, e);
        }
        this.postgres = columns;
        this.postgresTypes = types;
    }

    @Test
    void theSuiteSchemaAndPostgresShouldHaveTheSameTablesAndColumns() {
        Set<String> suiteSchema = new TreeSet<>();
        for (java.util.Map<String, Object> row : h2.queryForList(
            "select table_name, column_name from information_schema.columns "
                + "where table_schema = 'PUBLIC' order by table_name, column_name")) {
            suiteSchema.add(row.get("TABLE_NAME").toString().toUpperCase()
                + "|" + row.get("COLUMN_NAME").toString().toUpperCase());
        }

        assertThat(postgres)
            .as("the PostgreSQL schema is empty or unreachable - did the migrations run against it?")
            .isNotEmpty();

        Set<String> onlyInSuite = new TreeSet<>(suiteSchema);
        onlyInSuite.removeAll(postgres);
        Set<String> onlyInPostgres = new TreeSet<>(postgres);
        onlyInPostgres.removeAll(suiteSchema);

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
        Set<String> suiteSchema = new TreeSet<>();
        Map<String, String> suiteTypes = new LinkedHashMap<>();
        for (java.util.Map<String, Object> row : h2.queryForList(
            "select table_name, column_name, data_type from information_schema.columns "
                + "where table_schema = 'PUBLIC' order by table_name, column_name")) {
            String key = row.get("TABLE_NAME").toString().toUpperCase()
                + "|" + row.get("COLUMN_NAME").toString().toUpperCase();
            suiteSchema.add(key);
            suiteTypes.put(key, row.get("DATA_TYPE").toString());
        }

        Map<String, String> divergence = new LinkedHashMap<>();
        for (var entry : postgresTypes.entrySet()) {
            String suiteType = suiteTypes.get(entry.getKey());
            if (suiteType == null) {
                continue; // Already reported by the name comparison.
            }
            String left = canonical("h2", suiteType);
            String right = canonical("pg", entry.getValue());
            if (!left.equals(right)) {
                divergence.put(entry.getKey(), suiteType + " vs " + entry.getValue());
            }
        }

        assertThat(divergence)
            .as("these columns have the same name on both engines but a different type, so a migration "
                + "means one thing in the suite and another in production")
            .isEmpty();
    }

    /**
     * The two spellings that differ, and only those.
     *
     * <p>Measured across all 582 columns: 162 differ raw, in exactly two pairs - H2 reports TEXT columns
     * as {@code CHARACTER VARYING} (63 columns) and calls {@code TIMESTAMP} what PostgreSQL spells
     * {@code TIMESTAMP WITHOUT TIME ZONE} (99). The other 420 already agree exactly, including the
     * integer/bigint split, which is kept distinct on purpose.
     *
     * <p>What this cannot see: H2's information_schema does not report {@code text} separately from
     * {@code varchar}, so a column that is {@code VARCHAR(255)} in PostgreSQL and {@code TEXT} in the
     * suite passes here. Catching that needs {@code character_maximum_length} compared across engines,
     * which is a separate piece of work rather than a claim this makes.
     */
    private static String canonical(String engine, String rawType) {
        String type = rawType.toUpperCase(java.util.Locale.ROOT);
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

    @Test
    void theSequenceColumnRound55FoughtForShouldExistOnBoth() {
        // The narrow case that motivated all of this. It was declared in V50 and in production, and was
        // absent from the suite's schema until round 55 spelled it out in the entity mapping.
        assertThat(h2.queryForList(
            "select column_name from information_schema.columns "
                + "where table_schema = 'PUBLIC' and upper(table_name) = 'SIGNAL_STATUS_HISTORY' "
                + "and upper(column_name) = 'SEQ'"))
            .as("the audit trail's insertion sequence is missing from the suite's schema")
            .hasSize(1);
        assertThat(postgres)
            .as("the audit trail's insertion sequence is missing from PostgreSQL")
            .contains("SIGNAL_STATUS_HISTORY|SEQ");
    }
}
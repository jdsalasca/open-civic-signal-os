package org.opencivic.signalos;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashSet;
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

    private static final String URL = "jdbc:postgresql://localhost:55432/signalos_verify";

    @Autowired
    private JdbcTemplate h2;

    private Set<String> postgres;

    @BeforeEach
    void requirePostgres() {
        String url = System.getenv("POSTGRES_VERIFY_URL");
        Assumptions.assumeTrue(url != null && !url.isBlank(),
            "no POSTGRES_VERIFY_URL set; run npm run verify:pg-schema to exercise this");

        Set<String> columns = new LinkedHashSet<>();
        try (Connection connection = DriverManager.getConnection(url, "verify", "verify_only_not_a_secret");
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                 "select table_name, column_name from information_schema.columns "
                     + "where table_schema = 'public' order by table_name, column_name")) {
            while (rows.next()) {
                columns.add(rows.getString(1).toUpperCase() + "|" + rows.getString(2).toUpperCase());
            }
        } catch (Exception e) {
            throw new IllegalStateException("could not read the PostgreSQL schema at " + url, e);
        }
        this.postgres = columns;
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
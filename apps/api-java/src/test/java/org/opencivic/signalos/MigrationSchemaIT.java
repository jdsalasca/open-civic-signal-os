package org.opencivic.signalos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * The suite used to build its schema with {@code ddl-auto: create-drop}, which drops whatever Flyway
 * built and recreates every table from the entity mappings. That is convenient and it is also why none
 * of the 49 migrations had ever run in a test: a column that only exists in a {@code .sql} file simply
 * did not exist for the suite, and the foreign keys that guard the data never fired.
 *
 * <p>It hid two production bugs. Both were "replace the current row, then insert the new one" flows
 * where Hibernate orders inserts before updates and deletes, so the unique index on the slot column
 * rejected the insert. Under a schema without that index they passed. See {@code BacklogPublicationService}
 * and {@code AssemblyFacilitationService}.
 *
 * <p>This test exists so nobody can quietly turn the migrations off again.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:migrationschematitdb;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class MigrationSchemaIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void everyMigrationShouldHaveActuallyRun() {
        // Quoted because Flyway creates this table lower-cased and H2 upper-cases unquoted identifiers.
        Integer applied = jdbc.queryForObject(
            "select count(*) from \"flyway_schema_history\" where \"success\" = true", Integer.class);

        assertThat(applied)
            .as("the test schema must be built by Flyway, otherwise the migrations ship untested")
            .isNotNull()
            .isGreaterThanOrEqualTo(49);
    }

    @Test
    void theForeignKeysThatGuardTheDataShouldBeEnforced() {
        // A named constraint from V1, not a count. "Some foreign key exists" also passes on a schema
        // Hibernate built, because an entity mapping that declares a relationship does produce one; what
        // cannot pass without the migration is the specific key V1 wrote by hand.
        Integer constraint = jdbc.queryForObject(
            "select count(*) from information_schema.table_constraints "
                + "where constraint_name = 'FK_SIGNAL_AUTHOR'",
            Integer.class);

        assertThat(constraint)
            .as("V1's author foreign key is missing, so the schema came from the entities, not the migrations")
            .isNotNull()
            .isGreaterThan(0);
    }

    @Test
    void aUniqueIndexFromAMigrationShouldStillExist() {
        // V38: at most one current backlog publication per community. The test that caught the flush
        // ordering bug needed this index to exist.
        Integer index = jdbc.queryForObject(
            "select count(*) from information_schema.indexes "
                + "where index_name = 'IDX_BACKLOG_PUBLICATIONS_CURRENT'",
            Integer.class);

        assertThat(index)
            .as("V38's unique index is missing, so the migrations did not build this schema")
            .isNotNull()
            .isGreaterThan(0);
    }
}
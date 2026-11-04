package kz.iitu.springlab.bank;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** Task E. The in-memory database is empty at start, so Flyway must apply every migration. */
@SpringBootTest
class MigrationTest {

    @Autowired JdbcTemplate jdbc;

    @Test
    void allMigrationsAreAppliedInOrder() {
        List<Map<String, Object>> history = jdbc.queryForList(
                "SELECT \"installed_rank\", \"version\", \"description\", \"success\" "
                        + "FROM \"flyway_schema_history\" ORDER BY \"installed_rank\"");
        history.forEach(row -> System.out.println(">>> " + row));

        // the first row is Flyway's own "<< Flyway Schema History table created >>", it has no version
        assertThat(history).extracting(row -> row.get("version")).containsExactly(null, "1", "2", "3", "4");
        assertThat(history).allMatch(row -> Boolean.TRUE.equals(row.get("success")));
    }

    @Test
    void v2AddedStatusWithDefaultAndV4AddedTheIndex() {
        assertThat(jdbc.queryForList("SELECT DISTINCT status FROM accounts", String.class)).containsExactly("ACTIVE");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE INDEX_NAME = 'IDX_ACCOUNTS_OWNER_NAME'",
                Integer.class)).isEqualTo(1);
    }
}

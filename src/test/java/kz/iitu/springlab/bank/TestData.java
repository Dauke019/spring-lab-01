package kz.iitu.springlab.bank;

import org.springframework.jdbc.core.JdbcTemplate;

/** Puts the two accounts back to the state created by V1 before every test. */
final class TestData {

    private TestData() {
    }

    static void reset(JdbcTemplate jdbc) {
        jdbc.update("DELETE FROM audit_log");
        jdbc.update("UPDATE accounts SET balance = 1000.00, version = 0 WHERE owner_name = 'Alice'");
        jdbc.update("UPDATE accounts SET balance = 500.00, version = 0 WHERE owner_name = 'Bob'");
    }
}

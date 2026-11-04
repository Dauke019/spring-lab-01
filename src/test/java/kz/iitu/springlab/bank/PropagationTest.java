package kz.iitu.springlab.bank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** Task B. The outer method fails after the inner (audit) method has returned. */
@SpringBootTest
class PropagationTest {

    @Autowired BankOperations operations;
    @Autowired AccountRepository accounts;
    @Autowired AuditRepository audit;
    @Autowired JdbcTemplate jdbc;

    Long alice;
    Long bob;

    @BeforeEach
    void setUp() {
        TestData.reset(jdbc);
        alice = accounts.findByOwnerName("Alice").orElseThrow().getId();
        bob = accounts.findByOwnerName("Bob").orElseThrow().getId();
    }

    @Test
    void required_auditIsRolledBackWithTheOuterTransaction() {
        assertThatThrownBy(() -> operations.transferAuditRequiredThenFail(alice, bob, new BigDecimal("100.00")))
                .isInstanceOf(IllegalStateException.class);

        System.out.println(">>> REQUIRED: audit rows after rollback = " + audit.count());
        assertThat(audit.count()).isZero();
        assertThat(accounts.findById(alice).orElseThrow().getBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void requiresNew_auditSurvivesTheOuterRollback() {
        assertThatThrownBy(() -> operations.transferAuditRequiresNewThenFail(alice, bob, new BigDecimal("100.00")))
                .isInstanceOf(IllegalStateException.class);

        System.out.println(">>> REQUIRES_NEW: audit rows after rollback = " + audit.count()
                + ", message = " + audit.findAll().get(0).getMessage());
        assertThat(audit.count()).isEqualTo(1);
        // the transfer itself belonged to the outer transaction and was rolled back
        assertThat(accounts.findById(alice).orElseThrow().getBalance()).isEqualByComparingTo("1000.00");
        assertThat(accounts.findById(bob).orElseThrow().getBalance()).isEqualByComparingTo("500.00");
    }
}

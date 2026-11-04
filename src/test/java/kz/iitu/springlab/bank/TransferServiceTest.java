package kz.iitu.springlab.bank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** Task A. The test class itself is NOT @Transactional: every service call commits or rolls back for real. */
@SpringBootTest
class TransferServiceTest {

    @Autowired TransferService service;
    @Autowired AccountRepository repository;
    @Autowired JdbcTemplate jdbc;

    Long alice;
    Long bob;

    @BeforeEach
    void setUp() {
        TestData.reset(jdbc);
        alice = repository.findByOwnerName("Alice").orElseThrow().getId();
        bob = repository.findByOwnerName("Bob").orElseThrow().getId();
    }

    BigDecimal balance(Long id) {
        return repository.findById(id).orElseThrow().getBalance();
    }

    @Test
    void a1_successfulTransferMovesTheMoney() {
        service.transfer(alice, bob, new BigDecimal("100.00"));

        assertThat(balance(alice)).isEqualByComparingTo("900.00");
        assertThat(balance(bob)).isEqualByComparingTo("600.00");
    }

    @Test
    void a2_insufficientFundsChangesNothing() {
        assertThatThrownBy(() -> service.transfer(alice, bob, new BigDecimal("5000.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Insufficient funds");

        assertThat(balance(alice)).isEqualByComparingTo("1000.00");
        assertThat(balance(bob)).isEqualByComparingTo("500.00");
    }

    @Test
    void a3_uncheckedExceptionAfterDebitRollsBack() {
        assertThatThrownBy(() -> service.transferFailingAfterDebit(alice, bob, new BigDecimal("100.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Simulated failure after debit");

        // the UPDATE of Alice was flushed, but the transaction was rolled back
        assertThat(balance(alice)).isEqualByComparingTo("1000.00");
        assertThat(balance(bob)).isEqualByComparingTo("500.00");
    }

    @Test
    void a4_checkedExceptionCommitsByDefault() {
        assertThatThrownBy(() -> service.transferFailingChecked(alice, bob, new BigDecimal("100.00")))
                .isInstanceOf(TransferAuditException.class);

        // the default rule: checked exceptions do not mark the transaction for rollback,
        // so the debit is committed and 100.00 is "lost"
        assertThat(balance(alice)).isEqualByComparingTo("900.00");
        assertThat(balance(bob)).isEqualByComparingTo("500.00");
    }

    @Test
    void a4_rollbackForMakesCheckedExceptionRollBack() {
        assertThatThrownBy(() -> service.transferFailingCheckedWithRollbackFor(alice, bob, new BigDecimal("100.00")))
                .isInstanceOf(TransferAuditException.class);

        assertThat(balance(alice)).isEqualByComparingTo("1000.00");
        assertThat(balance(bob)).isEqualByComparingTo("500.00");
    }
}

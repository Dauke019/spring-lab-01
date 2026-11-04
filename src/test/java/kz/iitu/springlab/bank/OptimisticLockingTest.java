package kz.iitu.springlab.bank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Task D. Two independent transactions load the same row; the second commit is based on a stale copy. */
@SpringBootTest
class OptimisticLockingTest {

    @Autowired AccountRepository repository;
    @Autowired PlatformTransactionManager txManager;
    @Autowired JdbcTemplate jdbc;

    Long alice;

    @BeforeEach
    void setUp() {
        TestData.reset(jdbc);
        alice = repository.findByOwnerName("Alice").orElseThrow().getId();
    }

    @Test
    void staleCopyCannotOverwriteTheFirstUpdate() {
        TransactionTemplate tx = new TransactionTemplate(txManager);

        // 1. two independent persistence contexts load the same account (version 0)
        Account first = tx.execute(s -> repository.findById(alice).orElseThrow());
        Account second = tx.execute(s -> repository.findById(alice).orElseThrow());
        System.out.println(">>> loaded twice: version " + first.getVersion() + " and " + second.getVersion());

        // 2. the first copy is updated and committed: version 0 -> 1
        first.debit(new BigDecimal("100.00"));
        tx.executeWithoutResult(s -> repository.save(first));
        System.out.println(">>> first copy committed, balance 900.00, version now "
                + repository.findById(alice).orElseThrow().getVersion());

        // 3. the stale second copy still has version 0
        second.debit(new BigDecimal("50.00"));
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> repository.save(second)))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class)
                .satisfies(e -> System.out.println(">>> second copy rejected: " + e.getMessage()));

        // 4. the first update was not silently overwritten
        Account stored = repository.findById(alice).orElseThrow();
        assertThat(stored.getBalance()).isEqualByComparingTo("900.00");
        assertThat(stored.getVersion()).isEqualTo(1L);
    }

    @Test
    void safeResponse_reloadAndApplyAgain() {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        Account stale = tx.execute(s -> repository.findById(alice).orElseThrow());
        tx.executeWithoutResult(s -> repository.findById(alice).orElseThrow().debit(new BigDecimal("100.00")));

        stale.debit(new BigDecimal("50.00"));
        try {
            tx.executeWithoutResult(s -> repository.save(stale));
        } catch (ObjectOptimisticLockingFailureException conflict) {
            // reload the CURRENT state and apply the business operation again, with all its checks
            tx.executeWithoutResult(s -> repository.findById(alice).orElseThrow().debit(new BigDecimal("50.00")));
        }

        Account stored = repository.findById(alice).orElseThrow();
        assertThat(stored.getBalance()).isEqualByComparingTo("850.00"); // both debits, none lost
        assertThat(stored.getVersion()).isEqualTo(2L);
    }
}

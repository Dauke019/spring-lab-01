package kz.iitu.springlab.bank;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Task C. Transaction T1 reads Alice's balance twice; between the two reads transaction T2
 * (another thread, another connection) changes the balance and commits.
 * Plain JDBC reads are used on purpose: a JPA find() would return the cached entity
 * from the first-level cache and hide what the database does.
 */
@SpringBootTest
class IsolationTest {

    @Autowired PlatformTransactionManager txManager;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;

    @BeforeEach
    void setUp() {
        TestData.reset(jdbc);
    }

    BigDecimal readAlice() {
        return jdbc.queryForObject("SELECT balance FROM accounts WHERE owner_name = 'Alice'", BigDecimal.class);
    }

    /** Returns {first read, second read} of transaction T1. */
    BigDecimal[] run(int isolation) throws Exception {
        TransactionTemplate t1 = new TransactionTemplate(txManager);
        t1.setIsolationLevel(isolation);
        TransactionTemplate t2 = new TransactionTemplate(txManager);
        t2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        CountDownLatch firstReadDone = new CountDownLatch(1);
        CountDownLatch updateCommitted = new CountDownLatch(1);

        CompletableFuture<BigDecimal[]> reader = CompletableFuture.supplyAsync(() -> t1.execute(status -> {
            String level = jdbc.queryForObject(
                    "SELECT ISOLATION_LEVEL FROM INFORMATION_SCHEMA.SESSIONS WHERE SESSION_ID = SESSION_ID()",
                    String.class);
            BigDecimal first = readAlice();
            System.out.println(">>> T1 [" + level + "] first read  = " + first);
            firstReadDone.countDown();
            await(updateCommitted);
            BigDecimal second = readAlice();
            System.out.println(">>> T1 [" + level + "] second read = " + second);
            return new BigDecimal[] {first, second};
        }));

        CompletableFuture<Void> writer = CompletableFuture.runAsync(() -> {
            await(firstReadDone);
            t2.executeWithoutResult(status ->
                    jdbc.update("UPDATE accounts SET balance = balance - 300 WHERE owner_name = 'Alice'"));
            System.out.println(">>> T2 committed: balance - 300");
            updateCommitted.countDown();
        });

        writer.get(10, TimeUnit.SECONDS);
        return reader.get(10, TimeUnit.SECONDS);
    }

    static void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void printsTheDatabase() throws Exception {
        try (Connection c = dataSource.getConnection()) {
            System.out.println(">>> Database: " + c.getMetaData().getDatabaseProductName() + " "
                    + c.getMetaData().getDatabaseProductVersion() + ", default isolation = "
                    + c.getTransactionIsolation());
        }
    }

    @Test
    void readCommitted_secondReadSeesTheCommittedChange() throws Exception {
        BigDecimal[] reads = run(TransactionDefinition.ISOLATION_READ_COMMITTED);

        assertThat(reads[0]).isEqualByComparingTo("1000.00");
        assertThat(reads[1]).isEqualByComparingTo("700.00"); // non-repeatable read
    }

    @Test
    void repeatableRead_secondReadReturnsTheSameValue() throws Exception {
        BigDecimal[] reads = run(TransactionDefinition.ISOLATION_REPEATABLE_READ);

        assertThat(reads[0]).isEqualByComparingTo("1000.00");
        assertThat(reads[1]).isEqualByComparingTo("1000.00"); // a snapshot: T2's commit is invisible to T1
        assertThat(readAlice()).isEqualByComparingTo("700.00"); // but the change is really in the database
    }
}

package kz.iitu.springlab.bank;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

    private final AccountRepository repository;

    public TransferService(AccountRepository repository) {
        this.repository = repository;
    }

    // Task A: both changes are committed together or not at all
    @Transactional
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        Account from = repository.findById(fromId).orElseThrow();
        Account to = repository.findById(toId).orElseThrow();
        from.debit(amount);
        to.credit(amount);
        repository.save(from);
        repository.save(to);
    }

    // Task A.3: an unchecked exception after the debit and before the credit
    @Transactional
    public void transferFailingAfterDebit(Long fromId, Long toId, BigDecimal amount) {
        Account from = repository.findById(fromId).orElseThrow();
        Account to = repository.findById(toId).orElseThrow();
        from.debit(amount);
        repository.saveAndFlush(from); // the UPDATE really reaches the database inside the transaction
        if (to != null) { // always true; the "if" only keeps the credit below compilable
            throw new RuntimeException("Simulated failure after debit");
        }
        to.credit(amount);
        repository.save(to);
    }

    // Task A.4: a checked exception, default rules: the transaction is COMMITTED
    @Transactional
    public void transferFailingChecked(Long fromId, Long toId, BigDecimal amount) throws TransferAuditException {
        Account from = repository.findById(fromId).orElseThrow();
        from.debit(amount);
        repository.save(from);
        throw new TransferAuditException("Audit service unavailable");
    }

    // Task A.4: the same checked exception, but rollbackFor makes Spring roll back
    @Transactional(rollbackFor = TransferAuditException.class)
    public void transferFailingCheckedWithRollbackFor(Long fromId, Long toId, BigDecimal amount)
            throws TransferAuditException {
        Account from = repository.findById(fromId).orElseThrow();
        from.debit(amount);
        repository.save(from);
        throw new TransferAuditException("Audit service unavailable");
    }
}

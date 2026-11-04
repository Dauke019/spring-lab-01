package kz.iitu.springlab.bank;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The outer methods of task B: write an audit record, then fail after the inner method has returned. */
@Service
public class BankOperations {

    private final TransferService transfers;
    private final AuditService audit;

    public BankOperations(TransferService transfers, AuditService audit) {
        this.transfers = transfers;
        this.audit = audit;
    }

    @Transactional
    public void transferAuditRequiredThenFail(Long fromId, Long toId, BigDecimal amount) {
        transfers.transfer(fromId, toId, amount);
        audit.logRequired("REQUIRED: transfer " + amount + " from " + fromId + " to " + toId);
        throw new IllegalStateException("Outer transaction fails after the audit");
    }

    @Transactional
    public void transferAuditRequiresNewThenFail(Long fromId, Long toId, BigDecimal amount) {
        transfers.transfer(fromId, toId, amount);
        audit.logRequiresNew("REQUIRES_NEW: transfer " + amount + " from " + fromId + " to " + toId);
        throw new IllegalStateException("Outer transaction fails after the audit");
    }
}

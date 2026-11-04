package kz.iitu.springlab.bank;

/** A checked exception: by default Spring does NOT roll back for it (task A.4). */
public class TransferAuditException extends Exception {

    public TransferAuditException(String message) {
        super(message);
    }
}

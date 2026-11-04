package kz.iitu.springlab.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import kz.iitu.springlab.bank.Account;
import kz.iitu.springlab.bank.AccountRepository;
import kz.iitu.springlab.bank.TransferService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AccountRestController {

    private final AccountRepository accounts;
    private final TransferService transfers;

    public AccountRestController(AccountRepository accounts, TransferService transfers) {
        this.accounts = accounts;
        this.transfers = transfers;
    }

    @GetMapping("/accounts")
    public List<Account> list() {
        return accounts.findAll();
    }

    // POST /api/transfers?from=1&to=2&amount=100.00
    @PostMapping("/transfers")
    public List<Account> transfer(@RequestParam Long from, @RequestParam Long to, @RequestParam BigDecimal amount) {
        transfers.transfer(from, to, amount);
        return accounts.findAll();
    }

    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> badRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    // the stale copy lost the race: report the conflict instead of overwriting
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> conflict(ObjectOptimisticLockingFailureException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "The account was changed by another transaction, reload and try again"));
    }
}

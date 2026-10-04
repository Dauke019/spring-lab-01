package kz.iitu.springlab.service;

import kz.iitu.springlab.audit.Audited;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

@Service
public class CatalogService {

    // Reference to our own proxy: calls through it pass the aspects (fix for self-invocation, task 4.5)
    @Autowired
    @Lazy
    private CatalogService self;

    public String findById(long id) {
        sleep(50);                              // simulating a database call
        return "Item no. " + id;
    }

    @Audited(action = "CATALOG_LIST", logArguments = true)
    public List<String> findAll(int limit) {
        sleep(300);                             // a deliberately «slow» method
        return IntStream.rangeClosed(1, limit)
                .mapToObj(i -> "Item no. " + i)
                .toList();
    }

    @Audited(action = "CATALOG_REMOVE")
    public String remove(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Invalid identifier: " + id);
        }
        return "Removed item no. " + id;
    }

    // BEFORE the fix: remove() is called via this, past the proxy — no advice on these calls
    public String removeTwice(long id) {
        String first  = remove(id);      // self-invocation: via this, past the proxy
        String second = remove(id + 1);  // self-invocation as well
        return first + "; " + second;
    }

    // AFTER the fix: remove() is called via self (the proxy) — advice runs on every call
    public String removeTwiceFixed(long id) {
        String first  = self.remove(id);
        String second = self.remove(id + 1);
        return first + "; " + second;
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}

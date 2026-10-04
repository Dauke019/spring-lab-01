package kz.iitu.springlab.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// Individual variant 1: call counter for the service layer
@Aspect
@Component
@Order(4)
public class CallCounterAspect {

    private static final Logger log =
            LoggerFactory.getLogger(CallCounterAspect.class);

    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();

    @Before("kz.iitu.springlab.aspect.Pointcuts.serviceLayer()")
    public void count(JoinPoint jp) {
        String method = jp.getSignature().toShortString();
        long n = counters.computeIfAbsent(method, k -> new AtomicLong()).incrementAndGet();
        log.info("[COUNT] {} — call no. {}", method, n);
    }

    public Map<String, Long> getStats() {
        Map<String, Long> stats = new TreeMap<>();
        counters.forEach((method, n) -> stats.put(method, n.get()));
        return stats;
    }
}

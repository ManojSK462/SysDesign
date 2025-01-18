package com.example.sysdesign.rateLimiter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TokenBucket {
    private final int capacity;
    private final int refillRate;
    private final AtomicInteger tokens;

    public TokenBucket() {
        this(10, 1); // Default: 10 tokens capacity, 1 token per second
    }

    public TokenBucket(int capacity, int refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.tokens = new AtomicInteger(capacity);
        System.out.println("Initial tokens: " + tokens.get());
    }

    public boolean tryConsume() {
        int currTokens = tokens.get();
        System.out.println("Attempting to consume: " + currTokens + " tokens available");
        return tokens.updateAndGet(currentTokens ->
                currentTokens > 0 ? currentTokens - 1 : currentTokens
        ) > 0;
    }
    @Scheduled(fixedRate = 30000)
    public void refill() {
        tokens.updateAndGet(currentTokens ->
                Math.min(capacity, currentTokens + refillRate)
        );
    }

    public int getAvailableTokens() {
        return tokens.get();
    }
}

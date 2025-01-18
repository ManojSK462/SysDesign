package com.example.sysdesign.rateLimiter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RateLimitController {

    private final TokenBucket tokenBucket;

    @Autowired
    public RateLimitController(TokenBucket tokenBucket) {
        this.tokenBucket = tokenBucket;
    }

    @GetMapping("/api/test")
    public ResponseEntity<String> testEndpoint() {
        if (tokenBucket.tryConsume()) {
            System.out.println("if block");
            return ResponseEntity.ok("Request processed successfully");
        } else {
            System.out.println("else block");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate limit exceeded. Please try again later.");
        }
    }

    @GetMapping("/api/tokens")
    public ResponseEntity<String> getAvailableTokens() {
        int availableTokens = tokenBucket.getAvailableTokens();
        return ResponseEntity.ok("Available tokens: " + availableTokens);
    }
}

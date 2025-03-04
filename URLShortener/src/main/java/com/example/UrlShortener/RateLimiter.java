package com.example.UrlShortener;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiter {
    private final Map<String, Integer> requestCounts = new ConcurrentHashMap<>();

    public boolean allowRequest(String clientIp) {
        long currTimeStamp = System.currentTimeMillis();
        int WINDOW_SIZE_MS = 60000;
        String key = clientIp + ":" + (currTimeStamp / WINDOW_SIZE_MS);

        if (requestCounts.containsKey(key)) {
            int count = requestCounts.get(key);
            int limit = 2;
            if (count >= limit) {
                return false;
            }
            requestCounts.put(key, count + 1);
        } else {
            requestCounts.put(key, 1);
        }

        return true;
    }
}

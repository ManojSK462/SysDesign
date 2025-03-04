package com.example.UrlShortener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class UrlCleanupJob {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Scheduled(fixedRate = 3600000) // Run every hour
    public void cleanupExpiredUrls() {
        Set<String> keys = redisTemplate.keys("*");
        for (String key : keys) {
            if (redisTemplate.getExpire(key) <= 0) {
                redisTemplate.delete(key);
            }
        }
    }
}

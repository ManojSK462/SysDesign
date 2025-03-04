package com.example.UrlShortener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
public class UrlShortenerService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ConsistentHash consistentHash;

    public String shortenUrl(String originalUrl) {
        String shortUrl = generateShortUrl(originalUrl);
        String server = consistentHash.getServer(shortUrl);
        redisTemplate.opsForValue().set(shortUrl, originalUrl, 24, TimeUnit.HOURS);
        redisTemplate.opsForValue().set(shortUrl + ":server", server, 24, TimeUnit.HOURS);
        return shortUrl;
    }

    private String generateShortUrl(String originalUrl) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(originalUrl.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().encodeToString(hash).substring(0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating short URL", e);
        }
    }

    public String getOriginalUrl(String shortUrl) {
        return redisTemplate.opsForValue().get(shortUrl);
    }

    public String getServerForShortUrl(String shortUrl) {
        return redisTemplate.opsForValue().get(shortUrl + ":server");
    }

}

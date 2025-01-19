package com.example.hashing.ConsistentHashing;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.time.Instant;

public class CacheServer {
    private final String serverId;
    private final ConcurrentMap<String, CacheEntry> cache;

    public CacheServer(String serverId) {
        this.serverId = serverId;
        this.cache = new ConcurrentHashMap<>();
    }

    public void set(String key, String value, long ttlSeconds) {
        long expiryTime = Instant.now().getEpochSecond() + ttlSeconds;
        cache.put(key, new CacheEntry(value, expiryTime));
    }

    public String get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }
        if (entry != null) {
            cache.remove(key);
        }
        return null;
    }

    public void delete(String key) {
        cache.remove(key);
    }
    public String getServerId() {
        return serverId;
    }

    private static class CacheEntry {
        private final String value;
        private final long expiryTime;

        public CacheEntry(String value, long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }
        public String getValue() {
            return value;
        }
        public boolean isExpired() {
            return Instant.now().getEpochSecond() > expiryTime;
        }
    }
}

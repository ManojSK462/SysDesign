package com.example.hashing.ConsistentHashing;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DistributedCache {
    private final ConsistentHash<CacheServer> consistentHash;
    private final Map<String, CacheServer> servers = new ConcurrentHashMap<>();

    public DistributedCache(List<CacheServer> initialServers) {
        consistentHash = new ConsistentHash<>(100); // Using 100 as default number of replicas
        for (CacheServer server : initialServers) {
            addServer(server);
        }
    }

    public void set(String key, String value, long ttlInSeconds) {
        CacheServer server = consistentHash.get(key);
        if (server != null) {
            server.set(key, value, ttlInSeconds);
        }
    }
    public String get(String key) {
        CacheServer server = consistentHash.get(key);
        return server != null ? server.get(key) : null;
    }
    public void delete(String key) {
        CacheServer server = consistentHash.get(key);
        if (server != null) {
            server.delete(key);
        }
    }

    public CacheServer getServerForKey(String key) {
        return consistentHash.get(key);
    }

    public List<String> getAllServerIds() {
        List<String> serverIds = new ArrayList<>();
        for (CacheServer server : servers.values()) {
            serverIds.add(server.getServerId());
        }
        return serverIds;
    }


    public void addServer(CacheServer server) {
        servers.put(server.getServerId(), server);
        consistentHash.add(server, 1); // Adding with default weight of 1
    }
    public void removeServer(String serverId) {
        CacheServer server = servers.remove(serverId);
        if (server != null) {
            consistentHash.remove(server);
        }
    }
    public void updateServerWeight(String serverId, int newWeight) {
        CacheServer server = servers.get(serverId);
        if (server != null) {
            consistentHash.updateWeight(server, newWeight);
        }
    }
}

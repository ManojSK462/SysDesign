package com.example.UrlShortener;

import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.SortedMap;
import java.util.TreeMap;

@Component
public class ConsistentHash {

    private final SortedMap<Integer, String> circle = new TreeMap<>();

    @Value("${urlshortener.servers}")
    private String servers;

    @PostConstruct
    public void init() {
        String[] serverList = servers.split(",");
        for (String server : serverList) {
            addServer(server.trim());
        }
    }

    public void addServer(String server) {
        int numberOfReplicas = 3;
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.put(Hashing.consistentHash(Hashing.sha256().hashString(server + i, StandardCharsets.UTF_8), Integer.MAX_VALUE), server);
        }
    }

    public String getServer(String key) {
        if (circle.isEmpty()) {
            return null;
        }
        int hash = Hashing.consistentHash(Hashing.sha256().hashString(key, StandardCharsets.UTF_8), Integer.MAX_VALUE);
        if (!circle.containsKey(hash)) {
            SortedMap<Integer, String> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }
}

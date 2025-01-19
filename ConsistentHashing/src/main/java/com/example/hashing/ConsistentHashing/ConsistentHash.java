package com.example.hashing.ConsistentHashing;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class ConsistentHash<T> {
    private final SortedMap<Long, T> circle = new TreeMap<>();
    private final int defaultReplicas;
    private final Map<T, Integer> nodeWeights = new HashMap<>();

    public ConsistentHash(int defaultReplicas) {
        this.defaultReplicas = defaultReplicas;
    }

    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(key.getBytes());
            byte[] digest = md.digest();
            long h = 0;
            for (int i = 0; i < 4; i++) {
                h <<= 8;
                h |= ((int) digest[i]) & 0xFF;
            }
            return h;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not found", e);
        }
    }

    public void add(T node, int weight) {
        nodeWeights.put(node, weight);
        int replicas = weight * defaultReplicas;
        for (int i = 0; i < replicas; i++) {
            circle.put(hash(node.toString() + i), node);
        }
    }

    public void remove(T node) {
        int weight = nodeWeights.getOrDefault(node, 1);
        int replicas = weight * defaultReplicas;
        for (int i = 0; i < replicas; i++) {
            circle.remove(hash(node.toString() + i));
        }
        nodeWeights.remove(node);
    }

    public T get(Object key) {
        if (circle.isEmpty()) {
            return null;
        }
        long hash = hash(key.toString());
        if (!circle.containsKey(hash)) {
            SortedMap<Long, T> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }

    public void updateWeight(T node, int newWeight) {
        remove(node);
        add(node, newWeight);
    }

    public Map<T, Integer> getNodeWeights() {
        return new HashMap<>(nodeWeights);
    }
}

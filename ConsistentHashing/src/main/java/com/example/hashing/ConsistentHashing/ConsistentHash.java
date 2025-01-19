package com.example.hashing.ConsistentHashing;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHash<T> {
    private final SortedMap<Long, T> ring = new TreeMap<>();
    private final int numberOfReplicas;

    public ConsistentHash(int numberOfReplicas, Collection<T> nodes) {
        this.numberOfReplicas = numberOfReplicas;

        for (T node : nodes) {
            add(node);
        }
    }

    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(key.getBytes());
            byte[] digest = md.digest();

            // Convert first 4 bytes to long
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

    public void add(T node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            ring.put(hash(node.toString() + i), node);
        }
    }

    public void remove(T node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            ring.remove(hash(node.toString() + i));
        }
    }

    public T get(Object key) {
        if (ring.isEmpty()) {
            return null;
        }

        long hash = hash(key.toString());

        // If hash not in ring, find next higher hash
        if (!ring.containsKey(hash)) {
            SortedMap<Long, T> tailMap = ring.tailMap(hash);
            hash = tailMap.isEmpty()
                    ? ring.firstKey()
                    : tailMap.firstKey();
        }

        return ring.get(hash);
    }
}

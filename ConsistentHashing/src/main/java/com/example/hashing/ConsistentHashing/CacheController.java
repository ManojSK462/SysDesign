package com.example.hashing.ConsistentHashing;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cache")
public class CacheController {
    private final DistributedCache distributedCache;

    public CacheController(DistributedCache distributedCache) {
        this.distributedCache = distributedCache;
    }

    @PostMapping("/{key}")
    public void set(@PathVariable String key, @RequestBody String value, @RequestParam(defaultValue = "60") long ttl) {
        distributedCache.set(key, value, ttl);
    }

    @GetMapping("/{key}")
    public String get(@PathVariable String key) {
        return distributedCache.get(key);
    }

    @DeleteMapping("/{key}")
    public void delete(@PathVariable String key) {
        distributedCache.delete(key);
    }

    @GetMapping("/server/{key}")
    public String getServer(@PathVariable String key) {
        CacheServer server = distributedCache.getServerForKey(key);
        return server != null ? server.getServerId() : "No server found";
    }

    @PostMapping("/server")
    public String addServer(@RequestParam String serverId) {
        CacheServer newServer = new CacheServer(serverId);
        distributedCache.addServer(newServer);
        return "Server " + serverId + " added successfully";
    }

    @DeleteMapping("/server/{serverId}")
    public String removeServer(@PathVariable String serverId) {
        distributedCache.removeServer(serverId);
        return "Server " + serverId + " removed successfully";
    }

    @GetMapping("/servers")
    public List<String> getAllServers() {
        return distributedCache.getAllServerIds();
    }

    @PostMapping("/server/{serverId}/weight")
    public String updateServerWeight(@PathVariable String serverId, @RequestParam int weight) {
        distributedCache.updateServerWeight(serverId, weight);
        return "Weight updated for server " + serverId;
    }
}

package com.example.hashing.ConsistentHashing;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

//    @GetMapping("/{key}")
//    public String get(@PathVariable String key) {
//        return distributedCache.get(key);
//    }

    @GetMapping("/{key}")
    public ResponseEntity<String> get(@PathVariable String key) {
        String value = distributedCache.get(key);
        if (value == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Value not found for key: " + key);
        }
        return ResponseEntity.ok(value);
    }

//    @DeleteMapping("/{key}")
//    public void delete(@PathVariable String key) {
//        distributedCache.delete(key);
//    }

    @DeleteMapping("/{key}")
    public ResponseEntity<String> delete(@PathVariable String key) {
        String value = distributedCache.get(key);
        if (value == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Value not found for key: " + key);
        }
        distributedCache.delete(key);
        return ResponseEntity.ok("Value deleted for key: " + key);
    }


    //    @GetMapping("/server/{key}")
//    public String getServer(@PathVariable String key) {
//        CacheServer server = distributedCache.getServerForKey(key);
//        return server != null ? server.getServerId() : "No server found";
//    }
    @GetMapping("/server/{key}")
    public ResponseEntity<String> getServer(@PathVariable String key) {
        CacheServer server = distributedCache.getServerForKey(key);
        if (server == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No server found for key: " + key);
        }
        return ResponseEntity.ok(server.getServerId());
    }


    @PostMapping("/server")
    public String addServer(@RequestParam String serverId) {
        CacheServer newServer = new CacheServer(serverId);
        distributedCache.addServer(newServer);
        return "Server " + serverId + " added successfully";
    }

//    @DeleteMapping("/server/{serverId}")
//    public String removeServer(@PathVariable String serverId) {
//        distributedCache.removeServer(serverId);
//        return "Server " + serverId + " removed successfully";
//    }

    @DeleteMapping("/server/{serverId}")
    public ResponseEntity<String> removeServer(@PathVariable String serverId) {
        if (!distributedCache.servers.containsKey(serverId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Server not found: " + serverId);
        }
        distributedCache.removeServer(serverId);
        return ResponseEntity.ok("Server " + serverId + " removed successfully");
    }

    @GetMapping("/servers")
    public List<String> getAllServers() {
        return distributedCache.getAllServerIds();
    }

    @PostMapping("/server/{serverId}/weight")
    public ResponseEntity<String> updateServerWeight(@PathVariable String serverId, @RequestParam int weight) {

        if (!distributedCache.servers.containsKey(serverId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error: Server not found: " + serverId);
        }
        if (weight < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: Weight must be a non-negative integer.");
        }
        distributedCache.updateServerWeight(serverId, weight);
        return ResponseEntity.ok("Weight updated for server " + serverId + " to " + weight);
    }

}

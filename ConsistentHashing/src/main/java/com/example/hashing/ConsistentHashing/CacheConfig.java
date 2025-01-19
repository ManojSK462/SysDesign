package com.example.hashing.ConsistentHashing;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CacheConfig {
    @Bean
    public List<CacheServer> initialCacheServers() {
        return Arrays.asList(
                new CacheServer("server1"),
                new CacheServer("server2"),
                new CacheServer("server3")
        );
    }
    @Bean
    public DistributedCache distributedCache(List<CacheServer> initialCacheServers) {
        return new DistributedCache(initialCacheServers);
    }
}

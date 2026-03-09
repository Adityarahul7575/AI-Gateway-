package com.aditya.aigateway.ratelimit;


import com.aditya.aigateway.config.ModelRoutingConfig;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
public class RateLimiterService {

    private final ModelRoutingConfig config;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>() ;

    @Autowired
    public RateLimiterService(ModelRoutingConfig config) {
        this.config = config;
    }

    //if request is allowed ok

    public boolean isAllowed(String clientId){
        Bucket bucket = buckets.computeIfAbsent(clientId,this::createNewBucket);
        boolean allowed = bucket.tryConsume(1);
        if (!allowed) {
            log.warn("Rate limit exceeded | clientId={}", clientId);
        }

        return allowed;

    }

    public long getAvailableTokens(String clientId) {
        Bucket bucket = buckets.computeIfAbsent(clientId, this::createNewBucket);
        return bucket.getAvailableTokens();
    }

    private Bucket createNewBucket(String clientId) {
        int requestsPerMinute = config.getRateLimit().getRequestsPerMinute();
        int burstCapacity = config.getRateLimit().getBurstCapacity();

        log.debug("Creating rate limit bucket | clientId={} rpm={} burst={}",
                clientId, requestsPerMinute, burstCapacity);

        // Allow burst first, then steady rate
        Bandwidth steadyRate = Bandwidth.builder()
                .capacity(requestsPerMinute)
                .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                .build();

        Bandwidth burst = Bandwidth.builder()
                .capacity(burstCapacity)
                .refillGreedy(burstCapacity, Duration.ofSeconds(10))
                .build();

        return Bucket.builder()
                .addLimit(steadyRate)
                .addLimit(burst)
                .build();
    }



}

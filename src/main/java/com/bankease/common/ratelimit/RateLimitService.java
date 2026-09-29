package com.bankease.common.ratelimit;

import com.bankease.common.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // 10 requests per minute for sensitive operations (OTP, login)
    public Bucket resolveBucketSensitive(String key) {
        return buckets.computeIfAbsent("sensitive:" + key, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(10)
                                .refillGreedy(10, Duration.ofMinutes(1))
                                .build())
                        .build());
    }

    // 100 requests per minute for general API
    public Bucket resolveBucketGeneral(String key) {
        return buckets.computeIfAbsent("general:" + key, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(100)
                                .refillGreedy(100, Duration.ofMinutes(1))
                                .build())
                        .build());
    }

    // 5 requests per minute for payment operations
    public Bucket resolveBucketPayment(String key) {
        return buckets.computeIfAbsent("payment:" + key, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(5)
                                .refillGreedy(5, Duration.ofMinutes(1))
                                .build())
                        .build());
    }

    public void checkSensitiveRateLimit(String key) {
        Bucket bucket = resolveBucketSensitive(key);
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Too many requests. Please try again after a minute.");
        }
    }

    public void checkGeneralRateLimit(String key) {
        Bucket bucket = resolveBucketGeneral(key);
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Rate limit exceeded. Please slow down your requests.");
        }
    }

    public void checkPaymentRateLimit(String key) {
        Bucket bucket = resolveBucketPayment(key);
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Payment rate limit exceeded. Please wait before making another payment.");
        }
    }
}

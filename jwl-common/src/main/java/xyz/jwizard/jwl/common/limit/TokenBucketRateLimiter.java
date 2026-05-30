/*
 * Copyright (c) 2022-2026 JWizard. All Rights Reserved.
 *
 * NOTICE: This source code is publicly available for reference
 * and educational purposes only. It is NOT open-source software.
 *
 * You are granted permission to view this code. However, you are strictly
 * PROHIBITED from copying, modifying, or merging this code into other software,
 * distributing, publishing, or sublicensing this code, using this code for
 * commercial purposes or in production environments.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO WARRANTIES OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 *
 * Please refer to the LICENSE file in the root directory for full restrictions.
 */
package xyz.jwizard.jwl.common.limit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.common.util.Assert;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class TokenBucketRateLimiter implements RateLimiter {
  private static final Logger LOG = LoggerFactory.getLogger(TokenBucketRateLimiter.class);

  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
  private final Supplier<Bucket> bucketSupplier;

  private TokenBucketRateLimiter(Builder builder) {
    final Bandwidth limit =
        Bandwidth.builder()
            .capacity(builder.capacity)
            .refillGreedy(builder.refillTokens, builder.refillPeriod)
            .build();
    bucketSupplier = () -> Bucket.builder().addLimit(limit).build();
    LOG.debug(
        "Initialized TokenBucketRateLimiter with capacity: {}, refill tokens: {}, "
            + "refill period: {}ms",
        builder.capacity,
        builder.refillTokens,
        builder.refillPeriod.toMillis());
  }

  public static Builder builder() {
    return new Builder();
  }

  public static TokenBucketRateLimiter createDefault() {
    return new TokenBucketRateLimiter(builder());
  }

  @Override
  public boolean tryAcquire(String key) {
    final Bucket bucket = buckets.computeIfAbsent(key, k -> bucketSupplier.get());
    return bucket.tryConsume(1);
  }

  @Override
  public void reset(String key) {
    buckets.remove(key);
  }

  public static class Builder {
    private long capacity = 20;
    private long refillTokens = 10;
    private Duration refillPeriod = Duration.ofSeconds(1);

    private Builder() {}

    public Builder capacity(long capacity) {
      this.capacity = capacity;
      return this;
    }

    public Builder refillTokens(long refillTokens) {
      this.refillTokens = refillTokens;
      return this;
    }

    public Builder refillPeriod(Duration refillPeriod) {
      this.refillPeriod = refillPeriod;
      return this;
    }

    public RateLimiter build() {
      Assert.notNull(refillPeriod, "RefillPeriod cannot be null");
      return new TokenBucketRateLimiter(this);
    }
  }
}

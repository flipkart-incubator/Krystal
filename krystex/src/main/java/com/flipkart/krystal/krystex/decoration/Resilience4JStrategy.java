package com.flipkart.krystal.krystex.decoration;

import static com.flipkart.krystal.krystex.RateLimitingStrategy.SEMAPHORE;

import com.flipkart.krystal.krystex.LogicDecorationStrategy;
import com.flipkart.krystal.krystex.Node;
import com.flipkart.krystal.krystex.NonBlockingNodeDefinition;
import com.flipkart.krystal.krystex.RateLimitingStrategy;
import com.flipkart.krystal.krystex.config.ConfigProvider;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.decorators.Decorators.DecorateFunction;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.internal.SemaphoreBasedRateLimiter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

// TODO move to a dedicated module
public class Resilience4JStrategy implements LogicDecorationStrategy {

  private final ConfigProvider configProvider;

  private final Map<String, RateLimiter> rateLimiters = new HashMap<>();

  public Resilience4JStrategy(ConfigProvider configProvider) {
    this.configProvider = configProvider;
  }

  @Override
  public <T> Function<ImmutableMap<String, ?>, CompletableFuture<ImmutableList<T>>> decorateLogic(
      Node<T> node,
      Function<ImmutableMap<String, ?>, CompletableFuture<ImmutableList<T>>> logicToDecorate) {
    if (node.definition() instanceof NonBlockingNodeDefinition<?>) {
      return node.definition()::logic;
    }
    DecorateFunction<ImmutableMap<String, ?>, CompletableFuture<ImmutableList<T>>>
        decorateCompletionStage = Decorators.ofFunction(logicToDecorate);
    decorateWithRateLimiter(node, decorateCompletionStage);
    decorateWithCircuitBreaker(node, decorateCompletionStage);
    return decorateCompletionStage.decorate();
  }

  private <T> void decorateWithCircuitBreaker(
      Node<T> node,
      DecorateFunction<ImmutableMap<String, ?>, CompletableFuture<ImmutableList<T>>>
          decorateFunction) {
    // TODO Implement circuit breaker
  }

  private <T> void decorateWithRateLimiter(
      Node<T> node,
      DecorateFunction<ImmutableMap<String, ?>, CompletableFuture<ImmutableList<T>>>
          decorateFunction) {
    RateLimiter rateLimiter = rateLimiters.get(node.definition().nodeId());
    if (rateLimiter == null) {
      rateLimiter =
          rateLimiters.computeIfAbsent(
              node.definition().nodeId(), nodeId -> createRateLimiter(nodeId).orElse(null));
    } else {
      updateRateLimiter(node.definition().nodeId(), rateLimiter);
    }
    if (rateLimiter != null) {
      decorateFunction.withRateLimiter(rateLimiter);
    }
  }

  private void updateRateLimiter(String nodeId, RateLimiter rateLimiter) {
    try {
      Optional<Integer> rateLimit = configProvider.getInt(nodeId + ".concurrentExecutions");
      configProvider
          .getString(nodeId + ".rate_limiting_strategy")
          .map(RateLimitingStrategy::valueOf)
          .ifPresent(
              rateLimitingStrategy -> {
                if (SEMAPHORE.equals(rateLimitingStrategy) && rateLimit.isPresent()) {
                  if (rateLimiter.getRateLimiterConfig().getLimitForPeriod() != rateLimit.get()) {
                    rateLimiter.changeLimitForPeriod(rateLimit.get());
                  }
                }
              });
    } catch (Exception e) {
      // TODO Log exception
    }
  }

  private Optional<RateLimiter> createRateLimiter(String nodeId) {
    try {

      Optional<Integer> rateLimit = configProvider.getInt(nodeId + ".concurrentExecutions");
      Optional<String> rateLimitingStrategy =
          configProvider.getString(nodeId + ".rate_limiting_strategy");
      if (rateLimitingStrategy.isEmpty() || rateLimit.isEmpty()) {
        return Optional.empty();
      }
      if (SEMAPHORE.equals(RateLimitingStrategy.valueOf(rateLimitingStrategy.get()))) {
        return Optional.of(
            new SemaphoreBasedRateLimiter(
                nodeId, RateLimiterConfig.custom().limitForPeriod(rateLimit.get()).build()));
      }
    } catch (Exception e) {
      // TODO Log exception
    }
    return Optional.empty();
  }
}

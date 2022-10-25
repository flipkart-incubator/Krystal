package com.flipkart.krystal.krystex.decoration;

import static com.flipkart.krystal.krystex.RateLimitingStrategy.SEMAPHORE;

import com.flipkart.krystal.krystex.LogicDecorationStrategy;
import com.flipkart.krystal.krystex.Node;
import com.flipkart.krystal.krystex.NonBlockingNodeDefinition;
import com.flipkart.krystal.krystex.RateLimitingStrategy;
import com.flipkart.krystal.krystex.config.ConfigProvider;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.decorators.Decorators.DecorateCompletionStage;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.internal.SemaphoreBasedRateLimiter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

// TODO move to a dedicated module
public class Resilience4JStrategy implements LogicDecorationStrategy {

  private final ConfigProvider configProvider;

  private final Map<String, RateLimiter> rateLimiters = new HashMap<>();

  public Resilience4JStrategy(ConfigProvider configProvider) {
    this.configProvider = configProvider;
  }

  @Override
  public <T> Supplier<CompletionStage<T>> decorateLogic(Node<T> node) {
    if (node.definition() instanceof NonBlockingNodeDefinition<?> result) {
      return () -> node.definition().logic();
    }
    DecorateCompletionStage<T> decorateCompletionStage =
        Decorators.ofCompletionStage(() -> node.definition().logic());
    decorateWithRateLimiter(node, decorateCompletionStage);
    decorateWithCircuitBreaker(node, decorateCompletionStage);
    return decorateCompletionStage.decorate();
  }

  private <T> void decorateWithCircuitBreaker(
      Node<T> node, DecorateCompletionStage<T> decorateCompletionStage) {}

  private <T> void decorateWithRateLimiter(
      Node<T> node, DecorateCompletionStage<T> decorateCompletionStage) {
    RateLimiter rateLimiter = rateLimiters.get(node.definition().nodeId());
    if (rateLimiter == null) {
      rateLimiter =
          rateLimiters.computeIfAbsent(
              node.definition().nodeId(), nodeId -> createRateLimiter(nodeId).orElse(null));
    } else {
      updateRateLimiter(node.definition().nodeId(), rateLimiter);
    }
    if (rateLimiter != null) {
      decorateCompletionStage.withRateLimiter(rateLimiter);
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

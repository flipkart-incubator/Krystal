package com.flipkart.krystal.krystex.decoration;

import static com.flipkart.krystal.krystex.RateLimitingStrategy.SEMAPHORE;

import com.flipkart.krystal.krystex.MultiResult;
import com.flipkart.krystal.krystex.NodeDecorator;
import com.flipkart.krystal.krystex.NodeDefinition;
import com.flipkart.krystal.krystex.NodeInputs;
import com.flipkart.krystal.krystex.NodeLogic;
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

// TODO move to a dedicated module
public class Resilience4JStrategy<T> implements NodeDecorator<T> {

  private final ConfigProvider configProvider;

  private final Map<String, RateLimiter> rateLimiters = new HashMap<>();

  public Resilience4JStrategy(ConfigProvider configProvider) {
    this.configProvider = configProvider;
  }

  @Override
  public NodeLogic<T> decorateLogic(NodeDefinition<T> nodeDef, NodeLogic<T> logicToDecorate) {
    if (nodeDef instanceof NonBlockingNodeDefinition<?>) {
      return logicToDecorate;
    }
    DecorateFunction<ImmutableList<NodeInputs>, ImmutableMap<NodeInputs, MultiResult<T>>>
        decorateCompletionStage = Decorators.ofFunction(logicToDecorate);
    decorateWithRateLimiter(nodeDef, decorateCompletionStage);
    decorateWithCircuitBreaker(nodeDef, decorateCompletionStage);
    return decorateCompletionStage.decorate()::apply;
  }

  private void decorateWithRateLimiter(
      NodeDefinition<T> nodeDef, DecorateFunction<?, ?> decorateFunction) {
    RateLimiter rateLimiter = rateLimiters.get(nodeDef.nodeId());
    if (rateLimiter == null) {
      rateLimiter =
          rateLimiters.computeIfAbsent(
              nodeDef.nodeId(), nodeId -> createRateLimiter(nodeId).orElse(null));
    } else {
      updateRateLimiter(nodeDef.nodeId(), rateLimiter);
    }
    if (rateLimiter != null) {
      decorateFunction.withRateLimiter(rateLimiter);
    }
  }

  private void decorateWithCircuitBreaker(
      NodeDefinition<T> nodeDef, DecorateFunction<?, ?> decorateFunction) {
    // TODO Implement circuit breaker
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

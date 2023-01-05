package com.flipkart.krystal.krystex.decoration;

import static com.flipkart.krystal.krystex.RateLimitingStrategy.SEMAPHORE;

import com.flipkart.krystal.krystex.MultiResultFuture;
import com.flipkart.krystal.krystex.RateLimitingStrategy;
import com.flipkart.krystal.krystex.config.ConfigProvider;
import com.flipkart.krystal.krystex.node.NodeDecorator;
import com.flipkart.krystal.krystex.node.NodeInputs;
import com.flipkart.krystal.krystex.node.NodeLogic;
import com.flipkart.krystal.krystex.node.NodeLogicDefinition;
import com.flipkart.krystal.krystex.node.NodeLogicId;
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

  private final Map<NodeLogicId, RateLimiter> rateLimiters = new HashMap<>();

  public Resilience4JStrategy(ConfigProvider configProvider) {
    this.configProvider = configProvider;
  }

  @Override
  public NodeLogic<T> decorateLogic(NodeLogicDefinition<T> nodeDef, NodeLogic<T> logicToDecorate) {
    DecorateFunction<ImmutableList<NodeInputs>, ImmutableMap<NodeInputs, MultiResultFuture<T>>>
        decorateCompletionStage = Decorators.ofFunction(logicToDecorate);
    decorateWithRateLimiter(nodeDef, decorateCompletionStage);
    decorateWithCircuitBreaker(nodeDef, decorateCompletionStage);
    return decorateCompletionStage.decorate()::apply;
  }

  private void decorateWithRateLimiter(
      NodeLogicDefinition<T> nodeDef, DecorateFunction<?, ?> decorateFunction) {
    RateLimiter rateLimiter = rateLimiters.get(nodeDef.nodeLogicId());
    if (rateLimiter == null) {
      rateLimiter =
          rateLimiters.computeIfAbsent(
              nodeDef.nodeLogicId(), nodeId -> createRateLimiter(nodeId).orElse(null));
    } else {
      updateRateLimiter(nodeDef.nodeLogicId(), rateLimiter);
    }
    if (rateLimiter != null) {
      decorateFunction.withRateLimiter(rateLimiter);
    }
  }

  private void decorateWithCircuitBreaker(
      NodeLogicDefinition<T> nodeDef, DecorateFunction<?, ?> decorateFunction) {
    // TODO Implement circuit breaker
  }

  private void updateRateLimiter(NodeLogicId nodeLogicId, RateLimiter rateLimiter) {
    try {
      Optional<Integer> rateLimit = configProvider.getInt(nodeLogicId + ".concurrentExecutions");
      configProvider
          .getString(nodeLogicId + ".rate_limiting_strategy")
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

  private Optional<RateLimiter> createRateLimiter(NodeLogicId nodeLogicId) {
    try {

      Optional<Integer> rateLimit = configProvider.getInt(nodeLogicId + ".concurrentExecutions");
      Optional<String> rateLimitingStrategy =
          configProvider.getString(nodeLogicId + ".rate_limiting_strategy");
      if (rateLimitingStrategy.isEmpty() || rateLimit.isEmpty()) {
        return Optional.empty();
      }
      if (SEMAPHORE.equals(RateLimitingStrategy.valueOf(rateLimitingStrategy.get()))) {
        return Optional.of(
            new SemaphoreBasedRateLimiter(
                nodeLogicId.asString(),
                RateLimiterConfig.custom().limitForPeriod(rateLimit.get()).build()));
      }
    } catch (Exception e) {
      // TODO Log exception
    }
    return Optional.empty();
  }
}

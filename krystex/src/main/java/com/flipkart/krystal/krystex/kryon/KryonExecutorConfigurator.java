package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig.KryonExecutorConfigBuilder;

/**
 * Provides a way to configure a {@link KryonExecutorConfigBuilder} with default configurations.
 * This allows for configuration code to be reused instead of repeated multiple times in application
 * code. If an application needs custom configurations, it can directly configure the
 * KryonExecutorConfigBuilder.
 */
@FunctionalInterface
public interface KryonExecutorConfigurator {
  KryonExecutorConfigurator NO_OP = configBuilder -> {};

  void addToConfig(KryonExecutorConfigBuilder configBuilder);
}

package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.krystex.KrystalExecutorConfig.KrystalExecutorConfigBuilder;

/**
 * Provides a way to configure a {@link KrystalExecutorConfigBuilder} with default configurations.
 * This allows for configuration code to be reused instead of repeated multiple times in application
 * code. If an application needs custom configurations, it can directly configure the
 * KryonExecutorConfigBuilder.
 */
@FunctionalInterface
public interface KryonExecutorConfigurator {
  KryonExecutorConfigurator NO_OP = configBuilder -> {};

  void addToConfig(KrystalExecutorConfigBuilder configBuilder);

  interface KryonExecutorConfiguratorProvider {
    KryonExecutorConfigurator asKryonExecutorConfigurator();
  }
}

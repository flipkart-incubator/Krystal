package com.flipkart.krystal.config;

import java.util.Optional;

public record NestedConfig(String configPrefix, ConfigProvider delegate)
    implements ConfigProvider {

  @Override
  public <T> Optional<T> getConfig(String key) {
    return delegate.getConfig(configPrefix + key);
  }
}

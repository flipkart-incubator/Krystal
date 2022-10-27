package com.flipkart.krystal.krystex.config;

import java.util.Map;
import java.util.Optional;

public record DefaultConfigProvider(Map<String, Object> configs) implements ConfigProvider {

  @Override
  public <T> T getConfig(String key) {
    //noinspection unchecked
    return (T) configs.get(key);
  }

  @Override
  public Optional<String> getString(String key) {
    return Optional.ofNullable(getConfig(key));
  }

  @Override
  public Optional<Integer> getInt(String key) {
    return Optional.ofNullable(getConfig(key));
  }
}

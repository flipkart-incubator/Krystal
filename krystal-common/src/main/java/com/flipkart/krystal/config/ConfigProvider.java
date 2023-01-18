package com.flipkart.krystal.config;

import java.util.Optional;

public interface ConfigProvider {

  <T> Optional<T> getConfig(String key);
}

package com.flipkart.krystal.krystex.config;

import java.util.Optional;

public interface ConfigProvider {

  <T> Optional<T> getConfig(String key);
}

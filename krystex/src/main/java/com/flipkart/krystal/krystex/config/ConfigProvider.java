package com.flipkart.krystal.krystex.config;

import java.util.Optional;

public interface ConfigProvider {

  <T> T getConfig(String key);

  Optional<String> getString(String key);

  Optional<Integer> getInt(String key);
}

package com.flipkart.krystal.config;

public interface ConfigListener {

  default void onConfigUpdate(ConfigProvider configProvider) {}
}

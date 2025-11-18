package com.flipkart.krystal.lattice.ext.rest.config;

import lombok.Builder;

@Builder(toBuilder = true)
public record RestServerConfig(String name, int port) {

  public RestServerConfig {
    if (name == null) {
      name = "";
    }
  }

  public RestServerConfig withNameIfNotNamed(String name) {
    if (!this.name.isBlank()) {
      return this;
    }
    return toBuilder().name(name).build();
  }
}

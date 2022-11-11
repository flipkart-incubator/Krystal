package com.flipkart.krystal.vajram.exec.test_vajrams.hello;

import com.flipkart.krystal.vajram.RequestBuilder;
import com.flipkart.krystal.vajram.VajramRequest;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;

public record HelloRequest(String name) implements VajramRequest {

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public ImmutableMap<String, Optional<Object>> asMap() {
    return ImmutableMap.<String, Optional<Object>>builder()
        .put("name", Optional.ofNullable(name()))
        .build();
  }

  static class Builder implements RequestBuilder<HelloRequest> {

    private String name;

    String name() {
      return name;
    }

    Builder name(String name) {
      this.name = name;
      return this;
    }

    @Override
    public HelloRequest build() {
      return new HelloRequest(name);
    }

    private Builder() {}
  }
}

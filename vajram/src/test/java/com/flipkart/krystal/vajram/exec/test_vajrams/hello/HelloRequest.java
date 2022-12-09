package com.flipkart.krystal.vajram.exec.test_vajrams.hello;

import com.flipkart.krystal.vajram.RequestBuilder;
import com.flipkart.krystal.vajram.VajramRequest;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public record HelloRequest(String name) implements VajramRequest {

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public ImmutableMap<String, ?> asMap() {
    Map<String, Object> builder = new HashMap<>();
    builder.put("name", name());
    return ImmutableMap.copyOf(Maps.filterValues(builder, Objects::nonNull));
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

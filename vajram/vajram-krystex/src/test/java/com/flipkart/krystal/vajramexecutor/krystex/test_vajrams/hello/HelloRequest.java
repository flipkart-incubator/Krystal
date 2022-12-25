package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello;

import com.flipkart.krystal.vajram.RequestBuilder;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.inputs.SingleValue;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

public record HelloRequest(String name) implements VajramRequest {

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public ImmutableMap<String, SingleValue<?>> asMap() {
    Map<String, SingleValue<?>> builder = new HashMap<>();
    builder.put("name", new SingleValue<>(name()));
    return ImmutableMap.copyOf(builder);
  }

  public static class Builder implements RequestBuilder<HelloRequest> {

    private String name;

    String name() {
      return name;
    }

    public Builder name(String name) {
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

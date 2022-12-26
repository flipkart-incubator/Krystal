package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello;

import com.flipkart.krystal.vajram.RequestBuilder;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.inputs.ValueOrError;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record HelloRequest(String name, Optional<String> greeting) implements VajramRequest {

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public ImmutableMap<String, ValueOrError<?>> asMap() {
    Map<String, ValueOrError<?>> builder = new HashMap<>();
    builder.put("name", new ValueOrError<>(name()));
    builder.put("greeting", new ValueOrError<>(greeting()));
    return ImmutableMap.copyOf(builder);
  }

  public static class Builder implements RequestBuilder<HelloRequest> {

    private String name;
    private Optional<String> greeting;

    String name() {
      return name;
    }

    Optional<String> greeting() {
      return greeting;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder greeting(String greeting) {
      this.greeting = greeting.describeConstable();
      return this;
    }

    @Override
    public HelloRequest build() {
      return new HelloRequest(name, greeting);
    }

    private Builder() {}
  }
}

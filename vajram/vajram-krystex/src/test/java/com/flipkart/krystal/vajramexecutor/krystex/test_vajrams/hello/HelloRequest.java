package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello;

import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.inputs.ValueOrError;
import com.google.common.collect.ImmutableMap;
import java.lang.Override;
import java.lang.String;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class HelloRequest implements VajramRequest {
  private final String name;

  private final String greeting;

  private HelloRequest(String name, String greeting) {
    this.name = name;
    this.greeting = greeting;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String name() {
    return this.name;
  }

  public Optional<String> greeting() {
    return Optional.ofNullable(this.greeting);
  }

  @Override
  public ImmutableMap<String, ValueOrError<?>> asMap() {
    Map<String, ValueOrError<?>> builder = new HashMap<>();;
    builder.put("name", new ValueOrError<>(name()));
    builder.put("greeting", new ValueOrError<>(greeting()));
    return ImmutableMap.copyOf(builder);
  }

  public static class Builder {
    private String name;

    private String greeting;

    private Builder() {
    }

    public String name() {
      return this.name;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public String greeting() {
      return this.greeting;
    }

    public Builder greeting(String greeting) {
      this.greeting = greeting;
      return this;
    }

    public HelloRequest build() {
      return new HelloRequest(name, greeting);
    }
  }
}

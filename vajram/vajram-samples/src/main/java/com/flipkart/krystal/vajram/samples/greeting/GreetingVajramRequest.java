package com.flipkart.krystal.vajram.samples.greeting;

import com.flipkart.krystal.vajram.RequestBuilder;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.inputs.ValueOrError;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// Auto-generated and managed by Krystal
public record GreetingVajramRequest(String userId) implements VajramRequest {

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public ImmutableMap<String, ValueOrError<?>> asMap() {
    Map<String, ValueOrError<?>> builder = new HashMap<>();
    builder.put("user_id", new ValueOrError<>(userId()));
    return ImmutableMap.copyOf(Maps.filterValues(builder, Objects::nonNull));
  }

  public static class Builder implements RequestBuilder<GreetingVajramRequest> {

    private String userId;

    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

    @Override
    public GreetingVajramRequest build() {
      return new GreetingVajramRequest(this.userId);
    }

    private Builder() {}
  }
}

package com.flipkart.krystal.vajram.samples.greeting;

import com.flipkart.krystal.vajram.RequestBuilder;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.inputs.ValueOrError;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

// Auto-generated and managed by Krystal
public record UserServiceVajramRequest(String userId) implements VajramRequest {

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public ImmutableMap<String, ValueOrError<?>> asMap() {
    Map<String, ValueOrError<?>> builder = new HashMap<>();
    builder.put("user_id", new ValueOrError<>(userId()));
    return ImmutableMap.copyOf(builder);
  }

  public static final class Builder implements RequestBuilder<UserServiceVajramRequest> {

    private String userId;

    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

    @Override
    public UserServiceVajramRequest build() {
      return new UserServiceVajramRequest(this.userId);
    }

    private Builder() {}
  }
}

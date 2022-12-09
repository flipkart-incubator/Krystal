package com.flipkart.krystal.vajram.samples.greeting;

import com.flipkart.krystal.vajram.RequestBuilder;
import com.flipkart.krystal.vajram.VajramRequest;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// Auto-generated and managed by Krystal
public record UserServiceVajramRequest(String userId) implements VajramRequest {

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public ImmutableMap<String, ?> asMap() {
    Map<String, Object> map = new HashMap<>();
    map.put("user_id", userId());
    return ImmutableMap.copyOf(Maps.filterValues(map, Objects::nonNull));
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

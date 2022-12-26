package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2;

import static com.flipkart.krystal.vajram.inputs.ValueOrError.empty;

import com.flipkart.krystal.vajram.RequestBuilder;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.inputs.ValueOrError;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

// Auto-generated and managed by Krystal
public record HelloFriendsV2Request(String userId) implements VajramRequest {

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public ImmutableMap<String, ValueOrError<?>> asMap() {
    Map<String, ValueOrError<?>> map = new HashMap<>();
    map.put("user_id", new ValueOrError<>(userId()));
    return ImmutableMap.copyOf(map);
  }

  static HelloFriendsV2Request fromMap(ImmutableMap<String, ValueOrError<?>> values) {
    return HelloFriendsV2Request.builder()
        .userId((String) values.getOrDefault("user_id", empty()).value().orElse(null))
        .build();
  }

  public static class Builder implements RequestBuilder<HelloFriendsV2Request> {

    private String userId;

    Builder() {}

    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

    public HelloFriendsV2Request build() {
      return new HelloFriendsV2Request(userId);
    }
  }
}

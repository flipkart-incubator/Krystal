package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice;

import com.flipkart.krystal.vajram.RequestBuilder;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.inputs.SingleValue;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

// Auto-generated and managed by Krystal
public record TestUserServiceRequest(String userId) implements VajramRequest {

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public ImmutableMap<String, SingleValue<?>> asMap() {
    Map<String, SingleValue<?>> map = new HashMap<>();
    if (userId() != null) {
      map.put("user_id", new SingleValue<>(userId()));
    }
    return ImmutableMap.copyOf(map);
  }

  public static final class Builder implements RequestBuilder<TestUserServiceRequest> {

    private String userId;

    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

    @Override
    public TestUserServiceRequest build() {
      return new TestUserServiceRequest(this.userId);
    }

    private Builder() {}
  }
}

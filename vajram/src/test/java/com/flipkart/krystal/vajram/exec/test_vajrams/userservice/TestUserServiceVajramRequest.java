package com.flipkart.krystal.vajram.exec.test_vajrams.userservice;

import com.flipkart.krystal.vajram.RequestBuilder;
import com.flipkart.krystal.vajram.VajramRequest;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

// Auto-generated and managed by Krystal
public record TestUserServiceVajramRequest(String userId) implements VajramRequest {

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public ImmutableMap<String, ?> asMap() {
    Map<String, Object> map = new HashMap<>();
    if (userId() != null) {
      map.put("user_id", userId());
    }
    return ImmutableMap.copyOf(map);
  }

  public static final class Builder implements RequestBuilder<TestUserServiceVajramRequest> {

    private String userId;

    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

    @Override
    public TestUserServiceVajramRequest build() {
      return new TestUserServiceVajramRequest(this.userId);
    }

    private Builder() {}
  }
}

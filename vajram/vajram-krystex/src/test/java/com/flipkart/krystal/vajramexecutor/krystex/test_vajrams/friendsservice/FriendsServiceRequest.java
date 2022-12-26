package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice;

import com.flipkart.krystal.vajram.RequestBuilder;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.inputs.ValueOrError;
import com.google.common.collect.ImmutableMap;

public record FriendsServiceRequest(String userId) implements VajramRequest {

  public static FriendsServiceRequestBuilder builder() {
    return new FriendsServiceRequestBuilder();
  }

  @Override
  public ImmutableMap<String, ValueOrError<?>> asMap() {
    return ImmutableMap.of("user_id", new ValueOrError<>(userId()));
  }

  public static class FriendsServiceRequestBuilder implements RequestBuilder {

    private String userId;

    FriendsServiceRequestBuilder() {}

    public FriendsServiceRequestBuilder userId(String userId) {
      this.userId = userId;
      return this;
    }

    public FriendsServiceRequest build() {
      return new FriendsServiceRequest(userId);
    }
  }
}

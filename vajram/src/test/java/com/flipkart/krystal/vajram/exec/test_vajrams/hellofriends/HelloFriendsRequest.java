package com.flipkart.krystal.vajram.exec.test_vajrams.hellofriends;

import com.flipkart.krystal.vajram.RequestBuilder;
import com.flipkart.krystal.vajram.VajramRequest;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

// Auto-generated and managed by Krystal
public record HelloFriendsRequest(String userId, int numberOfFriends) implements VajramRequest {

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public ImmutableMap<String, ?> asMap() {
    Map<String, Object> map = new HashMap<>();
    map.put("user_id", userId());
    map.put("number_of_friends", numberOfFriends());
    return ImmutableMap.copyOf(map);
  }

  static HelloFriendsRequest fromMap(ImmutableMap<String, Object> values) {
    return HelloFriendsRequest.builder()
        .userId((String) values.get("user_id"))
        .numberOfFriends((Integer) values.get("number_of_friends"))
        .build();
  }

  public static class Builder implements RequestBuilder<HelloFriendsRequest> {

    private String userId;
    private int numberOfFriends;

    Builder() {}

    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

    public Builder numberOfFriends(int numberOfUsers) {
      this.numberOfFriends = numberOfUsers;
      return this;
    }

    public HelloFriendsRequest build() {
      return new HelloFriendsRequest(userId, numberOfFriends);
    }
  }
}

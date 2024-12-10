package com.flipkart.krystal.vajramexecutor.krystex;

import java.util.Optional;
import lombok.Data;
import lombok.Setter;

@Data
public class TestRequestContext {
  private String requestId;
  @Setter private Optional<String> loggedInUserId;
  @Setter private int numberOfFriends;

  public TestRequestContext(Optional<String> loggedInUserId, int numberOfFriends) {
    this.loggedInUserId = loggedInUserId;
    this.numberOfFriends = numberOfFriends;
  }

  public void requestId(String requestId) {
    this.requestId = requestId;
  }

  public Optional<String> loggedInUserId() {
    return loggedInUserId;
  }

  public int numberOfFriends() {
    return numberOfFriends;
  }

  public String requestId() {
    return requestId;
  }
}

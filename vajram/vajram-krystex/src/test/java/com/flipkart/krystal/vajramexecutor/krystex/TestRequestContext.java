package com.flipkart.krystal.vajramexecutor.krystex;

import com.flipkart.krystal.vajram.ApplicationRequestContext;
import java.util.Optional;

// @Builder
public class TestRequestContext implements ApplicationRequestContext {
  String requestId;
  Optional<String> loggedInUserId;
  int numberOfFriends;

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

  public void setLoggedInUserId(Optional<String> loggedInUserId) {
    this.loggedInUserId = loggedInUserId;
  }

  public int numberOfFriends() {
    return numberOfFriends;
  }

  public void setNumberOfFriends(int numberOfFriends) {
    this.numberOfFriends = numberOfFriends;
  }

  @Override
  public String requestId() {
    return requestId;
  }
}

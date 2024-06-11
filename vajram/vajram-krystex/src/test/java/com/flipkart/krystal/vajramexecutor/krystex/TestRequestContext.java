package com.flipkart.krystal.vajramexecutor.krystex;

import com.flipkart.krystal.vajram.ApplicationRequestContext;
import java.util.Optional;
import lombok.Data;
import lombok.Setter;

@Data
public class TestRequestContext implements ApplicationRequestContext {
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

  @Override
  public String requestId() {
    return requestId;
  }
}

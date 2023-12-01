package com.flipkart.krystal.vajramexecutor.krystex.testharness.mock_repository;

import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.vajram.samples.greeting.UserInfo;
import com.flipkart.krystal.vajram.samples.greeting.UserServiceRequest;
import java.io.IOException;

public final class UserServiceMocks {
  public static final String USER_ID = "user@123";
  public static final String NAME = "Ranchoddas Shyamakdas Chanchad";

  private UserServiceMocks() {}

  public static MockData<UserInfo> getUserInfoSuccess() {
    return new MockData<>(
        UserServiceRequest.builder().userId(USER_ID).build(),
        ValueOrError.withValue(new UserInfo(USER_ID, NAME)));
  }

  public static MockData<UserInfo> getUserInfoFailureWhenTimeOut() {
    return new MockData<>(
        UserServiceRequest.builder().userId(USER_ID).build(),
        ValueOrError.withError(new IOException("Timeout")));
  }

  public static MockData<UserInfo> getUserInfoFailureWhenServiceDown() {
    return new MockData<>(
        UserServiceRequest.builder().userId(USER_ID).build(),
        ValueOrError.withError(new IOException("Server unavailable")));
  }
}

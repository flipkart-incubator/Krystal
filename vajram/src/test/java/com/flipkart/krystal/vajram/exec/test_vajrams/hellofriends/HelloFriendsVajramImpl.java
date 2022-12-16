package com.flipkart.krystal.vajram.exec.test_vajrams.hellofriends;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.flipkart.krystal.vajram.ExecutionContextMap;
import com.flipkart.krystal.vajram.exec.test_vajrams.hellofriends.HelloFriendsInputUtils.EnrichedRequest;
import com.flipkart.krystal.vajram.exec.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajram.inputs.InputValues;
import com.flipkart.krystal.vajram.inputs.SingleValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;

public class HelloFriendsVajramImpl extends HelloFriendsVajram {

  @Override
  public ImmutableList<InputValues> resolveInputOfDependency(
      String dependency,
      ImmutableSet<String> resolvableInputs,
      ExecutionContextMap executionContext) {
    HelloFriendsRequest helloFriendsRequest = HelloFriendsRequest.fromMap(executionContext.asMap());
    switch (dependency) {
      case "user_service":
        {
          if (Set.of("user_id").equals(resolvableInputs)) {
            return userIdsForUserService(
                    helloFriendsRequest.userId(), helloFriendsRequest.numberOfFriends())
                .stream()
                .map(s -> new InputValues(ImmutableMap.of("user_id", new SingleValue<Object>(s))))
                .collect(toImmutableList());
          }
        }
    }
    throw new IllegalArgumentException();
  }

  @Override
  public String executeNonBlocking(ExecutionContextMap executionContext) {
    Object value = executionContext.getValue(HelloFriendsVajram.USER_SERVICE);
    Collection<TestUserInfo> testUserInfos;
    if (value instanceof TestUserInfo testUserInfo) {
      testUserInfos = ImmutableList.of(testUserInfo);
    } else {
      //noinspection unchecked
      testUserInfos = (Collection<TestUserInfo>) value;
    }
    return sayHellos(
        new EnrichedRequest(HelloFriendsRequest.fromMap(executionContext.asMap()), testUserInfos));
  }
}

package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.flipkart.krystal.vajram.ExecutionContextMap;
import com.flipkart.krystal.vajram.inputs.InputValues;
import com.flipkart.krystal.vajram.inputs.ValueOrError;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsInputUtils.EnrichedRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserInfo;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.Set;

public class HelloFriendsVajramImpl extends HelloFriendsVajram {

  @Override
  public ImmutableList<InputValues> resolveInputOfDependency(
      String dependency,
      ImmutableSet<String> resolvableInputs,
      ExecutionContextMap executionContext) {
    String userId = executionContext.getValue("user_id");
    Optional<Integer> numberOfFriends = executionContext.optValue("number_of_friends");
    switch (dependency) {
      case USER_INFO:
        {
          if (Set.of("user_id").equals(resolvableInputs)) {
            return ImmutableList.of(
                new InputValues(
                    ImmutableMap.of("user_id", new ValueOrError<>(userIdForUserService(userId)))));
          }
        }
      case FRIEND_INFOS:
        {
          if (Set.of("user_id").equals(resolvableInputs)) {
            if (numberOfFriends.isPresent()) {
              return friendIdsForUserService(userId, numberOfFriends.get()).stream()
                  .map(
                      s -> new InputValues(ImmutableMap.of("user_id", new ValueOrError<Object>(s))))
                  .collect(toImmutableList());
            } else {
              return ImmutableList.of(new InputValues());
            }
          }
        }
    }
    throw new IllegalArgumentException();
  }

  @Override
  public String executeCompute(ExecutionContextMap executionContext) {
    ImmutableList<TestUserInfo> friendInfos = executionContext.getValue(FRIEND_INFOS);
    ImmutableList<TestUserInfo> userInfo = executionContext.getValue(USER_INFO);
    return sayHellos(
        new EnrichedRequest(
            HelloFriendsRequest.fromMap(executionContext.asMap()),
            userInfo.iterator().next(),
            friendInfos));
  }
}

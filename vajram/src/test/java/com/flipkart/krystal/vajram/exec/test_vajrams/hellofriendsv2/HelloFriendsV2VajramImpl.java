package com.flipkart.krystal.vajram.exec.test_vajrams.hellofriendsv2;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.flipkart.krystal.vajram.ExecutionContextMap;
import com.flipkart.krystal.vajram.exec.test_vajrams.hellofriendsv2.HelloFriendsV2InputUtils.EnrichedRequest;
import com.flipkart.krystal.vajram.exec.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajram.inputs.InputValues;
import com.flipkart.krystal.vajram.inputs.SingleValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.Set;

public class HelloFriendsV2VajramImpl extends HelloFriendsV2Vajram {

  @Override
  public ImmutableList<InputValues> resolveInputOfDependency(
      String dependency,
      ImmutableSet<String> resolvableInputs,
      ExecutionContextMap executionContext) {
    Optional<String> userId = executionContext.context().<String>getValue("user_id").value();
    Optional<Set<String>> friendIds =
        executionContext
            .context()
            .<ImmutableList<Set<String>>>getValue("friend_ids")
            .value()
            .map(sets -> sets.get(0));
    switch (dependency) {
      case USER_INFO:
        {
          if (Set.of("user_id").equals(resolvableInputs)) {
            if (friendIds.isPresent()) {
              Set<String> userIdsForUserService = userIdsForUserService(friendIds.get());
              return userIdsForUserService.stream()
                  .map(s -> new InputValues(ImmutableMap.of("user_id", new SingleValue<>(s))))
                  .collect(toImmutableList());
            } else {
              return ImmutableList.of(new InputValues());
            }
          }
        }
      case FRIEND_IDS:
        {
          if (Set.of("user_id").equals(resolvableInputs)) {
            if (userId.isPresent()) {
              return ImmutableList.of(
                  new InputValues(
                      ImmutableMap.of(
                          "user_id",
                          new SingleValue<Object>(userIdForFriendService(userId.get())))));
            } else {
              return ImmutableList.of(new InputValues());
            }
          }
        }
    }
    throw new IllegalArgumentException();
  }

  @Override
  public String executeNonBlocking(ExecutionContextMap executionContext) {
    ImmutableList<Set<String>> friendInfos = executionContext.getValue(FRIEND_IDS);
    ImmutableList<TestUserInfo> userInfo = executionContext.getValue(USER_INFO);
    return sayHellos(
        new EnrichedRequest(
            HelloFriendsV2Request.fromMap(executionContext.asMap()),
            friendInfos.iterator().next(),
            userInfo));
  }
}

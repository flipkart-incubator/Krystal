package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends;

import static com.flipkart.krystal.vajram.inputs.MultiExecute.executeFanoutWith;
import static com.flipkart.krystal.vajram.inputs.MultiExecute.skipFanout;
import static com.flipkart.krystal.vajram.inputs.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.inputs.resolution.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.inputs.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest.friendInfos_n;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest.numberOfFriends_n;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest.userId_n;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest.userId_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest.userInfos_s;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.MultiExecute;
import com.flipkart.krystal.vajram.inputs.Using;
import com.flipkart.krystal.vajram.inputs.resolution.InputResolver;
import com.flipkart.krystal.vajram.inputs.resolution.Resolve;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsInputUtil.HelloFriendsInputs;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceRequest;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.stream.IntStream;

@VajramDef(HelloFriendsVajram.ID)
public abstract class HelloFriendsVajram extends ComputeVajram<String> {

  public static final String ID = "HelloFriendsVajram";

  @Override
  public ImmutableCollection<InputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(
            userInfos_s,
            depInput(TestUserServiceRequest.userId_s).usingAsIs(userId_s).asResolver()));
  }

  @Resolve(depName = friendInfos_n, depInputs = TestUserServiceRequest.userId_n)
  public static MultiExecute<String> friendIdsForUserService(
      @Using(userId_n) String userId, @Using(numberOfFriends_n) Optional<Integer> numberOfFriends) {
    if (numberOfFriends.isEmpty()) {
      return skipFanout("numberOfFriends is missing ");
    } else if (numberOfFriends.get() < 1) {
      return skipFanout("numberOfFriends is less than 1");
    } else {
      return executeFanoutWith(getFriendsFor(userId, numberOfFriends.get()));
    }
  }

  @VajramLogic
  public static String sayHellos(HelloFriendsInputs request) throws Exception {
    return "Hello Friends of %s! %s"
        .formatted(
            request
                .userInfos()
                .getOrThrow(
                    TestUserServiceRequest.builder().userId(request.userId()).build(),
                    () ->
                        new IllegalArgumentException(
                            "Did not receive userInfo of user %s".formatted(request.userId())))
                .userName(),
            request.friendInfos().values().stream()
                .filter(voe -> voe.value().isPresent())
                .map(voe -> voe.value().get())
                .map(TestUserInfo::userName)
                .collect(joining(", ")));
  }

  private static ImmutableSet<String> getFriendsFor(String userId, int numberOfFriends) {
    return IntStream.range(1, numberOfFriends + 1)
        .mapToObj(i -> userId + ":friend_" + i)
        .collect(toImmutableSet());
  }
}

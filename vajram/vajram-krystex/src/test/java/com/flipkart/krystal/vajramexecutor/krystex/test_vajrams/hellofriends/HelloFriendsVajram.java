package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.Resolve;
import com.flipkart.krystal.vajram.inputs.Using;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsInputUtil.HelloFriendsAllInputs;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceVajram;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

@VajramDef(HelloFriendsVajram.ID)
public abstract class HelloFriendsVajram extends ComputeVajram<String> {

  public static final String ID = "HelloFriendsVajram";

  public static final String USER_ID = "user_id";
  public static final String NUMBER_OF_FRIENDS = "number_of_friends";

  public static final String USER_INFOS = "user_infos";
  public static final String FRIEND_INFOS = "friend_infos";

  @Resolve(depName = USER_INFOS, depInputs = TestUserServiceVajram.USER_ID)
  public static String userIdForUserService(@Using(USER_ID) String userId) {
    return userId;
  }

  @Resolve(depName = FRIEND_INFOS, depInputs = TestUserServiceVajram.USER_ID)
  public static Set<String> friendIdsForUserService(
      @Using(USER_ID) String userId, @Using(NUMBER_OF_FRIENDS) Optional<Integer> numberOfFriends) {
    if (numberOfFriends.isPresent()) {
      return getFriendsFor(userId, numberOfFriends.get());
    } else return Collections.emptySet();
  }

  @VajramLogic
  public static String sayHellos(HelloFriendsAllInputs request) throws Exception {
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

  private ImmutableSet<String> getFriendsFor(String userId, int numberOfFriends) {
    return IntStream.range(1, numberOfFriends + 1)
        .mapToObj(i -> userId + ":friend_" + i)
        .collect(toImmutableSet());
  }
}

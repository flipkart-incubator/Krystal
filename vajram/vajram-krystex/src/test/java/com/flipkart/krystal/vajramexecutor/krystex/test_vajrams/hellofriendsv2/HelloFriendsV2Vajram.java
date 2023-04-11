package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2;

import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Vajram.ID;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.DependencyResponse;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.From;
import com.flipkart.krystal.vajram.inputs.ResolveDep;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceVajram;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2InputUtil.HelloFriendsV2AllInputs;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceVajram;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@VajramDef(ID)
public abstract class HelloFriendsV2Vajram extends ComputeVajram<String> {

  public static final String ID = "HelloFriendsV2Vajram";

  public static final String USER_ID = "user_id";

  public static final String FRIEND_IDS = "friend_ids";
  public static final String FRIEND_INFOS = "friend_infos";

  @ResolveDep(depName = FRIEND_IDS, depInputs = FriendsServiceVajram.USER_ID)
  public static String userIdForFriendService(@From(USER_ID) String userId) {
    return userId;
  }

  @ResolveDep(depName = FRIEND_INFOS, depInputs = TestUserServiceVajram.USER_ID)
  public static Set<String> userIdsForUserService(
      @From(FRIEND_IDS) DependencyResponse<FriendsServiceRequest, Set<String>> friendIds) {
    return friendIds.values().stream()
        .map(ValueOrError::value)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .flatMap(Collection::stream)
        .collect(toImmutableSet());
  }

  @VajramLogic
  public String sayHellos(HelloFriendsV2AllInputs request) {
    return "Hello Friends! %s"
        .formatted(
            request.friendInfos().values().stream()
                .map(voe -> voe.value().orElse(null))
                .filter(Objects::nonNull)
                .map(TestUserInfo::userName)
                .collect(joining(", ")));
  }
}

package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.DependencyResponse;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.ValueOrError;
import com.flipkart.krystal.vajram.inputs.BindFrom;
import com.flipkart.krystal.vajram.inputs.Resolve;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceVajram;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2InputUtil.AllInputs;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceVajram;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@VajramDef(HelloFriendsV2Vajram.ID)
public abstract class HelloFriendsV2Vajram extends ComputeVajram<String> {

  public static final String ID = "HelloFriendsV2Vajram";

  public static final String USER_ID = "user_id";

  public static final String FRIEND_IDS = "friend_ids";
  public static final String FRIEND_INFOS = "friend_infos";

  @Resolve(value = FRIEND_IDS, inputs = FriendsServiceVajram.USER_ID)
  public String userIdForFriendService(@BindFrom(USER_ID) String userId) {
    return userId;
  }

  @Resolve(value = FRIEND_INFOS, inputs = TestUserServiceVajram.USER_ID)
  public static Set<String> userIdsForUserService(
      @BindFrom(FRIEND_IDS) DependencyResponse<FriendsServiceRequest, Set<String>> friendIds) {
    return friendIds.values().stream()
        .map(ValueOrError::value)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .flatMap(Collection::stream)
        .collect(toImmutableSet());
  }

  @VajramLogic
  public static String sayHellos(AllInputs request) {
    return "Hello Friends! %s"
        .formatted(
            request.friendInfos().values().stream()
                .filter(voe -> voe.value().isPresent())
                .map(voe -> voe.value().get())
                .map(TestUserInfo::userName)
                .collect(joining(", ")));
  }
}

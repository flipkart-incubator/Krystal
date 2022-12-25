package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2;

import static com.flipkart.krystal.datatypes.StringType.string;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.vajram.NonBlockingVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.BindFrom;
import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.Resolve;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceVajram;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2InputUtils.EnrichedRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceVajram;
import com.google.common.collect.ImmutableList;
import java.util.Set;

@VajramDef(HelloFriendsV2Vajram.ID)
public abstract class HelloFriendsV2Vajram extends NonBlockingVajram<String> {

  public static final String ID = "HelloFriendsV2Vajram";

  public static final String USER_ID = "user_id";

  public static final String USER_INFO = "user_info";
  public static final String FRIEND_IDS = "friend_ids";

  @Override
  public ImmutableList<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(
        Input.builder().name(USER_ID).type(string()).mandatory().build(),
        Dependency.builder()
            .name(FRIEND_IDS)
            .dataAccessSpec(new VajramID(FriendsServiceVajram.ID))
            .mandatory(true)
            .build(),
        Dependency.builder()
            .name(USER_INFO)
            .dataAccessSpec(new VajramID(TestUserServiceVajram.ID))
            .mandatory(true)
            .build());
  }

  @Resolve(value = FRIEND_IDS, inputs = FriendsServiceVajram.USER_ID)
  public String userIdForFriendService(@BindFrom(USER_ID) String userId) {
    return userId;
  }

  @Resolve(value = USER_INFO, inputs = TestUserServiceVajram.USER_ID)
  public Set<String> userIdsForUserService(@BindFrom(FRIEND_IDS) Set<String> userIds) {
    return userIds;
  }

  @VajramLogic
  public String sayHellos(EnrichedRequest request) {
    return "Hello Friends! %s"
        .formatted(request.userInfo().stream().map(TestUserInfo::userName).collect(joining(", ")));
  }
}

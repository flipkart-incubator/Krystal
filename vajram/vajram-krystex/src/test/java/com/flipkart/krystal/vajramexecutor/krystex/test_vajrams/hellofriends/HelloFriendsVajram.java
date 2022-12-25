package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends;

import static com.flipkart.krystal.datatypes.IntegerType.integer;
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
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsInputUtils.EnrichedRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceVajram;
import com.google.common.collect.ImmutableList;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@VajramDef(HelloFriendsVajram.ID)
public abstract class HelloFriendsVajram extends NonBlockingVajram<String> {

  public static final String ID = "HelloFriendsVajram";

  public static final String USER_ID = "user_id";
  public static final String NUMBER_OF_FRIENDS = "number_of_friends";

  public static final String USER_INFO = "user_infos";
  public static final String FRIEND_INFOS = "friend_infos";

  @Override
  public ImmutableList<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(
        Input.builder().name(USER_ID).type(string()).mandatory().build(),
        Input.builder().name(NUMBER_OF_FRIENDS).type(integer()).defaultValue(2).build(),
        Dependency.builder()
            .name(USER_INFO)
            .dataAccessSpec(new VajramID(TestUserServiceVajram.ID))
            .mandatory(true)
            .build(),
        Dependency.builder()
            .name(FRIEND_INFOS)
            .dataAccessSpec(new VajramID(TestUserServiceVajram.ID))
            .mandatory(true)
            .build());
  }

  @Resolve(value = FRIEND_INFOS, inputs = TestUserServiceVajram.USER_ID)
  public Set<String> friendIdsForUserService(
      @BindFrom(USER_ID) String userId, @BindFrom(NUMBER_OF_FRIENDS) int numberOfFriends) {
    return getFriendsFor(userId, numberOfFriends);
  }

  @Resolve(value = USER_INFO, inputs = TestUserServiceVajram.USER_ID)
  public String userIdForUserService(@BindFrom(USER_ID) String userId) {
    return userId;
  }

  @VajramLogic
  public String sayHellos(EnrichedRequest request) {
    return "Hello Friends of %s! %s"
        .formatted(
            request.userInfo().userName(),
            request.friendInfos().stream().map(TestUserInfo::userName).collect(joining(", ")));
  }

  private Set<String> getFriendsFor(String userId, int numberOfFriends) {
    return IntStream.range(1, numberOfFriends + 1)
        .mapToObj(operand -> userId + ":friend_" + operand)
        .collect(Collectors.toSet());
  }
}

package com.flipkart.krystal.vajram.exec.test_vajrams.hellofriends;

import static com.flipkart.krystal.datatypes.IntegerType.integer;
import static com.flipkart.krystal.datatypes.StringType.string;
import static com.flipkart.krystal.vajram.exec.test_vajrams.hellofriends.HelloFriendsVajram.ID;

import com.flipkart.krystal.vajram.NonBlockingVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.exec.test_vajrams.hellofriends.HelloFriendsInputUtils.EnrichedRequest;
import com.flipkart.krystal.vajram.exec.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajram.exec.test_vajrams.userservice.TestUserServiceVajram;
import com.flipkart.krystal.vajram.inputs.BindFrom;
import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.Resolve;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.google.common.collect.ImmutableList;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@VajramDef(ID)
public abstract class HelloFriendsVajram extends NonBlockingVajram<String> {

  public static final String ID = "HelloFriendsVajram";

  public static final String USER_ID = "user_id";
  public static final String NUMBER_OF_FRIENDS = "number_of_friends";

  public static final String USER_SERVICE = "user_service";

  @Override
  public ImmutableList<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(/*Ram*/ /*Prasad*/
        Input.builder().name(USER_ID).type(string()).mandatory().build(),
        Input.builder().name(NUMBER_OF_FRIENDS).type(integer()).defaultValue(2).build(),
        Dependency.builder()
            .name(USER_SERVICE)
            .dataAccessSpec(new VajramID(TestUserServiceVajram.ID))
            .mandatory(true)
            .build());
  }

  @Resolve(value = USER_SERVICE, inputs = TestUserServiceVajram.USER_ID)
  public Set<String> userIdsForUserService(
      @BindFrom(USER_ID) String userId, @BindFrom(NUMBER_OF_FRIENDS) int numberOfFriends) {
    return getFriendsFor(userId, numberOfFriends);
  }

  @VajramLogic
  public String sayHellos(EnrichedRequest request) {
    return "Hello Friends! %s"
        .formatted(
            request.userInfos().stream().map(TestUserInfo::userName).collect(Collectors.toList()));
  }

  private Set<String> getFriendsFor(String userId, int numberOfFriends) {
    return IntStream.range(1, numberOfFriends + 1)
        .mapToObj(operand -> userId + ":friend_" + operand)
        .collect(Collectors.toSet());
  }
}

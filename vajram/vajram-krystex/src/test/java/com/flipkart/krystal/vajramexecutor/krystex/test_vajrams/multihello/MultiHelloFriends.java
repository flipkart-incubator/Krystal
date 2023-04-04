package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello;

import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriends.ID;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.From;
import com.flipkart.krystal.vajram.inputs.ResolveInputsOf;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsVajram;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriendsInputUtil.MultiHelloFriendsAllInputs;
import java.util.ArrayList;
import java.util.List;

@VajramDef(ID)
public abstract class MultiHelloFriends extends ComputeVajram<String> {
  public static final String ID = "MultiHelloFriends";

  @ResolveInputsOf(dep = "hellos", depInputs = HelloFriendsVajram.USER_ID)
  public static List<String> userIdsForHellos(@From("user_ids") List<String> userIds) {
    return userIds;
  }

  @ResolveInputsOf(dep = "hellos", depInputs = HelloFriendsVajram.NUMBER_OF_FRIENDS)
  public static List<Integer> numberOfFriendsForHellos(@From("user_ids") List<String> userIds) {
    return List.of(1, 2);
  }

  @VajramLogic
  public static String sayHellos(MultiHelloFriendsAllInputs allInputs) {
    ArrayList<String> userIds = allInputs.userIds();
    List<Integer> numberOfFriends = numberOfFriendsForHellos(userIds);

    List<String> result = new ArrayList<>();
    for (String userId : userIds) {
      for (Integer numberOfFriend : numberOfFriends) {
        allInputs
            .hellos()
            .get(
                HelloFriendsRequest.builder()
                    .userId(userId)
                    .numberOfFriends(numberOfFriend)
                    .build())
            .value()
            .ifPresent(result::add);
      }
    }
    return String.join("\n", result);
  }
}

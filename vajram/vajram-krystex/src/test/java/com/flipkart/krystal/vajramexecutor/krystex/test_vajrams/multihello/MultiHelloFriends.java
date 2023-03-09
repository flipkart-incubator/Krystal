package com.flipkart.krystal.vajramexecutor.krystex.testVajrams.multihello;

import static com.flipkart.krystal.vajramexecutor.krystex.testVajrams.multihello.MultiHelloFriends.ID;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.BindFrom;
import com.flipkart.krystal.vajram.inputs.Resolve;
import com.flipkart.krystal.vajramexecutor.krystex.testVajrams.hellofriends.HelloFriendsRequest;
import com.flipkart.krystal.vajramexecutor.krystex.testVajrams.hellofriends.HelloFriendsVajram;
import com.flipkart.krystal.vajramexecutor.krystex.testVajrams.multihello.MultiHelloFriendsInputUtil.MultiHelloFriendsAllInputs;
import java.util.ArrayList;
import java.util.List;

@VajramDef(ID)
public abstract class MultiHelloFriends extends ComputeVajram<String> {
  public static final String ID = "MultiHelloFriends";

  @Resolve(value = "hellos", inputs = HelloFriendsVajram.USER_ID)
  public List<String> userIdsForHellos(@BindFrom("user_ids") List<String> userIds) {
    return userIds;
  }

  @Resolve(value = "hellos", inputs = HelloFriendsVajram.NUMBER_OF_FRIENDS)
  public List<Integer> numberOfFriendsForHellos(@BindFrom("user_ids") List<String> userIds) {
    return List.of(1, 2);
  }

  @VajramLogic
  public String sayHellos(MultiHelloFriendsAllInputs allInputs) {
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

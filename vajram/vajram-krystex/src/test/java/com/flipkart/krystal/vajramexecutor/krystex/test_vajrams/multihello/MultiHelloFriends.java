package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello;

import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest.numberOfFriends_n;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest.userId_n;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriends.ID;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriendsRequest.hellos_n;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriendsRequest.userIds_n;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.Resolve;
import com.flipkart.krystal.vajram.inputs.Using;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriendsInputUtil.MultiHelloFriendsAllInputs;
import java.util.ArrayList;
import java.util.List;

@VajramDef(ID)
public abstract class MultiHelloFriends extends ComputeVajram<String> {
  public static final String ID = "MultiHelloFriends";

  @Resolve(depName = hellos_n, depInputs = userId_n)
  public static List<String> userIdsForHellos(@Using(userIds_n) List<String> userIds) {
    return userIds;
  }

  @Resolve(depName = hellos_n, depInputs = numberOfFriends_n)
  public static List<Integer> numberOfFriendsForHellos(@Using(userIds_n) List<String> userIds) {
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

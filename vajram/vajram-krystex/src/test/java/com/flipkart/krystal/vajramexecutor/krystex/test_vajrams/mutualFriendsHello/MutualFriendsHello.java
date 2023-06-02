package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.mutualFriendsHello;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.DependencyResponse;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.flipkart.krystal.vajram.inputs.DependencyCommand.MultiExecute;
import com.flipkart.krystal.vajram.inputs.Resolve;
import com.flipkart.krystal.vajram.inputs.Using;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceVajram;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Request;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Vajram;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.mutualFriendsHello.MutualFriendsHelloInputUtil.MutualFriendsHelloAllInputs;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@VajramDef("MutualFriendsHello")
public abstract class MutualFriendsHello extends ComputeVajram<String> {

  public static final String FRIEND_IDS = "friend_ids";
  public static final String FRIEND_INFOS = "friend_infos";
  public static final String USER_IDS = "user_ids";

  @VajramLogic
  public static String sayHelloToMutualFriends(
      MutualFriendsHelloAllInputs mutualFriendsHelloAllInputs) {
    LinkedHashSet<String> userIds = mutualFriendsHelloAllInputs.userIds();
    List<String> result = new ArrayList<>();
    for (String userId :
        mutualFriendsHelloAllInputs.friendIds().values().asList().get(0).value().get()) {
      mutualFriendsHelloAllInputs
          .hellos()
          .get(HelloFriendsV2Request.builder().userId(userId).build())
          .value()
          .ifPresent(result::add);
    }
    return String.join("\n", result);
  }

  @Resolve(depName = "friend_ids", depInputs = FriendsServiceVajram.USER_ID)
  public static MultiExecute<String> userIdForFriendService(@Using(USER_IDS) Set<String> userIds) {
    return DependencyCommand.multiExecuteWith(userIds);
  }

  @Resolve(depName = "hellos", depInputs = HelloFriendsV2Vajram.USER_ID)
  public static MultiExecute<String> userIDForHelloService(
      @Using(FRIEND_IDS) DependencyResponse<FriendsServiceRequest, Set<String>> friendIdMap) {

    Set<String> friendIds =
        friendIdMap.values().asList().stream()
            .flatMap(val -> val.value().stream())
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    return DependencyCommand.multiExecuteWith(friendIds);
  }
}

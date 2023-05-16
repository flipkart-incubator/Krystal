package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello;

import static com.flipkart.krystal.vajram.inputs.resolution.InputResolvers.resolveFanout;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest.numberOfFriends_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriends.ID;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriendsRequest.hellos_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriendsRequest.userIds_s;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.resolution.InputResolver;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriendsInputUtil.MultiHelloFriendsAllInputs;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

@VajramDef(ID)
public abstract class MultiHelloFriends extends ComputeVajram<String> {
  public static final String ID = "MultiHelloFriends";

  private static final List<Integer> NUMBER_OF_FRIENDS = List.of(1, 2);

  @Override
  public ImmutableCollection<InputResolver> getSimpleInputResolvers() {
    return ImmutableList.of(
        resolveFanout(hellos_s, HelloFriendsRequest.userId_s)
            .using(userIds_s)
            .asResolver(userIds -> userIds),
        resolveFanout(hellos_s, numberOfFriends_s)
            .using(userIds_s)
            .asResolver(userIds -> NUMBER_OF_FRIENDS));
  }

  @VajramLogic
  public static String sayHellos(MultiHelloFriendsAllInputs allInputs) {
    ArrayList<String> userIds = allInputs.userIds();
    List<Integer> numberOfFriends = NUMBER_OF_FRIENDS;

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

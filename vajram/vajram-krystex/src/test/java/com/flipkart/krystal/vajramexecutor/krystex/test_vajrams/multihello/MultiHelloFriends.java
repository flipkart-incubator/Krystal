package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello;

import static com.flipkart.krystal.vajram.inputs.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.inputs.resolution.InputResolvers.fanout;
import static com.flipkart.krystal.vajram.inputs.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriends.ID;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriendsRequest.hellos_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriendsRequest.userIds_s;
import static java.util.function.Function.identity;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.resolution.InputResolver;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriendsInputUtil.MultiHelloFriendsAllInputs;
import com.google.common.collect.ImmutableCollection;
import java.util.ArrayList;
import java.util.List;

@VajramDef(ID)
public abstract class MultiHelloFriends extends ComputeVajram<String> {
  public static final String ID = "MultiHelloFriends";

  private static final List<Integer> NUMBER_OF_FRIENDS = List.of(1, 2);

  @Override
  public ImmutableCollection<InputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(
            hellos_s,
            fanout(HelloFriendsRequest.userId_s).using(userIds_s).with(identity()),
            fanout(HelloFriendsRequest.numberOfFriends_s)
                .using(userIds_s)
                .with(_x -> NUMBER_OF_FRIENDS)));
  }

  @VajramLogic
  public static String sayHellos(MultiHelloFriendsAllInputs allInputs) {
    List<String> result = new ArrayList<>();
    for (String userId : allInputs.userIds()) {
      for (Integer numberOfFriend : NUMBER_OF_FRIENDS) {
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

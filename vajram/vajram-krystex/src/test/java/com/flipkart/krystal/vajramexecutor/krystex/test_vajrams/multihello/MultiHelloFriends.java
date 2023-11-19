package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello;

import static com.flipkart.krystal.vajram.inputs.MultiExecute.executeFanoutWith;
import static com.flipkart.krystal.vajram.inputs.MultiExecute.skipFanout;
import static com.flipkart.krystal.vajram.inputs.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.inputs.resolution.InputResolvers.fanout;
import static com.flipkart.krystal.vajram.inputs.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriendsRequest.hellos_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriendsRequest.userIds_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2.MultiHelloFriendsV2Request.skip_n;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2.MultiHelloFriendsV2Request.userIds_n;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.Dependency;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.MultiExecute;
import com.flipkart.krystal.vajram.inputs.Using;
import com.flipkart.krystal.vajram.inputs.resolution.InputResolver;
import com.flipkart.krystal.vajram.inputs.resolution.Resolve;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriends;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriendsInputUtil.MultiHelloFriendsInputs;
import com.google.common.collect.ImmutableCollection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@VajramDef()
public abstract class MultiHelloFriends extends ComputeVajram<String> {

  private static final List<Integer> NUMBER_OF_FRIENDS = List.of(1, 2);

  @Input List<String> userIds;
  @Input Optional<Boolean> skip;

  @Dependency(onVajram = HelloFriends.class, canFanout = true)
  String hellos;

  @Override
  public ImmutableCollection<InputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(
            hellos_s,
            fanout(HelloFriendsRequest.numberOfFriends_s)
                .using(userIds_s)
                .with(_x -> NUMBER_OF_FRIENDS)));
  }

  @Resolve(depName = MultiHelloFriendsRequest.hellos_n, depInputs = HelloFriendsRequest.userId_n)
  public static MultiExecute<String> userIdsForHellos(
      @Using(userIds_n) List<String> userIds, @Using(skip_n) Optional<Boolean> skip) {
    if (skip.orElse(false)) {
      return skipFanout("skip requested");
    }
    return executeFanoutWith(userIds);
  }

  @VajramLogic
  public static String sayHellos(MultiHelloFriendsInputs allInputs) {
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

package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello;

import static com.flipkart.krystal.vajram.facets.FanoutCommand.executeFanoutWith;
import static com.flipkart.krystal.vajram.facets.FanoutCommand.skipFanout;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriends_Fac.hellos_i;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.FanoutCommand;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriends;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriends_ImmutReqPojo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriends_ImmutReqPojo.Builder;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriends_Req;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExternalInvocation(allow = true)
@VajramDef
public abstract class MultiHelloFriends extends ComputeVajram<String> {
  static class _Facets {
    @Mandatory @Input List<String> userIds;
    @Input boolean skip;

    @Dependency(onVajram = HelloFriends.class, canFanout = true)
    String hellos;
  }

  private static final List<Integer> NUMBER_OF_FRIENDS = List.of(1, 2);

  @Resolve(
      dep = hellos_i,
      depInputs = {HelloFriends_Req.userId_i, HelloFriends_Req.numberOfFriends_i})
  static FanoutCommand<Builder> sayHello(List<String> userIds, Optional<Boolean> skip) {
    if (skip.orElse(false)) {
      return skipFanout("skip requested");
    }
    List<HelloFriends_ImmutReqPojo.Builder> requests = new ArrayList<>();
    for (String userId : userIds) {
      for (int numberOfFriend : NUMBER_OF_FRIENDS) {
        requests.add(
            HelloFriends_ImmutReqPojo._builder().userId(userId).numberOfFriends(numberOfFriend));
      }
    }
    return executeFanoutWith(requests);
  }

  @Output
  static String sayHellos(
      List<String> userIds, FanoutDepResponses<HelloFriends_Req, String> hellos) {
    List<String> result = new ArrayList<>();
    for (String userId : userIds) {
      for (Integer numberOfFriend : NUMBER_OF_FRIENDS) {
        hellos
            .getForRequest(
                HelloFriends_ImmutReqPojo._builder()
                    .userId(userId)
                    .numberOfFriends(numberOfFriend)
                    ._build())
            .valueOpt()
            .ifPresent(result::add);
      }
    }
    return String.join("\n", result);
  }
}

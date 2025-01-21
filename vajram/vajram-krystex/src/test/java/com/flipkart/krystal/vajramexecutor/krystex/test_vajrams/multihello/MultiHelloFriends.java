package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello;

import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.depInputFanout;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.resolve;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriendsFacets.hellos_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriendsRequest.skip_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriendsRequest.userIds_s;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriends;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest;
import com.google.common.collect.ImmutableCollection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExternalInvocation(allow = true)
@VajramDef
public abstract class MultiHelloFriends extends ComputeVajram<String> {
  static class _Facets {
    @Input List<String> userIds;
    @Input Optional<Boolean> skip;

    @Dependency(onVajram = HelloFriends.class, canFanout = true)
    String hellos;
  }

  private static final List<Integer> NUMBER_OF_FRIENDS = List.of(1, 2);

  @Override
  public ImmutableCollection<SimpleInputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(
            hellos_s,
            depInputFanout(HelloFriendsRequest.numberOfFriends_s)
                .using(userIds_s)
                .asResolver(_x -> NUMBER_OF_FRIENDS),
            depInputFanout(HelloFriendsRequest.userId_s)
                .using(userIds_s, skip_s)
                .skipIf((userIds, skip) -> skip.valueOpt().orElse(false), "skip requested")
                .asResolver((userIds, skip) -> userIds.valueOpt().orElse(List.of()))));
  }

  @Output
  static String sayHellos(
      List<String> userIds, FanoutDepResponses hellos) {
    List<String> result = new ArrayList<>();
    for (String userId : userIds) {
      for (Integer numberOfFriend : NUMBER_OF_FRIENDS) {
        hellos
            .getForRequest(
                HelloFriendsRequest._builder()
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

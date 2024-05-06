package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello;

import static com.flipkart.krystal.data.Errable.nil;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.depInputFanout;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.resolve;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriendsFacets.hellos_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriendsRequest.skip_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriendsRequest.userIds_s;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriends;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest;
import com.google.common.collect.ImmutableCollection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
  public ImmutableCollection<InputResolver> getSimpleInputResolvers() {
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
  static String sayHellos(MultiHelloFriendsFacets facets) {
    List<String> result = new ArrayList<>();
    for (String userId : facets.userIds()) {
      for (Integer numberOfFriend : NUMBER_OF_FRIENDS) {
        facets
            .hellos()
            .asMap()
            .getOrDefault(
                HelloFriendsRequest._builder()
                    .userId(userId)
                    .numberOfFriends(numberOfFriend)
                    ._build(),
                nil())
            .valueOpt()
            .ifPresent(result::add);
      }
    }
    return String.join("\n", result);
  }
}

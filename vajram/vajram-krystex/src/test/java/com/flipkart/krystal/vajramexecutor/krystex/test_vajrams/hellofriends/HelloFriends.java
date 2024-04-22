package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends;

import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.depInputFanout;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.resolve;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsFacetUtil.friendInfos_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsFacetUtil.userInfo_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest.numberOfFriends_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest.userId_s;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsFacetUtil.HelloFriendsFacets;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserService;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceRequest;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.IntStream;

@VajramDef
public abstract class HelloFriends extends ComputeVajram<String> {
  static class _Facets {
    @Input String userId;
    @Input Optional<Integer> numberOfFriends;

    @Dependency(onVajram = TestUserService.class)
    TestUserInfo userInfo;

    @Dependency(onVajram = TestUserService.class, canFanout = true)
    TestUserInfo friendInfos;
  }

  @Override
  public ImmutableCollection<InputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(
            userInfo_s,
            depInput(TestUserServiceRequest.userId_s)
                .using(userId_s)
                .asResolver(s -> s.value().map(String::trim).orElse(null))),
        dep(
            friendInfos_s,
            depInputFanout(TestUserServiceRequest.userId_s)
                .using(userId_s, numberOfFriends_s)
                .asResolver(
                    (userId, numberOfFriends) -> {
                      if (numberOfFriends.value().isPresent()) {
                        return getFriendsFor(
                            userId.value().orElseThrow(), numberOfFriends.value().get());
                      } else {
                        return Collections.emptySet();
                      }
                    })));
  }

  @Output
  static String sayHellos(HelloFriendsFacets facets) {
    return "Hello Friends of %s! %s"
        .formatted(
            facets.userInfo().userName(),
            facets.friendInfos().values().stream()
                .filter(voe -> voe.value().isPresent())
                .map(voe -> voe.value().get())
                .map(TestUserInfo::userName)
                .collect(joining(", ")));
  }

  private static ImmutableSet<String> getFriendsFor(String userId, int numberOfFriends) {
    return IntStream.range(1, numberOfFriends + 1)
        .mapToObj(i -> userId + ":friend_" + i)
        .collect(toImmutableSet());
  }
}

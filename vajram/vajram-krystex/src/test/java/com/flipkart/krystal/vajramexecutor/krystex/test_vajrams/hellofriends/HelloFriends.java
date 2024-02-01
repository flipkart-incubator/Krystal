package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends;

import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsFacetUtil.userInfo_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest.friendInfos_n;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest.numberOfFriends_n;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest.userId_n;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest.userId_s;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.Dependency;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.Output;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.Using;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsFacetUtil.HelloFriendsFacets;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserService;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceRequest;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

@VajramDef
public abstract class HelloFriends extends ComputeVajram<String> {

  @Input String userId;
  @Input Optional<Integer> numberOfFriends;

  @Dependency(onVajram = TestUserService.class)
  TestUserInfo userInfo;

  @Dependency(onVajram = TestUserService.class, canFanout = true)
  TestUserInfo friendInfos;

  @Override
  public ImmutableCollection<InputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(
            userInfo_s,
            depInput(TestUserServiceRequest.userId_s)
                .using(userId_s)
                .asResolver(s -> s.map(String::trim).orElse(null))));
  }

  @Resolve(depName = friendInfos_n, depInputs = TestUserServiceRequest.userId_n)
  public static Set<String> friendIdsForUserService(
      @Using(userId_n) String userId, @Using(numberOfFriends_n) Optional<Integer> numberOfFriends) {
    if (numberOfFriends.isPresent()) {
      return getFriendsFor(userId, numberOfFriends.get());
    } else return Collections.emptySet();
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

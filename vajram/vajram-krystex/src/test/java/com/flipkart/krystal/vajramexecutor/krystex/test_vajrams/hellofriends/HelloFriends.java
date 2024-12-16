package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends;

import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.depInputFanout;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.resolve;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsFacets.friendInfos_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsFacets.userInfo_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest.numberOfFriends_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest.userId_s;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserService;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceRequest;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.IntStream;

@ExternalInvocation(allow = true)
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
  public ImmutableCollection<SimpleInputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(
            userInfo_s,
            depInput(TestUserServiceRequest.userId_s)
                .using(userId_s)
                .asResolver(s -> s.valueOpt().map(String::trim).orElse(null))),
        dep(
            friendInfos_s,
            depInputFanout(TestUserServiceRequest.userId_s)
                .using(userId_s, numberOfFriends_s)
                .asResolver(
                    (userId, numberOfFriends) -> {
                      if (numberOfFriends.valueOpt().isPresent()) {
                        return getFriendsFor(
                            userId.valueOpt().orElseThrow(), numberOfFriends.valueOpt().get());
                      } else {
                        return Collections.emptySet();
                      }
                    })));
  }

  @Output
  static String sayHellos(HelloFriendsFacets _allFacets) {
    return "Hello Friends of %s! %s"
        .formatted(
            _allFacets.userInfo().userName(),
            _allFacets.friendInfos().requestResponses().stream()
                .map(errable -> errable.response().valueOpt())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(TestUserInfo::userName)
                .collect(joining(", ")));
  }

  private static ImmutableSet<String> getFriendsFor(String userId, int numberOfFriends) {
    return IntStream.range(1, numberOfFriends + 1)
        .mapToObj(i -> userId + ":friend_" + i)
        .collect(toImmutableSet());
  }
}

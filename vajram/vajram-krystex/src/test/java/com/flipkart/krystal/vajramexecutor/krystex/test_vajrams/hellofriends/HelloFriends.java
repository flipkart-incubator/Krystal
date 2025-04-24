package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends;

import static com.flipkart.krystal.data.IfNull.IfNullThen.FAIL;
import static com.flipkart.krystal.vajram.facets.FanoutCommand.executeFanoutWith;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriends_Fac.friendInfos_n;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriends_Fac.userId_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriends_Fac.userInfo_s;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.IfNull;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.FanoutCommand;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserService;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserService_Req;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

@ExternallyInvocable
@Vajram
public abstract class HelloFriends extends ComputeVajramDef<String> {
  static class _Inputs {
    @IfNull(FAIL)
    String userId;

    int numberOfFriends;
  }

  static class _InternalFacets {
    @IfNull(FAIL)
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
            depInput(TestUserService_Req.userId_s).using(userId_s).asResolver(String::trim)));
  }

  @Resolve(dep = friendInfos_n, depInputs = TestUserService_Req.userId_n)
  static FanoutCommand<String> userIdsForFriendInfos(
      String userId, Optional<Integer> numberOfFriends) {
    if (numberOfFriends.isPresent()) {
      return executeFanoutWith(getFriendsFor(userId, numberOfFriends.get()));
    } else {
      return executeFanoutWith(Set.of());
    }
  }

  @Output
  static String sayHellos(
      TestUserInfo userInfo, FanoutDepResponses<TestUserService_Req, TestUserInfo> friendInfos) {
    return "Hello Friends of %s! %s"
        .formatted(
            userInfo.userName(),
            friendInfos.requestResponsePairs().stream()
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

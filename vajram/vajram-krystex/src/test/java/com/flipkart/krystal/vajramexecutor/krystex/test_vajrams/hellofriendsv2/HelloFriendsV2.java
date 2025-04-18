package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2;

import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.depInputFanout;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2_Fac.friendIds_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2_Fac.friendInfos_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2_Fac.userId_s;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsService;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsService_Req;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserService;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserService_Req;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import java.util.Objects;
import java.util.Set;

@ExternallyInvocable
@Vajram
public abstract class HelloFriendsV2 extends ComputeVajramDef<String> {
  static class _Inputs {
    String userId;
  }

  static class _InternalFacets {
    @Dependency(onVajram = FriendsService.class)
    Set<String> friendIds;

    @Dependency(onVajram = TestUserService.class, canFanout = true)
    /*Collection of*/ TestUserInfo friendInfos;
  }

  @Override
  public ImmutableCollection<SimpleInputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(
            friendInfos_s,
            depInputFanout(TestUserService_Req.userId_s)
                .using(friendIds_s)
                .asResolver(friendIds -> friendIds.valueOpt().orElseThrow())),
        dep(
            friendIds_s,
            depInputFanout(FriendsService_Req.userId_s)
                .using(userId_s)
                .asResolver(
                    stringErrable -> ImmutableList.of(stringErrable.valueOpt().orElseThrow()))));
  }

  @Output
  static String sayHellos(FanoutDepResponses<TestUserInfo, TestUserService_Req> friendInfos) {
    return "Hello Friends! %s"
        .formatted(
            friendInfos.requestResponsePairs().stream()
                .map(rr -> rr.response().valueOpt().orElse(null))
                .filter(Objects::nonNull)
                .map(TestUserInfo::userName)
                .collect(joining(", ")));
  }
}

package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2;

import static com.flipkart.krystal.facets.resolution.ResolverCommand.executeWithRequests;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.depInputFanout;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Facets.friendIds_i;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Facets.friendIds_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Facets.friendInfos_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Request.userId_i;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.RequestBuilder;
import com.flipkart.krystal.facets.resolution.ResolverCommand;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.facets.resolution.ResolutionTarget;
import com.flipkart.krystal.vajram.facets.resolution.AbstractFanoutInputResolver;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsService;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceImmutableRequest.Builder;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserService;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceRequest;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@ExternalInvocation(allow = true)
@VajramDef
public abstract class HelloFriendsV2 extends ComputeVajram<String> {
  static class _Facets {
    @Input String userId;

    @Dependency(onVajram = FriendsService.class)
    Set<String> friendIds;

    @Dependency(onVajram = TestUserService.class, canFanout = true)
    /*Collection of*/ TestUserInfo friendInfos;
  }

  @Override
  public ImmutableCollection<InputResolver> getSimpleInputResolvers() {
    List<InputResolver> resolvers =
        new ArrayList<>(
            resolve(
                dep(
                    friendInfos_s,
                    depInputFanout(TestUserServiceRequest.userId_s)
                        .using(friendIds_s)
                        .asResolver(friendIds -> friendIds.valueOpt().orElseThrow()))));
    /*
     Following is not a preferred way to write resolvers by application developers.
     But it is mentioned here because some platform code might contain resolvers
     written in this way (for example as a result of some code generation).
     We write the below resolver so that these kind of resolvers are also tested in the unit tests.
    */
    resolvers.add(
        new AbstractFanoutInputResolver(
            ImmutableSet.of(userId_i),
            new ResolutionTarget(friendIds_i, FriendsServiceRequest.userId_i)) {

          @Override
          public ResolverCommand resolve(RequestBuilder requestBuilder, Facets facets) {
            HelloFriendsV2Facets helloFriendsV2Facets = (HelloFriendsV2Facets) facets;
            Builder builder = ((Builder) requestBuilder).userId(helloFriendsV2Facets.userId());
            return executeWithRequests(ImmutableList.of(builder));
          }
        });
    return ImmutableList.copyOf(resolvers);
  }

  @Output
  static String sayHellos(FanoutDepResponses friendInfos) {
    return "Hello Friends! %s"
        .formatted(
            friendInfos.requestResponsePairs().stream()
                .map(rr -> rr.response().valueOpt().orElse(null))
                .filter(Objects::nonNull)
                .map(TestUserInfo::userName)
                .collect(joining(", ")));
  }
}

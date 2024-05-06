package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2;

import static com.flipkart.krystal.vajram.facets.MultiExecute.executeFanoutWith;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.depInputFanout;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.resolve;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Facets.friendIds_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Facets.friendInfos_s;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestBuilder;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.DependencyCommand;
import com.flipkart.krystal.vajram.facets.QualifiedInputs;
import com.flipkart.krystal.vajram.facets.resolution.AbstractInputResolver;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsService;
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
        new AbstractInputResolver(
            ImmutableSet.of(1), new QualifiedInputs(2, FriendsServiceRequest.userId_n)) {

          @Override
          public DependencyCommand<? extends Request<Object>> resolve(
              ImmutableList<? extends RequestBuilder<Object>> depRequests, Facets facets) {
            HelloFriendsV2Facets helloFriendsV2Facets = (HelloFriendsV2Facets) facets;
            for (RequestBuilder<?> requestBuilder : depRequests) {
              ((FriendsServiceRequest.Builder) requestBuilder)
                  .userId(helloFriendsV2Facets.userId());
            }
            return executeFanoutWith(depRequests);
          }
        });
    return ImmutableList.copyOf(resolvers);
  }

  @Output
  static String sayHellos(HelloFriendsV2Facets facets) {
    return "Hello Friends! %s"
        .formatted(
            facets.friendInfos().requestResponses().stream()
                .map(rr -> rr.response().valueOpt().orElse(null))
                .filter(Objects::nonNull)
                .map(TestUserInfo::userName)
                .collect(joining(", ")));
  }
}

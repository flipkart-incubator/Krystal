package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2;

import static com.flipkart.krystal.data.ValueOrError.valueOrError;
import static com.flipkart.krystal.vajram.facets.SingleExecute.executeWith;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.fanout;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Request.friendIds_n;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Request.friendIds_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Request.friendInfos_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Request.userId_n;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.Dependency;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.facets.DependencyCommand;
import com.flipkart.krystal.vajram.facets.QualifiedInputs;
import com.flipkart.krystal.vajram.facets.resolution.AbstractInputResolver;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsService;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2InputUtil.HelloFriendsV2Inputs;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserService;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceRequest;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@VajramDef
public abstract class HelloFriendsV2 extends ComputeVajram<String> {

  @Input String userId;

  @Dependency(onVajram = FriendsService.class)
  Set<String> friendIds;

  @Dependency(onVajram = TestUserService.class, canFanout = true)
  /*Collection of*/ TestUserInfo friendInfos;

  @Override
  public ImmutableCollection<InputResolver> getSimpleInputResolvers() {
    List<InputResolver> resolvers =
        new ArrayList<>(
            resolve(
                dep(
                    friendInfos_s,
                    fanout(TestUserServiceRequest.userId_s)
                        .using(friendIds_s)
                        .with(Optional::orElseThrow))));
    /*
     Following is not a preferred way to write resolvers by application developers.
     But it is mentioned here because some platform code might contain resolvers
     written in this way (for example as a result of some code generation).
     We write the below resolver so that these kind of resolvers are also tested in the unit tests.
    */
    resolvers.add(
        new AbstractInputResolver(
            ImmutableSet.of(userId_n),
            new QualifiedInputs(friendIds_n, FriendsServiceRequest.userId_n)) {
          @Override
          public DependencyCommand<Inputs> resolve(
              String dependencyName, ImmutableSet<String> inputsToResolve, Inputs inputs) {
            return executeWith(
                new Inputs(
                    ImmutableMap.of(
                        FriendsServiceRequest.userId_n,
                        valueOrError(() -> inputs.getInputValue(userId_n).getValueOrThrow()))));
          }
        });
    return ImmutableList.copyOf(resolvers);
  }

  @VajramLogic
  public static String sayHellos(HelloFriendsV2Inputs request) {
    return "Hello Friends! %s"
        .formatted(
            request.friendInfos().values().stream()
                .map(voe -> voe.value().orElse(null))
                .filter(Objects::nonNull)
                .map(TestUserInfo::userName)
                .collect(joining(", ")));
  }
}

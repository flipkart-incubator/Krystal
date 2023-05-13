package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2;

import static com.flipkart.krystal.vajram.inputs.ForwardingResolver.resolve;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Request.friendIds_n;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Request.friendIds_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Request.friendInfos_n;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Request.userId_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Vajram.ID;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.DependencyResponse;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.InputResolver;
import com.flipkart.krystal.vajram.inputs.Resolve;
import com.flipkart.krystal.vajram.inputs.Using;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2InputUtil.HelloFriendsV2AllInputs;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserInfo;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@VajramDef(ID)
public abstract class HelloFriendsV2Vajram extends ComputeVajram<String> {

  public static final String ID = "HelloFriendsV2Vajram";

  @Override
  public ImmutableCollection<InputResolver> getSimpleInputResolvers() {
    return ImmutableList.of(
        resolve(friendIds_s, FriendsServiceRequest.userId_s).using(userId_s).build());
  }

  @Resolve(depName = friendInfos_n, depInputs = FriendsServiceRequest.userId_n)
  public static Set<String> userIdsForUserService(
      @Using(friendIds_n) DependencyResponse<FriendsServiceRequest, Set<String>> friendIds) {
    return friendIds.values().stream()
        .map(ValueOrError::value)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .flatMap(Collection::stream)
        .collect(toImmutableSet());
  }

  @VajramLogic
  public String sayHellos(HelloFriendsV2AllInputs request) {
    return "Hello Friends! %s"
        .formatted(
            request.friendInfos().values().stream()
                .map(voe -> voe.value().orElse(null))
                .filter(Objects::nonNull)
                .map(TestUserInfo::userName)
                .collect(joining(", ")));
  }
}

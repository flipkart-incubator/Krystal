package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2;

import static com.flipkart.krystal.vajram.inputs.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.inputs.resolution.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.inputs.resolution.InputResolvers.fanout;
import static com.flipkart.krystal.vajram.inputs.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Request.friendIds_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Request.friendInfos_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Request.userId_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Vajram.ID;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.resolution.InputResolver;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2InputUtil.HelloFriendsV2Inputs;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceRequest;
import com.google.common.collect.ImmutableCollection;
import java.util.Objects;

@VajramDef(ID)
public abstract class HelloFriendsV2Vajram extends ComputeVajram<String> {

  public static final String ID = "HelloFriendsV2Vajram";

  @Override
  public ImmutableCollection<InputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(friendIds_s, depInput(FriendsServiceRequest.userId_s).usingAsIs(userId_s).asResolver()),
        dep(
            friendInfos_s,
            fanout(TestUserServiceRequest.userId_s).using(friendIds_s).with(identity())));
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

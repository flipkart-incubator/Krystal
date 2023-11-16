package com.flipkart.krystal.vajram.samples.greeting;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.CompletableFuture.completedFuture;

import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.modulation.ModulatedInput;
import com.flipkart.krystal.vajram.samples.greeting.UserServiceInputUtil.UserServiceCommonInputs;
import com.flipkart.krystal.vajram.samples.greeting.UserServiceInputUtil.UserServiceModInputs;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@VajramDef(UserServiceVajram.ID)
public abstract class UserServiceVajram extends IOVajram<UserInfo> {

  public static final String ID = "userServiceVajram";

  @Input(modulated = true)
  String userId;

  @VajramLogic
  static Map<UserServiceModInputs, CompletableFuture<UserInfo>> callUserService(
      ModulatedInput<UserServiceModInputs, UserServiceCommonInputs> modulatedRequest) {

    // Make a call to user service and get user info
    CompletableFuture<List<UserInfo>> serviceResponse =
        batchServiceCall(modulatedRequest.modInputs());

    CompletableFuture<Map<UserServiceModInputs, UserInfo>> resultsFuture =
        serviceResponse.thenApply(
            userInfos ->
                userInfos.stream()
                    .collect(
                        Collectors.toMap(
                            userInfo -> new UserServiceModInputs(userInfo.userId()),
                            userInfo -> userInfo)));
    return modulatedRequest.modInputs().stream()
        .collect(
            toImmutableMap(
                im -> im,
                im ->
                    resultsFuture.thenApply(
                        results -> Optional.ofNullable(results.get(im)).orElseThrow())));
  }

  private static CompletableFuture<List<UserInfo>> batchServiceCall(
      List<UserServiceModInputs> modInputs) {
    return completedFuture(
        modInputs.stream()
            .map(UserServiceModInputs::userId)
            .map(userId -> new UserInfo(userId, "Firstname Lastname (%s)".formatted(userId)))
            .toList());
  }
}

package com.flipkart.krystal.vajram.samples.greeting;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.CompletableFuture.completedFuture;

import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.batching.Batch;
import com.flipkart.krystal.vajram.batching.BatchedFacets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@VajramDef
@SuppressWarnings("initialization.field.uninitialized")
public abstract class UserService extends IOVajram<UserInfo> {
  static class _Facets {
    @Batch @Input String userId;
    @Input Optional<String> test;
  }

  @Output
  static Map<UserServiceBatchFacets, CompletableFuture<UserInfo>> callUserService(
      BatchedFacets<UserServiceBatchFacets, UserServiceCommonFacets> batchedRequest) {

    // Make a call to user service and get user info
    CompletableFuture<List<UserInfo>> serviceResponse = batchServiceCall(batchedRequest.batch());

    CompletableFuture<Map<UserServiceBatchFacets, UserInfo>> resultsFuture =
        serviceResponse.thenApply(
            userInfos ->
                userInfos.stream()
                    .collect(
                        Collectors.toMap(
                            userInfo ->
                                UserServiceBatchFacets._builder()
                                    .userId(userInfo.userId())
                                    ._build(),
                            userInfo -> userInfo)));
    return batchedRequest.batch().stream()
        .collect(
            toImmutableMap(
                im -> im,
                im ->
                    resultsFuture.thenApply(
                        results -> Optional.ofNullable(results.get(im)).orElseThrow())));
  }

  private static CompletableFuture<List<UserInfo>> batchServiceCall(
      List<UserServiceBatchFacets> modInputs) {
    return completedFuture(
        modInputs.stream()
            .map(UserServiceBatchFacets::userId)
            .map(userId -> new UserInfo(userId, "Firstname Lastname (%s)".formatted(userId)))
            .toList());
  }
}

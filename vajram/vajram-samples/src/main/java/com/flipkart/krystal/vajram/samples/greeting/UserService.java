package com.flipkart.krystal.vajram.samples.greeting;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;

import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.batching.Batched;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.flipkart.krystal.vajram.facets.Output;
import com.google.common.collect.ImmutableCollection;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@VajramDef
@SuppressWarnings("initialization.field.uninitialized")
public abstract class UserService extends IOVajram<UserInfo> {
  static class _Facets {
    @Mandatory @Batched @Input String userId;
    @Input String test;
  }

  @Output
  @SuppressWarnings("method.invocation")
  static Map<UserService_BatchItem, CompletableFuture<UserInfo>> callUserService(
      ImmutableCollection<UserService_BatchItem> _batchItems) {

    // Make a call to user service and get user info
    CompletableFuture<Map<UserService_BatchItem, UserInfo>> resultsFuture =
        batchServiceCall(_batchItems);

    return _batchItems.stream()
        .collect(
            toImmutableMap(
                identity(),
                batch -> resultsFuture.thenApply(results -> checkNotNull(results.get(batch)))));
  }

  private static CompletableFuture<Map<UserService_BatchItem, UserInfo>> batchServiceCall(
      Collection<UserService_BatchItem> modInputs) {
    return completedFuture(
        modInputs.stream()
            .collect(
                Collectors.toMap(
                    identity(),
                    batch ->
                        new UserInfo(
                            batch.userId(), "Firstname Lastname (%s)".formatted(batch.userId())))));
  }
}

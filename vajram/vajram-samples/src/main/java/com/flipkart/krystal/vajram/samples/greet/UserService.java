package com.flipkart.krystal.vajram.samples.greet;

import static com.flipkart.krystal.data.IfAbsent.IfAbsentThen.FAIL;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;

import com.flipkart.krystal.data.IfAbsent;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.batching.Batched;
import com.flipkart.krystal.vajram.facets.Output;
import com.google.common.collect.ImmutableCollection;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Vajram
@SuppressWarnings("initialization.field.uninitialized")
public abstract class UserService extends IOVajramDef<UserInfo> {

  static class _Inputs {
    @IfAbsent(FAIL)
    @Batched
    String userId;
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

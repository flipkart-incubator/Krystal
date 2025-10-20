package com.flipkart.krystal.vajram.samples.greet;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.batching.Batched;
import com.flipkart.krystal.vajram.facets.Output;
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

  @Output.Batched
  @SuppressWarnings("method.invocation")
  static CompletableFuture<Map<UserService_BatchItem, UserInfo>> callUserService(
      Collection<UserService_BatchItem> _batchItems) {
    // Make a call to user service and get user info
    return completedFuture(
        _batchItems.stream()
            .collect(
                Collectors.toMap(
                    identity(),
                    batch ->
                        new UserInfo(
                            batch.userId(), "Firstname Lastname (%s)".formatted(batch.userId())))));
  }

  @Output.Unbatch
  static Map<UserService_BatchItem, Errable<UserInfo>> unbatch(
      Map<UserService_BatchItem, UserInfo> _batchedOutput) {
    return _batchedOutput.entrySet().stream()
        .collect(toImmutableMap(Map.Entry::getKey, entry -> Errable.withValue(entry.getValue())));
  }
}

package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static java.util.concurrent.CompletableFuture.completedFuture;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.batching.Batched;
import com.flipkart.krystal.vajram.facets.Output;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.LongAdder;

@Vajram
public abstract class FriendsService extends IOVajramDef<Set<String>> {
  static class _Inputs {
    @IfAbsent(FAIL)
    @Batched
    String userId;
  }

  public static final LongAdder CALL_COUNTER = new LongAdder();

  @Output.Batched
  static CompletableFuture<ImmutableMap<FriendsService_BatchItem, Set<String>>> call(
      Collection<FriendsService_BatchItem> _batchItems) {
    CALL_COUNTER.increment();
    Map<FriendsService_BatchItem, Set<String>> result = new LinkedHashMap<>();
    for (FriendsService_BatchItem inputsBatch : _batchItems) {
      String userId = inputsBatch.userId();
      result.put(inputsBatch, getFriends(userId));
    }
    return completedFuture(ImmutableMap.copyOf(result));
  }

  @Output.Unbatch
  static ImmutableMap<FriendsService_BatchItem, Errable<Set<String>>> unbatch(
      Map<FriendsService_BatchItem, Set<String>> _batchedOutput) {
    return _batchedOutput.entrySet().stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Map.Entry::getKey, entry -> Errable.withValue(entry.getValue())));
  }

  private static Set<String> getFriends(String userId) {
    return ImmutableSet.of(userId + ":friend1", userId + ":friend2");
  }
}

package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice;

import static com.flipkart.krystal.data.IfAbsent.IfAbsentThen.FAIL;
import static java.util.concurrent.CompletableFuture.completedFuture;

import com.flipkart.krystal.data.IfAbsent;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.batching.Batched;
import com.flipkart.krystal.vajram.facets.Output;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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

  @Output
  static ImmutableMap<FriendsService_BatchItem, CompletableFuture<Set<String>>> call(
      ImmutableCollection<FriendsService_BatchItem> _batchItems) {
    CALL_COUNTER.increment();
    Map<FriendsService_BatchItem, CompletableFuture<Set<String>>> result = new LinkedHashMap<>();
    for (FriendsService_BatchItem inputsBatch : _batchItems) {
      String userId = inputsBatch.userId();
      result.put(inputsBatch, completedFuture(getFriends(userId)));
    }
    return ImmutableMap.copyOf(result);
  }

  private static Set<String> getFriends(String userId) {
    return ImmutableSet.of(userId + ":friend1", userId + ":friend2");
  }
}

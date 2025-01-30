package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.batching.Batch;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Output;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.LongAdder;

@VajramDef
public abstract class FriendsService extends IOVajram<Set<String>> {
  static class _Facets {
    @Batch @Input String userId;
  }

  public static final LongAdder CALL_COUNTER = new LongAdder();

  @Output
  static ImmutableMap<FriendsService_BatchElem, CompletableFuture<Set<String>>> call(
      ImmutableList<FriendsService_BatchElem> _batches) {
    CALL_COUNTER.increment();
    Map<FriendsService_BatchElem, CompletableFuture<Set<String>>> result = new LinkedHashMap<>();
    for (FriendsService_BatchElem inputsBatch : _batches) {
      String userId = inputsBatch.userId();
      result.put(inputsBatch, completedFuture(getFriends(userId)));
    }
    return ImmutableMap.copyOf(result);
  }

  private static Set<String> getFriends(String userId) {
    return ImmutableSet.of(userId + ":friend1", userId + ":friend2");
  }
}

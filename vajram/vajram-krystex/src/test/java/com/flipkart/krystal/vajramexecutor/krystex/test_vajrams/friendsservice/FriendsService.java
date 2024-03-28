package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.Output;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.batching.Batch;
import com.flipkart.krystal.vajram.batching.BatchedFacets;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceFacetUtil.FriendsServiceCommonFacets;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceFacetUtil.FriendsServiceInputBatch;
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
  static ImmutableMap<FriendsServiceInputBatch, CompletableFuture<Set<String>>> call(
      BatchedFacets<FriendsServiceInputBatch, FriendsServiceCommonFacets> batchedFacets) {
    CALL_COUNTER.increment();
    Map<FriendsServiceInputBatch, CompletableFuture<Set<String>>> result = new LinkedHashMap<>();
    for (FriendsServiceInputBatch inputsBatch : batchedFacets.batch()) {
      String userId = inputsBatch.userId();
      result.put(inputsBatch, completedFuture(getFriends(userId)));
    }
    return ImmutableMap.copyOf(result);
  }

  private static Set<String> getFriends(String userId) {
    return ImmutableSet.of(userId + ":friend1", userId + ":friend2");
  }
}

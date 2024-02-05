package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.Output;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.modulation.Modulated;
import com.flipkart.krystal.vajram.modulation.ModulatedFacets;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceFacetUtil.FriendsServiceCommonFacets;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceFacetUtil.FriendsServiceModInputs;
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
    @Modulated @Input String userId;
  }

  public static final LongAdder CALL_COUNTER = new LongAdder();

  @Output
  static ImmutableMap<FriendsServiceModInputs, CompletableFuture<Set<String>>> call(
      ModulatedFacets<FriendsServiceModInputs, FriendsServiceCommonFacets> modulatedFacets) {
    CALL_COUNTER.increment();
    Map<FriendsServiceModInputs, CompletableFuture<Set<String>>> result = new LinkedHashMap<>();
    for (FriendsServiceModInputs inputsNeedingModulation : modulatedFacets.modInputs()) {
      String userId = inputsNeedingModulation.userId();
      result.put(inputsNeedingModulation, completedFuture(getFriends(userId)));
    }
    return ImmutableMap.copyOf(result);
  }

  private static Set<String> getFriends(String userId) {
    return ImmutableSet.of(userId + ":friend1", userId + ":friend2");
  }
}

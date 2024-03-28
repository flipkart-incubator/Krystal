package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.Output;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.batching.Batch;
import com.flipkart.krystal.vajram.batching.BatchedFacets;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceFacetUtil.TestUserServiceCommonFacets;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceFacetUtil.TestUserServiceInputBatch;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.LongAdder;

@VajramDef
public abstract class TestUserService extends IOVajram<TestUserInfo> {
  static class _Facets {
    @Batch @Input String userId;
  }

  private static final ScheduledExecutorService LATENCY_INDUCER =
      newSingleThreadScheduledExecutor();

  public static final LongAdder CALL_COUNTER = new LongAdder();
  public static final Set<TestUserServiceRequest> REQUESTS = new LinkedHashSet<>();

  @Output
  static ImmutableMap<TestUserServiceInputBatch, CompletableFuture<TestUserInfo>> callUserService(
      BatchedFacets<TestUserServiceInputBatch, TestUserServiceCommonFacets> batchedRequest) {
    CALL_COUNTER.increment();
    batchedRequest.batch().stream()
        .map(im -> TestUserServiceRequest.builder().userId(im.userId()).build())
        .forEach(REQUESTS::add);

    // Make a call to user service and get user info
    return batchedRequest.batch().stream()
        .collect(
            toImmutableMap(
                inputBatch -> inputBatch,
                modInputs -> {
                  CompletableFuture<TestUserInfo> future = new CompletableFuture<>();
                  LATENCY_INDUCER.schedule(
                      (Runnable)
                          () ->
                              future.complete(
                                  new TestUserInfo(
                                      "Firstname Lastname (%s)".formatted(modInputs.userId()))),
                      50,
                      MILLISECONDS);
                  return future;
                }));
  }
}

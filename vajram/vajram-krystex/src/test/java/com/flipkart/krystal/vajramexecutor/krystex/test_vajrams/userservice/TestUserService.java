package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.batching.Batch;
import com.flipkart.krystal.vajram.batching.BatchedFacets;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceFacets.BatchFacets;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceFacets.CommonInputs;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.LongAdder;

@ExternalInvocation(allow = true)
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
  static ImmutableMap<BatchFacets, CompletableFuture<TestUserInfo>> callUserService(
      ImmutableList<BatchFacets> _batches) {
    CALL_COUNTER.increment();
    _batches.stream()
        .map(im -> TestUserServiceRequest._builder().userId(im.userId())._build())
        .forEach(REQUESTS::add);

    // Make a call to user service and get user info
    return _batches.stream()
        .collect(
            toImmutableMap(
                inputBatch -> inputBatch,
                modInputs -> {
                  CompletableFuture<TestUserInfo> future = new CompletableFuture<>();
                  @SuppressWarnings({"FutureReturnValueIgnored", "unused"})
                  ScheduledFuture<?> unused =
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

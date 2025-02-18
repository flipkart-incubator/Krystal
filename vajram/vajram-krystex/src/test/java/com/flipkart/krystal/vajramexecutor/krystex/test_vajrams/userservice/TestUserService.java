package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.batching.Batched;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.flipkart.krystal.vajram.facets.Output;
import com.google.common.collect.ImmutableCollection;
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
    @Mandatory @Batched @Input String userId;
  }

  private static final ScheduledExecutorService LATENCY_INDUCER =
      newSingleThreadScheduledExecutor();

  public static final LongAdder CALL_COUNTER = new LongAdder();
  public static final Set<TestUserService_Req> REQUESTS = new LinkedHashSet<>();

  @Output
  static ImmutableMap<TestUserService_BatchItem, CompletableFuture<TestUserInfo>> callUserService(
      ImmutableCollection<TestUserService_BatchItem> _batchItems) {
    CALL_COUNTER.increment();
    _batchItems.stream()
        .map(im -> TestUserService_ImmutReqPojo._builder().userId(im.userId())._build())
        .forEach(REQUESTS::add);

    // Make a call to user service and get user info
    return _batchItems.stream()
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

package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.batching.Batched;
import com.flipkart.krystal.vajram.facets.Output;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.LongAdder;

@InvocableOutsideGraph
@Vajram
public abstract class TestUserService extends IOVajramDef<TestUserInfo> {
  static class _Inputs {
    @IfAbsent(FAIL)
    @Batched
    String userId;
  }

  private static final ScheduledExecutorService LATENCY_INDUCER =
      newSingleThreadScheduledExecutor();

  public static final LongAdder CALL_COUNTER = new LongAdder();
  public static final Set<TestUserService_Req> REQUESTS = new LinkedHashSet<>();

  @Output.Batched
  static CompletableFuture<ImmutableMap<TestUserService_BatchItem, TestUserInfo>> userServiceOutput(
      ImmutableCollection<TestUserService_BatchItem> _batchItems) {
    CALL_COUNTER.increment();
    _batchItems.stream()
        .map(im -> TestUserService_ReqImmutPojo._builder().userId(im.userId())._build())
        .forEach(REQUESTS::add);

    CompletableFuture<ImmutableMap<TestUserService_BatchItem, TestUserInfo>> returnValue =
        new CompletableFuture<>();

    @SuppressWarnings({"FutureReturnValueIgnored", "unused"})
    ScheduledFuture<?> unused =
        LATENCY_INDUCER.schedule(
            () -> {
              // Make a call to user service and get user info
              returnValue.complete(
                  _batchItems.stream()
                      .collect(
                          toImmutableMap(
                              inputBatch -> inputBatch,
                              modInputs ->
                                  new TestUserInfo(
                                      "Firstname Lastname (%s)".formatted(modInputs.userId())))));
            },
            50,
            MILLISECONDS);

    return returnValue;
  }

  @Output.Unbatch
  static Map<TestUserService_BatchItem, Errable<TestUserInfo>> unbatch(
      Map<TestUserService_BatchItem, TestUserInfo> _batchedOutput) {
    return _batchedOutput.entrySet().stream()
        .collect(toImmutableMap(Map.Entry::getKey, entry -> Errable.withValue(entry.getValue())));
  }
}

package com.flipkart.krystal.vajram.samples.calculator.add;

import static com.flipkart.krystal.traits.matchers.InputValueMatcher.isAnyValue;
import static com.flipkart.krystal.vajram.samples.Util.TEST_TIMEOUT;
import static com.flipkart.krystal.vajram.samples.calculator.add.ChainAdd_Fac.chainSum_s;
import static com.flipkart.krystal.vajram.samples.calculator.add.MultiAdd.MultiAddType.CHAIN;
import static com.flipkart.krystal.vajram.samples.calculator.add.MultiAdd.MultiAddType.SIMPLE;
import static com.flipkart.krystal.vajram.samples.calculator.add.MultiAdd.MultiAddType.SPLIT;
import static com.flipkart.krystal.vajram.samples.calculator.add.MultiAdd_Req.numbers_s;
import static com.flipkart.krystal.vajram.samples.calculator.add.SplitAdd_Fac.splitSum1_s;
import static com.flipkart.krystal.vajram.samples.calculator.add.SplitAdd_Fac.splitSum2_s;
import static com.flipkart.krystal.vajramexecutor.krystex.traits.PredicateDispatchUtil.dispatchTrait;
import static com.flipkart.krystal.vajramexecutor.krystex.traits.PredicateDispatchUtil.when;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.except.KrystalCompletionException;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.traits.TraitDispatchPolicies;
import com.flipkart.krystal.vajram.guice.traitbinding.StaticDispatchPolicyImpl;
import com.flipkart.krystal.vajram.guice.traitbinding.TraitBinder;
import com.flipkart.krystal.vajram.samples.Util;
import com.flipkart.krystal.vajram.samples.calculator.add.MultiAdd.MultiAddQualifier;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexGraph;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexGraph.KrystexGraphBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig.KrystexVajramExecutorConfigBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.VajramGraph;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MultiAddTest {

  private static final String REQUEST_ID = "addUsingTraitsTest";
  private static final int MAX_THREADS = 1;

  @SuppressWarnings("unchecked")
  private static final Lease<SingleThreadExecutor>[] EXECUTOR_LEASES = new Lease[MAX_THREADS];

  private VajramGraph vGraph;
  private KrystexGraphBuilder kGraph;
  private Lease<SingleThreadExecutor> executorLease;

  @BeforeAll
  static void beforeAll() throws LeaseUnavailableException {
    SingleThreadExecutorsPool execPool = new SingleThreadExecutorsPool("Test", MAX_THREADS);
    for (int i = 0; i < MAX_THREADS; i++) {
      EXECUTOR_LEASES[i] = execPool.lease();
    }
  }

  @AfterAll
  static void afterAll() {
    for (Lease<SingleThreadExecutor> lease : EXECUTOR_LEASES) {
      lease.close();
    }
  }

  @BeforeEach
  void setUp() {
    this.executorLease = EXECUTOR_LEASES[0];
    this.vGraph = Util.loadFromClasspath(AddUsingTraits.class.getPackageName()).build();
    this.kGraph = KrystexGraph.builder().vajramGraph(vGraph);
  }

  @Test
  void chainAdd_staticDispatch_success() {
    registerStaticDispatchPolicy();
    // Setup test data
    List<Integer> numbers1 = asList(1, 2, 3);

    // Expected sums
    int expectedSum1 = 6; // 1+2+3 using CHAIN

    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor krystexVajramExecutor =
        kGraph.build().createExecutor(executorConfig())) {
      future =
          krystexVajramExecutor.execute(
              MultiAdd_ReqImmutPojo._builder().numbers(numbers1)._build(),
              KryonExecutionConfig.builder()
                  .disabledDependentChains(getDisabledDependentChains(vGraph))
                  .executionId(REQUEST_ID)
                  .staticDispatchQualifier(MultiAddQualifier.Creator.create(CHAIN))
                  .build());
    }

    // Verify results
    assertThat(future)
        .succeedsWithin(TEST_TIMEOUT)
        .satisfies(sum -> assertThat(sum).isEqualTo(expectedSum1));
  }

  @Test
  void splitAdd_staticDispatch_success() {
    registerStaticDispatchPolicy();
    // Setup test data
    List<Integer> numbers1 = asList(1, 2, 3);

    // Expected sums
    int expectedSum1 = 6; // 1+2+3 using CHAIN

    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor krystexVajramExecutor =
        kGraph.build().createExecutor(executorConfig())) {
      future =
          krystexVajramExecutor.execute(
              MultiAdd_ReqImmutPojo._builder().numbers(numbers1)._build(),
              KryonExecutionConfig.builder()
                  .disabledDependentChains(getDisabledDependentChains(vGraph))
                  .executionId(REQUEST_ID)
                  .staticDispatchQualifier(MultiAddQualifier.Creator.create(SPLIT))
                  .build());
    }

    // Verify results
    assertThat(future)
        .succeedsWithin(TEST_TIMEOUT)
        .satisfies(sum -> assertThat(sum).isEqualTo(expectedSum1));
  }

  @Test
  void simpleAdd_staticDispatch_success() {
    registerStaticDispatchPolicy();
    // Setup test data
    List<Integer> numbers1 = asList(1, 2, 3);

    // Expected sums
    int expectedSum1 = 6; // 1+2+3 using CHAIN

    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor krystexVajramExecutor =
        kGraph.build().createExecutor(executorConfig())) {
      future =
          krystexVajramExecutor.execute(
              MultiAdd_ReqImmutPojo._builder().numbers(numbers1)._build(),
              KryonExecutionConfig.builder()
                  .executionId(REQUEST_ID)
                  .staticDispatchQualifier(MultiAddQualifier.Creator.create(SIMPLE))
                  .build());
    }

    // Verify results
    assertThat(future)
        .succeedsWithin(TEST_TIMEOUT)
        .satisfies(sum -> assertThat(sum).isEqualTo(expectedSum1));
  }

  @Test
  void chainAdd_predicateDispatch_success() {
    this.kGraph.traitDispatchPolicies(
        new TraitDispatchPolicies(
            dispatchTrait(MultiAdd_Req.class, vGraph)
                .conditionally(when(numbers_s, isAnyValue()).to(ChainAdd_Req.class))));

    // Setup test data
    List<Integer> numbers1 = asList(1, 2, 3);

    // Expected sums
    int expectedSum1 = 6; // 1+2+3 using CHAIN

    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor krystexVajramExecutor =
        kGraph.build().createExecutor(executorConfig())) {
      future =
          krystexVajramExecutor.execute(
              MultiAdd_ReqImmutPojo._builder().numbers(numbers1)._build(),
              KryonExecutionConfig.builder()
                  .disabledDependentChains(getDisabledDependentChains(vGraph))
                  .executionId(REQUEST_ID)
                  .build());
    }

    // Verify results
    assertThat(future)
        .succeedsWithin(TEST_TIMEOUT)
        .satisfies(sum -> assertThat(sum).isEqualTo(expectedSum1));
  }

  @Test
  void splitAdd_predicateDispatch_success() {
    this.kGraph.traitDispatchPolicies(
        new TraitDispatchPolicies(
            dispatchTrait(MultiAdd_Req.class, vGraph)
                .conditionally(when(numbers_s, isAnyValue()).to(SplitAdd_Req.class))));

    // Setup test data
    List<Integer> numbers1 = asList(1, 2, 3);

    // Expected sums
    int expectedSum1 = 6; // 1+2+3 using CHAIN

    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor krystexVajramExecutor =
        kGraph.build().createExecutor(executorConfig())) {
      future =
          krystexVajramExecutor.execute(
              MultiAdd_ReqImmutPojo._builder().numbers(numbers1)._build(),
              KryonExecutionConfig.builder()
                  .disabledDependentChains(getDisabledDependentChains(vGraph))
                  .executionId(REQUEST_ID)
                  .build());
    }

    // Verify results
    assertThat(future)
        .succeedsWithin(TEST_TIMEOUT)
        .satisfies(sum -> assertThat(sum).isEqualTo(expectedSum1));
  }

  @Test
  void simpleAdd_predicateDispatch_success() {
    this.kGraph.traitDispatchPolicies(
        new TraitDispatchPolicies(
            dispatchTrait(MultiAdd_Req.class, vGraph)
                .conditionally(when(numbers_s, isAnyValue()).to(SimpleAdd_Req.class))));

    // Setup test data
    List<Integer> numbers1 = asList(1, 2, 3);

    // Expected sums
    int expectedSum1 = 6; // 1+2+3 using CHAIN

    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor krystexVajramExecutor =
        kGraph.build().createExecutor(executorConfig())) {
      future =
          krystexVajramExecutor.execute(
              MultiAdd_ReqImmutPojo._builder().numbers(numbers1)._build(),
              KryonExecutionConfig.builder()
                  .disabledDependentChains(getDisabledDependentChains(vGraph))
                  .executionId(REQUEST_ID)
                  .build());
    }

    // Verify results
    assertThat(future)
        .succeedsWithin(TEST_TIMEOUT)
        .satisfies(sum -> assertThat(sum).isEqualTo(expectedSum1));
  }

  @Test
  void chainAdd_computeDispatch_success() {
    this.kGraph.traitDispatchPolicies(
        new TraitDispatchPolicies(
            dispatchTrait(MultiAdd_Req.class, vGraph)
                .computingTargetWith(
                    multiAddReq -> Optional.of(ChainAdd_Req.class),
                    ImmutableSet.of(ChainAdd_Req.class))));

    // Setup test data
    List<Integer> numbers1 = asList(1, 2, 3);

    // Expected sums
    int expectedSum1 = 6; // 1+2+3 using CHAIN

    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor krystexVajramExecutor =
        kGraph.build().createExecutor(executorConfig())) {
      future =
          krystexVajramExecutor.execute(
              MultiAdd_ReqImmutPojo._builder().numbers(numbers1)._build(),
              KryonExecutionConfig.builder()
                  .disabledDependentChains(getDisabledDependentChains(vGraph))
                  .executionId(REQUEST_ID)
                  .build());
    }

    // Verify results
    assertThat(future)
        .succeedsWithin(TEST_TIMEOUT)
        .satisfies(sum -> assertThat(sum).isEqualTo(expectedSum1));
  }

  @Test
  void splitAdd_computeDispatch_success() {
    this.kGraph.traitDispatchPolicies(
        new TraitDispatchPolicies(
            dispatchTrait(MultiAdd_Req.class, vGraph)
                .computingTargetWith(
                    multiAddReq -> Optional.of(SplitAdd_Req.class),
                    ImmutableSet.of(SplitAdd_Req.class))));

    // Setup test data
    List<Integer> numbers1 = asList(1, 2, 3);

    // Expected sums
    int expectedSum1 = 6; // 1+2+3 using CHAIN

    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor krystexVajramExecutor =
        kGraph.build().createExecutor(executorConfig())) {
      future =
          krystexVajramExecutor.execute(
              MultiAdd_ReqImmutPojo._builder().numbers(numbers1)._build(),
              KryonExecutionConfig.builder()
                  .disabledDependentChains(getDisabledDependentChains(vGraph))
                  .executionId(REQUEST_ID)
                  .build());
    }

    // Verify results
    assertThat(future)
        .succeedsWithin(TEST_TIMEOUT)
        .satisfies(sum -> assertThat(sum).isEqualTo(expectedSum1));
  }

  @Test
  void simpleAdd_computeDispatch_success() {
    this.kGraph.traitDispatchPolicies(
        new TraitDispatchPolicies(
            dispatchTrait(MultiAdd_Req.class, vGraph)
                .computingTargetWith(
                    multiAddReq -> Optional.of(SimpleAdd_Req.class),
                    ImmutableSet.of(SimpleAdd_Req.class))));

    // Setup test data
    List<Integer> numbers1 = asList(1, 2, 3);

    // Expected sums
    int expectedSum1 = 6; // 1+2+3 using CHAIN

    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor krystexVajramExecutor =
        kGraph.build().createExecutor(executorConfig())) {
      future =
          krystexVajramExecutor.execute(
              MultiAdd_ReqImmutPojo._builder().numbers(numbers1)._build(),
              KryonExecutionConfig.builder()
                  .disabledDependentChains(getDisabledDependentChains(vGraph))
                  .executionId(REQUEST_ID)
                  .build());
    }

    // Verify results
    assertThat(future)
        .succeedsWithin(TEST_TIMEOUT)
        .satisfies(sum -> assertThat(sum).isEqualTo(expectedSum1));
  }

  @Test
  void nullResolution_computeDispatch_throws() {
    this.kGraph.traitDispatchPolicies(
        new TraitDispatchPolicies(
            dispatchTrait(MultiAdd_Req.class, vGraph)
                .computingTargetWith(multiAddReq -> Optional.empty(), ImmutableSet.of())));

    // Setup test data
    List<Integer> numbers1 = asList(1, 2, 3);

    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor krystexVajramExecutor =
        kGraph.build().createExecutor(executorConfig())) {
      future =
          krystexVajramExecutor.execute(
              MultiAdd_ReqImmutPojo._builder().numbers(numbers1)._build(),
              KryonExecutionConfig.builder()
                  .disabledDependentChains(getDisabledDependentChains(vGraph))
                  .executionId(REQUEST_ID)
                  .build());
    }

    // Verify results
    assertThat(future)
        .failsWithin(TEST_TIMEOUT)
        .withThrowableOfType(ExecutionException.class)
        .havingCause()
        .isInstanceOf(KrystalCompletionException.class)
        .withMessageContaining(" resolved vajramID 'null' for Trait Id v<MultiAdd>");
  }

  private void registerStaticDispatchPolicy() {
    TraitBinder traitBinder = new TraitBinder();
    traitBinder
        .bindTrait(MultiAdd_Req.class)
        .annotatedWith(MultiAddQualifier.Creator.create(SIMPLE))
        .to(SimpleAdd_Req.class);
    traitBinder
        .bindTrait(MultiAdd_Req.class)
        .annotatedWith(MultiAddQualifier.Creator.create(CHAIN))
        .to(ChainAdd_Req.class);
    traitBinder
        .bindTrait(MultiAdd_Req.class)
        .annotatedWith(MultiAddQualifier.Creator.create(SPLIT))
        .to(SplitAdd_Req.class);
    this.kGraph.traitDispatchPolicies(
        new TraitDispatchPolicies(
            new StaticDispatchPolicyImpl(
                vGraph, vGraph.getVajramIdByVajramDefType(MultiAdd.class), traitBinder)));
  }

  private KrystexVajramExecutorConfigBuilder executorConfig() {
    return KrystexVajramExecutorConfig.builder()
        .graph(kGraph.build())
        .requestId(REQUEST_ID)
        .kryonExecutorConfig(
            KryonExecutorConfig.builder().executorService(executorLease.get()).build());
  }

  private static ImmutableSet<DependentChain> getDisabledDependentChains(VajramGraph graph) {
    String chainAdd = graph.getVajramIdByVajramDefType(ChainAdd.class).id();
    String splitAdd = graph.getVajramIdByVajramDefType(SplitAdd.class).id();
    return ImmutableSet.of(
        graph.computeDependentChain(chainAdd, chainSum_s, chainSum_s),
        graph.computeDependentChain(splitAdd, splitSum1_s, splitSum1_s, splitSum1_s),
        graph.computeDependentChain(splitAdd, splitSum1_s, splitSum1_s, splitSum2_s),
        graph.computeDependentChain(splitAdd, splitSum1_s, splitSum2_s, splitSum1_s),
        graph.computeDependentChain(splitAdd, splitSum1_s, splitSum2_s, splitSum2_s),
        graph.computeDependentChain(splitAdd, splitSum2_s, splitSum1_s, splitSum1_s),
        graph.computeDependentChain(splitAdd, splitSum2_s, splitSum1_s, splitSum2_s),
        graph.computeDependentChain(splitAdd, splitSum2_s, splitSum2_s, splitSum1_s),
        graph.computeDependentChain(splitAdd, splitSum2_s, splitSum2_s, splitSum2_s));
  }
}

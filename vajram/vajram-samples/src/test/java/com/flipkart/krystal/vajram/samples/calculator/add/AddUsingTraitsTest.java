package com.flipkart.krystal.vajram.samples.calculator.add;

import static com.flipkart.krystal.vajram.samples.Util.TEST_TIMEOUT;
import static com.flipkart.krystal.vajram.samples.calculator.add.AddUsingTraits_Fac.sum2_s;
import static com.flipkart.krystal.vajram.samples.calculator.add.AddUsingTraits_Fac.sum3_s;
import static com.flipkart.krystal.vajram.samples.calculator.add.ChainAdd_Fac.chainSum_s;
import static com.flipkart.krystal.vajram.samples.calculator.add.MultiAdd.MultiAddType.CHAIN;
import static com.flipkart.krystal.vajram.samples.calculator.add.MultiAdd.MultiAddType.SIMPLE;
import static com.flipkart.krystal.vajram.samples.calculator.add.MultiAdd.MultiAddType.SPLIT;
import static com.flipkart.krystal.vajram.samples.calculator.add.SplitAdd_Fac.splitSum1_s;
import static com.flipkart.krystal.vajram.samples.calculator.add.SplitAdd_Fac.splitSum2_s;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajram.guice.traitbinding.StaticDispatchPolicyImpl;
import com.flipkart.krystal.vajram.guice.traitbinding.TraitBinder;
import com.flipkart.krystal.vajram.samples.Util;
import com.flipkart.krystal.vajram.samples.calculator.add.AddUsingTraits.ThreeSums;
import com.flipkart.krystal.vajram.samples.calculator.add.MultiAdd.MultiAddQualifier;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig.KrystexVajramExecutorConfigBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AddUsingTraitsTest {

  private static final String REQUEST_ID = "addUsingTraitsTest";
  private static final int MAX_THREADS = 1;
  private static final Lease<SingleThreadExecutor>[] EXECUTOR_LEASES = new Lease[MAX_THREADS];

  private VajramKryonGraph graph;
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
    this.graph = Util.loadFromClasspath(AddUsingTraits.class.getPackageName()).build();
    this.graph.registerTraitDispatchPolicies(
        new StaticDispatchPolicyImpl(
            graph, graph.getVajramIdByVajramDefType(MultiAdd.class), traitBinder));
  }

  @Test
  void addUsingTraits_success() {
    // Setup test data
    List<Integer> numbers1 = asList(1, 2, 3);
    List<Integer> numbers2 = asList(4, 5, 6);
    List<Integer> numbers3 = asList(7, 8, 9);

    // Expected sums
    int expectedSum1 = 6; // 1+2+3 using SIMPLE
    int expectedSum2 = 15; // 4+5+6 using CHAIN
    int expectedSum3 = 24; // 7+8+9 using SPLIT

    CompletableFuture<ThreeSums> future;

    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(executorConfig().build())) {
      // Execute the vajram
      future = executeVajram(graph, krystexVajramExecutor, numbers1, numbers2, numbers3);
    }

    // Verify results
    assertThat(future)
        .succeedsWithin(TEST_TIMEOUT)
        .satisfies(
            threeSums -> {
              assertThat(threeSums.sum1()).isEqualTo(expectedSum1);
              assertThat(threeSums.sum2()).isEqualTo(expectedSum2);
              assertThat(threeSums.sum3()).isEqualTo(expectedSum3);
            });
  }

  @Test
  void addUsingTraits_withEmptyLists() {
    // Setup test data with empty lists
    List<Integer> emptyList = List.of();

    CompletableFuture<ThreeSums> future;

    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(executorConfig().build())) {

      // Execute the vajram with empty lists
      future = executeVajram(graph, krystexVajramExecutor, emptyList, emptyList, emptyList);
    }

    // Verify that all sums are 0 for empty lists
    assertThat(future)
        .succeedsWithin(TEST_TIMEOUT)
        .satisfies(
            threeSums -> {
              assertThat(threeSums.sum1()).isEqualTo(0);
              assertThat(threeSums.sum2()).isEqualTo(0);
              assertThat(threeSums.sum3()).isEqualTo(0);
            });
  }

  private KrystexVajramExecutorConfigBuilder executorConfig() {
    return KrystexVajramExecutorConfig.builder()
        .requestId(REQUEST_ID)
        .kryonExecutorConfigBuilder(
            KryonExecutorConfig.builder().singleThreadExecutor(executorLease.get()));
  }

  @Test
  void addUsingTraits_withDifferentSizedLists() {
    // Setup test data with different sized lists
    List<Integer> smallList = asList(1);
    List<Integer> mediumList = asList(2, 3);
    List<Integer> largeList = asList(4, 5, 6, 7);

    // Expected sums
    int expectedSum1 = 1; // small list using SIMPLE
    int expectedSum2 = 5; // medium list using CHAIN
    int expectedSum3 = 22; // large list using SPLIT

    CompletableFuture<ThreeSums> future;

    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(executorConfig().build())) {

      // Execute the vajram with different sized lists
      future = executeVajram(graph, krystexVajramExecutor, smallList, mediumList, largeList);
    }

    // Verify results
    assertThat(future)
        .succeedsWithin(TEST_TIMEOUT)
        .satisfies(
            threeSums -> {
              assertThat(threeSums.sum1()).isEqualTo(expectedSum1);
              assertThat(threeSums.sum2()).isEqualTo(expectedSum2);
              assertThat(threeSums.sum3()).isEqualTo(expectedSum3);
            });
  }

  private static CompletableFuture<ThreeSums> executeVajram(
      VajramKryonGraph graph,
      KrystexVajramExecutor krystexVajramExecutor,
      List<Integer> numbers1,
      List<Integer> numbers2,
      List<Integer> numbers3) {

    return krystexVajramExecutor.execute(
        AddUsingTraits_ImmutReqPojo._builder()
            .numbers1(numbers1)
            .numbers2(numbers2)
            .numbers3(numbers3)
            ._build(),
        KryonExecutionConfig.builder()
            .disabledDependentChains(getDisabledDependentChains(graph))
            .executionId(REQUEST_ID)
            .build());
  }

  private static ImmutableSet<DependentChain> getDisabledDependentChains(VajramKryonGraph graph) {
    String vajramId = graph.getVajramIdByVajramDefType(AddUsingTraits.class).id();
    return ImmutableSet.of(
        graph.computeDependentChain(vajramId, sum2_s, chainSum_s, chainSum_s),
        graph.computeDependentChain(vajramId, sum3_s, splitSum1_s, splitSum1_s, splitSum1_s),
        graph.computeDependentChain(vajramId, sum3_s, splitSum1_s, splitSum1_s, splitSum2_s),
        graph.computeDependentChain(vajramId, sum3_s, splitSum1_s, splitSum2_s, splitSum1_s),
        graph.computeDependentChain(vajramId, sum3_s, splitSum1_s, splitSum2_s, splitSum2_s),
        graph.computeDependentChain(vajramId, sum3_s, splitSum2_s, splitSum1_s, splitSum1_s),
        graph.computeDependentChain(vajramId, sum3_s, splitSum2_s, splitSum1_s, splitSum2_s),
        graph.computeDependentChain(vajramId, sum3_s, splitSum2_s, splitSum2_s, splitSum1_s),
        graph.computeDependentChain(vajramId, sum3_s, splitSum2_s, splitSum2_s, splitSum2_s));
  }
}

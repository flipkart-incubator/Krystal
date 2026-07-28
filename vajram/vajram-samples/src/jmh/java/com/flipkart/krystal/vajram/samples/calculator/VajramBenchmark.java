package com.flipkart.krystal.vajram.samples.calculator;

import static com.flipkart.krystal.vajram.VajramID.vajramID;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.batching.InputBatcherImpl;
import com.flipkart.krystal.vajram.samples.calculator.adder.Adder;
import com.flipkart.krystal.vajram.samples.calculator.adder.ChainAdder;
import com.flipkart.krystal.vajram.samples.calculator.adder.ChainAdderRequest;
import com.flipkart.krystal.vajram.samples.calculator.adder.SplitAdder;
import com.flipkart.krystal.vajram.samples.calculator.adder.SplitAdderRequest;
import com.flipkart.krystal.vajramexecutor.krystex.InputBatcherConfig;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Measures a complete graph execution, including the single-use executor lifecycle.
 *
 * <p>Krystal 7:
 *
 * <pre>
 * Benchmark             Mode  Cnt      Score      Error  Units
 * -----------------------------------------------------------------
 * chainAdd                    thrpt    5   9384.372 ±  479.383  ops/s
 * chainAddBatched             thrpt    5   9178.772 ±  428.988  ops/s
 * chainAddTenRequests         thrpt    5   1369.916 ±   76.538  ops/s
 * chainAddTenRequestsBatched  thrpt    5   1374.228 ±   59.572  ops/s
 * formula                     thrpt    5  39361.727 ± 4096.927  ops/s
 * formulaTenRequests          thrpt    5  11144.989 ±  399.647  ops/s
 * splitAdd                    thrpt    5   4221.516 ±  142.111  ops/s
 * splitAddTenRequests         thrpt    5   1025.806 ±   27.483  ops/s
 * </pre>
 *
 * Krystal 8:
 *
 * <pre>
 * Benchmark                  Mode  Cnt      Score      Error  Units
 * -----------------------------------------------------------------
 * chainAdd                    thrpt    5   9155.202 ±  560.568  ops/s
 * chainAddBatched             thrpt    5   9090.014 ±  198.828  ops/s
 * chainAddTenRequests         thrpt    5   1434.753 ±   31.601  ops/s
 * chainAddTenRequestsBatched  thrpt    5   1396.423 ±  207.039  ops/s
 * formula                     thrpt    5  30807.059 ± 8838.200  ops/s
 * formulaTenRequests          thrpt    5  10548.213 ±  581.670  ops/s
 * splitAdd                    thrpt    5   4272.903 ±  398.323  ops/s
 * splitAddTenRequests         thrpt    5   1013.944 ±   32.664  ops/s
 * </pre>
 */
@State(Scope.Benchmark)
@Threads(1)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class VajramBenchmark {
  private static final KryonExecutionConfig EXECUTION_CONFIG =
      KryonExecutionConfig.builder().build();
  private static final FormulaRequest FORMULA_REQUEST =
      FormulaRequest.builder().a(100).p(20).q(5).build();
  private static final List<Integer> RECURSIVE_ADDENDS = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
  private static final ChainAdderRequest CHAIN_ADD_REQUEST =
      ChainAdderRequest.builder().numbers(RECURSIVE_ADDENDS).build();
  private static final SplitAdderRequest SPLIT_ADD_REQUEST =
      SplitAdderRequest.builder().numbers(RECURSIVE_ADDENDS).build();
  //  private static final MultiAdd_ReqImmutPojo MULTI_ADD_REQUEST =
  //      MultiAdd_ReqImmutPojo._builder().numbers(RECURSIVE_ADDENDS)._build();

  private SingleThreadExecutorsPool executorPool;
  private Lease<SingleThreadExecutor> executorLease;
  private VajramKryonGraph formulaGraph;
  private VajramKryonGraph splitAddGraph;
  private VajramKryonGraph chainAddGraph;
  private VajramKryonGraph chainAddBatchedGraph;
  private VajramKryonGraph multiAddGraph;

  @Setup(Level.Trial)
  public void setUp() throws LeaseUnavailableException {
    executorPool = new SingleThreadExecutorsPool("jmh", 1);
    executorLease = executorPool.lease();
    formulaGraph = graphFor();
    //        graphFor(
    //            Formula_Req._VAJRAM_ID,
    //            _graph -> ImmutableSet.of(),
    //            Formula.class,
    //            Add.class,
    //            Divide.class);
    splitAddGraph = graphFor();
    //        graphFor(
    //            SplitAdd_Req._VAJRAM_ID,
    //            VajramBenchmark::splitAddDisabledChains,
    //            SplitAdd.class,
    //            Add.class);
    chainAddGraph = graphFor();
    chainAddBatchedGraph = graphFor();
    chainAddBatchedGraph.registerInputBatchers(
        chainAddBatchedGraph.getVajramId(Adder.class),
        InputBatcherConfig.sharedBatcher(
            () -> new InputBatcherImpl<>(100),
            "adderBatcher",
            getChainAdderBatchedDepChains(chainAddGraph)));
    //        graphFor(
    //            ChainAdd_Req._VAJRAM_ID,
    //            VajramBenchmark::chainAddDisabledChains,
    //            ChainAdd.class,
    //            Add.class);

    //    VajramGraph graph = VajramGraph.builder().loadClasses(MultiAdd.class,
    // SimpleAdd.class).build();
    multiAddGraph = graphFor();
    //        KrystexGraph.builder()
    //            .vajramGraph(graph)
    //            .externallyInvocableVajramIds(ImmutableSet.of(MultiAdd_Req._VAJRAM_ID))
    //            .traitDispatchPolicies(
    //                new TraitDispatchPolicies(
    //                    dispatchTrait(MultiAdd_Req.class, graph)
    //                        .conditionally(when(numbers_s,
    // isAnyValue()).to(SimpleAdd_Req.class))))
    //            .build();
  }

  @TearDown(Level.Trial)
  public void tearDown() {
    executorLease.close();
    executorPool.close();
  }

  @Benchmark
  public int formula() {
    return execute(
        formulaGraph,
        formulaGraph.getVajramId(Formula.class),
        FORMULA_REQUEST,
        splitAddDisabledChains(splitAddGraph));
  }

  @Benchmark
  public int formulaTenRequests() {
    return executeTenRequests(
        formulaGraph,
        formulaGraph.getVajramId(Formula.class),
        FORMULA_REQUEST,
        splitAddDisabledChains(splitAddGraph));
  }

  @Benchmark
  public int splitAdd() {
    return execute(
        splitAddGraph,
        splitAddGraph.getVajramId(SplitAdder.class),
        SPLIT_ADD_REQUEST,
        splitAddDisabledChains(splitAddGraph));
  }

  @Benchmark
  public int splitAddTenRequests() {
    return executeTenRequests(
        splitAddGraph,
        splitAddGraph.getVajramId(SplitAdder.class),
        SPLIT_ADD_REQUEST,
        splitAddDisabledChains(splitAddGraph));
  }

  @Benchmark
  public int chainAdd() {
    return execute(
        chainAddGraph,
        chainAddGraph.getVajramId(ChainAdder.class),
        CHAIN_ADD_REQUEST,
        chainAddDisabledChains(chainAddGraph));
  }

  @Benchmark
  public int chainAddTenRequests() {
    return executeTenRequests(
        chainAddGraph,
        chainAddGraph.getVajramId(ChainAdder.class),
        CHAIN_ADD_REQUEST,
        chainAddDisabledChains(chainAddGraph));
  }

  @Benchmark
  public int chainAddBatched() {
    return execute(
        chainAddBatchedGraph,
        chainAddBatchedGraph.getVajramId(ChainAdder.class),
        CHAIN_ADD_REQUEST,
        chainAddDisabledChains(chainAddBatchedGraph));
  }

  @Benchmark
  public int chainAddTenRequestsBatched() {
    return executeTenRequests(
        chainAddBatchedGraph,
        chainAddBatchedGraph.getVajramId(ChainAdder.class),
        CHAIN_ADD_REQUEST,
        chainAddDisabledChains(chainAddBatchedGraph));
  }

  //  @Benchmark
  //  public int multiAdd() {
  //    return execute(multiAddGraph, VajramID.ofVajram(Formula.class), MULTI_ADD_REQUEST);
  //  }

  private static VajramKryonGraph graphFor() {
    return VajramKryonGraph.builder()
        .loadFromPackage("com.flipkart.krystal.vajram.samples.calculator")
        .build();
  }

  //  @SafeVarargs
  //  private static VajramKryonGraph graphFor(
  //      VajramID vajramId,
  //      Function<VajramGraph, ImmutableSet<DependentChain>> disabledChains,
  //      Class<? extends VajramDefRoot>... vajrams) {
  //    VajramGraph graph = VajramGraph.builder().loadClasses(vajrams).build();
  //    return KrystexGraph.builder()
  //        .externallyInvocableVajramIds(ImmutableSet.of(vajramId))
  //        .dependentChainDisabler(new SimpleDependentChainDisabler(disabledChains.apply(graph)))
  //        .build();
  //  }

  private static ImmutableSet<DependantChain> chainAddDisabledChains(VajramKryonGraph graph) {
    String vajramId = graph.getVajramId(ChainAdder.class).vajramId();
    return ImmutableSet.of(
        graph.computeDependantChain(
            vajramId,
            "chainSum",
            "chainSum",
            "chainSum",
            "chainSum",
            "chainSum",
            "chainSum",
            "chainSum",
            "chainSum",
            "chainSum"));
  }

  private static ImmutableSet<DependantChain> splitAddDisabledChains(VajramKryonGraph graph) {
    String vajramId = graph.getVajramId(SplitAdder.class).vajramId();
    return ImmutableSet.of(
        graph.computeDependantChain(
            vajramId, "splitSum1", "splitSum1", "splitSum1", "splitSum1", "splitSum1"),
        graph.computeDependantChain(
            vajramId, "splitSum1", "splitSum1", "splitSum1", "splitSum1", "splitSum2"),
        graph.computeDependantChain(
            vajramId, "splitSum1", "splitSum1", "splitSum1", "splitSum2", "splitSum1"),
        graph.computeDependantChain(
            vajramId, "splitSum1", "splitSum1", "splitSum1", "splitSum2", "splitSum2"),
        graph.computeDependantChain(
            vajramId, "splitSum1", "splitSum1", "splitSum2", "splitSum1", "splitSum1"),
        graph.computeDependantChain(
            vajramId, "splitSum1", "splitSum1", "splitSum2", "splitSum1", "splitSum2"),
        graph.computeDependantChain(
            vajramId, "splitSum1", "splitSum1", "splitSum2", "splitSum2", "splitSum1"),
        graph.computeDependantChain(
            vajramId, "splitSum1", "splitSum1", "splitSum2", "splitSum2", "splitSum2"),
        graph.computeDependantChain(
            vajramId, "splitSum1", "splitSum2", "splitSum1", "splitSum1", "splitSum1"),
        graph.computeDependantChain(
            vajramId, "splitSum1", "splitSum2", "splitSum1", "splitSum1", "splitSum2"),
        graph.computeDependantChain(
            vajramId, "splitSum1", "splitSum2", "splitSum1", "splitSum2", "splitSum1"),
        graph.computeDependantChain(
            vajramId, "splitSum1", "splitSum2", "splitSum1", "splitSum2", "splitSum2"),
        graph.computeDependantChain(
            vajramId, "splitSum1", "splitSum2", "splitSum2", "splitSum1", "splitSum1"),
        graph.computeDependantChain(
            vajramId, "splitSum1", "splitSum2", "splitSum2", "splitSum1", "splitSum2"),
        graph.computeDependantChain(
            vajramId, "splitSum1", "splitSum2", "splitSum2", "splitSum2", "splitSum1"),
        graph.computeDependantChain(
            vajramId, "splitSum1", "splitSum2", "splitSum2", "splitSum2", "splitSum2"),
        graph.computeDependantChain(
            vajramId, "splitSum2", "splitSum1", "splitSum1", "splitSum1", "splitSum1"),
        graph.computeDependantChain(
            vajramId, "splitSum2", "splitSum1", "splitSum1", "splitSum1", "splitSum2"),
        graph.computeDependantChain(
            vajramId, "splitSum2", "splitSum1", "splitSum1", "splitSum2", "splitSum1"),
        graph.computeDependantChain(
            vajramId, "splitSum2", "splitSum1", "splitSum1", "splitSum2", "splitSum2"),
        graph.computeDependantChain(
            vajramId, "splitSum2", "splitSum1", "splitSum2", "splitSum1", "splitSum1"),
        graph.computeDependantChain(
            vajramId, "splitSum2", "splitSum1", "splitSum2", "splitSum1", "splitSum2"),
        graph.computeDependantChain(
            vajramId, "splitSum2", "splitSum1", "splitSum2", "splitSum2", "splitSum1"),
        graph.computeDependantChain(
            vajramId, "splitSum2", "splitSum1", "splitSum2", "splitSum2", "splitSum2"),
        graph.computeDependantChain(
            vajramId, "splitSum2", "splitSum2", "splitSum1", "splitSum1", "splitSum1"),
        graph.computeDependantChain(
            vajramId, "splitSum2", "splitSum2", "splitSum1", "splitSum1", "splitSum2"),
        graph.computeDependantChain(
            vajramId, "splitSum2", "splitSum2", "splitSum1", "splitSum2", "splitSum1"),
        graph.computeDependantChain(
            vajramId, "splitSum2", "splitSum2", "splitSum1", "splitSum2", "splitSum2"),
        graph.computeDependantChain(
            vajramId, "splitSum2", "splitSum2", "splitSum2", "splitSum1", "splitSum1"),
        graph.computeDependantChain(
            vajramId, "splitSum2", "splitSum2", "splitSum2", "splitSum1", "splitSum2"),
        graph.computeDependantChain(
            vajramId, "splitSum2", "splitSum2", "splitSum2", "splitSum2", "splitSum1"),
        graph.computeDependantChain(
            vajramId, "splitSum2", "splitSum2", "splitSum2", "splitSum2", "splitSum2"));
  }

  private int execute(
      VajramKryonGraph graph,
      VajramID vajramID,
      VajramRequest<Integer> request,
      ImmutableSet<DependantChain> disabledDependantChains) {
    CompletableFuture<Integer> result;
    try (KrystexVajramExecutor executor =
        graph.createExecutor(
            KrystexVajramExecutorConfig.builder()
                .kryonExecutorConfigBuilder(
                    KryonExecutorConfig.builder()
                        .singleThreadExecutor(executorLease.get())
                        .disabledDependantChains(disabledDependantChains))
                .build())) {
      result = executor.execute(vajramID, request);
    }
    return result.join();
  }

  private int executeTenRequests(
      VajramKryonGraph graph,
      VajramID vajramID,
      VajramRequest<Integer> request,
      ImmutableSet<DependantChain> disabledDependantChains) {
    CompletableFuture<Integer>[] results = new CompletableFuture[10];
    try (KrystexVajramExecutor executor =
        graph.createExecutor(
            KrystexVajramExecutorConfig.builder()
                .kryonExecutorConfigBuilder(
                    KryonExecutorConfig.builder()
                        .singleThreadExecutor(executorLease.get())
                        .disabledDependantChains(disabledDependantChains))
                .build())) {
      for (int i = 0; i < results.length; i++) {
        results[i] = executor.execute(vajramID, request);
      }
    }
    return CompletableFuture.allOf(results).thenApply(ignored -> results[0].join()).join();
  }

  private DependantChain[] getChainAdderBatchedDepChains(VajramKryonGraph graph) {
    String chainAdderId = graph.getVajramId(ChainAdder.class).vajramId();
    return new DependantChain[] {
      graph.computeDependantChain(chainAdderId, "sum"),
      graph.computeDependantChain(chainAdderId, "chainSum", "sum"),
      graph.computeDependantChain(chainAdderId, "chainSum", "chainSum", "sum"),
      graph.computeDependantChain(chainAdderId, "chainSum", "chainSum", "chainSum", "sum"),
      graph.computeDependantChain(
          chainAdderId, "chainSum", "chainSum", "chainSum", "chainSum", "sum"),
      graph.computeDependantChain(
          chainAdderId, "chainSum", "chainSum", "chainSum", "chainSum", "chainSum", "sum"),
      graph.computeDependantChain(
          chainAdderId,
          "chainSum",
          "chainSum",
          "chainSum",
          "chainSum",
          "chainSum",
          "chainSum",
          "sum"),
      graph.computeDependantChain(
          chainAdderId,
          "chainSum",
          "chainSum",
          "chainSum",
          "chainSum",
          "chainSum",
          "chainSum",
          "chainSum",
          "sum"),
      graph.computeDependantChain(
          chainAdderId,
          "chainSum",
          "chainSum",
          "chainSum",
          "chainSum",
          "chainSum",
          "chainSum",
          "chainSum",
          "chainSum",
          "sum")
    };
  }
}

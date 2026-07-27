package com.flipkart.krystal.vajram.samples.calculator;

import static com.flipkart.krystal.traits.matchers.InputValueMatcher.isAnyValue;
import static com.flipkart.krystal.vajram.samples.calculator.add.ChainAdd_Fac.chainSum_s;
import static com.flipkart.krystal.vajram.samples.calculator.add.MultiAdd_Req.numbers_s;
import static com.flipkart.krystal.vajram.samples.calculator.add.SplitAdd_Fac.splitSum1_s;
import static com.flipkart.krystal.vajram.samples.calculator.add.SplitAdd_Fac.splitSum2_s;
import static com.flipkart.krystal.vajramexecutor.krystex.traits.PredicateDispatchUtil.dispatchTrait;
import static com.flipkart.krystal.vajramexecutor.krystex.traits.PredicateDispatchUtil.when;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajram.samples.calculator.add.ChainAdd;
import com.flipkart.krystal.vajram.samples.calculator.add.ChainAdd_Req;
import com.flipkart.krystal.vajram.samples.calculator.add.ChainAdd_ReqImmutPojo;
import com.flipkart.krystal.vajram.samples.calculator.add.MultiAdd_Req;
import com.flipkart.krystal.vajram.samples.calculator.add.MultiAdd_ReqImmutPojo;
import com.flipkart.krystal.vajram.samples.calculator.add.SimpleAdd_Req;
import com.flipkart.krystal.vajram.samples.calculator.add.SplitAdd;
import com.flipkart.krystal.vajram.samples.calculator.add.SplitAdd_Req;
import com.flipkart.krystal.vajram.samples.calculator.add.SplitAdd_ReqImmutPojo;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.traits.PredicateDispatchPolicyImpl;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
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
 * chainAdd             thrpt    5   9283.580 ±  538.633  ops/s
 * chainAddTenRequests  thrpt    5   1374.468 ±   56.580  ops/s
 * formula              thrpt    5  39480.908 ± 3024.456  ops/s
 * formulaTenRequests   thrpt    5  11019.657 ±  544.938  ops/s
 * splitAdd             thrpt    5   4434.492 ±   83.799  ops/s
 * splitAddTenRequests  thrpt    5   1026.046 ±   28.474  ops/s
 * </pre>
 *
 * Krystal 8:
 *
 * <pre>
 * Benchmark                  Mode  Cnt      Score      Error  Units
 * -----------------------------------------------------------------
 * chainAdd             thrpt    5   9265.238 ±  539.952  ops/s
 * chainAddTenRequests  thrpt    5   1382.489 ±  112.046  ops/s
 * formula              thrpt    5  37223.270 ± 3935.870  ops/s
 * formulaTenRequests   thrpt    5  10933.280 ±  511.317  ops/s
 * splitAdd             thrpt    5   4242.873 ±   40.167  ops/s
 * splitAddTenRequests  thrpt    5   1038.458 ±   44.929  ops/s
 * </pre>
 *
 * Krystal 9:
 *
 * <pre>
 * Benchmark                  Mode  Cnt      Score      Error  Units
 * -----------------------------------------------------------------
 * chainAdd                           thrpt    5  44853.195 ±  2627.742  ops/s
 * chainAddTenRequests                thrpt    5  13030.911 ±   479.204  ops/s
 * formula                            thrpt    5  55569.819 ±  1773.361  ops/s
 * formulaTenRequests                 thrpt    5  18151.606 ±   420.070  ops/s
 * multiAddWithSimpleAdd              thrpt    5  87963.394 ± 11119.658  ops/s
 * multiAddWithSimpleAddTenRequests   thrpt    5  50977.265 ±  1551.162  ops/s
 * splitAdd                           thrpt    5  38869.913 ±   852.005  ops/s
 * splitAddTenRequests                thrpt    5  12481.518 ±   578.171  ops/s
 * </pre>
 */
@State(Scope.Benchmark)
@Threads(1)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class VajramBenchmark {
  private static final KryonExecutionConfig EXECUTION_CONFIG =
      KryonExecutionConfig.builder().build();
  private static final Formula_Req FORMULA_REQUEST =
      Formula_ReqImmutPojo._builder().a(100).p(20).q(5)._build();
  private static final List<Integer> RECURSIVE_ADDENDS = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
  private static final ChainAdd_Req CHAIN_ADD_REQUEST =
      ChainAdd_ReqImmutPojo._builder().numbers(RECURSIVE_ADDENDS)._build();
  private static final SplitAdd_Req SPLIT_ADD_REQUEST =
      SplitAdd_ReqImmutPojo._builder().numbers(RECURSIVE_ADDENDS)._build();
  private static final MultiAdd_Req MULTI_ADD_REQUEST =
      MultiAdd_ReqImmutPojo._builder().numbers(RECURSIVE_ADDENDS)._build();

  private SingleThreadExecutorsPool executorPool;
  private Lease<SingleThreadExecutor> executorLease;
  private VajramKryonGraph formulaGraph;
  private VajramKryonGraph splitAddGraph;
  private VajramKryonGraph chainAddGraph;
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
    //        graphFor(
    //            ChainAdd_Req._VAJRAM_ID,
    //            VajramBenchmark::chainAddDisabledChains,
    //            ChainAdd.class,
    //            Add.class);

    //    VajramGraph graph = VajramGraph.builder().loadClasses(MultiAdd.class,
    // SimpleAdd.class).build();
    multiAddGraph = graphFor();
    multiAddGraph.registerTraitDispatchPolicies(
        dispatchTrait(MultiAdd_Req.class, multiAddGraph)
            .conditionally(when(numbers_s, isAnyValue()).to(SimpleAdd_Req.class)));
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
    return execute(formulaGraph, FORMULA_REQUEST, splitAddDisabledChains(splitAddGraph));
  }

  @Benchmark
  public int formulaTenRequests() {
    return executeTenRequests(formulaGraph, FORMULA_REQUEST, splitAddDisabledChains(splitAddGraph));
  }

  @Benchmark
  public int splitAdd() {
    return execute(splitAddGraph, SPLIT_ADD_REQUEST, splitAddDisabledChains(splitAddGraph));
  }

  @Benchmark
  public int splitAddTenRequests() {
    return executeTenRequests(
        splitAddGraph, SPLIT_ADD_REQUEST, splitAddDisabledChains(splitAddGraph));
  }

  @Benchmark
  public int chainAdd() {
    return execute(chainAddGraph, CHAIN_ADD_REQUEST, chainAddDisabledChains(chainAddGraph));
  }

  @Benchmark
  public int chainAddTenRequests() {
    return executeTenRequests(
        chainAddGraph, CHAIN_ADD_REQUEST, chainAddDisabledChains(chainAddGraph));
  }

  @Benchmark
  public int multiAddWithSimpleAdd() {
    return execute(
        multiAddGraph,
        MULTI_ADD_REQUEST,
        ImmutableSet.copyOf(
            Sets.union(
                chainAddDisabledChains(multiAddGraph), splitAddDisabledChains(multiAddGraph))));
  }

  @Benchmark
  public int multiAddWithSimpleAddTenRequests() {
    return executeTenRequests(
        multiAddGraph,
        MULTI_ADD_REQUEST,
        ImmutableSet.copyOf(
            Sets.union(
                chainAddDisabledChains(multiAddGraph), splitAddDisabledChains(multiAddGraph))));
  }

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

  private static ImmutableSet<DependentChain> chainAddDisabledChains(VajramKryonGraph graph) {
    return ImmutableSet.of(
        graph.computeDependentChain(
            graph.getVajramIdByVajramDefType(ChainAdd.class).id(),
            chainSum_s,
            chainSum_s,
            chainSum_s,
            chainSum_s,
            chainSum_s,
            chainSum_s,
            chainSum_s,
            chainSum_s,
            chainSum_s));
  }

  private static ImmutableSet<DependentChain> splitAddDisabledChains(VajramKryonGraph graph) {
    String splitAdderId = graph.getVajramIdByVajramDefType(SplitAdd.class).id();
    return ImmutableSet.of(
        graph.computeDependentChain(
            splitAdderId, splitSum1_s, splitSum1_s, splitSum1_s, splitSum1_s, splitSum1_s),
        graph.computeDependentChain(
            splitAdderId, splitSum1_s, splitSum1_s, splitSum1_s, splitSum1_s, splitSum2_s),
        graph.computeDependentChain(
            splitAdderId, splitSum1_s, splitSum1_s, splitSum1_s, splitSum2_s, splitSum1_s),
        graph.computeDependentChain(
            splitAdderId, splitSum1_s, splitSum1_s, splitSum1_s, splitSum2_s, splitSum2_s),
        graph.computeDependentChain(
            splitAdderId, splitSum1_s, splitSum1_s, splitSum2_s, splitSum1_s, splitSum1_s),
        graph.computeDependentChain(
            splitAdderId, splitSum1_s, splitSum1_s, splitSum2_s, splitSum1_s, splitSum2_s),
        graph.computeDependentChain(
            splitAdderId, splitSum1_s, splitSum1_s, splitSum2_s, splitSum2_s, splitSum1_s),
        graph.computeDependentChain(
            splitAdderId, splitSum1_s, splitSum1_s, splitSum2_s, splitSum2_s, splitSum2_s),
        graph.computeDependentChain(
            splitAdderId, splitSum1_s, splitSum2_s, splitSum1_s, splitSum1_s, splitSum1_s),
        graph.computeDependentChain(
            splitAdderId, splitSum1_s, splitSum2_s, splitSum1_s, splitSum1_s, splitSum2_s),
        graph.computeDependentChain(
            splitAdderId, splitSum1_s, splitSum2_s, splitSum1_s, splitSum2_s, splitSum1_s),
        graph.computeDependentChain(
            splitAdderId, splitSum1_s, splitSum2_s, splitSum1_s, splitSum2_s, splitSum2_s),
        graph.computeDependentChain(
            splitAdderId, splitSum1_s, splitSum2_s, splitSum2_s, splitSum1_s, splitSum1_s),
        graph.computeDependentChain(
            splitAdderId, splitSum1_s, splitSum2_s, splitSum2_s, splitSum1_s, splitSum2_s),
        graph.computeDependentChain(
            splitAdderId, splitSum1_s, splitSum2_s, splitSum2_s, splitSum2_s, splitSum1_s),
        graph.computeDependentChain(
            splitAdderId, splitSum1_s, splitSum2_s, splitSum2_s, splitSum2_s, splitSum2_s),
        graph.computeDependentChain(
            splitAdderId, splitSum2_s, splitSum1_s, splitSum1_s, splitSum1_s, splitSum1_s),
        graph.computeDependentChain(
            splitAdderId, splitSum2_s, splitSum1_s, splitSum1_s, splitSum1_s, splitSum2_s),
        graph.computeDependentChain(
            splitAdderId, splitSum2_s, splitSum1_s, splitSum1_s, splitSum2_s, splitSum1_s),
        graph.computeDependentChain(
            splitAdderId, splitSum2_s, splitSum1_s, splitSum1_s, splitSum2_s, splitSum2_s),
        graph.computeDependentChain(
            splitAdderId, splitSum2_s, splitSum1_s, splitSum2_s, splitSum1_s, splitSum1_s),
        graph.computeDependentChain(
            splitAdderId, splitSum2_s, splitSum1_s, splitSum2_s, splitSum1_s, splitSum2_s),
        graph.computeDependentChain(
            splitAdderId, splitSum2_s, splitSum1_s, splitSum2_s, splitSum2_s, splitSum1_s),
        graph.computeDependentChain(
            splitAdderId, splitSum2_s, splitSum1_s, splitSum2_s, splitSum2_s, splitSum2_s),
        graph.computeDependentChain(
            splitAdderId, splitSum2_s, splitSum2_s, splitSum1_s, splitSum1_s, splitSum1_s),
        graph.computeDependentChain(
            splitAdderId, splitSum2_s, splitSum2_s, splitSum1_s, splitSum1_s, splitSum2_s),
        graph.computeDependentChain(
            splitAdderId, splitSum2_s, splitSum2_s, splitSum1_s, splitSum2_s, splitSum1_s),
        graph.computeDependentChain(
            splitAdderId, splitSum2_s, splitSum2_s, splitSum1_s, splitSum2_s, splitSum2_s),
        graph.computeDependentChain(
            splitAdderId, splitSum2_s, splitSum2_s, splitSum2_s, splitSum1_s, splitSum1_s),
        graph.computeDependentChain(
            splitAdderId, splitSum2_s, splitSum2_s, splitSum2_s, splitSum1_s, splitSum2_s),
        graph.computeDependentChain(
            splitAdderId, splitSum2_s, splitSum2_s, splitSum2_s, splitSum2_s, splitSum1_s),
        graph.computeDependentChain(
            splitAdderId, splitSum2_s, splitSum2_s, splitSum2_s, splitSum2_s, splitSum2_s));
  }

  private int execute(
      VajramKryonGraph graph,
      Request<Integer> request,
      ImmutableSet<DependentChain> disabledDependantChains) {
    return executeNTimes(graph, request, disabledDependantChains, 1);
  }

  private int executeTenRequests(
      VajramKryonGraph graph,
      Request<Integer> request,
      ImmutableSet<DependentChain> disabledDependantChains) {
    return executeNTimes(graph, request, disabledDependantChains, 10);
  }

  private Integer executeNTimes(
      VajramKryonGraph graph,
      Request<Integer> request,
      ImmutableSet<DependentChain> disabledDependantChains,
      int times) {
    CompletableFuture<Integer>[] results = new CompletableFuture[times];
    try (KrystexVajramExecutor executor =
        graph.createExecutor(
            KrystexVajramExecutorConfig.builder()
                .vajramKryonGraph(graph)
                .kryonExecutorConfig(
                    KryonExecutorConfig.builder()
                        .singleThreadExecutor(executorLease.get())
                        .disabledDependentChains(disabledDependantChains)
                        .build())
                .build())) {
      for (int i = 0; i < results.length; i++) {
        results[i] = executor.execute(request._build());
      }
    }
    return CompletableFuture.allOf(results).thenApply(ignored -> results[0].join()).join();
  }
}

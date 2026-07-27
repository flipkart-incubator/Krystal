package com.flipkart.krystal.vajram.samples.calculator;

import static com.flipkart.krystal.krystex.traits.PredicateDispatchUtil.dispatchTrait;
import static com.flipkart.krystal.krystex.traits.PredicateDispatchUtil.when;
import static com.flipkart.krystal.traits.matchers.InputValueMatcher.isAnyValue;
import static com.flipkart.krystal.vajram.samples.calculator.add.ChainAdd_Fac.chainSum_s;
import static com.flipkart.krystal.vajram.samples.calculator.add.MultiAdd_Req.numbers_s;
import static com.flipkart.krystal.vajram.samples.calculator.add.SplitAdd_Fac.splitSum1_s;
import static com.flipkart.krystal.vajram.samples.calculator.add.SplitAdd_Fac.splitSum2_s;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.krystex.KrystalExecutorConfig;
import com.flipkart.krystal.krystex.KrystexGraph;
import com.flipkart.krystal.krystex.SimpleDependentChainDisabler;
import com.flipkart.krystal.krystex.VajramGraph;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.VajramExecutionConfig;
import com.flipkart.krystal.krystex.kryon.VajramKryonExecutor;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.traits.TraitDispatchPolicies;
import com.flipkart.krystal.vajram.VajramDefRoot;
import com.flipkart.krystal.vajram.samples.calculator.add.Add;
import com.flipkart.krystal.vajram.samples.calculator.add.ChainAdd;
import com.flipkart.krystal.vajram.samples.calculator.add.ChainAdd_Req;
import com.flipkart.krystal.vajram.samples.calculator.add.ChainAdd_ReqImmutPojo;
import com.flipkart.krystal.vajram.samples.calculator.add.MultiAdd;
import com.flipkart.krystal.vajram.samples.calculator.add.MultiAdd_Req;
import com.flipkart.krystal.vajram.samples.calculator.add.MultiAdd_ReqImmutPojo;
import com.flipkart.krystal.vajram.samples.calculator.add.SimpleAdd;
import com.flipkart.krystal.vajram.samples.calculator.add.SimpleAdd_Req;
import com.flipkart.krystal.vajram.samples.calculator.add.SplitAdd;
import com.flipkart.krystal.vajram.samples.calculator.add.SplitAdd_Req;
import com.flipkart.krystal.vajram.samples.calculator.add.SplitAdd_ReqImmutPojo;
import com.flipkart.krystal.vajram.samples.calculator.divide.Divide;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
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
 * Benchmark             Mode  Cnt      Score      Error  Units
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
 * Benchmark                          Mode  Cnt      Score      Error  Units
 * -----------------------------------------------------------------
 * chainAdd                          thrpt    5  14754.443 ±  3452.352  ops/s
 * chainAddTenRequests               thrpt    5   2708.863 ±   155.802  ops/s
 * formula                           thrpt    5  44321.926 ±  3367.481  ops/s
 * formulaTenRequests                thrpt    5  16996.218 ±  1522.687  ops/s
 * multiAddWithSimpleAdd             thrpt    5  62730.325 ± 10245.642  ops/s
 * multiAddWithSimpleAddTenRequests  thrpt    5  38436.398 ±  1368.890  ops/s
 * splitAdd                          thrpt    5   5696.577 ±  1094.206  ops/s
 * splitAddTenRequests               thrpt    5   1660.296 ±   125.501  ops/s
 * </pre>
 *
 * Krystal 10:
 *
 * <pre>
 * Benchmark                          Mode  Cnt      Score      Error  Units
 * --------------------------------------------------------------------------
 * chainAdd                          thrpt    5   22544.942 ±  2629.140  ops/s
 * chainAddTenRequests               thrpt    5    5227.917 ±   132.176  ops/s
 * formula                           thrpt    5   34365.315 ±  3287.154  ops/s
 * formulaTenRequests                thrpt    5   15474.605 ±   551.554  ops/s
 * multiAddWithSimpleAdd             thrpt    5  157125.328 ± 30848.510  ops/s
 * multiAddWithSimpleAddTenRequests  thrpt    5   41827.628 ±  1594.955  ops/s
 * splitAdd                          thrpt    5    7249.799 ±   897.954  ops/s
 * splitAddTenRequests               thrpt    5    2800.829 ±   702.517  ops/s
 * </pre>
 *
 * Krystal 11:
 *
 * <pre>
 * Benchmark                          Mode  Cnt      Score      Error  Units
 * -----------------------------------------------------------------
 * chainAdd                          thrpt    5   57903.597 ±   5456.565  ops/s
 * chainAddTenRequests               thrpt    5   21863.351 ±   1533.453  ops/s
 * formula                           thrpt    5   42273.086 ±   6099.932  ops/s
 * formulaTenRequests                thrpt    5   24702.678 ±   3431.427  ops/s
 * multiAddWithSimpleAdd             thrpt    5  148826.406 ± 125428.852  ops/s
 * multiAddWithSimpleAddTenRequests  thrpt    5  103041.960 ±  18661.613  ops/s
 * splitAdd                          thrpt    5   86079.560 ±   2167.610  ops/s
 * splitAddTenRequests               thrpt    5   33766.330 ±  13915.911  ops/s
 * </pre>
 */
@State(Scope.Benchmark)
@Threads(1)
@Warmup(iterations = 5, time = 2, timeUnit = SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = SECONDS)
@Fork(1)
public class VajramBenchmark {
  private static final VajramExecutionConfig EXECUTION_CONFIG =
      VajramExecutionConfig.builder().build();
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
  private KrystexGraph formulaGraph;
  private KrystexGraph splitAddGraph;
  private KrystexGraph chainAddGraph;
  private KrystexGraph multiAddGraph;

  @Setup(Level.Trial)
  public void setUp() throws LeaseUnavailableException {
    executorPool = new SingleThreadExecutorsPool("jmh", 1);
    executorLease = executorPool.lease();
    formulaGraph =
        graphFor(
            Formula_Req._VAJRAM_ID,
            _graph -> ImmutableSet.of(),
            Formula.class,
            Add.class,
            Divide.class);
    splitAddGraph =
        graphFor(
            SplitAdd_Req._VAJRAM_ID,
            VajramBenchmark::splitAddDisabledChains,
            SplitAdd.class,
            Add.class);
    chainAddGraph =
        graphFor(
            ChainAdd_Req._VAJRAM_ID,
            VajramBenchmark::chainAddDisabledChains,
            ChainAdd.class,
            Add.class);

    VajramGraph graph = VajramGraph.builder().loadClasses(MultiAdd.class, SimpleAdd.class).build();
    multiAddGraph =
        KrystexGraph.builder()
            .vajramGraph(graph)
            .externallyInvocableVajramIds(ImmutableSet.of(MultiAdd_Req._VAJRAM_ID))
            .traitDispatchPolicies(
                new TraitDispatchPolicies(
                    dispatchTrait(MultiAdd_Req.class, graph)
                        .conditionally(when(numbers_s, isAnyValue()).to(SimpleAdd_Req.class))))
            .build();
  }

  @TearDown(Level.Trial)
  public void tearDown() {
    executorLease.close();
    executorPool.close();
  }

  @Benchmark
  public int formula() {
    return execute(
        formulaGraph, FORMULA_REQUEST, splitAddDisabledChains(splitAddGraph.vajramGraph()));
  }

  @Benchmark
  public int formulaTenRequests() {
    return executeTenRequests(
        formulaGraph, FORMULA_REQUEST, splitAddDisabledChains(splitAddGraph.vajramGraph()));
  }

  @Benchmark
  public int splitAdd() {
    return execute(
        splitAddGraph, SPLIT_ADD_REQUEST, splitAddDisabledChains(splitAddGraph.vajramGraph()));
  }

  @Benchmark
  public int splitAddTenRequests() {
    return executeTenRequests(
        splitAddGraph, SPLIT_ADD_REQUEST, splitAddDisabledChains(splitAddGraph.vajramGraph()));
  }

  @Benchmark
  public int chainAdd() {
    return execute(
        chainAddGraph, CHAIN_ADD_REQUEST, chainAddDisabledChains(chainAddGraph.vajramGraph()));
  }

  @Benchmark
  public int chainAddTenRequests() {
    return executeTenRequests(
        chainAddGraph, CHAIN_ADD_REQUEST, chainAddDisabledChains(chainAddGraph.vajramGraph()));
  }

  @Benchmark
  public int multiAddWithSimpleAdd() {
    return execute(multiAddGraph, MULTI_ADD_REQUEST, ImmutableSet.of());
  }

  @Benchmark
  public int multiAddWithSimpleAddTenRequests() {
    return executeTenRequests(multiAddGraph, MULTI_ADD_REQUEST, ImmutableSet.of());
  }

  @SafeVarargs
  private static KrystexGraph graphFor(
      VajramID vajramId,
      Function<VajramGraph, ImmutableSet<DependentChain>> disabledChains,
      Class<? extends VajramDefRoot>... vajrams) {
    VajramGraph graph = VajramGraph.builder().loadClasses(vajrams).build();
    return KrystexGraph.builder()
        .vajramGraph(graph)
        .externallyInvocableVajramIds(ImmutableSet.of(vajramId))
        .dependentChainDisabler(new SimpleDependentChainDisabler(disabledChains.apply(graph)))
        .build();
  }

  private static ImmutableSet<DependentChain> chainAddDisabledChains(VajramGraph graph) {
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

  private static ImmutableSet<DependentChain> splitAddDisabledChains(VajramGraph graph) {
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
      KrystexGraph graph,
      Request<Integer> request,
      ImmutableSet<DependentChain> disabledDependantChains) {
    return executeNTimes(graph, request, disabledDependantChains, 1);
  }

  private int executeTenRequests(
      KrystexGraph graph,
      Request<Integer> request,
      ImmutableSet<DependentChain> disabledDependantChains) {
    return executeNTimes(graph, request, disabledDependantChains, 10);
  }

  private Integer executeNTimes(
      KrystexGraph graph,
      Request<Integer> request,
      ImmutableSet<DependentChain> disabledDependantChains,
      int times) {
    CompletableFuture<Integer>[] results = new CompletableFuture[times];
    try (VajramKryonExecutor executor =
        graph.createExecutor(
            KrystalExecutorConfig.builder()
                .executorService(executorLease.get())
                .disabledDependentChains(disabledDependantChains))) {
      for (int i = 0; i < results.length; i++) {
        results[i] =
            executor.execute(
                request._build(),
                VajramExecutionConfig.builder()
                    .disabledDependentChains(disabledDependantChains)
                    .build());
      }
    }
    return CompletableFuture.allOf(results).thenApply(ignored -> results[0].join()).join();
  }
}

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
import com.flipkart.krystal.krystex.KrystexGraph.KrystexGraphBuilder;
import com.flipkart.krystal.krystex.SimpleDependentChainDisabler;
import com.flipkart.krystal.krystex.VajramGraph;
import com.flipkart.krystal.krystex.batching.InputBatcherStrategy.DefaultBatcherStrategy;
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
 * Benchmark             Mode  Cnt      Score      Error  Units
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
 *
 * Krystal 9:
 *
 * <pre>
 * Benchmark                          Mode  Cnt      Score      Error  Units
 * -----------------------------------------------------------------
 * chainAdd                          thrpt    5  14784.531 ± 2779.631  ops/s
 * chainAddBatched                   thrpt    5  14010.276 ± 1651.020  ops/s
 * chainAddTenRequests               thrpt    5   2752.175 ±   98.626  ops/s
 * chainAddTenRequestsBatched        thrpt    5   2677.978 ±   65.749  ops/s
 * formula                           thrpt    5  46279.594 ± 2391.455  ops/s
 * formulaTenRequests                thrpt    5  17062.473 ±  399.770  ops/s
 * multiAddWithSimpleAdd             thrpt    5  64492.598 ± 1773.772  ops/s
 * multiAddWithSimpleAddTenRequests  thrpt    5  37708.755 ±  807.959  ops/s
 * splitAdd                          thrpt    5   5894.065 ±  100.944  ops/s
 * splitAddBatched                   thrpt    5   5111.545 ±  130.098  ops/s
 * splitAddTenRequests               thrpt    5   1555.872 ±   45.929  ops/s
 * splitAddTenRequestsBatched        thrpt    5   1552.031 ±   73.449  ops/s
 * </pre>
 *
 * Krystal 10:
 *
 * <pre>
 * Benchmark                          Mode  Cnt      Score      Error  Units
 * --------------------------------------------------------------------------
 * chainAdd                          thrpt    5   22509.394 ±  4389.478  ops/s
 * chainAddBatched                   thrpt    5   23873.368 ±  2292.460  ops/s
 * chainAddTenRequests               thrpt    5    5310.146 ±   196.610  ops/s
 * chainAddTenRequestsBatched        thrpt    5    5885.704 ±   157.620  ops/s
 * formula                           thrpt    5   34142.873 ±  4403.152  ops/s
 * formulaTenRequests                thrpt    5   16020.431 ±   572.713  ops/s
 * multiAddWithSimpleAdd             thrpt    5  170912.413 ± 16193.777  ops/s
 * multiAddWithSimpleAddTenRequests  thrpt    5   40365.056 ±  3837.764  ops/s
 * splitAdd                          thrpt    5    6690.845 ±   360.244  ops/s
 * splitAddBatched                   thrpt    5    6962.749 ±   563.504  ops/s
 * splitAddTenRequests               thrpt    5    2784.085 ±   106.404  ops/s
 * splitAddTenRequestsBatched        thrpt    5    3104.847 ±   121.929  ops/s
 * </pre>
 *
 * Krystal 11:
 *
 * <pre>
 * Benchmark                          Mode  Cnt      Score      Error  Units
 * -----------------------------------------------------------------
 * chainAdd                          thrpt    5   29005.062 ±  1031.420  ops/s
 * chainAddBatched                   thrpt    5   29482.634 ±  4086.956  ops/s
 * chainAddTenRequests               thrpt    5    6844.021 ±   631.362  ops/s
 * chainAddTenRequestsBatched        thrpt    5    7589.766 ±   190.022  ops/s
 * formula                           thrpt    5   41749.789 ±  6750.195  ops/s
 * formulaTenRequests                thrpt    5   22645.667 ±  3851.465  ops/s
 * multiAddWithSimpleAdd             thrpt    5  172631.797 ± 43027.517  ops/s
 * multiAddWithSimpleAddTenRequests  thrpt    5  104980.144 ±  3429.577  ops/s
 * splitAdd                          thrpt    5   18216.737 ±   718.836  ops/s
 * splitAddBatched                   thrpt    5   13920.811 ±  1902.600  ops/s
 * splitAddTenRequests               thrpt    5    6155.536 ±   336.507  ops/s
 * splitAddTenRequestsBatched        thrpt    5    6081.799 ±   186.328  ops/s
 * </pre>
 */
@State(Scope.Benchmark)
@Threads(1)
@Warmup(iterations = 5, time = 1, timeUnit = SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = SECONDS)
@Fork(1)
public class VajramBenchmark {
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
  private KrystexGraph splitAddBatchedGraph;
  private KrystexGraph chainAddGraph;
  private KrystexGraph chainAddBatchedGraph;
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
    splitAddBatchedGraph =
        batchedGraphFor(
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

    chainAddBatchedGraph =
        batchedGraphFor(
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
  public int splitAddBatched() {
    return execute(
        splitAddBatchedGraph,
        SPLIT_ADD_REQUEST,
        splitAddDisabledChains(splitAddBatchedGraph.vajramGraph()));
  }

  @Benchmark
  public int splitAddTenRequestsBatched() {
    return executeTenRequests(
        splitAddBatchedGraph,
        SPLIT_ADD_REQUEST,
        splitAddDisabledChains(splitAddBatchedGraph.vajramGraph()));
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
  public int chainAddBatched() {
    return execute(
        chainAddBatchedGraph,
        CHAIN_ADD_REQUEST,
        chainAddDisabledChains(chainAddBatchedGraph.vajramGraph()));
  }

  @Benchmark
  public int chainAddTenRequestsBatched() {
    return executeTenRequests(
        chainAddBatchedGraph,
        CHAIN_ADD_REQUEST,
        chainAddDisabledChains(chainAddBatchedGraph.vajramGraph()));
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
    return graphBuilderFor(vajramId, disabledChains, vajrams).build();
  }

  @SafeVarargs
  private static KrystexGraph batchedGraphFor(
      VajramID vajramId,
      Function<VajramGraph, ImmutableSet<DependentChain>> disabledChains,
      Class<? extends VajramDefRoot>... vajrams) {
    return graphBuilderFor(vajramId, disabledChains, vajrams)
        .inputBatcherStrategy(new DefaultBatcherStrategy(_v -> 100))
        .build();
  }

  @SafeVarargs
  private static KrystexGraphBuilder graphBuilderFor(
      VajramID vajramId,
      Function<VajramGraph, ImmutableSet<DependentChain>> disabledChains,
      Class<? extends VajramDefRoot>... vajrams) {
    VajramGraph graph = VajramGraph.builder().loadClasses(vajrams).build();
    KrystexGraphBuilder krystexGraphBuilder =
        KrystexGraph.builder()
            .vajramGraph(graph)
            .externallyInvocableVajramIds(ImmutableSet.of(vajramId))
            .dependentChainDisabler(new SimpleDependentChainDisabler(disabledChains.apply(graph)));
    return krystexGraphBuilder;
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

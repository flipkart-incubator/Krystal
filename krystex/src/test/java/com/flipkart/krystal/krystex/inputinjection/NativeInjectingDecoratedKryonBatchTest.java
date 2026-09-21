package com.flipkart.krystal.krystex.inputinjection;

import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.FacetValuesBuilder;
import com.flipkart.krystal.krystex.KrystalExecutorConfig;
import com.flipkart.krystal.krystex.KrystexGraph;
import com.flipkart.krystal.krystex.KrystexGraph.KrystexGraphBuilder;
import com.flipkart.krystal.krystex.VajramGraph;
import com.flipkart.krystal.krystex.kryon.VajramExecutionConfig;
import com.flipkart.krystal.krystex.kryon.VajramKryonExecutor;
import com.flipkart.krystal.krystex.test_vajrams.nativebatch.Greeter_FacImmutPojo;
import com.flipkart.krystal.krystex.test_vajrams.nativebatch.MultiGreeter_ReqImmutPojo;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.flipkart.krystal.vajram.inputinjection.VajramInjectionProvider;
import jakarta.inject.Provider;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link NativeInjectingDecoratedKryon} merges injected facets into every invocation
 * of a {@code ForwardReceiveBatch} (the command krystex uses when a vajram is invoked multiple
 * times concurrently, e.g. via fanout), not just the single-request path.
 */
class NativeInjectingDecoratedKryonBatchTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(1);
  private static SingleThreadExecutorsPool EXEC_POOL;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL = new SingleThreadExecutorsPool("Test", 2);
  }

  private Lease<SingleThreadExecutor> executorLease;

  @BeforeEach
  void setUp() throws LeaseUnavailableException {
    executorLease = EXEC_POOL.lease();
  }

  @AfterEach
  void tearDown() {
    executorLease.close();
  }

  @Test
  void nativeInjection_populatesEveryInvocationInABatch() {
    VajramGraph graph =
        VajramGraph.builder()
            .loadFromPackage("com.flipkart.krystal.krystex.test_vajrams.nativebatch")
            .build();
    Function<VajramID, Supplier<FacetValuesBuilder>> injectedFacetsSupplierProvider =
        vajramId ->
            VajramID.vajramID("Greeter").equals(vajramId)
                ? () -> Greeter_FacImmutPojo._builder().greetingPrefix("Hi")
                : null;
    KrystexGraphBuilder kGraph = KrystexGraph.builder().vajramGraph(graph);
    kGraph.injectedFacetsSupplierProvider(injectedFacetsSupplierProvider);

    CompletableFuture<String> future;
    try (VajramKryonExecutor executor =
        kGraph
            .build()
            .createExecutor(
                KrystalExecutorConfig.builder()
                    .executorId("nativeBatchTest")
                    .executorService(executorLease.get()))) {
      future =
          executor.execute(
              MultiGreeter_ReqImmutPojo._builder().names(List.of("Alice", "Bob", "Carol"))._build(),
              VajramExecutionConfig.builder().executionId("req_1").build());
    }

    assertThat(future).succeedsWithin(TIMEOUT).isEqualTo("Hi Alice, Hi Bob, Hi Carol");
  }

  @Test
  void nativeSupplier_takesPrecedenceOverLegacyProvider_forSameVajram() {
    VajramGraph graph =
        VajramGraph.builder()
            .loadFromPackage("com.flipkart.krystal.krystex.test_vajrams.nativebatch")
            .build();
    Function<VajramID, Supplier<FacetValuesBuilder>> injectedFacetsSupplierProvider =
        vajramId ->
            VajramID.vajramID("Greeter").equals(vajramId)
                ? () -> Greeter_FacImmutPojo._builder().greetingPrefix("Hi")
                : null;
    // A legacy provider which fails the test if krystex ever calls it for a vajram that already
    // has a native supplier registered. VajramInjectionProvider#get is a generic method, so this
    // must be an anonymous class rather than a lambda.
    VajramInjectionProvider legacyProvider =
        new VajramInjectionProvider() {
          @Override
          public <T> Provider<T> get(VajramID vajramId, FacetSpec<T, ?> facetDef) {
            throw new AssertionError(
                "Legacy VajramInjectionProvider must not be used for vajramId="
                    + vajramId
                    + " since a native injectedFacetsSupplierProvider is registered for it");
          }
        };
    KrystexGraphBuilder kGraph = KrystexGraph.builder().vajramGraph(graph);
    kGraph.injectedFacetsSupplierProvider(injectedFacetsSupplierProvider);
    kGraph.injectionProvider(legacyProvider);

    CompletableFuture<String> future;
    try (VajramKryonExecutor executor =
        kGraph
            .build()
            .createExecutor(
                KrystalExecutorConfig.builder()
                    .executorId("nativeVsLegacyTest")
                    .executorService(executorLease.get()))) {
      future =
          executor.execute(
              MultiGreeter_ReqImmutPojo._builder().names(List.of("Alice"))._build(),
              VajramExecutionConfig.builder().executionId("req_1").build());
    }

    assertThat(future).succeedsWithin(TIMEOUT).isEqualTo("Hi Alice");
  }
}

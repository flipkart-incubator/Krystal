package com.flipkart.krystal.vajram.samples_errors;

import static com.flipkart.krystal.vajram.samples.Util.TEST_TIMEOUT;
import static com.google.inject.Guice.createInjector;
import static com.google.inject.name.Names.named;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajram.exception.MandatoryFacetsMissingException;
import com.flipkart.krystal.vajram.guice.inputinjection.VajramGuiceInputInjector;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph.VajramKryonGraphBuilder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TooManyQualifiersTest {
  private static SingleThreadExecutorsPool EXEC_POOL;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL = new SingleThreadExecutorsPool("Test", Runtime.getRuntime().availableProcessors());
  }

  private VajramKryonGraphBuilder graph;
  private Lease<SingleThreadExecutor> executorLease;

  @BeforeEach
  void setUp() throws LeaseUnavailableException {
    this.executorLease = EXEC_POOL.lease();
    graph = VajramKryonGraph.builder().loadFromPackage(TooManyQualifiers.class.getPackageName());
  }

  @AfterEach
  void tearDown() {
    executorLease.close();
  }

  @Test
  void tooManyQualifiersInjectionFacet_throws() {
    CompletableFuture<@Nullable String> result;
    try (VajramKryonGraph vajramKryonGraph = graph.build();
        KrystexVajramExecutor executor = createExecutor(vajramKryonGraph)) {
      result = executor.execute(TooManyQualifiers_ImmutReqPojo._builder().input("i1")._build());
    }
    assertThat(result)
        .failsWithin(TEST_TIMEOUT)
        .withThrowableOfType(ExecutionException.class)
        .withCauseInstanceOf(MandatoryFacetsMissingException.class)
        .withMessageContaining(
            "Vajram v<TooManyQualifiers> did not receive these mandatory inputs:"
                + " [ 'inject' (Cause: Mandatory facet 'inject' of vajram 'TooManyQualifiers'"
                + " does not have a value) ]");
  }

  private KrystexVajramExecutor createExecutor(VajramKryonGraph vajramKryonGraph) {
    return vajramKryonGraph.createExecutor(
        KrystexVajramExecutorConfig.builder()
            .kryonExecutorConfigBuilder(
                KryonExecutorConfig.builder().singleThreadExecutor(executorLease.get()))
            .inputInjectionProvider(
                new VajramGuiceInputInjector(
                    createInjector(
                        binder -> {
                          binder
                              .bind(String.class)
                              .annotatedWith(named("toInject"))
                              .toInstance("i2a");
                          binder
                              .bind(String.class)
                              .annotatedWith(TooManyQualifiers.InjectionQualifier.class)
                              .toInstance("i2b");
                        })))
            .build());
  }
}

package com.flipkart.krystal.vajram.samples.sql.traits;

import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajram.guice.inputinjection.VajramGuiceInputInjector;
import com.flipkart.krystal.vajram.guice.traitbinding.StaticDispatchPolicyImpl;
import com.flipkart.krystal.vajram.guice.traitbinding.TraitBinder;
import com.flipkart.krystal.vajram.samples.sql.SqlGuiceModule;
import com.flipkart.krystal.vajram.sql.r2dbc.SQLWrite;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph.VajramKryonGraphBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class UpdateUserTraitTest {
  private static final Logger logger = Logger.getLogger(UpdateUserTrait.class.getName());
  private static final int MAX_THREADS = Runtime.getRuntime().availableProcessors();

  @SuppressWarnings("unchecked")
  private static final Lease<SingleThreadExecutor>[] EXECUTOR_LEASES = new Lease[MAX_THREADS];

  private static SingleThreadExecutorsPool EXEC_POOL;
  private static final Duration TEST_TIMEOUT = Duration.ofSeconds(10);

  private VajramKryonGraph graph;
  private Lease<SingleThreadExecutor> executorLease;
  private Injector injector;

  @BeforeAll
  static void beforeAll() throws LeaseUnavailableException {
    EXEC_POOL = new SingleThreadExecutorsPool("Test", MAX_THREADS);
    for (int i = 0; i < MAX_THREADS; i++) {
      EXECUTOR_LEASES[i] = EXEC_POOL.lease();
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
    this.injector = Guice.createInjector(new SqlGuiceModule());

    TraitBinder traitBinder = new TraitBinder();
    traitBinder.bindTrait(UpdateUserTrait_Req.class).to(UpdateUserTrait_sql_Req.class);
    // Load vajrams from the generated package and SQLWrite package
    VajramKryonGraphBuilder graphBuilder =
        VajramKryonGraph.builder()
            .loadFromPackage(UpdateUserTrait.class.getPackageName())
            .loadFromPackage(SQLWrite.class.getPackageName());
    this.graph = graphBuilder.build();
    graph.registerInputInjector(new VajramGuiceInputInjector(injector));
    graph.registerTraitDispatchPolicies(
        new StaticDispatchPolicyImpl(
            graph, graph.getVajramIdByVajramDefType(UpdateUserTrait.class), traitBinder));
  }

  @Test
  @Disabled("Requires MySQL server - enable manually when database is available")
  void updateUserSqlTrait_realDatabase_success() {
    // Configure connection to MySQL database
    logger.info("Initializing updateUserSqlTrait test with real database");
    // Create the VajramKryonGraph
    KrystexVajramExecutorConfig config =
        KrystexVajramExecutorConfig.builder()
            .kryonExecutorConfigBuilder(
                KryonExecutorConfig.builder()
                    .executorId("updateUserTest")
                    .executorService(executorLease.get()))
            .build();

    // Execute the vajram
    CompletableFuture<Long> future;
    try (KrystexVajramExecutor vajramExecutor = graph.createExecutor(config)) {
      future =
          vajramExecutor.execute(
              UpdateUserTrait_sql_ReqImmutPojo._builder()
                  .parameters(List.of("updated_user", "updated_user@gmail.com", 2))
                  ._build(),
              KryonExecutionConfig.builder().executionId("test-execution-update-db").build());
    }

    // Assert results
    assertThat(future).succeedsWithin(TEST_TIMEOUT);
    Long updatedRows = future.join();

    assertThat(updatedRows).isNotNull();
    // assertThat(updatedRows).isEqualTo(1);

  }
}

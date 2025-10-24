package com.flipkart.krystal.vajram.samples.sql;

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
import com.flipkart.krystal.vajram.samples.sql.traits.GetUserByIdTrait;
import com.flipkart.krystal.vajram.samples.sql.traits.GetUserByIdTrait_Req;
import com.flipkart.krystal.vajram.samples.sql.traits.GetUserByIdTrait_sql_Req;
import com.flipkart.krystal.vajram.sql.r2dbc.SQLRead;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph.VajramKryonGraphBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetUserByIdTest {

  private static final Logger logger = Logger.getLogger(GetUserByIdTest.class.getName());
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
    traitBinder.bindTrait(GetUserByIdTrait_Req.class).to(GetUserByIdTrait_sql_Req.class);
    // Load vajrams from the generated package and SQLRead package
    VajramKryonGraphBuilder graphBuilder =
        VajramKryonGraph.builder()
            .loadFromPackage(GetUserById.class.getPackageName())
            .loadFromPackage(SQLRead.class.getPackageName());
    this.graph = graphBuilder.build();
    graph.registerInputInjector(new VajramGuiceInputInjector(injector));
    graph.registerTraitDispatchPolicies(
        new StaticDispatchPolicyImpl(
            graph, graph.getVajramIdByVajramDefType(GetUserByIdTrait.class), traitBinder));
  }

  @Test
  // @Disabled("Requires MySQL server - enable manually when database is available")
  void getUserByIdSqlTrait_realDatabase_success() {
    // Configure connection to MySQL database
    logger.info("Initializing GetUserSqlTrait test with real database");
    // Create the VajramKryonGraph
    KrystexVajramExecutorConfig config =
        KrystexVajramExecutorConfig.builder()
            .kryonExecutorConfigBuilder(
                KryonExecutorConfig.builder()
                    .executorId("getUserByIdTest")
                    .executorService(executorLease.get()))
            .build();

    // Execute the vajram
    CompletableFuture<User> future;
    try (KrystexVajramExecutor vajramExecutor = graph.createExecutor(config)) {
      future =
          vajramExecutor.execute(
              GetUserById_ReqImmutPojo._builder().userId(2122)._build(),
              KryonExecutionConfig.builder().executionId("test-real-db").build());
    }

    // Assert results
    assertThat(future).succeedsWithin(TEST_TIMEOUT);
    User user = future.join();

    System.out.println(user);
    // Verify we got some results
    //    assertThat(user).isNotNull();
    //    assertThat(user.getId()).isEqualTo(2);
    //    assertThat(user.getName()).isNotBlank();
    //    assertThat(user.getEmailId()).isNotBlank();
  }
}

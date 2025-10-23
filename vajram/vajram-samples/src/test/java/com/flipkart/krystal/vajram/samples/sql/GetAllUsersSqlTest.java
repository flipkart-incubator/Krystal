package com.flipkart.krystal.vajram.samples.sql;

import static com.google.inject.Guice.createInjector;
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
import com.flipkart.krystal.vajram.sql.r2dbc.SQLRead;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph.VajramKryonGraphBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.asyncer.r2dbc.mysql.MySqlConnectionConfiguration;
import io.asyncer.r2dbc.mysql.MySqlConnectionFactory;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactory;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Test for SQL Trait code generation.
 *
 * <p>This test demonstrates:
 *
 * <ul>
 *   <li>Using SQL traits with the code generator
 *   <li>Mocking database connections for unit tests
 *   <li>Executing generated vajrams with Krystex
 * </ul>
 */
class GetAllUsersSqlTest {

  private static final Logger logger = Logger.getLogger(GetAllUsersSqlTest.class.getName());
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
    this.injector = Guice.createInjector(new GuiceModule());

    TraitBinder traitBinder = new TraitBinder();
    traitBinder.bindTrait(GetAllUsersTrait_Req.class)
        .to(GetAllUsersTrait_sql_Req.class);
    // Load vajrams from the generated package and SQLRead package
    VajramKryonGraphBuilder graphBuilder = VajramKryonGraph.builder()
        .loadFromPackage(GetAllUsersTrait.class.getPackageName())
        .loadFromPackage(SQLRead.class.getPackageName());
    this.graph = graphBuilder.build();
    graph.registerInputInjector(new VajramGuiceInputInjector(injector));
    graph.registerTraitDispatchPolicies(
        new StaticDispatchPolicyImpl(
            graph, graph.getVajramIdByVajramDefType(GetAllUsersTrait.class), traitBinder));

  }

  @Test
  //  @Disabled("Requires MySQL server - enable manually when database is available")
  void getAllUsersSqlTrait_realDatabase_success() {
    // Configure connection to MySQL database
    logger.info("Initializing GetUserSqlTrait test with real database");
    // Create the VajramKryonGraph
    KrystexVajramExecutorConfig config =
        KrystexVajramExecutorConfig.builder()
            .kryonExecutorConfigBuilder(
                KryonExecutorConfig.builder()
                    .executorId("getUsersTest")
                    .executorService(executorLease.get()))
            .build();

    // Execute the vajram
    CompletableFuture<List<User>> future;
    try (KrystexVajramExecutor vajramExecutor = graph.createExecutor(config)) {
      future =
          vajramExecutor.execute(
              GetAllUsersTrait_sql_ReqImmutPojo._builder()
                  .parameters(Collections.emptyList())
                  ._build(),
              KryonExecutionConfig.builder().executionId("test-execution-real-db").build());
    }

    // Assert results
    assertThat(future).succeedsWithin(TEST_TIMEOUT);
    List<User> results = future.join();

    // Verify we got some results
    assertThat(results).isNotEmpty();


    logger.info("Fetched {} users from database:"+ results.size());
    results.forEach(user -> logger.info("User: {}"+ user));

    // Verify structure
    results.forEach(
        user -> {
          assertThat(user.getId()).isNotNull();
          assertThat(user.getName()).isNotNull();
          assertThat(user.getEmailId()).isNotNull();
        });
  }

  private static class GuiceModule extends AbstractModule {
    @Provides
    @Singleton
    //@Named("connectionFactory")
    public ConnectionPool provideConnectionPool() {
      MySqlConnectionConfiguration configuration =
          MySqlConnectionConfiguration.builder()
              .host("localhost")
              .port(3306)
              .username("root")
              .database("users_p")
              .build();

      ConnectionFactory connectionFactory = MySqlConnectionFactory.from(configuration);
      return new ConnectionPool(
          ConnectionPoolConfiguration.builder(connectionFactory)
              .maxIdleTime(Duration.ofMinutes(30))
              .initialSize(5)
              .maxSize(20)
              .build());
    }
  }
}

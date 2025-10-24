package com.flipkart.krystal.vajram.sql;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.krystex.caching.TestRequestLevelCache;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajram.guice.inputinjection.VajramGuiceInputInjector;
import com.flipkart.krystal.vajram.sql.r2dbc.SQLWrite;
import com.flipkart.krystal.vajram.sql.r2dbc.SQLWrite_ReqImmutPojo;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph.VajramKryonGraphBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SQLWriteTest {
  private static final Logger logger = LoggerFactory.getLogger(SQLWriteTest.class);
  public static final int MAX_THREADS = Runtime.getRuntime().availableProcessors();

  @SuppressWarnings("unchecked")
  private static final Lease<SingleThreadExecutor>[] EXECUTOR_LEASES = new Lease[MAX_THREADS];

  private static SingleThreadExecutorsPool EXEC_POOL;
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
    stream(EXECUTOR_LEASES).forEach(Lease::close);
  }

  private VajramKryonGraphBuilder graph;
  private static final String REQUEST_ID = "mySQLWriteTest";
  private final TestRequestLevelCache requestLevelCache = spy(new TestRequestLevelCache());
  private static final Duration TEST_TIMEOUT = Duration.ofSeconds(10);

  private Lease<SingleThreadExecutor> executorLease;

  @BeforeEach
  void setUp() {
    this.executorLease = EXECUTOR_LEASES[0];
    VajramKryonGraphBuilder builder = VajramKryonGraph.builder();
    this.graph = builder.loadFromPackage(SQLWrite.class.getPackageName());
    this.injector = Guice.createInjector(new Utils.GuiceModule());
  }

  /**
   * Real database integration test - INSERT operation.
   *
   * <p>Prerequisites: 1. MySQL server running on localhost:3306 2. Database 'testdb' created 3.
   * Table 'users' with columns: id (INT AUTO_INCREMENT), name (VARCHAR), email (VARCHAR)
   *
   * <p>Setup commands: CREATE DATABASE IF NOT EXISTS testdb; USE testdb; CREATE TABLE IF NOT EXISTS
   * users ( id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100), email VARCHAR(100) );
   *
   * <p>To run: Remove @Disabled annotation and configure connection details
   */
  @Test
  @Disabled("Requires MySQL server - enable manually when database is available")
  void writeToMySQL_insert_success() {
    logger.info("Testing INSERT operation");

    // Create the VajramKryonGraph
    VajramKryonGraph graph = this.graph.build();
    graph.registerInputInjector(new VajramGuiceInputInjector(injector));
    KrystexVajramExecutorConfig config =
        KrystexVajramExecutorConfig.builder()
            .kryonExecutorConfigBuilder(
                KryonExecutorConfig.builder()
                    .executorId("writeToMySQLTest")
                    .executorService(executorLease.get()))
            .build();

    // INSERT query with parameters
    String insertQuery = "INSERT INTO user_profile (name, email) VALUES (?, ?)";
    List<Object> parameters = Arrays.asList("Test User", "test@example.com");

    // Execute the vajram
    CompletableFuture<SQLResult> future;
    try (KrystexVajramExecutor vajramExecutor = graph.createExecutor(config)) {
      future =
          vajramExecutor.execute(
              SQLWrite_ReqImmutPojo._builder().query(insertQuery).parameters(parameters)._build(),
              KryonExecutionConfig.builder().executionId("test-execution-insert").build());
    }

    // Assert results
    assertThat(future).succeedsWithin(TEST_TIMEOUT);
    SQLResult rowsAffected = future.join();

    // Verify one row was inserted
    assertThat(rowsAffected.rowsUpdated()).isEqualTo(1L);
    logger.info("Successfully inserted {} row(s)", rowsAffected);
  }

  @Test
  @Disabled("Requires MySQL server - enable manually when database is available")
  void writeToMySQL_insert_success1() {
    logger.info("Testing INSERT operation");

    // Create the VajramKryonGraph
    VajramKryonGraph graph = this.graph.build();
    graph.registerInputInjector(new VajramGuiceInputInjector(injector));
    KrystexVajramExecutorConfig config =
        KrystexVajramExecutorConfig.builder()
            .kryonExecutorConfigBuilder(
                KryonExecutorConfig.builder()
                    .executorId("writeToMySQLTest")
                    .executorService(executorLease.get()))
            .build();


    String query = "INSERT INTO user_profile (name, email_id) VALUES (?, ?), (?, ?),(?, ?)";
    List<Object> parameters =
        Arrays.asList(
            "Test User",
            "test@example.com",
            "Test User1",
            "test1@example.com",
            "Test User2",
            "test2@example.com");

    CompletableFuture<SQLResult> future;
    try (KrystexVajramExecutor vajramExecutor = graph.createExecutor(config)) {
      future =
          vajramExecutor.execute(
              SQLWrite_ReqImmutPojo._builder()
                  .query(query)
                  .parameters(parameters)
                  // .generatedColumns(List.of("id"))
                  ._build(),
              KryonExecutionConfig.builder().executionId("test-execution-insert").build());
    }

    // Assert results
    assertThat(future).succeedsWithin(TEST_TIMEOUT);

  }
}

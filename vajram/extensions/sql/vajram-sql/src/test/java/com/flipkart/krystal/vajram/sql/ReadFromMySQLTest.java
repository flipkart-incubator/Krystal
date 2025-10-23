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
import com.flipkart.krystal.vajram.sql.r2dbc.SQLRead_ReqImmutPojo;
import com.flipkart.krystal.vajram.sql.r2dbc.SQLRead;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph.VajramKryonGraphBuilder;
import io.asyncer.r2dbc.mysql.MySqlConnectionConfiguration;
import io.asyncer.r2dbc.mysql.MySqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ReadFromMySQLTest {
  private static final Logger logger = LoggerFactory.getLogger(ReadFromMySQLTest.class);
  public static final int MAX_THREADS = Runtime.getRuntime().availableProcessors();

  @SuppressWarnings("unchecked")
  private static final Lease<SingleThreadExecutor>[] EXECUTOR_LEASES = new Lease[MAX_THREADS];

  private static SingleThreadExecutorsPool EXEC_POOL;

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
  private static final String REQUEST_ID = "mySQLTest";
  private final TestRequestLevelCache requestLevelCache = spy(new TestRequestLevelCache());
  private static final Duration TEST_TIMEOUT = Duration.ofSeconds(10);

  private Lease<SingleThreadExecutor> executorLease;

  @BeforeEach
  void setUp() {
    this.executorLease = EXECUTOR_LEASES[0];
    VajramKryonGraphBuilder builder = VajramKryonGraph.builder();
    this.graph = builder.loadFromPackage(SQLRead.class.getPackageName());
  }

  /**
   * Real database integration test.
   *
   * <p>Prerequisites: 1. MySQL server running on localhost:3306 2. Database 'testdb' created 3.
   * Table 'users' with columns: id (INT), name (VARCHAR), email (VARCHAR) 4. At least one row of
   * test data
   *
   * <p>Setup commands: CREATE DATABASE IF NOT EXISTS testdb; USE testdb; CREATE TABLE IF NOT EXISTS
   * users ( id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100), email VARCHAR(100) ); INSERT INTO
   * users (name, email) VALUES ('John Doe', 'john@example.com'); INSERT INTO users (name, email)
   * VALUES ('Jane Smith', 'jane@example.com');
   *
   * <p>To run: Remove @Disabled annotation and configure connection details
   */
  @Test
  @Disabled("Requires MySQL server - enable manually when database is available")
  void readFromMySQL_realDatabase_success() {
    // Configure connection to your MySQL database
    logger.info("Initializing the test");
    MySqlConnectionConfiguration configuration =
        MySqlConnectionConfiguration.builder()
            .host("localhost")
            .port(3306)
            .username("root") // Change to your username
            .database("cam")
            .build();

    ConnectionFactory connectionFactory = MySqlConnectionFactory.from(configuration);
    System.out.println("Fetched rows from database:");

    // Create the VajramKryonGraph
    VajramKryonGraph graph = this.graph.build();

    KrystexVajramExecutorConfig config =
        KrystexVajramExecutorConfig.builder()
            .kryonExecutorConfigBuilder(
                KryonExecutorConfig.builder()
                    .executorId("readFromMySQLTest")
                    .executorService(executorLease.get()))
            .build();

    // Query to fetch all users
    String selectQuery = "select * from namespaces limit 1";

    // Execute the vajram
    CompletableFuture<List<Map<String, Object>>> future;
    try (KrystexVajramExecutor vajramExecutor = graph.createExecutor(config)) {
      future =
          vajramExecutor.execute(
              SQLRead_ReqImmutPojo._builder()
                  .selectQuery(selectQuery)
                  //.connectionFactory(connectionFactory)
                  ._build(),
              KryonExecutionConfig.builder().executionId("test-execution-real-db").build());
    }
    // System.out.println("Fetched " + results.size() + " rows from database:");

    // Assert results
    // assertThat(future).succeedsWithin(TEST_TIMEOUT);
    List<Map<String, Object>> results = future.join();

    // Verify we got some results
    assertThat(results).isNotEmpty();
    assertThat(results.size()).isEqualTo(1);

    // Print results for verification
    System.out.println("Fetched " + results.size() + " rows from database:");
    results.forEach(System.out::println);

    // Verify structure - each row should have id, name, email
    results.forEach(
        row -> {
          assertThat(row).containsKeys("id", "name", "version");
        });
  }

  /** Real database integration test - single row query. */
  /*@Test
  @Disabled("Requires MySQL server - enable manually when database is available")
  void readFromMySQL_realDatabase_singleRow_success() {
    // Configure connection
    MySqlConnectionConfiguration configuration = MySqlConnectionConfiguration.builder()
        .host("localhost")
        .port(3306)
        .username("root")
        .password("password")
        .database("testdb")
        .build();

    ConnectionFactory connectionFactory = MySqlConnectionFactory.from(configuration);

    // Create the VajramKryonGraph
    VajramKryonGraph graph = this.graph.build();

    KrystexVajramExecutorConfig config = KrystexVajramExecutorConfig.builder()
        .kryonExecutorConfigBuilder(
            KryonExecutorConfig.builder()
                .executorId("readFromMySQLTest")
                .executorService(executorLease.get()))
        .build();

    // Query with WHERE clause
    String selectQuery = "SELECT id, name, email FROM users WHERE id = 1";

    CompletableFuture<List<Map<String, Object>>> future;
    try (KrystexVajramExecutor vajramExecutor = graph.createExecutor(config)) {
      future = vajramExecutor.execute(
          ReadFromMySQL_ReqImmutPojo._builder()
              .selectQuery(selectQuery)
              .connectionFactory(connectionFactory)
              ._build(),
          KryonExecutionConfig.builder().executionId("test-execution-single-row").build());
    }

    // Assert results
    assertThat(future).succeedsWithin(TEST_TIMEOUT);
    List<Map<String, Object>> results = future.join();

    assertThat(results).hasSize(1);
    assertThat(results.get(0)).containsKeys("id", "name", "email");
    assertThat(results.get(0).get("id")).isEqualTo(1);
  }*/

}

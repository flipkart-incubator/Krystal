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
import com.flipkart.krystal.vajram.sql.r2dbc.SQLReadV1_ReqImmutPojo;
import com.flipkart.krystal.vajram.sql.r2dbc.SQLWrite;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph.VajramKryonGraphBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.time.Duration;
import java.util.Collections;
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

class SQLReadV1Test {
  private static final Logger logger = LoggerFactory.getLogger(SQLReadV1Test.class);
  public static final int MAX_THREADS = Runtime.getRuntime().availableProcessors();

  // Sample domain class for mapping
  public static class UserProfile {
    private Integer id;
    private String name;
    private String emailId;

    public UserProfile() {}

    public Integer getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public String getEmailId() {
      return emailId;
    }

    @Override
    public String toString() {
      return "UserProfile{id=" + id + ", name='" + name + "', emailId='" + emailId + "'}";
    }
  }

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
  void readfromMySQL_withMapResult() {
    logger.info("Testing SELECT operation with Map result");

    // Create the VajramKryonGraph
    VajramKryonGraph graph = this.graph.build();
    graph.registerInputInjector(new VajramGuiceInputInjector(injector));
    KrystexVajramExecutorConfig config =
        KrystexVajramExecutorConfig.builder()
            .kryonExecutorConfigBuilder(
                KryonExecutorConfig.builder()
                    .executorId("readFromMySQLTest")
                    .executorService(executorLease.get()))
            .build();

    // SELECT query with parameters
    String query = "select * from user_profile";
    List<Object> parameters = Collections.emptyList();

    // Execute the vajram without resultType (returns List<Map<String, Object>>)
    CompletableFuture<List<?>> future;
    try (KrystexVajramExecutor vajramExecutor = graph.createExecutor(config)) {
      future =
          vajramExecutor.execute(
              SQLReadV1_ReqImmutPojo._builder()
                  .selectQuery(query)
                  .parameters(parameters)
                  // No resultType specified - defaults to Map
                  ._build(),
              KryonExecutionConfig.builder().executionId("test-execution-map-read").build());
    }

    // Assert results
    System.out.println("Waiting for result...");
    List<?> result = future.join();
    System.out.println("Received result.");
    System.out.println("Queried rows: " + result.size());

    // Cast to Map type and print first row
    if (!result.isEmpty()) {
      @SuppressWarnings("unchecked")
      Map<String, Object> firstRow = (Map<String, Object>) result.get(0);
      System.out.println("First row: " + firstRow);
    }
  }

  @Test
    // @Disabled("Requires MySQL server - enable manually when database is available")
  void readfromMySQL_withCustomType() {
    logger.info("Testing SELECT operation with custom type mapping");

    // Create the VajramKryonGraph
    VajramKryonGraph graph = this.graph.build();
    graph.registerInputInjector(new VajramGuiceInputInjector(injector));
    KrystexVajramExecutorConfig config =
        KrystexVajramExecutorConfig.builder()
            .kryonExecutorConfigBuilder(
                KryonExecutorConfig.builder()
                    .executorId("readFromMySQLTest")
                    .executorService(executorLease.get()))
            .build();

    // SELECT query
    String query = "select * from user_profile";
    List<Object> parameters = Collections.emptyList();

    // Execute the vajram with resultType (returns List<UserProfile>)
    CompletableFuture<SQLResult> future;
    try (KrystexVajramExecutor vajramExecutor = graph.createExecutor(config)) {
      future =
          vajramExecutor.execute(
              SQLReadV1_ReqImmutPojo._builder()
                  .selectQuery(query)
                  .parameters(parameters)
                  .resultType(UserProfile.class)  // Specify custom type
                  ._build(),
              KryonExecutionConfig.builder().executionId("test-execution-typed-read").build());
    }

    // Assert results
    System.out.println("Waiting for result...");
    SQLResult result = future.join();
    System.out.println("Received result.");
    System.out.println("Queried rows: " + result.rows().size());

    // Cast to UserProfile type and print first row
    if (!result.rows().isEmpty()) {
      List<UserProfile> firstUser = (List<UserProfile>) result.rows();
      System.out.println("First user: " + firstUser.get(0));
    }
  }

}

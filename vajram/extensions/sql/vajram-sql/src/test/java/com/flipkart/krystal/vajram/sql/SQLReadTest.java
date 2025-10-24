package com.flipkart.krystal.vajram.sql;

import static java.util.Arrays.stream;
import static org.mockito.Mockito.spy;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.krystex.caching.TestRequestLevelCache;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajram.guice.inputinjection.VajramGuiceInputInjector;
import com.flipkart.krystal.vajram.sql.Utils.UserProfile;
import com.flipkart.krystal.vajram.sql.Utils.UserRecord;
import com.flipkart.krystal.vajram.sql.r2dbc.SQLRead_ReqImmutPojo;
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

class SQLReadTest {
  private static final Logger logger = LoggerFactory.getLogger(SQLReadTest.class);
  public static final int MAX_THREADS = Runtime.getRuntime().availableProcessors();

  @SuppressWarnings("unchecked")
  private static final Lease<SingleThreadExecutor>[] EXECUTOR_LEASES = new Lease[MAX_THREADS];

  private static SingleThreadExecutorsPool EXEC_POOL;
  private Injector injector;
  private VajramKryonGraph graph;
  private Lease<SingleThreadExecutor> executorLease;

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


  @BeforeEach
  void setUp() {
    this.executorLease = EXECUTOR_LEASES[0];
    VajramKryonGraphBuilder builder = VajramKryonGraph.builder();
    builder.loadFromPackage(SQLWrite.class.getPackageName());
    this.injector = Guice.createInjector(new Utils.GuiceModule());
    // Create the VajramKryonGraph
    this.graph = builder.build();
    graph.registerInputInjector(new VajramGuiceInputInjector(injector));

  }

  @Test
  @Disabled("Requires MySQL server - enable manually when database is available")
  void readfromMySQL_withMapResult() {
    logger.info("Testing SELECT operation with Map result");
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
    CompletableFuture<SQLResult> future;
    try (KrystexVajramExecutor vajramExecutor = graph.createExecutor(config)) {
      future =
          vajramExecutor.execute(
              SQLRead_ReqImmutPojo._builder()
                  .selectQuery(query)
                  .parameters(parameters)
                  // No resultType specified - defaults to Map
                  ._build(),
              KryonExecutionConfig.builder().executionId("test-execution-map-read").build());
    }

    // Assert results
    System.out.println("Waiting for result...");
    SQLResult result = future.join();
    System.out.println("Received result.");
    System.out.println("Queried rows: " + result.rows().size());

    // Cast to Map type and print first row
    if (!result.rows().isEmpty()) {
      @SuppressWarnings("unchecked")
      Map<String, Object> firstRow = (Map<String, Object>) result.rows().get(0);
      System.out.println("First row: " + firstRow);
    }
  }

  @Test
  @Disabled("Requires MySQL server - enable manually when database is available")
  void readfromMySQL_withCustomType() {
    logger.info("Testing SELECT operation with custom type mapping");

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
              SQLRead_ReqImmutPojo._builder()
                  .selectQuery(query)
                  .parameters(parameters)
                  .resultType(UserProfile.class) // Specify custom type
                  ._build(),
              KryonExecutionConfig.builder().executionId("test-execution-typed-read").build());
    }

    // Assert results
    SQLResult result = future.join();


    // Cast to UserProfile type and print first row
    if (!result.rows().isEmpty()) {
      List<UserProfile> firstUser = (List<UserProfile>) result.rows();
      System.out.println("First user: " + firstUser.get(0));
    }
  }

  @Test
  @Disabled("Requires MySQL server - enable manually when database is available")
  void readfromMySQL_withRecordType() {

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
              SQLRead_ReqImmutPojo._builder()
                  .selectQuery(query)
                  .parameters(parameters)
                  .resultType(UserRecord.class) // Specify custom type
                  ._build(),
              KryonExecutionConfig.builder().executionId("test-execution-typed-read").build());
    }

    // Assert results
    SQLResult result = future.join();


    // Cast to UserProfile type and print first row
    if (!result.rows().isEmpty()) {
      List<UserRecord> firstUser = (List<UserRecord>) result.rows();
      System.out.println("First user: " + firstUser.get(0));
    }
  }
}

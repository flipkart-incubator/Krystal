package com.flipkart.krystal.vajram.samples.user;

import static com.flipkart.krystal.vajram.samples.Util.TEST_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajram.samples.user.response_pojos.UserWithProfile;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexGraph;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexGraph.KrystexGraphBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramGraph;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetUserProfilesFromUserIdsTest {

  private static SingleThreadExecutorsPool EXEC_POOL;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL = new SingleThreadExecutorsPool("GetUserProfilesFromUserIds", 4);
  }

  private VajramGraph graph;
  private KrystexGraphBuilder kGraph;
  private Lease<SingleThreadExecutor> executorLease;

  @BeforeEach
  void setUp() throws LeaseUnavailableException {
    this.executorLease = EXEC_POOL.lease();
    this.graph =
        VajramGraph.builder()
            .loadClasses(
                GetUserProfilesFromUserIds.class,
                GetUserWithProfile.class,
                GetUser.class,
                GetUserProfile.class)
            .build();
    this.kGraph = KrystexGraph.builder().vajramGraph(graph);
  }

  @AfterEach
  void tearDown() {
    executorLease.close();
  }

  @AfterAll
  static void afterAll() {
    EXEC_POOL.close();
  }

  @Test
  void testErrorContamination_CorrectAndIncorrectInputs() throws Exception {
    CompletableFuture<List<UserWithProfile>> future;
    try (KrystexVajramExecutor executor =
        kGraph
            .build()
            .createExecutor(
                KrystexVajramExecutorConfig.builder()
                    .kryonExecutorConfig(
                        KryonExecutorConfig.builder()
                            .executorId("error-contaminated-test")
                            .executorService(executorLease.get())
                            .build()))) {

      List<String> userIds = Arrays.asList("Incorrect_User_Id", "Correct_User_Id");

      future =
          executor.execute(
              GetUserProfilesFromUserIds_ReqImmutPojo._builder().userIds(userIds)._build(),
              KryonExecutionConfig.builder().build());
    }

    assertThat(future).succeedsWithin(TEST_TIMEOUT);
    List<UserWithProfile> results = future.get();
    assertThat(results.size()).isEqualTo(1);
  }

  @Test
  void testAllCorrectInputs_success() throws Exception {
    CompletableFuture<List<UserWithProfile>> future;
    try (KrystexVajramExecutor executor =
        kGraph
            .build()
            .createExecutor(
                KrystexVajramExecutorConfig.builder()
                    .kryonExecutorConfig(
                        KryonExecutorConfig.builder()
                            .executorId("all-correct-inputs-test")
                            .executorService(executorLease.get())
                            .build()))) {

      List<String> userIds =
          Arrays.asList("Correct_User_Id_1", "Correct_User_Id_2", "Correct_User_Id_3");

      future =
          executor.execute(
              GetUserProfilesFromUserIds_ReqImmutPojo._builder().userIds(userIds)._build(),
              KryonExecutionConfig.builder().build());
    }

    assertThat(future).succeedsWithin(TEST_TIMEOUT);
    List<UserWithProfile> results = future.get();
    assertThat(results).as("All 3 requests should succeed").hasSize(3);
  }

  @Test
  void testAllIncorrectInputs_success() throws Exception {
    CompletableFuture<List<UserWithProfile>> future;
    try (KrystexVajramExecutor executor =
        kGraph
            .build()
            .createExecutor(
                KrystexVajramExecutorConfig.builder()
                    .kryonExecutorConfig(
                        KryonExecutorConfig.builder()
                            .executorId("all-fail-test")
                            .executorService(executorLease.get())
                            .build()))) {

      List<String> userIds = Arrays.asList("Incorrect_User_Id", "Incorrect_User_Id");

      future =
          executor.execute(
              GetUserProfilesFromUserIds_ReqImmutPojo._builder().userIds(userIds)._build(),
              KryonExecutionConfig.builder().build());
    }

    assertThat(future).succeedsWithin(TEST_TIMEOUT);
    List<UserWithProfile> results = future.get();
    assertThat(results).as("No requests should succeed").isEmpty();
  }
}

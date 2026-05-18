package com.flipkart.krystal.vajram.samples.user;

import static com.flipkart.krystal.vajram.samples.Util.TEST_TIMEOUT;
import static com.google.inject.Guice.createInjector;
import static com.google.inject.name.Names.named;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.krystex.caching.RequestLevelCache;
import com.flipkart.krystal.krystex.caching.RequestLevelCacheInvalidator;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorExecutionInfo;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajram.guice.injection.VajramGuiceInputInjector;
import com.flipkart.krystal.vajram.samples.user.response_pojos.UserWithProfile;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexGraph;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexGraph.KrystexGraphBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramGraph;
import com.google.inject.AbstractModule;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UsersTest {

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
    GetUserProfile.CALL_COUNTER.reset();
    UpdateUserProfile.CALL_COUNTER.reset();
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
  void testAllCorrectInputs_success() {
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

    assertThat(future)
        .as("All 3 requests should succeed")
        .succeedsWithin(TEST_TIMEOUT)
        .asInstanceOf(list(UserWithProfile.class))
        .hasSize(3);
  }

  @Test
  void testAllIncorrectInputs_success() {
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

    assertThat(future)
        .as("No requests should succeed")
        .succeedsWithin(TEST_TIMEOUT)
        .matches(List::isEmpty);
  }

  @Test
  void mutatingVajram_invalidatesCacheOfReadVajram() {
    RequestLevelCache requestLevelCache = new RequestLevelCache(graph.kryonDefinitionRegistry());
    KryonExecutorExecutionInfo executionInfo = new KryonExecutorExecutionInfo();
    RequestLevelCacheInvalidator cacheInvalidator =
        new RequestLevelCacheInvalidator(
            requestLevelCache,
            graph::getVajramIdByVajramReqType,
            vajramID -> graph.getVajramDefinition(vajramID).vajramTags(),
            executionInfo);
    VajramGuiceInputInjector guiceInputInjector =
        new VajramGuiceInputInjector(
            createInjector(
                new AbstractModule() {
                  @Override
                  protected void configure() {
                    bind(RequestLevelCacheInvalidator.class).toInstance(cacheInvalidator);
                  }
                }));
    CompletableFuture<Boolean> future1;
    CompletableFuture<Boolean> future2;
    kGraph.injectionProvider(guiceInputInjector);
    try (KrystexVajramExecutor executor =
        kGraph
            .build()
            .createExecutor(
                KrystexVajramExecutorConfig.builder()
                    .kryonExecutorConfig(
                        KryonExecutorConfig.builder()
                            .executorId("all-correct-inputs-test")
                            .executorService(executorLease.get())
                            .configureWith(requestLevelCache.asKryonExecutorConfigurator())
                            .executorInfo(executionInfo)
                            .build()))) {

      future1 =
          executor.execute(RunUserWorkflow_ReqImmutPojo._builder().userId("UserId_1")._build());
      future2 =
          executor.execute(RunUserWorkflow_ReqImmutPojo._builder().userId("UserId_2")._build());
    }

    assertThat(future1).succeedsWithin(TEST_TIMEOUT).isEqualTo(true);
    assertThat(future2).succeedsWithin(TEST_TIMEOUT).isEqualTo(true);

    // GetUserProfile should be called 4 times (twice for each user id)- i.e. since
    // UpdateUserProfile invalidates cache keys, the second time should not be preempted by a cache
    // hit
    assertThat(GetUserProfile.CALL_COUNTER.sum()).isEqualTo(4);
  }

  @Test
  void mutatingVajram_invalidationDisabling_leadsToCacheHit() {
    RequestLevelCache requestLevelCache = new RequestLevelCache(graph.kryonDefinitionRegistry());
    KryonExecutorExecutionInfo executionInfo = new KryonExecutorExecutionInfo();
    RequestLevelCacheInvalidator cacheInvalidator =
        new RequestLevelCacheInvalidator(
            requestLevelCache,
            graph::getVajramIdByVajramReqType,
            vajramID -> graph.getVajramDefinition(vajramID).vajramTags(),
            executionInfo);
    VajramGuiceInputInjector guiceInputInjector =
        new VajramGuiceInputInjector(
            createInjector(
                new AbstractModule() {
                  @Override
                  protected void configure() {
                    bind(RequestLevelCacheInvalidator.class).toInstance(cacheInvalidator);
                    bind(Boolean.class)
                        .annotatedWith(named("UpdateUserProfile.shouldUpdate"))
                        .toInstance(false);
                  }
                }));
    CompletableFuture<Boolean> future1;
    CompletableFuture<Boolean> future2;
    kGraph.injectionProvider(guiceInputInjector);
    try (KrystexVajramExecutor executor =
        kGraph
            .build()
            .createExecutor(
                KrystexVajramExecutorConfig.builder()
                    .kryonExecutorConfig(
                        KryonExecutorConfig.builder()
                            .executorId("all-correct-inputs-test")
                            .executorService(executorLease.get())
                            .configureWith(requestLevelCache.asKryonExecutorConfigurator())
                            .executorInfo(executionInfo)
                            .build()))) {

      future1 =
          executor.execute(RunUserWorkflow_ReqImmutPojo._builder().userId("UserId_1")._build());
      future2 =
          executor.execute(RunUserWorkflow_ReqImmutPojo._builder().userId("UserId_2")._build());
    }

    assertThat(future1).succeedsWithin(TEST_TIMEOUT).isEqualTo(false);
    assertThat(future2).succeedsWithin(TEST_TIMEOUT).isEqualTo(false);

    // GetUserProfile should be called 2 times (once for each user id)- i.e. since
    // UpdateUserProfile invalidation is disabled by the injected boolean, cache hit should prevent
    // the second call for each user id
    assertThat(GetUserProfile.CALL_COUNTER.sum()).isEqualTo(2);
  }

  @Test
  void mutatingVajrams_areNotCached() {
    RequestLevelCache requestLevelCache = new RequestLevelCache(graph.kryonDefinitionRegistry());
    KryonExecutorExecutionInfo executionInfo = new KryonExecutorExecutionInfo();
    RequestLevelCacheInvalidator cacheInvalidator =
        new RequestLevelCacheInvalidator(
            requestLevelCache,
            graph::getVajramIdByVajramReqType,
            vajramID -> graph.getVajramDefinition(vajramID).vajramTags(),
            executionInfo);
    VajramGuiceInputInjector guiceInputInjector =
        new VajramGuiceInputInjector(
            createInjector(
                new AbstractModule() {
                  @Override
                  protected void configure() {
                    bind(RequestLevelCacheInvalidator.class).toInstance(cacheInvalidator);
                    bind(Boolean.class)
                        .annotatedWith(named("UpdateUserProfile.shouldUpdate"))
                        .toInstance(false);
                  }
                }));
    CompletableFuture<Boolean> future1;
    CompletableFuture<Boolean> future2;
    kGraph.injectionProvider(guiceInputInjector);
    try (KrystexVajramExecutor executor =
        kGraph
            .build()
            .createExecutor(
                KrystexVajramExecutorConfig.builder()
                    .kryonExecutorConfig(
                        KryonExecutorConfig.builder()
                            .executorId("all-correct-inputs-test")
                            .executorService(executorLease.get())
                            .configureWith(requestLevelCache.asKryonExecutorConfigurator())
                            .executorInfo(executionInfo)
                            .build()))) {

      future1 =
          executor.execute(RunUserWorkflow_ReqImmutPojo._builder().userId("UserId_1")._build());
      future2 =
          executor.execute(RunUserWorkflow_ReqImmutPojo._builder().userId("UserId_1")._build());
    }

    assertThat(future1).succeedsWithin(TEST_TIMEOUT).isEqualTo(false);
    assertThat(future2).succeedsWithin(TEST_TIMEOUT).isEqualTo(false);

    // UpdateUserProfile must be called twice because it declares @MutatesState(TRUE)
    // This means RunUserWorkflow has also been called twice since it transitively derives
    // @MutatesState(TRUE) from UpdateUserProfile
    assertThat(UpdateUserProfile.CALL_COUNTER.sum()).isEqualTo(2);
    // GetUserProfile must be called only once due to cache hit because it is not a mutating vajram
    // and
    // cache invalidation has been disabled above
    assertThat(GetUserProfile.CALL_COUNTER.sum()).isEqualTo(1);
  }

  @Test
  void invalidatingCacheKeys_fromNonPermittedVajrams_fails() {
    RequestLevelCache requestLevelCache = new RequestLevelCache(graph.kryonDefinitionRegistry());
    KryonExecutorExecutionInfo executionInfo = new KryonExecutorExecutionInfo();
    RequestLevelCacheInvalidator cacheInvalidator =
        new RequestLevelCacheInvalidator(
            requestLevelCache,
            graph::getVajramIdByVajramReqType,
            vajramID -> graph.getVajramDefinition(vajramID).vajramTags(),
            executionInfo);
    VajramGuiceInputInjector guiceInputInjector =
        new VajramGuiceInputInjector(
            createInjector(
                new AbstractModule() {
                  @Override
                  protected void configure() {
                    bind(RequestLevelCacheInvalidator.class).toInstance(cacheInvalidator);
                    bind(Boolean.class)
                        .annotatedWith(named("GetUser.shouldInvalidate"))
                        .toInstance(true);
                  }
                }));
    kGraph.injectionProvider(guiceInputInjector);
    CompletableFuture<List<UserWithProfile>> future1;
    try (KrystexVajramExecutor executor =
        kGraph
            .build()
            .createExecutor(
                KrystexVajramExecutorConfig.builder()
                    .kryonExecutorConfig(
                        KryonExecutorConfig.builder()
                            .executorId("all-correct-inputs-test")
                            .executorService(executorLease.get())
                            .configureWith(requestLevelCache.asKryonExecutorConfigurator())
                            .executorInfo(executionInfo)
                            .build()))) {

      future1 =
          executor.execute(
              GetUserProfilesFromUserIds_ReqImmutPojo._builder()
                  .userIds(List.of("UserId_1"))
                  ._build());
    }

    assertThat(future1)
        .failsWithin(TEST_TIMEOUT)
        .withThrowableOfType(ExecutionException.class)
        .withCauseInstanceOf(IllegalStateException.class)
        .withMessageContaining(
            "@RequestLevelCacheConfig missing on Vajram v<GetUserProfilesFromUserIds> - this is mandatory for cache invalidation");
  }
}

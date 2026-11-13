package com.flipkart.krystal.vajram.samples.fields;

import static com.flipkart.krystal.vajram.samples.Util.TEST_TIMEOUT;
import static com.flipkart.krystal.vajram.samples.Util.loadFromClasspath;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.krystex.KrystalExecutorConfig;
import com.flipkart.krystal.krystex.KrystexGraph;
import com.flipkart.krystal.krystex.VajramGraph;
import com.flipkart.krystal.krystex.kryon.VajramKryonExecutor;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajram.samples.calculator.add.Add;
import com.google.common.collect.ImmutableSet;
import java.util.concurrent.CompletableFuture;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChainedFieldsTest {
  private static SingleThreadExecutorsPool EXEC_POOL;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL = new SingleThreadExecutorsPool("ChainedFieldTest", 4);
  }

  private final VajramGraph graph =
      loadFromClasspath(ChainedFieldsVajram.class.getPackageName(), Add.class.getPackageName())
          .build();

  private Lease<SingleThreadExecutor> executorLease;

  @BeforeEach
  void setUp() throws LeaseUnavailableException {
    this.executorLease = EXEC_POOL.lease();
  }

  @AfterEach
  void tearDown() {
    executorLease.close();
  }

  @Test
  void chainedFields_withMiddleName_success() {
    CompletableFuture<String> future;
    try (VajramKryonExecutor executor =
        KrystexGraph.builder()
            .vajramGraph(graph)
            .externallyInvocableVajramIds(ImmutableSet.of(ChainedFieldsVajram_Req._VAJRAM_ID))
            .build()
            .createExecutor(
                KrystalExecutorConfig.builder()
                    .executorId("chainedFieldTest")
                    .executorService(executorLease.get()))) {
      future =
          executor.execute(
              ChainedFieldsVajram_ReqImmutPojo._builder()
                  .firstName("John")
                  .middleName("Michael")
                  .lastName("Doe")
                  ._build());
    }
    Assertions.assertThat(future)
        .succeedsWithin(TEST_TIMEOUT)
        .isEqualTo("Hello, John Michael Doe. Welcome!");
  }

  @Test
  void chainedFields_withoutMiddleName_success() {
    CompletableFuture<String> future;
    try (VajramKryonExecutor executor =
        KrystexGraph.builder()
            .vajramGraph(graph)
            .externallyInvocableVajramIds(ImmutableSet.of(ChainedFieldsVajram_Req._VAJRAM_ID))
            .build()
            .createExecutor(
                KrystalExecutorConfig.builder()
                    .executorId("chainedFieldTest2")
                    .executorService(executorLease.get()))) {
      future =
          executor.execute(
              ChainedFieldsVajram_ReqImmutPojo._builder()
                  .firstName("Jane")
                  .lastName("Smith")
                  ._build());
    }
    Assertions.assertThat(future)
        .succeedsWithin(TEST_TIMEOUT)
        .isEqualTo("Hello, Jane Smith. Welcome!");
  }
}

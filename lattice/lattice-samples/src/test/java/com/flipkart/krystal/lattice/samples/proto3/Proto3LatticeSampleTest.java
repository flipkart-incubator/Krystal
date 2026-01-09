package com.flipkart.krystal.lattice.samples.proto3.sampleProtoService;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.krystex.caching.TestRequestLevelCache;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.exception.MandatoryFacetsMissingException;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.testharness.VajramTestHarness;
import com.google.protobuf.ByteString;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Proto3LatticeSampleTest {

  private static SingleThreadExecutorsPool EXEC_POOL;
  private static final String REQUEST_ID = "protoExhaustiveTest";
  private final TestRequestLevelCache requestLevelCache = new TestRequestLevelCache();

  private VajramKryonGraph graph;
  private Lease<SingleThreadExecutor> executorLease;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL = new SingleThreadExecutorsPool("Test", 1);
  }

  @AfterAll
  static void afterAll() {
    EXEC_POOL.close();
  }

  @BeforeEach
  void setUp() throws LeaseUnavailableException {
    this.executorLease = EXEC_POOL.lease();
    this.graph = VajramKryonGraph.builder().loadClasses(Proto3LatticeSample.class).build();
  }

  @AfterEach
  void tearDown() {
    executorLease.close();
  }

  @Test
  void allInputsProvided_success() {
    // Create a request with all inputs provided
    Proto3LatticeSample_ReqImmut request =
        Proto3LatticeSample_ReqImmutProto._builder()
            .optionalInput(42)
            .mandatoryInput(100)
            .conditionallyMandatoryInput(200)
            .inputWithDefaultValue(300)
            .optionalLongInput(10L)
            .mandatoryLongInput(20L)
            .optionalByteString(ByteString.copyFromUtf8("test"))
            .defaultByteString(ByteString.copyFromUtf8("hello"))
            ._build();

    // Execute the vajram
    CompletableFuture<Proto3LatticeSampleResponse> result;
    try (KrystexVajramExecutor executor =
        graph.createExecutor(
            KrystexVajramExecutorConfig.builder()
                .vajramKryonGraph(graph)
                .requestId(REQUEST_ID)
                .kryonExecutorConfig(
                    KryonExecutorConfig.builder().singleThreadExecutor(executorLease.get()).build())
                .build())) {
      result =
          executor.execute(
              request, KryonExecutionConfig.builder().executionId("test_all_inputs").build());
    }

    // Verify the result
    assertThat(result)
        .succeedsWithin(1, SECONDS)
        .extracting(Proto3LatticeSampleResponse::string, STRING)
        .contains(
            "$$ optionalInput: Optional[42] $$",
            "$$ mandatoryInput: 100 $$",
            "$$ conditionallyMandatoryInput: Optional[200] $$",
            "$$ inputWithDefaultValue: 300 $$",
            "$$ optionalLongInput: Optional[10] $$",
            "$$ mandatoryLongInput: 20 $$",
            "$$ optionalByteString: Optional[test] $$",
            "$$ defaultByteString: hello $$");
  }

  @Test
  void optionalInputsOmitted_success() {
    // Create a request with only mandatory inputs
    Proto3LatticeSample_ReqImmut request =
        Proto3LatticeSample_ReqImmutProto._builder()
            .mandatoryInput(100)
            .conditionallyMandatoryInput(200)
            .inputWithDefaultValue(300)
            .mandatoryLongInput(20L)
            ._build();

    // Execute the vajram
    CompletableFuture<Proto3LatticeSampleResponse> result;
    try (KrystexVajramExecutor executor =
        graph.createExecutor(
            KrystexVajramExecutorConfig.builder()
                .vajramKryonGraph(graph)
                .requestId(REQUEST_ID)
                .kryonExecutorConfig(
                    KryonExecutorConfig.builder().singleThreadExecutor(executorLease.get()).build())
                .build())) {
      result =
          executor.execute(
              request, KryonExecutionConfig.builder().executionId("test_optional_omitted").build());
    }

    // Verify the result
    assertThat(result)
        .succeedsWithin(1, SECONDS)
        .extracting(Proto3LatticeSampleResponse::string, STRING)
        .contains(
            "$$ optionalInput: Optional.empty $$",
            "$$ mandatoryInput: 100 $$",
            "$$ inputWithDefaultValue: 300 $$",
            "$$ optionalLongInput: Optional.empty $$",
            "$$ mandatoryLongInput: 20 $$",
            "$$ optionalByteString: Optional.empty $$",
            "$$ defaultByteString:  $$");
  }

  @Test
  void defaultValueStrategy() {
    // Create a request without the input that has a default value strategy
    Proto3LatticeSample_ReqImmut request =
        Proto3LatticeSample_ReqImmutProto._builder()
            .mandatoryInput(100)
            .conditionallyMandatoryInput(200)
            .mandatoryLongInput(20L)
            ._build();

    // Execute the vajram
    CompletableFuture<Proto3LatticeSampleResponse> result;
    try (KrystexVajramExecutor executor =
        graph.createExecutor(
            KrystexVajramExecutorConfig.builder()
                .vajramKryonGraph(graph)
                .requestId(REQUEST_ID)
                .kryonExecutorConfig(
                    KryonExecutorConfig.builder().singleThreadExecutor(executorLease.get()).build())
                .build())) {
      result =
          executor.execute(
              request, KryonExecutionConfig.builder().executionId("test_default_value").build());
    }

    // Verify the result - inputWithDefaultValue should be 0 (default for int)
    assertThat(result)
        .succeedsWithin(1, SECONDS)
        .extracting(Proto3LatticeSampleResponse::string, STRING)
        .contains("inputWithDefaultValue: 0");
  }

  @Test
  void missingMandatoryInput_throws() {
    // Create a request missing a mandatory input
    Proto3LatticeSample_ReqImmut request =
        Proto3LatticeSample_ReqImmutProto._builder()
            // Missing mandatoryInput
            .conditionallyMandatoryInput(200)
            .inputWithDefaultValue(300)
            .mandatoryLongInput(20L)
            ._build();

    CompletableFuture<Proto3LatticeSampleResponse> result;
    // Execute the vajram and expect failure
    try (KrystexVajramExecutor executor =
        graph.createExecutor(
            KrystexVajramExecutorConfig.builder()
                .vajramKryonGraph(graph)
                .requestId(REQUEST_ID)
                .kryonExecutorConfig(
                    KryonExecutorConfig.builder().singleThreadExecutor(executorLease.get()).build())
                .build())) {
      result =
          executor.execute(
              request,
              KryonExecutionConfig.builder().executionId("test_missing_mandatory").build());
    }
    assertThat(result)
        .failsWithin(1, SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseInstanceOf(MandatoryFacetsMissingException.class)
        .withMessageContaining("mandatoryInput");
  }

  @Test
  void missingMandatoryByteInput_throws() {
    // Create a request missing a mandatory byte input
    Proto3LatticeSample_ReqImmut request =
        Proto3LatticeSample_ReqImmutProto._builder()
            .mandatoryInput(100)
            .conditionallyMandatoryInput(200)
            .inputWithDefaultValue(300)
            // Missing mandatoryByteInput
            ._build();

    CompletableFuture<Proto3LatticeSampleResponse> result;
    // Execute the vajram and expect failure
    try (KrystexVajramExecutor executor =
        graph.createExecutor(
            KrystexVajramExecutorConfig.builder()
                .vajramKryonGraph(graph)
                .requestId(REQUEST_ID)
                .kryonExecutorConfig(
                    KryonExecutorConfig.builder().singleThreadExecutor(executorLease.get()).build())
                .build())) {
      result =
          executor.execute(
              request,
              KryonExecutionConfig.builder().executionId("test_missing_mandatory_byte").build());
    }
    assertThat(result)
        .failsWithin(1, SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseInstanceOf(MandatoryFacetsMissingException.class)
        .withMessageContaining("mandatoryLongInput");
  }

  @Test
  void mockedResponse_success() {
    // Create a test harness with a mocked response
    Proto3LatticeSample_ReqImmut request =
        Proto3LatticeSample_ReqImmutProto._builder()
            .mandatoryInput(100)
            .conditionallyMandatoryInput(200)
            .mandatoryLongInput(20L)
            ._build();

    Proto3LatticeSampleResponse mockedOutput =
        Proto3LatticeSampleResponse_ImmutPojo._builder()
            .string("$$ This is a mocked response $$")
            .mandatoryInt(1)
            ._build();
    // Execute the vajram with a mocked response
    CompletableFuture<Proto3LatticeSampleResponse> result;
    try (KrystexVajramExecutor executor =
        graph.createExecutor(
            VajramTestHarness.prepareForTest(
                    KrystexVajramExecutorConfig.builder()
                        .vajramKryonGraph(graph)
                        .requestId(REQUEST_ID)
                        .kryonExecutorConfig(
                            KryonExecutorConfig.builder()
                                .singleThreadExecutor(executorLease.get())
                                .build())
                        .build(),
                    requestLevelCache)
                .withMock(
                    ((VajramDef<?>)
                            graph.getVajramDefinition(Proto3LatticeSample_Req._VAJRAM_ID).def())
                        .facetsFromRequest(request)
                        ._build(),
                    Errable.withValue(mockedOutput))
                .buildConfig())) {
      result =
          executor.execute(
              request, KryonExecutionConfig.builder().executionId("test_mocked_response").build());
    }

    // Verify the mocked result
    assertThat(result).succeedsWithin(1, SECONDS).isEqualTo(mockedOutput);
  }

  @Test
  void byteStringInput_success() {
    // Create a request with ByteString input
    ByteString byteString = ByteString.copyFromUtf8("Hello, World!");
    Proto3LatticeSample_ReqImmut request =
        Proto3LatticeSample_ReqImmutProto._builder()
            .mandatoryInput(100)
            .conditionallyMandatoryInput(200)
            .inputWithDefaultValue(300)
            .mandatoryLongInput(20L)
            .optionalByteString(byteString)
            ._build();

    // Execute the vajram
    CompletableFuture<Proto3LatticeSampleResponse> result;
    try (KrystexVajramExecutor executor =
        graph.createExecutor(
            KrystexVajramExecutorConfig.builder()
                .vajramKryonGraph(graph)
                .requestId(REQUEST_ID)
                .kryonExecutorConfig(
                    KryonExecutorConfig.builder().singleThreadExecutor(executorLease.get()).build())
                .build())) {
      result =
          executor.execute(
              request, KryonExecutionConfig.builder().executionId("test_byte_string").build());
    }

    // Verify the result
    assertThat(result)
        .succeedsWithin(100, SECONDS)
        .extracting(Proto3LatticeSampleResponse::string, STRING)
        .contains("$$ optionalByteString: Optional[Hello, World!] $$");
  }
}

package com.flipkart.krystal.lattice.samples.proto2024e;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.krystex.caching.TestRequestLevelCache;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.lattice.samples.grpc.proto2024e.sampleProtoService.Proto2024eLatticeSample;
import com.flipkart.krystal.lattice.samples.grpc.proto2024e.sampleProtoService.Proto2024eLatticeSampleResponse;
import com.flipkart.krystal.lattice.samples.grpc.proto2024e.sampleProtoService.Proto2024eLatticeSampleResponse_Immut;
import com.flipkart.krystal.lattice.samples.grpc.proto2024e.sampleProtoService.Proto2024eLatticeSampleResponse_ImmutPojo;
import com.flipkart.krystal.lattice.samples.grpc.proto2024e.sampleProtoService.Proto2024eLatticeSampleResponse_ImmutProto;
import com.flipkart.krystal.lattice.samples.grpc.proto2024e.sampleProtoService.Proto2024eLatticeSample_Req;
import com.flipkart.krystal.lattice.samples.grpc.proto2024e.sampleProtoService.Proto2024eLatticeSample_ReqImmut;
import com.flipkart.krystal.lattice.samples.grpc.proto2024e.sampleProtoService.Proto2024eLatticeSample_ReqImmutProto;
import com.flipkart.krystal.lattice.samples.grpc.proto2024e.sampleProtoService.ProtoMessageProto;
import com.flipkart.krystal.lattice.samples.grpc.proto2024e.sampleProtoService.ProtoMessage_ImmutProto;
import com.flipkart.krystal.lattice.samples.grpc.proto2024e.sampleProtoService.Status;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.exception.MandatoryFacetsMissingException;
import com.flipkart.krystal.vajram.guice.injection.VajramGuiceInputInjector;
import com.flipkart.krystal.vajram.protobuf.util.ProtoByteArray;
import com.flipkart.krystal.vajram.protobuf.util.SerializableProtoModel;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexGraph;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexGraph.KrystexGraphBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramGraph;
import com.flipkart.krystal.vajramexecutor.krystex.testharness.VajramTestHarness;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.protobuf.ByteString;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Proto2024eLatticeSampleTest {

  private static SingleThreadExecutorsPool EXEC_POOL;
  private static final String REQUEST_ID = "proto2024eExhaustiveTest";
  private final TestRequestLevelCache requestLevelCache = new TestRequestLevelCache();

  private VajramGraph graph;
  private Lease<SingleThreadExecutor> executorLease;
  private KrystexGraphBuilder kGraph;

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
    this.graph = VajramGraph.builder().loadClasses(Proto2024eLatticeSample.class).build();
    this.kGraph = KrystexGraph.builder().vajramGraph(graph);
    this.kGraph.injectionProvider(
        new VajramGuiceInputInjector(
            Guice.createInjector(
                new AbstractModule() {
                  @Override
                  protected void configure() {
                    bind(Proto2024eLatticeSampleResponse_Immut.Builder.class)
                        .to(Proto2024eLatticeSampleResponse_ImmutProto.Builder.class);
                  }
                })));
  }

  @AfterEach
  void tearDown() {
    executorLease.close();
  }

  @Test
  void allInputsProvided_success() {
    Proto2024eLatticeSample_ReqImmut request =
        Proto2024eLatticeSample_ReqImmutProto._builder()
            .optionalInput(42)
            .mandatoryInput(100)
            .conditionallyMandatoryInput(200)
            .inputWithDefaultValue(300)
            .optionalLongInput(10L)
            .mandatoryLongInput(20L)
            .optionalByteString(new ProtoByteArray(ByteString.copyFromUtf8("test")))
            .defaultByteString(new ProtoByteArray(ByteString.copyFromUtf8("hello")))
            ._build();

    CompletableFuture<Proto2024eLatticeSampleResponse> result;
    try (KrystexVajramExecutor executor =
        kGraph
            .build()
            .createExecutor(
                KrystexVajramExecutorConfig.builder()
                    .kryonExecutorConfig(
                        KryonExecutorConfig.builder()
                            .executorId(REQUEST_ID)
                            .executorService(executorLease.get())
                            .build()))) {
      result =
          executor.execute(
              request, KryonExecutionConfig.builder().executionId("test_all_inputs").build());
    }

    assertThat(result)
        .succeedsWithin(1, SECONDS)
        .extracting(Proto2024eLatticeSampleResponse::string, STRING)
        .contains(
            "$$ optionalInput: 42 $$",
            "$$ mandatoryInput: 100 $$",
            "$$ conditionallyMandatoryInput: 200 $$",
            "$$ inputWithDefaultValue: 300 $$",
            "$$ optionalLongInput: 10 $$",
            "$$ mandatoryLongInput: 20 $$",
            "$$ optionalByteString: [116, 101, 115, 116] $$",
            "$$ defaultByteString: [104, 101, 108, 108, 111] $$");
    assertThat(result)
        .succeedsWithin(1, SECONDS)
        .extracting(Proto2024eLatticeSampleResponse::protoMessage)
        .asInstanceOf(type(ProtoMessage_ImmutProto.class))
        .extracting(SerializableProtoModel::_proto)
        .isEqualTo(ProtoMessageProto.newBuilder().setCount(100).build());
  }

  @Test
  void optionalInputsOmitted_success() {
    Proto2024eLatticeSample_ReqImmut request =
        Proto2024eLatticeSample_ReqImmutProto._builder()
            .mandatoryInput(100)
            .conditionallyMandatoryInput(200)
            .inputWithDefaultValue(300)
            .mandatoryLongInput(20L)
            ._build();

    CompletableFuture<Proto2024eLatticeSampleResponse> result;
    try (KrystexVajramExecutor executor =
        kGraph
            .build()
            .createExecutor(
                KrystexVajramExecutorConfig.builder()
                    .kryonExecutorConfig(
                        KryonExecutorConfig.builder()
                            .executorId(REQUEST_ID)
                            .executorService(executorLease.get())
                            .build()))) {
      result =
          executor.execute(
              request, KryonExecutionConfig.builder().executionId("test_optional_omitted").build());
    }

    assertThat(result)
        .succeedsWithin(1, SECONDS)
        .extracting(Proto2024eLatticeSampleResponse::string, STRING)
        .contains(
            "$$ optionalInput: null $$",
            "$$ mandatoryInput: 100 $$",
            "$$ inputWithDefaultValue: 300 $$",
            "$$ optionalLongInput: null $$",
            "$$ mandatoryLongInput: 20 $$",
            "$$ optionalByteString: [] $$",
            "$$ defaultByteString: [] $$");
  }

  @Test
  void defaultValueStrategy() {
    Proto2024eLatticeSample_ReqImmut request =
        Proto2024eLatticeSample_ReqImmutProto._builder()
            .mandatoryInput(100)
            .conditionallyMandatoryInput(200)
            .mandatoryLongInput(20L)
            ._build();

    CompletableFuture<Proto2024eLatticeSampleResponse> result;
    try (KrystexVajramExecutor executor =
        kGraph
            .build()
            .createExecutor(
                KrystexVajramExecutorConfig.builder()
                    .kryonExecutorConfig(
                        KryonExecutorConfig.builder()
                            .executorId(REQUEST_ID)
                            .executorService(executorLease.get())
                            .build()))) {
      result =
          executor.execute(
              request, KryonExecutionConfig.builder().executionId("test_default_value").build());
    }

    assertThat(result)
        .succeedsWithin(1, SECONDS)
        .extracting(Proto2024eLatticeSampleResponse::string, STRING)
        .contains("inputWithDefaultValue: 0");
  }

  @Test
  void missingMandatoryInput_throws() {
    Proto2024eLatticeSample_ReqImmut request =
        Proto2024eLatticeSample_ReqImmutProto._builder()
            // Missing mandatoryInput
            .conditionallyMandatoryInput(200)
            .inputWithDefaultValue(300)
            .mandatoryLongInput(20L)
            ._build();

    CompletableFuture<Proto2024eLatticeSampleResponse> result;
    try (KrystexVajramExecutor executor =
        kGraph
            .build()
            .createExecutor(
                KrystexVajramExecutorConfig.builder()
                    .kryonExecutorConfig(
                        KryonExecutorConfig.builder()
                            .executorId(REQUEST_ID)
                            .executorService(executorLease.get())
                            .build()))) {
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
  void missingMandatoryLongInput_throws() {
    Proto2024eLatticeSample_ReqImmut request =
        Proto2024eLatticeSample_ReqImmutProto._builder()
            .mandatoryInput(100)
            .conditionallyMandatoryInput(200)
            .inputWithDefaultValue(300)
            // Missing mandatoryLongInput
            ._build();

    CompletableFuture<Proto2024eLatticeSampleResponse> result;
    try (KrystexVajramExecutor executor =
        kGraph
            .build()
            .createExecutor(
                KrystexVajramExecutorConfig.builder()
                    .kryonExecutorConfig(
                        KryonExecutorConfig.builder()
                            .executorId(REQUEST_ID)
                            .executorService(executorLease.get())
                            .build()))) {
      result =
          executor.execute(
              request,
              KryonExecutionConfig.builder().executionId("test_missing_mandatory_long").build());
    }
    assertThat(result)
        .failsWithin(1, SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseInstanceOf(MandatoryFacetsMissingException.class)
        .withMessageContaining("mandatoryLongInput");
  }

  @Test
  void mockedResponse_success() {
    Proto2024eLatticeSample_ReqImmut request =
        Proto2024eLatticeSample_ReqImmutProto._builder()
            .mandatoryInput(100)
            .conditionallyMandatoryInput(200)
            .mandatoryLongInput(20L)
            ._build();

    Proto2024eLatticeSampleResponse mockedOutput =
        Proto2024eLatticeSampleResponse_ImmutPojo._builder()
            .string("$$ This is a mocked response $$")
            .mandatoryInt(1)
            .mandatoryStringPartialConstruction("hello")
            .status(Status.PENDING)
            ._build();
    CompletableFuture<Proto2024eLatticeSampleResponse> result;
    try (KrystexVajramExecutor executor =
        kGraph
            .build()
            .createExecutor(
                VajramTestHarness.prepareForTest(
                        KrystexVajramExecutorConfig.builder()
                            .kryonExecutorConfig(
                                KryonExecutorConfig.builder()
                                    .executorId(REQUEST_ID)
                                    .executorService(executorLease.get())
                                    .build()),
                        requestLevelCache)
                    .withMock(
                        ((VajramDef<?>)
                                graph
                                    .getVajramDefinition(Proto2024eLatticeSample_Req._VAJRAM_ID)
                                    .def())
                            .facetsFromRequest(request)
                            ._build(),
                        Errable.withValue(mockedOutput))
                    .buildConfig())) {
      result =
          executor.execute(
              request, KryonExecutionConfig.builder().executionId("test_mocked_response").build());
    }

    assertThat(result).succeedsWithin(1, SECONDS).isEqualTo(mockedOutput);
  }

  @Test
  void byteStringInput_success() {
    ByteString byteString = ByteString.copyFromUtf8("Hello, World!");
    Proto2024eLatticeSample_ReqImmut request =
        Proto2024eLatticeSample_ReqImmutProto._builder()
            .mandatoryInput(100)
            .conditionallyMandatoryInput(200)
            .inputWithDefaultValue(300)
            .mandatoryLongInput(20L)
            .optionalByteString(new ProtoByteArray(byteString))
            ._build();

    CompletableFuture<Proto2024eLatticeSampleResponse> result;
    try (KrystexVajramExecutor executor =
        kGraph
            .build()
            .createExecutor(
                KrystexVajramExecutorConfig.builder()
                    .kryonExecutorConfig(
                        KryonExecutorConfig.builder()
                            .executorId(REQUEST_ID)
                            .executorService(executorLease.get())
                            .build()))) {
      result =
          executor.execute(
              request, KryonExecutionConfig.builder().executionId("test_byte_string").build());
    }

    assertThat(result)
        .succeedsWithin(100, SECONDS)
        .extracting(Proto2024eLatticeSampleResponse::string, STRING)
        .contains(
            "$$ optionalByteString: [72, 101, 108, 108, 111, 44, 32, 87, 111, 114, 108, 100, 33] $$");
  }
}

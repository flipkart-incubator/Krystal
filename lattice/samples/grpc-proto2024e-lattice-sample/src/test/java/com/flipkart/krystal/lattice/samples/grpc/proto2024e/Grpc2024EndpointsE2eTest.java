package com.flipkart.krystal.lattice.samples.grpc.proto2024e;

import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.lattice.samples.grpc.proto2024e.sampleProtoService.Proto2024eLatticeSampleReqProto;
import com.flipkart.krystal.lattice.samples.grpc.proto2024e.sampleProtoService.Proto2024eLatticeSampleResponseProto;
import com.flipkart.krystal.lattice.samples.grpc.proto2024e.sampleProtoService.app.GrpcApp_Impl;
import com.flipkart.krystal.lattice.samples.grpc.proto2024e.sampleProtoService.app.SampleGrpcServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

/**
 * End-to-end tests that boot the actual gRPC (proto edition 2024) server once per suite on port
 * 18790 and call {@code SampleGrpcService/Proto2024eLatticeSample} via a blocking gRPC stub.
 */
@TestInstance(Lifecycle.PER_CLASS)
class Grpc2024EndpointsE2eTest {

  private static final int GRPC_PORT = 18790;

  private ManagedChannel channel;
  private SampleGrpcServiceGrpc.SampleGrpcServiceBlockingStub stub;

  @BeforeAll
  void startServer() throws Exception {
    Thread serverThread =
        new Thread(
            () -> {
              try {
                new GrpcApp_Impl().run(new String[] {"-l", "test_grpc_app_config.yaml"});
              } catch (Throwable t) {
                t.printStackTrace();
              }
            },
            "lattice-grpc-proto2024e-test-app");
    serverThread.setDaemon(true);
    serverThread.start();

    long deadline = System.currentTimeMillis() + 60_000L;
    while (System.currentTimeMillis() < deadline) {
      if (isPortOpen()) {
        channel = ManagedChannelBuilder.forAddress("localhost", GRPC_PORT).usePlaintext().build();
        stub = SampleGrpcServiceGrpc.newBlockingStub(channel);
        return;
      }
      Thread.sleep(200);
    }
    throw new IllegalStateException(
        "gRPC proto2024e server did not start on port " + GRPC_PORT + " within 60s");
  }

  @AfterAll
  void stopServer() throws InterruptedException {
    if (channel != null) {
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void proto2024eLatticeSample_mandatoryInputsOnly_returnsFormattedResponse() {
    Proto2024eLatticeSampleReqProto req =
        Proto2024eLatticeSampleReqProto.newBuilder()
            .setMandatoryInput(7)
            .setMandatoryLongInput(99L)
            .build();

    Proto2024eLatticeSampleResponseProto resp = stub.proto2024eLatticeSample(req);

    assertThat(resp.getString()).contains("$$ mandatoryInput: 7 $$");
    assertThat(resp.getString()).contains("$$ mandatoryLongInput: 99 $$");
    assertThat(resp.getMandatoryInt()).isEqualTo(1);
  }

  @Test
  void proto2024eLatticeSample_allInputsProvided_returnsAllValues() {
    Proto2024eLatticeSampleReqProto req =
        Proto2024eLatticeSampleReqProto.newBuilder()
            .setOptionalInput(42)
            .setMandatoryInput(100)
            .setConditionallyMandatoryInput(200)
            .setInputWithDefaultValue(300)
            .setOptionalLongInput(10L)
            .setMandatoryLongInput(20L)
            .build();

    Proto2024eLatticeSampleResponseProto resp = stub.proto2024eLatticeSample(req);

    assertThat(resp.getString())
        .contains(
            "$$ optionalInput: 42 $$",
            "$$ mandatoryInput: 100 $$",
            "$$ conditionallyMandatoryInput: 200 $$",
            "$$ inputWithDefaultValue: 300 $$",
            "$$ optionalLongInput: 10 $$",
            "$$ mandatoryLongInput: 20 $$");
  }

  private static boolean isPortOpen() {
    try (Socket s = new Socket()) {
      s.connect(new InetSocketAddress("localhost", GRPC_PORT), 250);
      return true;
    } catch (IOException e) {
      return false;
    }
  }
}

package com.flipkart.krystal.lattice.samples.grpc.proto3;

import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.lattice.samples.grpc.proto.sampleProtoService.Proto3LatticeSampleReqProto3;
import com.flipkart.krystal.lattice.samples.grpc.proto.sampleProtoService.Proto3LatticeSampleResponseProto3;
import com.flipkart.krystal.lattice.samples.grpc.proto.sampleProtoService.app.GrpcApp_Impl;
import com.flipkart.krystal.lattice.samples.grpc.proto.sampleProtoService.app.SampleGrpcServiceGrpc;
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
 * End-to-end tests that boot the actual gRPC (proto3) server once per suite on port 18789 and call
 * {@code SampleGrpcService/Proto3LatticeSample} via a blocking gRPC stub.
 */
@TestInstance(Lifecycle.PER_CLASS)
class Grpc3EndpointsE2eTest {

  private static final int GRPC_PORT = 18789;

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
            "lattice-grpc-proto3-test-app");
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
        "gRPC proto3 server did not start on port " + GRPC_PORT + " within 60s");
  }

  @AfterAll
  void stopServer() throws InterruptedException {
    if (channel != null) {
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void proto3LatticeSample_mandatoryInputsOnly_returnsFormattedResponse() {
    Proto3LatticeSampleReqProto3 req =
        Proto3LatticeSampleReqProto3.newBuilder()
            .setMandatoryInput(7)
            .setMandatoryLongInput(99L)
            .build();

    Proto3LatticeSampleResponseProto3 resp = stub.proto3LatticeSample(req);

    assertThat(resp.getString()).contains("$$ mandatoryInput: 7 $$");
    assertThat(resp.getString()).contains("$$ mandatoryLongInput: 99 $$");
    assertThat(resp.getMandatoryInt()).isEqualTo(1);
  }

  @Test
  void proto3LatticeSample_allInputsProvided_returnsAllValues() {
    Proto3LatticeSampleReqProto3 req =
        Proto3LatticeSampleReqProto3.newBuilder()
            .setOptionalInput(42)
            .setMandatoryInput(100)
            .setConditionallyMandatoryInput(200)
            .setInputWithDefaultValue(300)
            .setOptionalLongInput(10L)
            .setMandatoryLongInput(20L)
            .build();

    Proto3LatticeSampleResponseProto3 resp = stub.proto3LatticeSample(req);

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

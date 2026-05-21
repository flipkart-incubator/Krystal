package com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService;

import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.app.ForyRestApp_Impl;
import com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.models.ForyRequest_ImmutFory;
import com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.models.ForyResponse_ImmutFory;
import io.quarkus.runtime.Quarkus;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

/**
 * End-to-end tests that boot the Quarkus REST server on port 18083, exercise Fory-backed endpoints
 * via HTTP, and verify serialization/deserialization round-trips. Run with {@code ./gradlew
 * :lattice:samples:rest:fory:quarkus:rest-fory-quarkus-lattice-sample:test -PunsafeCompile=true}.
 */
@TestInstance(Lifecycle.PER_CLASS)
class ForyQuarkusE2eTest {

  private static final int APP_PORT = 18084;
  private static final String BASE_URL = "http://localhost:" + APP_PORT;

  private @MonotonicNonNull HttpClient httpClient;

  @BeforeAll
  void startServer() throws Exception {
    System.setProperty("quarkus.http.port", String.valueOf(APP_PORT));

    Thread serverThread =
        new Thread(
            () -> {
              try {
                ForyRestApp_Impl.main(new String[] {});
              } catch (Throwable t) {
                t.printStackTrace();
              }
            },
            "lattice-fory-quarkus-test-app");
    serverThread.setDaemon(true);
    serverThread.start();

    long deadline = System.currentTimeMillis() + 120_000L;
    while (System.currentTimeMillis() < deadline) {
      if (isPortOpen()) {
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        return;
      }
      Thread.sleep(200);
    }
    throw new IllegalStateException(
        "Embedded Quarkus server did not start on port " + APP_PORT + " within 120s");
  }

  @AfterAll
  void stopServer() {
    Quarkus.asyncExit();
  }

  @Test
  void getMapping_returnsResponseWithQueryParams() throws Exception {
    HttpResponse<byte[]> resp =
        httpClient.send(
            HttpRequest.newBuilder(URI.create(BASE_URL + "/foo/bar?name=alice&age=42"))
                .GET()
                .header("Accept", "application/x-fory")
                .build(),
            BodyHandlers.ofByteArray());
    assertThat(resp.statusCode()).isEqualTo(200);

    ForyResponse_ImmutFory response = new ForyResponse_ImmutFory(resp.body());
    assertThat(response.path()).isEqualTo("foo/bar");
    assertThat(response.queryName()).isEqualTo("alice");
    assertThat(response.queryAge()).isEqualTo("42");
    assertThat(response.message()).isEqualTo("GET response via Fory serde");
  }

  @Test
  void postMapping_acceptsForyBinaryAndReturnsResponse() throws Exception {
    ForyRequest_ImmutFory request =
        ForyRequest_ImmutFory._builder().mandatoryInput(7).mandatoryLongInput(99L)._build();

    byte[] requestBytes = request._serialize();

    HttpResponse<byte[]> resp =
        httpClient.send(
            HttpRequest.newBuilder(URI.create(BASE_URL + "/foo/bar"))
                .POST(BodyPublishers.ofByteArray(requestBytes))
                .header("Content-Type", "application/x-fory")
                .header("Accept", "application/x-fory")
                .build(),
            BodyHandlers.ofByteArray());
    assertThat(resp.statusCode()).isEqualTo(200);

    ForyResponse_ImmutFory response = new ForyResponse_ImmutFory(resp.body());
    assertThat(response.message()).contains("mandatoryInput: 7");
    assertThat(response.message()).contains("mandatoryLongInput: 99");
  }

  private static boolean isPortOpen() {
    try (Socket s = new Socket()) {
      s.connect(new InetSocketAddress("localhost", APP_PORT), 250);
      return true;
    } catch (IOException e) {
      return false;
    }
  }
}

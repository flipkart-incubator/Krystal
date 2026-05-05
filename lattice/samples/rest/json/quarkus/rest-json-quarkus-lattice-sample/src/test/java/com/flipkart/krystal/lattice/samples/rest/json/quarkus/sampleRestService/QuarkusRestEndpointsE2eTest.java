package com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService;

import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.app.RestfulQuarkusApp_Impl;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

/**
 * End-to-end tests that boot the actual Quarkus REST server once per suite (port 18082), exercise
 * every vajram-backed endpoint via HTTP, and let the daemon server thread die with the forked test
 * JVM. Run with {@code ./gradlew
 * :lattice:samples:rest:json:quarkus:rest-json-quarkus-lattice-sample:test -PunsafeCompile=true}.
 * The build is configured with {@code forkEvery = 1} so each test class gets a fresh JVM, isolating
 * Quarkus state.
 */
@TestInstance(Lifecycle.PER_CLASS)
class QuarkusRestEndpointsE2eTest {

  private static final int APP_PORT = 18082;
  private static final String BASE_URL = "http://localhost:" + APP_PORT;

  private @MonotonicNonNull HttpClient httpClient;

  @BeforeAll
  void startServer() throws Exception {
    System.setProperty("quarkus.http.port", String.valueOf(APP_PORT));

    Thread serverThread =
        new Thread(
            () -> {
              try {
                RestfulQuarkusApp_Impl.main(new String[] {});
              } catch (Throwable t) {
                t.printStackTrace();
              }
            },
            "lattice-quarkus-test-app");
    serverThread.setDaemon(true);
    serverThread.start();

    long deadline = System.currentTimeMillis() + 60_000L;
    while (System.currentTimeMillis() < deadline) {
      if (isPortOpen()) {
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        return;
      }
      Thread.sleep(200);
    }
    throw new IllegalStateException(
        "Embedded Quarkus server did not start on port " + APP_PORT + " within 60s");
  }

  @AfterAll
  void stopServer() {
    Quarkus.asyncExit();
  }

  @Test
  void getMapping_returnsResponseWithQueryParams() throws Exception {
    HttpResponse<String> resp =
        httpClient.send(
            HttpRequest.newBuilder(URI.create(BASE_URL + "/foo/bar?name=alice&age=42"))
                .GET()
                .header("Accept", "application/json")
                .build(),
            BodyHandlers.ofString());
    assertThat(resp.statusCode()).isEqualTo(200);
    assertThat(resp.body()).contains("\"path\":\"foo/bar\"");
    assertThat(resp.body()).contains("\"qp_name\":\"alice\"");
    assertThat(resp.body()).contains("\"qp_age\":\"42\"");
  }

  @Test
  void postMapping_returnsResponseEchoingBodyAndPath() throws Exception {
    String body =
        """
        {
          "mandatoryInput": 7,
          "mandatoryLongInput": 99,
          "defaultByteString": "AA=="
        }
        """;
    HttpResponse<String> resp =
        httpClient.send(
            HttpRequest.newBuilder(URI.create(BASE_URL + "/foo/bar"))
                .POST(BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build(),
            BodyHandlers.ofString());
    assertThat(resp.statusCode()).isEqualTo(200);
    assertThat(resp.body()).contains("PATH: foo/bar");
    assertThat(resp.body()).contains("mandatoryInput: 7");
    assertThat(resp.body()).contains("mandatoryLongInput: 99");
  }

  @Test
  void headMapping_returns200WithoutBody() throws Exception {
    HttpResponse<String> resp =
        httpClient.send(
            HttpRequest.newBuilder(URI.create(BASE_URL + "/foo/bar?name=alice&age=42"))
                .method("HEAD", BodyPublishers.noBody())
                .build(),
            BodyHandlers.ofString());
    assertThat(resp.statusCode()).isEqualTo(200);
    assertThat(resp.body()).isEmpty();
  }

  private static boolean isPortOpen() {
    try (Socket s = new Socket()) {
      s.connect(new InetSocketAddress("localhost", APP_PORT), 250);
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  @Test
  @Disabled("Facing problem with making regex matching work in quarkus")
  void postComplexPathMatching_extractsAllPathParams() throws Exception {
    String body =
        """
            {
              "mandatoryInput": 1,
              "mandatoryLongInput": 2,
              "defaultByteString": "AA=="
            }
            """;
    HttpResponse<String> resp =
        httpClient.send(
            HttpRequest.newBuilder(URI.create(BASE_URL + "/complex/ctxA/path/nameA/p1/p2/p3/123"))
                .POST(BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build(),
            BodyHandlers.ofString());
    assertThat(resp.statusCode()).isEqualTo(200);
    assertThat(resp.body()).contains("context: ctxA");
    assertThat(resp.body()).contains("name: nameA");
    assertThat(resp.body()).contains("threePaths: p1/p2/p3");
    assertThat(resp.body()).contains("id: 123");
  }
}

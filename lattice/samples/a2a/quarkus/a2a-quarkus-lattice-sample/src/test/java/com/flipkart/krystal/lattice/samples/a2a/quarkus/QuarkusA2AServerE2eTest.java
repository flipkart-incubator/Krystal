package com.flipkart.krystal.lattice.samples.a2a.quarkus;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

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
 * End-to-end tests that boot the Quarkus A2A server once per suite (port 18085), exercise the A2A
 * REST endpoints via HTTP, and let the daemon server thread die with the forked test JVM.
 *
 * <p>Run with: {@code ./gradlew :lattice:samples:a2a:quarkus:a2a-quarkus-lattice-sample:test
 * -PunsafeCompile=true}
 *
 * <p>The A2A SDK (reference REST) maps A2A protocol to these HTTP routes:
 *
 * <ul>
 *   <li>{@code GET /.well-known/agent-card.json} – agent card (public, no auth)
 *   <li>{@code POST /message:send} – send a message (proto-JSON body)
 * </ul>
 */
@TestInstance(Lifecycle.PER_CLASS)
class QuarkusA2AServerE2eTest {

  private static final int APP_PORT = 18085;
  private static final String BASE_URL = "http://localhost:" + APP_PORT;

  private @MonotonicNonNull HttpClient httpClient;

  @BeforeAll
  void startServer() throws Exception {
    System.setProperty("quarkus.http.port", String.valueOf(APP_PORT));

    Thread serverThread =
        new Thread(
            () -> {
              try {
                QuarkusA2AServer_Impl.main(new String[] {});
              } catch (Throwable t) {
                t.printStackTrace();
              }
            },
            "lattice-a2a-quarkus-test-app");
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
        "Embedded Quarkus A2A server did not start on port " + APP_PORT + " within 60s");
  }

  @AfterAll
  void stopServer() {
    Quarkus.asyncExit();
  }

  @Test
  void agentCard_isServedAtWellKnownPath() throws Exception {
    HttpResponse<String> resp =
        requireNonNull(httpClient)
            .send(
                HttpRequest.newBuilder(URI.create(BASE_URL + "/.well-known/agent-card.json"))
                    .GET()
                    .header("Accept", "application/json")
                    .build(),
                BodyHandlers.ofString());

    assertThat(resp.statusCode()).as("agent-card status; body=%s", resp.body()).isEqualTo(200);
    assertThat(resp.body()).contains("Lattice A2A Sample Agent");
    assertThat(resp.body()).contains("echo");
    assertThat(resp.body()).contains("reverse");
  }

  @Test
  void sendMessage_echoSkill_returnsEchoedText() throws Exception {
    // The A2A REST transport uses proto-JSON bodies on /message:send
    String requestBody =
        """
        {
          "message": {
            "messageId": "test-echo-1",
            "role": "ROLE_USER",
            "parts": [{"text": "Hello A2A!"}]
          },
          "metadata": {"skillId": "echo"}
        }
        """;

    HttpResponse<String> resp =
        requireNonNull(httpClient)
            .send(
                HttpRequest.newBuilder(URI.create(BASE_URL + "/message:send"))
                    .POST(BodyPublishers.ofString(requestBody))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("A2A-Version", "1.0")
                    .build(),
                BodyHandlers.ofString());

    assertThat(resp.statusCode()).as("echo status; body=%s", resp.body()).isEqualTo(200);
    assertThat(resp.body()).contains("Echo: Hello A2A!");
  }

  @Test
  void sendMessage_reverseSkill_returnsReversedText() throws Exception {
    String requestBody =
        """
        {
          "message": {
            "messageId": "test-reverse-1",
            "role": "ROLE_USER",
            "parts": [{"text": "Hello"}]
          },
          "metadata": {"skillId": "reverse"}
        }
        """;

    HttpResponse<String> resp =
        requireNonNull(httpClient)
            .send(
                HttpRequest.newBuilder(URI.create(BASE_URL + "/message:send"))
                    .POST(BodyPublishers.ofString(requestBody))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("A2A-Version", "1.0")
                    .build(),
                BodyHandlers.ofString());

    assertThat(resp.statusCode()).as("reverse status; body=%s", resp.body()).isEqualTo(200);
    // "Hello" reversed is "olleH"
    assertThat(resp.body()).contains("olleH");
  }

  @Test
  void sendMessage_defaultsToFirstSkill_whenNoSkillIdInMetadata() throws Exception {
    String requestBody =
        """
        {
          "message": {
            "messageId": "test-default-1",
            "role": "ROLE_USER",
            "parts": [{"text": "No skill specified"}]
          }
        }
        """;

    HttpResponse<String> resp =
        requireNonNull(httpClient)
            .send(
                HttpRequest.newBuilder(URI.create(BASE_URL + "/message:send"))
                    .POST(BodyPublishers.ofString(requestBody))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("A2A-Version", "1.0")
                    .build(),
                BodyHandlers.ofString());

    assertThat(resp.statusCode()).as("default-skill status; body=%s", resp.body()).isEqualTo(200);
    // Default is "echo" (first declared agent)
    assertThat(resp.body()).contains("Echo: No skill specified");
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

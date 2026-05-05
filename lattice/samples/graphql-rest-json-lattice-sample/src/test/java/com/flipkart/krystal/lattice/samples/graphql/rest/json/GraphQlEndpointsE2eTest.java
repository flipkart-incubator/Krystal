package com.flipkart.krystal.lattice.samples.graphql.rest.json;

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
 * End-to-end tests that boot the actual Quarkus GraphQL server once per suite (port 18083), send
 * GraphQL queries over POST /graphql, and assert on the JSON response.
 */
@TestInstance(Lifecycle.PER_CLASS)
class GraphQlEndpointsE2eTest {

  private static final int APP_PORT = 18083;
  private static final String BASE_URL = "http://localhost:" + APP_PORT;

  private @MonotonicNonNull HttpClient httpClient;

  @BeforeAll
  void startServer() throws Exception {
    System.setProperty("quarkus.http.port", String.valueOf(APP_PORT));

    Thread serverThread =
        new Thread(
            () -> {
              try {
                SampleGraphQlServerApp_Impl.main(new String[] {});
              } catch (Throwable t) {
                t.printStackTrace();
              }
            },
            "lattice-graphql-quarkus-test-app");
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
        "Embedded Quarkus GraphQL server did not start on port " + APP_PORT + " within 60s");
  }

  @AfterAll
  void stopServer() {
    Quarkus.asyncExit();
  }

  @Test
  void graphQlQuery_returnsOwnerNameAndEmail() throws Exception {
    String query =
        """
        {
          "query": "{ account(id: \\"ACC123\\") { owner { name { firstName lastName } email } } }"
        }
        """;
    HttpResponse<String> resp =
        httpClient.send(
            HttpRequest.newBuilder(URI.create(BASE_URL + "/HttpPostGraphQl"))
                .POST(BodyPublishers.ofString(query))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build(),
            BodyHandlers.ofString());

    assertThat(resp.statusCode()).isEqualTo(200);
    // GetOwnerOfAccount: PersonId = "PRSN" + id  =>  "PRSNACC123"
    // GetPersonName: firstName = personId + "-FirstName" => "PRSNACC123-FirstName"
    assertThat(resp.body()).contains("PRSNACC123-FirstName");
    assertThat(resp.body()).contains("PRSNACC123-LastName");
    // GetPersonEmail: personId + "@" + personId + ".com" => "PRSNACC123@PRSNACC123.com"
    assertThat(resp.body()).contains("PRSNACC123@PRSNACC123.com");
  }

  @Test
  void graphQlQuery_onlyNameRequested_returnsName() throws Exception {
    String query =
        """
        {
          "query": "{ account(id: \\"XYZ\\") { owner { name { firstName } } } }"
        }
        """;
    HttpResponse<String> resp =
        httpClient.send(
            HttpRequest.newBuilder(URI.create(BASE_URL + "/HttpPostGraphQl"))
                .POST(BodyPublishers.ofString(query))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build(),
            BodyHandlers.ofString());

    assertThat(resp.statusCode()).isEqualTo(200);
    assertThat(resp.body()).contains("PRSNXYZ-FirstName");
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

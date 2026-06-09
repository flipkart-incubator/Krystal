package com.flipkart.krystal.lattice.samples.graphql.rest.json;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

/**
 * End-to-end tests that boot the actual Quarkus GraphQL server once per suite (port 18083), send
 * GraphQL queries over POST /graphql, and assert on the JSON response.
 */
@QuarkusTest
@TestInstance(Lifecycle.PER_CLASS)
class GraphQlEndpointsE2eTest {

  @TestHTTPResource private URI baseUri;

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

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
            HttpRequest.newBuilder(baseUri.resolve("HttpPostGraphQl"))
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
            HttpRequest.newBuilder(baseUri.resolve("HttpPostGraphQl"))
                .POST(BodyPublishers.ofString(query))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build(),
            BodyHandlers.ofString());

    assertThat(resp.statusCode()).isEqualTo(200);
    assertThat(resp.body()).contains("PRSNXYZ-FirstName");
  }
}

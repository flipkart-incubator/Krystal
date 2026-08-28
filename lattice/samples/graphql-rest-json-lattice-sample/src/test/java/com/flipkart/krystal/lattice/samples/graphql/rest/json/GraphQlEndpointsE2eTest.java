package com.flipkart.krystal.lattice.samples.graphql.rest.json;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
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

  private static final JsonMapper JSON_MAPPER = new JsonMapper();

  @TestHTTPResource private URI baseUri;

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  private JsonNode postGraphQl(String query) throws Exception {
    HttpResponse<String> resp =
        httpClient.send(
            HttpRequest.newBuilder(baseUri.resolve("HttpPostGraphQl"))
                .POST(
                    BodyPublishers.ofString(
                        "{\"query\": " + JSON_MAPPER.writeValueAsString(query) + "}"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build(),
            BodyHandlers.ofString());
    assertThat(resp.statusCode()).isEqualTo(200);
    return JSON_MAPPER.readTree(resp.body()).path("data");
  }

  @Test
  void graphQlQuery_returnsOwnerNameAndEmail() throws Exception {
    JsonNode data =
        postGraphQl("{ account(id: \"ACC123\") { owner { name { firstName lastName } email } } }");

    JsonNode name = data.path("account").path("owner").path("name");
    // GetOwnerOfAccount: PersonId = "PRSN" + id  =>  "PRSNACC123"
    // GetPersonName: firstName = personId + "-FirstName" => "PRSNACC123-FirstName"
    assertThat(name.path("firstName").asText()).isEqualTo("PRSNACC123-FirstName");
    assertThat(name.path("lastName").asText()).isEqualTo("PRSNACC123-LastName");
    // GetPersonEmail: personId + "@" + personId + ".com" => "PRSNACC123@PRSNACC123.com"
    assertThat(data.path("account").path("owner").path("email").asText())
        .isEqualTo("PRSNACC123@PRSNACC123.com");
  }

  @Test
  void graphQlQuery_onlyNameRequested_returnsName() throws Exception {
    JsonNode data = postGraphQl("{ account(id: \"XYZ\") { owner { name { firstName } } } }");

    JsonNode owner = data.path("account").path("owner");
    assertThat(owner.path("name").path("firstName").asText()).isEqualTo("PRSNXYZ-FirstName");
    // Only `firstName` was requested, so no sibling fields should be present.
    assertThat(owner.path("name").has("lastName")).isFalse();
    assertThat(owner.has("email")).isFalse();
  }

  @Test
  void graphQlQuery_aliasesOperationNormalAndComposedOnlyFields() throws Exception {
    JsonNode data =
        postGraphQl(
            """
            {
              accountAlias: account(id: "ACC123") {
                personAlias: owner {
                  imageAlias: imageData {
                    mainAlias: mainUrl
                    thumbnailAlias: thumbnailUrl
                  }
                }
              }
            }
            """);

    JsonNode imageAlias = data.path("accountAlias").path("personAlias").path("imageAlias");
    assertThat(data.has("accountAlias")).isTrue();
    assertThat(data.path("accountAlias").has("personAlias")).isTrue();
    assertThat(data.path("accountAlias").path("personAlias").has("imageAlias")).isTrue();
    assertThat(imageAlias.path("mainAlias").asText()).isEqualTo("PRSNACC123-mainUrl.png");
    assertThat(imageAlias.path("thumbnailAlias").asText()).isEqualTo("PRSNACC123-thumbnailUrl.png");
  }

  @Test
  void graphQlQuery_namedFragment_noAliases_returnsOwnerNameAndEmail() throws Exception {
    JsonNode data =
        postGraphQl(
            """
            query {
              account(id: "ACC123") {
                owner {
                  ...personFields
                }
              }
            }
            fragment personFields on Person {
              name {
                firstName
                lastName
              }
              email
            }
            """);

    JsonNode owner = data.path("account").path("owner");
    assertThat(owner.path("name").path("firstName").asText()).isEqualTo("PRSNACC123-FirstName");
    assertThat(owner.path("name").path("lastName").asText()).isEqualTo("PRSNACC123-LastName");
    assertThat(owner.path("email").asText()).isEqualTo("PRSNACC123@PRSNACC123.com");
  }

  @Test
  void graphQlQuery_namedFragment_withAliasesAtSpreadSiteAndInsideFragment_succeeds()
      throws Exception {
    JsonNode data =
        postGraphQl(
            """
            query {
              accountAlias: account(id: "ACC123") {
                ownerAlias: owner {
                  ...personFields
                }
              }
            }
            fragment personFields on Person {
              n: name {
                first: firstName
              }
              emailAlias: email
            }
            """);

    JsonNode ownerAlias = data.path("accountAlias").path("ownerAlias");
    assertThat(data.has("accountAlias")).isTrue();
    assertThat(data.path("accountAlias").has("ownerAlias")).isTrue();
    assertThat(ownerAlias.has("n")).isTrue();
    assertThat(ownerAlias.path("n").path("first").asText()).isEqualTo("PRSNACC123-FirstName");
    assertThat(ownerAlias.path("emailAlias").asText()).isEqualTo("PRSNACC123@PRSNACC123.com");
  }
}

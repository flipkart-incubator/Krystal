package com.flipkart.krystal.lattice.samples.graphql.rest.json;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.client.AccountAliased;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.client.GetAccountAliasedOp_GQlClientResp_ImmutJson;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.client.GetAccountAliasedOp_SpecReq;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.client.GetAccountAliasedVariables_ImmutJson;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.client.GetAccountNameOnlyOp_GQlClientResp_ImmutJson;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.client.GetAccountNameOnlyOp_SpecReq;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.client.GetAccountNameOnlyVariables_ImmutJson;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.client.GetAccountOwnerDetailsOp_GQlClientResp_ImmutJson;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.client.GetAccountOwnerDetailsOp_SpecReq;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.client.GetAccountOwnerDetailsVariables_ImmutJson;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.client.GetAccountOwnerFragmentAliasedOp_GQlClientResp_ImmutJson;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.client.GetAccountOwnerFragmentAliasedOp_SpecReq;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.client.GetAccountOwnerFragmentAliasedVariables_ImmutJson;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.client.GetAccountOwnerFragmentOp_GQlClientResp_ImmutJson;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.client.GetAccountOwnerFragmentOp_SpecReq;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.client.GetAccountOwnerFragmentVariables_ImmutJson;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.client.PersonAliased;
import com.flipkart.krystal.vajram.graphql.client.GraphQlSpecRequest;
import com.flipkart.krystal.vajram.json.Json;
import com.flipkart.krystal.vajram.json.serialized.StringJson;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Map;
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

  /**
   * Same as {@link #postGraphQl(String)}, but for a generated {@link GraphQlSpecRequest} -
   * serializes {@code req.query()}/{@code req.variables()} instead of a raw query string, and
   * deserializes the raw response body directly into {@code responseWrapperType} (the operation
   * root's generated {@code <Op>_GQlClientResp} class, whose {@code data} field already matches the
   * operation root's shape) in a single pass - no intermediate {@link JsonNode} tree parse. Any
   * top-level fields the wrapper doesn't declare (e.g. {@code errors}) are ignored, since {@link
   * Json} disables {@code FAIL_ON_UNKNOWN_PROPERTIES}. {@code req.variables()} is serialized via
   * {@link Json#OBJECT_WRITER} since the variables models in {@code client/} declare
   * {@code @SupportedModelProtocol(Json.class)}.
   */
  private StringJson postGraphQl(GraphQlSpecRequest req) throws Exception {
    HttpResponse<String> resp =
        httpClient.send(
            HttpRequest.newBuilder(baseUri.resolve("HttpPostGraphQl"))
                .POST(
                    BodyPublishers.ofString(
                        Json.OBJECT_WRITER.writeValueAsString(
                            Map.of("query", req.query(), "variables", req.variables()))))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build(),
            BodyHandlers.ofString());
    assertThat(resp.statusCode()).isEqualTo(200);
    return new StringJson(resp.body());
  }

  @Test
  void graphQlQuery_returnsOwnerNameAndEmail() throws Exception {
    GetAccountOwnerDetailsVariables_ImmutJson variables =
        GetAccountOwnerDetailsVariables_ImmutJson._builder().id("ACC123")._build();
    var response =
        new GetAccountOwnerDetailsOp_GQlClientResp_ImmutJson(
                postGraphQl(GetAccountOwnerDetailsOp_SpecReq.of(variables)))
            .data();

    // GetOwnerOfAccount: PersonId = "PRSN" + id  =>  "PRSNACC123"
    // GetPersonName: firstName = personId + "-FirstName" => "PRSNACC123-FirstName"
    assertThat(response.account().owner().name().firstName()).isEqualTo("PRSNACC123-FirstName");
    assertThat(response.account().owner().name().lastName()).isEqualTo("PRSNACC123-LastName");
    // GetPersonEmail: personId + "@" + personId + ".com" => "PRSNACC123@PRSNACC123.com"
    assertThat(response.account().owner().email()).isEqualTo("PRSNACC123@PRSNACC123.com");
  }

  @Test
  void graphQlQuery_onlyNameRequested_returnsName() throws Exception {
    GetAccountNameOnlyVariables_ImmutJson variables =
        GetAccountNameOnlyVariables_ImmutJson._builder().id("XYZ")._build();
    var response =
        new GetAccountNameOnlyOp_GQlClientResp_ImmutJson(
                postGraphQl(GetAccountNameOnlyOp_SpecReq.of(variables)))
            .data();

    assertThat(response.account().owner().name().firstName()).isEqualTo("PRSNXYZ-FirstName");
    // Only `firstName` was requested - `OwnerNameOnly`/`FirstNameOnly` don't even declare
    // `lastName`/`email` getters, so the narrower selection is enforced at compile time rather
    // than needing a runtime "sibling field absent" check.
  }

  @Test
  void graphQlQuery_aliasesOperationNormalAndComposedOnlyFields() throws Exception {
    GetAccountAliasedVariables_ImmutJson variables =
        GetAccountAliasedVariables_ImmutJson._builder().id("ACC123")._build();
    var response =
        new GetAccountAliasedOp_GQlClientResp_ImmutJson(
                postGraphQl(GetAccountAliasedOp_SpecReq.of(variables)))
            .data();

    AccountAliased accountAlias = response.accountAlias();
    assertThat(accountAlias).isNotNull();
    PersonAliased personAlias = accountAlias.personAlias();
    assertThat(personAlias).isNotNull();
    assertThat(personAlias.imageAlias()).isNotNull();
    assertThat(personAlias.imageAlias().mainAlias()).isEqualTo("PRSNACC123-mainUrl.png");
    assertThat(personAlias.imageAlias().thumbnailAlias()).isEqualTo("PRSNACC123-thumbnailUrl.png");
  }

  // The two tests below specifically exercise named GraphQL fragment syntax (`...PersonFields`)
  // via `@GraphQlFragment`-annotated client models, mirroring the other tests' `<Op>_SpecReq`
  // pattern.

  @Test
  void graphQlQuery_namedFragment_noAliases_returnsOwnerNameAndEmail() throws Exception {
    GetAccountOwnerFragmentVariables_ImmutJson variables =
        GetAccountOwnerFragmentVariables_ImmutJson._builder().id("ACC123")._build();
    var response =
        new GetAccountOwnerFragmentOp_GQlClientResp_ImmutJson(
                postGraphQl(GetAccountOwnerFragmentOp_SpecReq.of(variables)))
            .data();

    assertThat(response.account().owner().name().firstName()).isEqualTo("PRSNACC123-FirstName");
    assertThat(response.account().owner().name().lastName()).isEqualTo("PRSNACC123-LastName");
    assertThat(response.account().owner().email()).isEqualTo("PRSNACC123@PRSNACC123.com");
  }

  @Test
  void graphQlQuery_namedFragment_withAliasesAtSpreadSiteAndInsideFragment_succeeds()
      throws Exception {
    GetAccountOwnerFragmentAliasedVariables_ImmutJson variables =
        GetAccountOwnerFragmentAliasedVariables_ImmutJson._builder().id("ACC123")._build();
    var response =
        new GetAccountOwnerFragmentAliasedOp_GQlClientResp_ImmutJson(
                postGraphQl(GetAccountOwnerFragmentAliasedOp_SpecReq.of(variables)))
            .data();

    assertThat(response.accountAlias().ownerAlias().n().first()).isEqualTo("PRSNACC123-FirstName");
    assertThat(response.accountAlias().ownerAlias().emailAlias())
        .isEqualTo("PRSNACC123@PRSNACC123.com");
  }
}

package com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService;

import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models.JsonRequest_ImmutJson;
import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models.JsonResponse;
import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models.JsonResponse_ImmutJson;
import com.flipkart.krystal.model.array.SimpleByteArray;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

/**
 * End-to-end tests that boot the actual Quarkus REST server once per suite (port 18082), exercise
 * every vajram-backed endpoint via HTTP, and let the daemon server thread die with the forked test
 * JVM. Run with {@code ./gradlew
 * :lattice:samples:rest:json:quarkus:rest-json-quarkus-lattice-sample:test -PunsafeCompile=true}.
 */
@QuarkusTest
@TestInstance(Lifecycle.PER_CLASS)
class QuarkusRestEndpointsE2eTest {
  private static final Duration TIMEOUT = Duration.ofSeconds(1);

  @TestHTTPResource private URI baseUri;

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  @Test
  void getMapping_returnsResponseWithQueryParams() throws Exception {
    HttpResponse<CompletionStage<JsonResponse_ImmutJson>> resp =
        httpClient.send(
            HttpRequest.newBuilder(baseUri.resolve("foo/bar?name=Alisha&age=42"))
                .GET()
                .header("Accept", "application/json")
                .build(),
            BodyHandlers.fromSubscriber(
                BodySubscribers.mapping(BodySubscribers.ofByteArray(), JsonResponse_ImmutJson::new),
                BodySubscriber::getBody));
    assertThat(resp.statusCode()).isEqualTo(200);
    JsonResponse_ImmutJson respBody = assertThat(resp.body()).succeedsWithin(TIMEOUT).actual();
    assertThat(respBody.path()).isEqualTo("foo/bar");
    assertThat(respBody.qp_name()).isEqualTo("Alisha");
    assertThat(respBody.qp_age()).isEqualTo("42");
  }

  @Test
  void postMapping_returnsResponseEchoingBodyAndPath() throws Exception {
    var body =
        JsonRequest_ImmutJson._builder()
            .mandatoryInput(7)
            .mandatoryLongInput(99L)
            .defaultByteString(SimpleByteArray.copyOf("\0".getBytes(StandardCharsets.UTF_8)))
            ._build();

    HttpResponse<CompletionStage<JsonResponse_ImmutJson>> resp =
        httpClient.send(
            HttpRequest.newBuilder(baseUri.resolve("foo/bar"))
                .POST(BodyPublishers.ofByteArray(body._serialize().readAllBytes()))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build(),
            BodyHandlers.fromSubscriber(
                BodySubscribers.mapping(BodySubscribers.ofByteArray(), JsonResponse_ImmutJson::new),
                BodySubscriber::getBody));
    assertThat(resp.statusCode()).isEqualTo(200);
    var stringInResp =
        assertThat(resp.body())
            .succeedsWithin(TIMEOUT)
            .extracting(JsonResponse::string)
            .asInstanceOf(STRING);
    stringInResp.contains("PATH: foo/bar");
    stringInResp.contains("mandatoryInput: 7");
    stringInResp.contains("mandatoryLongInput: 99");
  }

  @Test
  void headMapping_returns200WithoutBody() throws Exception {
    HttpResponse<String> resp =
        httpClient.send(
            HttpRequest.newBuilder(baseUri.resolve("foo/bar?name=Alisha&age=42"))
                .method("HEAD", BodyPublishers.noBody())
                .build(),
            ofString());
    assertThat(resp.statusCode()).isEqualTo(200);
    assertThat(resp.body()).isEmpty();
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
            HttpRequest.newBuilder(baseUri.resolve("complex/ctxA/path/nameA/p1/p2/p3/123"))
                .POST(BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build(),
            ofString());
    assertThat(resp.statusCode()).isEqualTo(200);
    assertThat(resp.body()).contains("context: ctxA");
    assertThat(resp.body()).contains("name: nameA");
    assertThat(resp.body()).contains("threePaths: p1/p2/p3");
    assertThat(resp.body()).contains("id: 123");
  }
}

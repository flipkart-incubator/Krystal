package com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService;

import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.models.ForyRequest_ImmutFory;
import com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.models.ForyResponse_ImmutFory;
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
 * End-to-end tests that boot the Quarkus REST server on port 18083, exercise Fory-backed endpoints
 * via HTTP, and verify serialization/deserialization round-trips. Run with {@code ./gradlew
 * :lattice:samples:rest:fory:quarkus:rest-fory-quarkus-lattice-sample:test -PunsafeCompile=true}.
 */
@QuarkusTest
@TestInstance(Lifecycle.PER_CLASS)
class ForyQuarkusE2eTest {

  @TestHTTPResource private URI baseUri;

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  @Test
  void getMapping_returnsResponseWithQueryParams() throws Exception {
    HttpResponse<byte[]> resp =
        httpClient.send(
            HttpRequest.newBuilder(baseUri.resolve("foo/bar?name=Alisha&age=42"))
                .GET()
                .header("Accept", "application/x-fory")
                .build(),
            BodyHandlers.ofByteArray());
    assertThat(resp.statusCode()).isEqualTo(200);

    ForyResponse_ImmutFory response = new ForyResponse_ImmutFory(resp.body());
    assertThat(response.path()).isEqualTo("foo/bar");
    assertThat(response.queryName()).isEqualTo("Alisha");
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
            HttpRequest.newBuilder(baseUri.resolve("/foo/bar"))
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
}

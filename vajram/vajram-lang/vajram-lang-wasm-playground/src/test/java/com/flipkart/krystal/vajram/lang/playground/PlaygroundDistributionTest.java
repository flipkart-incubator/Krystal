package com.flipkart.krystal.vajram.lang.playground;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlaygroundDistributionTest {

  @Test
  void bundlesTheUiSpecificationAndAllSampleCategories() throws Exception {
    for (String resource :
        List.of(
            "web/index.html",
            "web/app.js",
            "web/app.css",
            "VAJRAM_LANGUAGE_SPEC.md",
            "samples/basic-dependency.vajram",
            "samples/fanout.vajram",
            "samples/async.vajram",
            "samples/file-picker.vajram",
            "samples/http-client.vajram")) {
      try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
        assertThat(input).as(resource).isNotNull();
        assertThat(new String(input.readAllBytes(), StandardCharsets.UTF_8)).isNotBlank();
      }
    }
  }
}

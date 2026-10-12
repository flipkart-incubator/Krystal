package com.flipkart.krystal.vajram.lang.samples;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VajramSamplesTest {

  private static final Path GENERATED_RUST = Path.of("build/generated-rust");

  @BeforeAll
  void buildGeneratedSamples() throws Exception {
    run("cargo", "build");
  }

  @Test
  void helloWorld() throws Exception {
    assertThat(runVajram("helloWorld")).isEqualTo("Hello from vajram-lang!");
  }

  @Test
  void helloWorld2() throws Exception {
    assertThat(runVajram("helloWorld2", "--name", "Mister"))
        .isEqualTo("Hello again from vajram-lang, Mister!");
  }

  @Test
  void headFile() throws Exception {
    Path file = Files.createTempFile("vajram-head-file-", ".txt");
    try {
      Files.writeString(file, "hello cafe");
      assertThat(runVajram("headFile", "--numChars", "5", "--filePath", file.toString()))
          .isEqualTo("hello");
    } finally {
      Files.deleteIfExists(file);
    }
  }

  @Test
  void multiHeadFiles() throws Exception {
    Path file = Files.createTempFile("vajram-multi-head-", ".txt");
    try {
      Files.writeString(file, "hello cafe");
      assertThat(
              runVajram("multiHeadFiles", "--separator", "|", "--filePath", file.toString()))
          .isEqualTo("hello cafe|hello cafe");
    } finally {
      Files.deleteIfExists(file);
    }
  }

  @Test
  void twoHeadFiles() throws Exception {
    Path first = Files.createTempFile("vajram-two-head-first-", ".txt");
    Path second = Files.createTempFile("vajram-two-head-second-", ".txt");
    try {
      Files.writeString(first, "hello");
      Files.writeString(second, "cafe");
      assertThat(
              runVajram(
                  "twoHeadFiles",
                  "--separator",
                  "|",
                  "--filePath1",
                  first.toString(),
                  "--filePath2",
                  second.toString()))
          .isEqualTo("hello|cafe");
    } finally {
      Files.deleteIfExists(first);
      Files.deleteIfExists(second);
    }
  }

  // ponytail: hits the live api.weather.gov API, so these only assert on the stable response
  // shape (not exact content) and can flake on network/API outages. Upgrade: stub callHttp.
  @Test
  void activeWeatherAlerts() throws Exception {
    assertThat(run("target/debug/vajram-lang-samples", "activeWeatherAlerts"))
        .contains("\"type\": \"FeatureCollection\"");
  }

  @Test
  void activeWeatherAlertsCount() throws Exception {
    assertThat(run("target/debug/vajram-lang-samples", "activeWeatherAlertsCount"))
        .contains("\"total\"");
  }

  private static String runVajram(String vajram, String... arguments) throws Exception {
    String[] command = new String[arguments.length + 2];
    command[0] = "target/debug/vajram-lang-samples";
    command[1] = vajram;
    System.arraycopy(arguments, 0, command, 2, arguments.length);
    return run(command).trim().lines().reduce((ignored, last) -> last).orElse("");
  }

  private static String run(String... command) throws Exception {
    Process process =
        new ProcessBuilder(command)
            .directory(GENERATED_RUST.toFile())
            .redirectErrorStream(true)
            .start();
    String output = new String(process.getInputStream().readAllBytes());
    assertThat(process.waitFor()).withFailMessage(output).isEqualTo(0);
    return output;
  }
}

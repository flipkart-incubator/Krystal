package com.flipkart.krystal.vajram.lang.samples;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
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
    assertEquals("Hello from vajram-lang!", runVajram("helloWorld"));
  }

  @Test
  void helloWorld2() throws Exception {
    assertEquals("Hello again from vajram-lang!", runVajram("helloWorld2"));
  }

  @Test
  void headFile() throws Exception {
    Path file = Files.createTempFile("vajram-head-file-", ".txt");
    try {
      Files.writeString(file, "hello cafe");
      assertEquals(
          "hello", runVajram("headFile", "--numChars", "5", "--filePath", file.toString()));
    } finally {
      Files.deleteIfExists(file);
    }
  }

  @Test
  void multiHeadFiles() throws Exception {
    Path file = Files.createTempFile("vajram-multi-head-", ".txt");
    try {
      Files.writeString(file, "hello cafe");
      assertEquals(
          "hello cafe|hello cafe",
          runVajram("multiHeadFiles", "--separator", "|", "--filePath", file.toString()));
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
      assertEquals(
          "hello|cafe",
          runVajram(
              "twoHeadFiles",
              "--separator",
              "|",
              "--filePath1",
              first.toString(),
              "--filePath2",
              second.toString()));
    } finally {
      Files.deleteIfExists(first);
      Files.deleteIfExists(second);
    }
  }

  private static String runVajram(String vajram, String... arguments) throws Exception {
    String[] command = new String[arguments.length + 2];
    command[0] = "target/debug/vajram-lang-samples";
    command[1] = vajram;
    System.arraycopy(arguments, 0, command, 2, arguments.length);
    return run(command).trim();
  }

  private static String run(String... command) throws Exception {
    Process process =
        new ProcessBuilder(command)
            .directory(GENERATED_RUST.toFile())
            .redirectErrorStream(true)
            .start();
    String output = new String(process.getInputStream().readAllBytes());
    assertEquals(0, process.waitFor(), output);
    return output;
  }
}

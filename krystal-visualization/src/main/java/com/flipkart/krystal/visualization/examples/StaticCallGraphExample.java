package com.flipkart.krystal.visualization.examples;

import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.visualization.StaticCallGraphGenerator;
import com.flipkart.krystal.visualization.models.GraphGenerationResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Example class demonstrating how to generate a static call graph visualization using sample
 * Vajrams from the codebase.
 */
public class StaticCallGraphExample {
  private static final String outputPath = "krystal-visualization/src/main/java/com/flipkart/krystal/visualization/examples";

  /**
   * Call this method to generate a call graph visualization using calculator sample Vajrams.
   *
   * @throws IOException If there's an error writing the file
   */
  private static void generateCallGraphFromCalculatorSamples()
      throws IOException, ClassNotFoundException {

    try (VajramKryonGraph vajramKryonGraph =
        VajramKryonGraph.builder()
            // Load all Vajrams from the calculator package and its sub-packages
            .loadFromPackage("com.flipkart.krystal.vajram.samples.calculator")
            .build()) {

      GraphGenerationResult result =
          StaticCallGraphGenerator.generateStaticCallGraphContent(vajramKryonGraph, "A2MinusB2");

      StaticCallGraphGenerator.generateStaticCallGraphFile(
          vajramKryonGraph, "NonExistentVajram", outputPath);

      String htmlContent = result.getHtml();
      String filename = result.getFileName();

      Path outputDir = Paths.get(outputPath);
      Files.createDirectories(outputDir);

      Path outputFile = outputDir.resolve(filename);

      Files.writeString(
          outputFile, htmlContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
  }
}

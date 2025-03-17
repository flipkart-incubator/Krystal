package com.flipkart.krystal.visualization;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class StaticCallGraphHtml {

  /** Loads the HTML template from the classpath. */
  private static String loadTemplate() {
    InputStream inputStream =
        StaticCallGraphHtml.class.getResourceAsStream("/templates/graph.html");
    if (inputStream == null) {
      throw new RuntimeException("Template file not found in resources/templates/graph.html");
    }
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      return reader.lines().collect(Collectors.joining("\n"));
    } catch (Exception e) {
      throw new RuntimeException("Error reading template", e);
    }
  }

  /**
   * Generates the final HTML by injecting the graph JSON data into the template.
   *
   * @param jsonGraphData the JSON string representing the graph data.
   * @return Final HTML content as a String.
   */
  public static String generateStaticCallGraphHtml(String jsonGraphData) {
    String template = loadTemplate();
    // Replace the placeholder __GRAPH_DATA__ with the actual JSON data.
    return template.replace("__GRAPH_DATA__", jsonGraphData);
  }
}

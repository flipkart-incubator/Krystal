package com.flipkart.krystal.visualization;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StaticCallGraphHtml {

  // Path Constants
  private static final String TEMPLATE_PATH = "/templates/graph.html";
  private static final String STATIC_PATH = "/static";
  private static final String JS_PATH = "/static/js";

  // Placeholders for resources
  private static final String CSS_PLACEHOLDER = "__CSS_CONTENT__";
  private static final String DATA_PLACEHOLDER = "__GRAPH_DATA__";

  // JS file placeholders
  private static final String MAIN_JS_PLACEHOLDER = "__MAIN_JS__";
  private static final String CONFIG_JS_PLACEHOLDER = "__CONFIG_JS__";
  private static final String DATA_PROCESSOR_JS_PLACEHOLDER = "__DATA_PROCESSOR_JS__";
  private static final String GRAPH_RENDERER_JS_PLACEHOLDER = "__GRAPH_RENDERER_JS__";
  private static final String NODE_CONTROLLER_JS_PLACEHOLDER = "__NODE_CONTROLLER_JS__";
  private static final String SEARCH_CONTROLLER_JS_PLACEHOLDER = "__SEARCH_CONTROLLER_JS__";
  private static final String TOOLTIP_JS_PLACEHOLDER = "__TOOLTIP_JS__";
  private static final String INTERACTION_HANDLER_JS_PLACEHOLDER = "__INTERACTION_HANDLER_JS__";

  // Pattern Constants
  private static final Pattern D3_IMPORT = Pattern.compile("import \\* as d3 from.*?;");
  private static final Pattern D3_DAG_IMPORT = Pattern.compile("import \\* as d3dag from.*?;");
  private static final Pattern JS_FILE_IMPORT =
      Pattern.compile("import \\{ .+? } from ['\"]\\./(.+?)\\.js['\"];");
  private static final Pattern EXPORT_FUNC_CLASS =
      Pattern.compile("export (class|function) (\\w+)");
  private static final Pattern EXPORT_CONST = Pattern.compile("export const ([\\w_]+) =");

  /** Loads the HTML template from the classpath. */
  private static String loadTemplate() {
    InputStream inputStream = StaticCallGraphHtml.class.getResourceAsStream(TEMPLATE_PATH);
    if (inputStream == null) {
      throw new RuntimeException("Template file not found in " + TEMPLATE_PATH);
    }
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      return reader.lines().collect(Collectors.joining("\n"));
    } catch (Exception e) {
      throw new RuntimeException("Error reading template", e);
    }
  }

  /**
   * Generates the final HTML by injecting the graph JSON data and all required static resources.
   *
   * @param jsonGraphData the JSON string representing the graph data.
   * @return Final HTML content as a String with all resources inlined.
   */
  public static String generateStaticCallGraphHtml(String jsonGraphData) {
    try {
      String template = loadTemplate();

      template = template.replace(DATA_PLACEHOLDER, jsonGraphData);

      String cssContent = loadResourceContent(STATIC_PATH + "/styles.css");
      template = template.replace(CSS_PLACEHOLDER, cssContent);

      Map<String, String> jsFilePlaceholders = new HashMap<>();
      jsFilePlaceholders.put(MAIN_JS_PLACEHOLDER, JS_PATH + "/main.js");
      jsFilePlaceholders.put(CONFIG_JS_PLACEHOLDER, JS_PATH + "/config.js");
      jsFilePlaceholders.put(DATA_PROCESSOR_JS_PLACEHOLDER, JS_PATH + "/dataProcessor.js");
      jsFilePlaceholders.put(GRAPH_RENDERER_JS_PLACEHOLDER, JS_PATH + "/graphRenderer.js");
      jsFilePlaceholders.put(NODE_CONTROLLER_JS_PLACEHOLDER, JS_PATH + "/nodeController.js");
      jsFilePlaceholders.put(SEARCH_CONTROLLER_JS_PLACEHOLDER, JS_PATH + "/searchController.js");
      jsFilePlaceholders.put(TOOLTIP_JS_PLACEHOLDER, JS_PATH + "/tooltip.js");
      jsFilePlaceholders.put(
          INTERACTION_HANDLER_JS_PLACEHOLDER, JS_PATH + "/interactionHandler.js");

      for (Map.Entry<String, String> entry : jsFilePlaceholders.entrySet()) {
        String placeholder = entry.getKey();
        String filePath = entry.getValue();

        String jsContent = loadResourceContent(filePath);
        jsContent = processJsContent(jsContent);

        template = template.replace(placeholder, jsContent);
      }

      return template;
    } catch (IOException e) {
      throw new RuntimeException("Error generating HTML with inlined resources", e);
    }
  }

  /**
   * Process JavaScript content to make it work in a single file context. Removes module imports and
   * adjusts exports.
   *
   * @param jsContent The JavaScript content to process
   * @return The processed JavaScript content
   */
  private static String processJsContent(String jsContent) {
    // Remove ES module imports since they're now handled in the template
    String processed =
        JS_FILE_IMPORT
            .matcher(
                D3_DAG_IMPORT.matcher(D3_IMPORT.matcher(jsContent).replaceAll("")).replaceAll(""))
            .replaceAll("");

    // Remove export statements since we're in a single file context
    processed =
        EXPORT_CONST
            .matcher(EXPORT_FUNC_CLASS.matcher(processed).replaceAll("$1 $2"))
            .replaceAll("const $1 =");

    return processed;
  }

  /**
   * Loads a resource file content from the classpath.
   *
   * @param resourcePath The path to the resource relative to the classpath
   * @return The content of the resource as a string
   * @throws IOException If the resource cannot be read
   */
  private static String loadResourceContent(String resourcePath) throws IOException {
    InputStream inputStream = StaticCallGraphHtml.class.getResourceAsStream(resourcePath);
    if (inputStream == null) {
      throw new IOException("Resource not found: " + resourcePath);
    }
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      return reader.lines().collect(Collectors.joining("\n"));
    }
  }
}

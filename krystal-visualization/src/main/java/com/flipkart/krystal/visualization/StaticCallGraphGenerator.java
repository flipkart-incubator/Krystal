package com.flipkart.krystal.visualization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajram.facets.DependencyDef;
import com.flipkart.krystal.vajram.facets.InputDef;
import com.flipkart.krystal.vajram.facets.VajramFacetDefinition;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.visualization.models.AnnotationInfo;
import com.flipkart.krystal.visualization.models.Graph;
import com.flipkart.krystal.visualization.models.GraphGenerationResult;
import com.flipkart.krystal.visualization.models.Input;
import com.flipkart.krystal.visualization.models.Link;
import com.flipkart.krystal.visualization.models.Node;
import com.flipkart.krystal.visualization.models.VajramType;
import com.google.common.collect.ImmutableCollection;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * Generates a static call graph visualization of Vajram dependencies using D3.js. This class takes
 * a VajramKryonGraph and creates an HTML visualization showing how different Vajrams depend on each
 * other.
 */
@Slf4j
public class StaticCallGraphGenerator {

  private static final String DEFAULT_FILE_NAME = "CompleteStaticCallGraph.html";

  /**
   * Generates a static call graph from a VajramKryonGraph and returns the HTML content and
   * filename.
   *
   * <p>If the provided startVajram (a node name) exists in the graph, then only the subgraph
   * reachable from that node is visualized, and the filename is "startVajram.html". If not, the
   * entire graph is visualized and the filename is "StaticCallGraph.html".
   *
   * @param vajramKryonGraph The graph containing all Vajram definitions and their dependencies.
   * @param startVajram The starting node name to filter the graph, or null/empty for the full
   *     graph.
   * @return A GraphGenerationResult containing the HTML content and filename.
   * @throws ClassNotFoundException If a required class is not found during processing.
   */
  public static GraphGenerationResult generateStaticCallGraphHtml(
      VajramKryonGraph vajramKryonGraph, String startVajram) throws ClassNotFoundException {

    Graph fullGraph = createGraphData(vajramKryonGraph);
    Graph graphToVisualize = fullGraph;
    String outputFileName = DEFAULT_FILE_NAME;

    if (startVajram != null && !startVajram.isBlank()) {
      Node startNode =
          fullGraph.getNodes().stream()
              .filter(node -> node.getName().equals(startVajram))
              .findFirst()
              .orElse(null);
      if (startNode != null) {
        graphToVisualize = filterGraph(fullGraph, startNode.getId());
        outputFileName = startVajram + ".html";
      }
    }

    String jsonGraph = graphToJson(graphToVisualize);
    String htmlContent = StaticCallGraphHtml.generateStaticCallGraphHtml(jsonGraph);
    Map<String, String> visualizationResources = getVisualizationResourceFiles();

    return GraphGenerationResult.builder()
        .html(htmlContent)
        .fileName(outputFileName)
        .visualizationResources(visualizationResources)
        .build();
  }

  private static Map<String, String> getVisualizationResourceFiles() {
    return new HashMap<>();
  }

  /**
   * Creates the graph data structure from the VajramKryonGraph.
   *
   * @param vajramKryonGraph The graph containing all Vajram definitions.
   * @return A Graph POJO representing the graph structure.
   * @throws ClassNotFoundException if a required class is not found during processing.
   */
  private static Graph createGraphData(VajramKryonGraph vajramKryonGraph)
      throws ClassNotFoundException {
    List<Node> nodes = new ArrayList<>();
    List<Link> links = new ArrayList<>();

    Map<VajramID, VajramDefinition> vajramDefinitions = vajramKryonGraph.vajramDefinitions();

    // Create nodes.
    for (Map.Entry<VajramID, VajramDefinition> entry : vajramDefinitions.entrySet()) {
      VajramID vajramId = entry.getKey();
      VajramDefinition definition = entry.getValue();

      List<Input> inputs = new ArrayList<>();
      for (VajramFacetDefinition facet : definition.vajram().getFacetDefinitions()) {
        if (facet instanceof InputDef<?> inputDef) {
          inputs.add(
              Input.builder()
                  .name(inputDef.name())
                  .type(inputDef.type().javaReflectType().getTypeName())
                  .isMandatory(inputDef.isMandatory())
                  .documentation(inputDef.documentation())
                  .build());
        }
      }

      ImmutableCollection<Annotation> annotations = definition.vajramTags().annotations();
      List<AnnotationInfo> annotationInfoList =
          annotations.stream()
              .map(
                  annotation -> {
                    // Extract annotation attributes using reflection
                    Map<String, String> attributes = new HashMap<>();

                    for (Method method : annotation.annotationType().getDeclaredMethods()) {
                      // Annotation attributes are defined as no-arg methods
                      if (method.getParameterCount() == 0
                          && !method.isDefault()
                          && !method.getName().equals("annotationType")
                          && method.getReturnType() != void.class) {
                        try {
                          Object value = method.invoke(annotation);
                          if (value != null) {
                            Object defaultValue = method.getDefaultValue();
                            if (!value.equals(defaultValue)) {
                              // Handle arrays
                              if (value.getClass().isArray()) {
                                attributes.put(method.getName(), formatArrayValue(value));
                              } else {
                                attributes.put(method.getName(), value.toString());
                              }
                            }
                          }
                        } catch (Exception e) {
                          log.error("Error extracting annotation attribute: {}", e.getMessage());
                        }
                      }
                    }

                    return AnnotationInfo.builder()
                        .name(annotation.annotationType().getSimpleName())
                        .attributes(attributes)
                        .build();
                  })
              .toList();

      Node node =
          Node.builder()
              .id(vajramId.vajramId())
              .name(definition.vajramDefClass().getSimpleName())
              .vajramType(getVajramType(definition.vajram()))
              .inputs(inputs)
              .annotationTags(annotationInfoList)
              .build();

      nodes.add(node);
    }

    // Create links for dependencies.
    for (Map.Entry<VajramID, VajramDefinition> entry : vajramDefinitions.entrySet()) {
      VajramID vajramId = entry.getKey();
      VajramDefinition definition = entry.getValue();

      for (VajramFacetDefinition facet : definition.vajram().getFacetDefinitions()) {
        if (facet instanceof DependencyDef<?> dependencyDef) {
          VajramID dependencyId = (VajramID) dependencyDef.dataAccessSpec();
          if (vajramDefinitions.containsKey(vajramId)
              && vajramDefinitions.containsKey(dependencyId)) {
            Link link =
                Link.builder()
                    .source(vajramId.vajramId())
                    .target(dependencyId.vajramId())
                    .name(dependencyDef.name())
                    .isMandatory(dependencyDef.isMandatory())
                    .canFanout(dependencyDef.canFanout())
                    .documentation(dependencyDef.documentation())
                    .build();
            links.add(link);
          }
        }
      }
    }

    return Graph.builder().nodes(nodes).links(links).build();
  }

  private static String formatArrayValue(Object array) {
    if (array == null) {
      return "null";
    }

    Class<?> arrayClass = array.getClass();
    if (!arrayClass.isArray()) {
      return array.toString();
    }

    try {
      // Get the appropriate Arrays.toString method for this array type
      Method toStringMethod = Arrays.class.getMethod("toString", arrayClass);
      return (String) toStringMethod.invoke(null, array);
    } catch (Exception e) {
      // Fallback to default handling
      return array.toString();
    }
  }

  private static VajramType getVajramType(Vajram<?> vajram) {
    VajramType vajramType;
    if (vajram instanceof ComputeVajram) {
      vajramType = VajramType.COMPUTE;
    } else if (vajram instanceof IOVajram) {
      vajramType = VajramType.IO;
    } else {
      vajramType = VajramType.ABSTRACT;
    }
    return vajramType;
  }

  /**
   * Converts the Graph object to a JSON string.
   *
   * @param graph The Graph object.
   * @return JSON string representation.
   */
  private static String graphToJson(Graph graph) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      return objectMapper.writeValueAsString(graph);
    } catch (Exception e) {
      throw new RuntimeException("Error converting graph data to JSON", e);
    }
  }

  /**
   * Filters the provided graph to include only the nodes reachable from the given start node id.
   *
   * @param fullGraph The full graph.
   * @param startNodeId The starting node id.
   * @return A Graph that contains only the reachable nodes and corresponding links.
   */
  private static Graph filterGraph(Graph fullGraph, String startNodeId) {
    // Build an adjacency list from source -> list of target node ids.
    Map<String, List<String>> adj = new HashMap<>();
    fullGraph
        .getLinks()
        .forEach(
            link -> {
              adj.computeIfAbsent(link.getSource(), k -> new ArrayList<>()).add(link.getTarget());
            });

    // Use DFS to find all reachable nodes starting from startNodeId.
    Set<String> reachable = new HashSet<>();
    Deque<String> stack = new ArrayDeque<>();
    stack.push(startNodeId);
    while (!stack.isEmpty()) {
      String current = stack.pop();
      if (reachable.add(current)) {
        List<String> neighbors = adj.getOrDefault(current, List.of());
        neighbors.forEach(stack::push);
      }
    }

    // Filter nodes and links based on the reachable set.
    List<Node> filteredNodes =
        fullGraph.getNodes().stream().filter(node -> reachable.contains(node.getId())).toList();
    List<Link> filteredLinks =
        fullGraph.getLinks().stream()
            .filter(
                link ->
                    reachable.contains(link.getSource()) && reachable.contains(link.getTarget()))
            .toList();

    return Graph.builder().nodes(filteredNodes).links(filteredLinks).build();
  }
}

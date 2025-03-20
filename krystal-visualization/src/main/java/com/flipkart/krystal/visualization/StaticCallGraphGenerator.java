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
import com.flipkart.krystal.visualization.models.Graph;
import com.flipkart.krystal.visualization.models.GraphGenerationResult;
import com.flipkart.krystal.visualization.models.Input;
import com.flipkart.krystal.visualization.models.Link;
import com.flipkart.krystal.visualization.models.Node;
import com.flipkart.krystal.visualization.models.VajramType;
import com.google.common.collect.ImmutableCollection;
import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Generates a static call graph visualization of Vajram dependencies using D3.js. This class takes
 * a VajramKryonGraph and creates an HTML visualization showing how different Vajrams depend on each
 * other.
 */
@Slf4j
public class StaticCallGraphGenerator {

  /**
   * Generates a static call graph from a VajramKryonGraph and returns the HTML content and
   * filename.
   *
   * <p>If the provided startVajram (a node name) exists in the graph, then only the subgraph
   * reachable from that node is visualized. If not, the entire graph is visualized.
   *
   * <p>The generated HTML is self-contained with all CSS and JavaScript embedded
   *
   * @param vajramKryonGraph The graph containing all Vajram definitions and their dependencies.
   * @param startVajram The starting node name to filter the graph, or null/empty for the full
   *     graph.
   * @return A GraphGenerationResult containing the self-contained HTML content.
   * @throws ClassNotFoundException If a required class is not found during processing.
   */
  public static GraphGenerationResult generateStaticCallGraphContent(
      VajramKryonGraph vajramKryonGraph, @Nullable String startVajram)
      throws ClassNotFoundException {

    Graph fullGraph = createGraphData(vajramKryonGraph);
    Graph graphToVisualize = fullGraph;

    if (startVajram != null && !startVajram.isBlank()) {
      Node startNode =
          fullGraph.getNodes().stream()
              .filter(node -> node.getName().equals(startVajram))
              .findFirst()
              .orElse(null);
      if (startNode != null) {
        graphToVisualize = filterGraph(fullGraph, startNode.getId());
      } else {
        throw new IllegalArgumentException("Start vajram: " + startVajram + " does not exist");
      }
    }

    String jsonGraph = graphToJson(graphToVisualize);
    String htmlContent = StaticCallGraphHtml.generateStaticCallGraphHtml(jsonGraph);

    return GraphGenerationResult.builder().html(htmlContent).build();
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

    // Create nodes
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

      List<String> annotationStringList = annotations.stream().map(Annotation::toString).toList();

      VajramType vajramType = getVajramType(definition.vajram());
      if (vajramType == VajramType.UNKNOWN) {
        throw new IllegalArgumentException("Unknown vajram type for: " + definition.vajram());
      }

      Node node =
          Node.builder()
              .id(vajramId.vajramId())
              .name(definition.vajramDefClass().getSimpleName())
              .vajramType(vajramType)
              .inputs(inputs)
              .annotationTags(annotationStringList)
              .build();

      nodes.add(node);
    }

    // Create links for dependencies
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

  private static VajramType getVajramType(Vajram<?> vajram) {
    VajramType vajramType;
    if (vajram instanceof ComputeVajram) {
      vajramType = VajramType.COMPUTE;
    } else if (vajram instanceof IOVajram) {
      vajramType = VajramType.IO;
    } else {
      vajramType = VajramType.UNKNOWN;
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
    // Build an adjacency list from source -> list of target node ids
    Map<String, List<String>> adj = new HashMap<>();
    fullGraph
        .getLinks()
        .forEach(
            link -> {
              adj.computeIfAbsent(link.getSource(), k -> new ArrayList<>()).add(link.getTarget());
            });

    // Use DFS to find all reachable nodes starting from startNodeId
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

    // Filter nodes and links based on the reachable set
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

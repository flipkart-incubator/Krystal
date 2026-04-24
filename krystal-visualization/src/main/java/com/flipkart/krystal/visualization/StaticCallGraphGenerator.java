package com.flipkart.krystal.visualization;

import static com.flipkart.krystal.visualization.StaticCallGraphHtml.generateStaticCallGraphHtml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.InputMirror;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.traits.StaticDispatchPolicy;
import com.flipkart.krystal.traits.TraitDispatchPolicy;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.VajramDefRoot;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajram.facets.specs.DependencySpec;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexGraph;
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
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
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

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * Generates a static call graph from a VajramKryonGraph and returns the HTML content and
   * filename.
   *
   * <p>If the provided startVajram (a node name) exists in the graph, then only the subgraph
   * reachable from that node is visualized. If not, the entire graph is visualized.
   *
   * <p>The generated HTML is self-contained with all CSS and JavaScript embedded
   *
   * @param vajramGraph The graph containing all Vajram definitions and their dependencies.
   * @param startVajram The starting node name to filter the graph, or null/empty for the full
   *     graph.
   * @return A GraphGenerationResult containing the self-contained HTML content.
   */
  public static GraphGenerationResult generateStaticCallGraphContent(
      KrystexGraph vajramGraph, @Nullable String startVajram) {

    Graph fullGraph = createGraphData(vajramGraph);
    Graph graphToVisualize = fullGraph;

    if (startVajram != null && !startVajram.isBlank()) {
      Node startNode =
          fullGraph.nodes().stream()
              .filter(node -> node.name().equals(startVajram))
              .findFirst()
              .orElse(null);
      if (startNode != null) {
        graphToVisualize = filterGraph(fullGraph, startNode.id());
      } else {
        throw new IllegalArgumentException("Start vajram: " + startVajram + " does not exist");
      }
    }

    String jsonGraph = graphToJson(graphToVisualize);
    String htmlContent = generateStaticCallGraphHtml(jsonGraph);

    return GraphGenerationResult.builder().html(htmlContent).build();
  }

  /**
   * Creates the graph data structure from the VajramKryonGraph.
   *
   * @param krystexGraph The graph containing all Vajram definitions.
   * @return A Graph POJO representing the graph structure.
   * @throws ClassNotFoundException if a required class is not found during processing.
   */
  private static Graph createGraphData(KrystexGraph krystexGraph) {
    List<Node> nodes = new ArrayList<>();
    List<Link> links = new ArrayList<>();

    Map<VajramID, VajramDefinition> vajramDefinitions =
        krystexGraph.vajramGraph().vajramDefinitions();

    // Phase 1: Pre-compute static dispatch resolutions.
    // For each static dispatch trait, group dependencies by their resolved dispatch target.
    // traitID -> (targetID -> list of dependency specs resolving to that target)
    Map<VajramID, Map<VajramID, List<DependencySpec<?, ?, ?>>>> staticDispatchMap = new HashMap<>();
    // Maps each DependencySpec (on a static dispatch trait) to the variant node ID it should
    // connect to
    Map<DependencySpec<?, ?, ?>, String> depSpecToVariantNodeId = new IdentityHashMap<>();

    for (Map.Entry<VajramID, VajramDefinition> entry : vajramDefinitions.entrySet()) {
      VajramID traitId = entry.getKey();
      VajramDefinition definition = entry.getValue();
      if (!definition.isTrait()) {
        continue;
      }
      TraitDispatchPolicy policy = krystexGraph.getTraitDispatchPolicy(traitId);
      if (!(policy instanceof StaticDispatchPolicy staticPolicy)) {
        continue;
      }
      Map<VajramID, List<DependencySpec<?, ?, ?>>> targetToDepSpecs = new LinkedHashMap<>();
      for (VajramDefinition depDef : vajramDefinitions.values()) {
        for (FacetSpec<?, ?> facet : depDef.facetSpecs()) {
          if (facet instanceof DependencySpec<?, ?, ?> depSpec
              && depSpec.onVajramID().equals(traitId)) {
            VajramID target = staticPolicy.getDispatchTargetID(depSpec);
            if (target != null) {
              targetToDepSpecs.computeIfAbsent(target, k -> new ArrayList<>()).add(depSpec);
              depSpecToVariantNodeId.put(depSpec, variantNodeId(traitId, target));
            }
          }
        }
      }
      if (!targetToDepSpecs.isEmpty()) {
        staticDispatchMap.put(traitId, targetToDepSpecs);
      }
    }

    // Phase 2: Create nodes
    for (Map.Entry<VajramID, VajramDefinition> entry : vajramDefinitions.entrySet()) {
      VajramID vajramId = entry.getKey();
      VajramDefinition definition = entry.getValue();

      List<Input> inputs = new ArrayList<>();
      for (InputMirror facet : definition.inputMirrors()) {
        String typeName;
        try {
          typeName = facet.type().javaReflectType().getTypeName();
        } catch (ClassNotFoundException e) {
          typeName = "<Unknown Type>";
        }
        inputs.add(
            Input.builder()
                .name(facet.name())
                .type(typeName)
                .isMandatory(
                    !facet
                        .tags()
                        .getAnnotationByType(IfAbsent.class)
                        .map(mandatory -> mandatory.value().usePlatformDefault())
                        .orElse(false))
                .documentation(facet.documentation())
                .build());
      }

      ImmutableCollection<Annotation> annotations = definition.vajramTags().annotations();

      List<String> annotationStringList = annotations.stream().map(Annotation::toString).toList();

      VajramType vajramType = getVajramType(definition);
      if (vajramType == VajramType.UNKNOWN) {
        throw new IllegalArgumentException("Unknown vajram type for: " + definition.def());
      }

      if (staticDispatchMap.containsKey(vajramId)) {
        // For static dispatch traits, create one node per dispatch target variant
        String traitName = definition.defType().getSimpleName();
        for (VajramID targetId : staticDispatchMap.get(vajramId).keySet()) {
          VajramDefinition targetDef = vajramDefinitions.get(targetId);
          String targetName =
              targetDef != null ? targetDef.defType().getSimpleName() : targetId.id();
          nodes.add(
              Node.builder()
                  .id(variantNodeId(vajramId, targetId))
                  .name(traitName + " \u2192 " + targetName)
                  .vajramType(vajramType)
                  .inputs(inputs)
                  .annotationTags(annotationStringList)
                  .build());
        }
      } else {
        nodes.add(
            Node.builder()
                .id(vajramId.id())
                .name(definition.defType().getSimpleName())
                .vajramType(vajramType)
                .inputs(inputs)
                .annotationTags(annotationStringList)
                .build());
      }
    }

    // Phase 3: Create links
    for (Map.Entry<VajramID, VajramDefinition> entry : vajramDefinitions.entrySet()) {
      VajramID vajramId = entry.getKey();
      VajramDefinition definition = entry.getValue();

      for (FacetSpec<?, ?> facet : definition.facetSpecs()) {
        if (facet instanceof DependencySpec<?, ?, ?> dependencySpec) {
          VajramID dependencyId = dependencySpec.onVajramID();
          if (vajramDefinitions.containsKey(vajramId)
              && vajramDefinitions.containsKey(dependencyId)) {
            // If this dependency is on a static dispatch trait, point to the variant node
            String targetNodeId =
                depSpecToVariantNodeId.getOrDefault(dependencySpec, dependencyId.id());
            links.add(
                Link.builder()
                    .source(vajramId.id())
                    .target(targetNodeId)
                    .name(facet.name())
                    .isMandatory(facet.isMandatoryOnServer())
                    .canFanout(facet.canFanout())
                    .documentation(facet.documentation())
                    .build());
          }
        }
      }
      // Create links from trait to its conforming dispatch targets
      if (definition.isTrait()) {
        if (staticDispatchMap.containsKey(vajramId)) {
          // For static dispatch, each variant node connects to its specific target
          for (VajramID targetId : staticDispatchMap.get(vajramId).keySet()) {
            links.add(
                Link.builder()
                    .source(variantNodeId(vajramId, targetId))
                    .target(targetId.id())
                    .name("<static dispatch>")
                    .isMandatory(false)
                    .canFanout(false)
                    .documentation("Trait dispatch")
                    .build());
          }
        } else {
          TraitDispatchPolicy traitDispatchPolicy = krystexGraph.getTraitDispatchPolicy(vajramId);
          if (traitDispatchPolicy != null) {
            // For dynamic dispatch, show all possible dispatch targets
            for (VajramID conformant : traitDispatchPolicy.dispatchTargetIDs()) {
              links.add(
                  Link.builder()
                      .source(vajramId.id())
                      .target(conformant.id())
                      .name("<dynamic dispatch>")
                      .isMandatory(false)
                      .canFanout(false)
                      .documentation("Trait dispatch")
                      .build());
            }
          }
        }
      }
    }

    return Graph.builder().nodes(nodes).links(links).build();
  }

  private static String variantNodeId(VajramID traitId, VajramID targetId) {
    return traitId.id() + "__to__" + targetId.id();
  }

  private static VajramType getVajramType(VajramDefinition vajramDef) {
    VajramType vajramType;
    VajramDefRoot<Object> vajram = vajramDef.def();
    if (vajram instanceof ComputeVajramDef<Object>) {
      vajramType = VajramType.COMPUTE;
    } else if (vajram instanceof IOVajramDef<Object>) {
      vajramType = VajramType.IO;
    } else if (vajramDef.isTrait()) {
      // TODO: https://github.com/flipkart-incubator/Krystal/issues/355
      vajramType = VajramType.COMPUTE;
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
      return OBJECT_MAPPER.writeValueAsString(graph);
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
        .links()
        .forEach(
            link -> adj.computeIfAbsent(link.source(), k -> new ArrayList<>()).add(link.target()));

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
        fullGraph.nodes().stream().filter(node -> reachable.contains(node.id())).toList();
    List<Link> filteredLinks =
        fullGraph.links().stream()
            .filter(link -> reachable.contains(link.source()) && reachable.contains(link.target()))
            .toList();

    return Graph.builder().nodes(filteredNodes).links(filteredLinks).build();
  }
}

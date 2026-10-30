package com.flipkart.krystal.vajram.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FacetDependencyGraphTest {

  @Test
  void findCycle_acyclicGraph_returnsEmpty() {
    FacetDependencyGraph graph = new FacetDependencyGraph();
    graph.addDependencies("a", List.of("b"));
    graph.addDependencies("b", List.of("c"));
    graph.addFacet("c");

    assertThat(graph.findCycle()).isEmpty();
  }

  @Test
  void findCycle_returnsOnlyTheCycle_withoutTheAcyclicPrefix() {
    FacetDependencyGraph graph = new FacetDependencyGraph();
    graph.addDependencies("a", List.of("b"));
    graph.addDependencies("b", List.of("c"));
    graph.addDependencies("c", List.of("b"));

    assertThat(graph.findCycle()).contains(List.of("b", "c", "b"));
  }

  @Test
  void findCycle_selfDependency() {
    FacetDependencyGraph graph = new FacetDependencyGraph();
    graph.addDependencies("a", List.of("a"));

    assertThat(graph.findCycle()).contains(List.of("a", "a"));
  }

  @Test
  void findDependencyOn_findsTransitiveDependents_exceptAllowedOnes() {
    FacetDependencyGraph graph = new FacetDependencyGraph();
    graph.addDependencies("x", List.of("y"));
    graph.addDependencies("y", List.of("target"));
    graph.addDependencies("allowed", List.of("target"));
    graph.addFacet("target");

    assertThat(graph.findDependencyOn(Set.of("target"), Set.of("allowed", "target")))
        .contains(new FacetDependencyGraph.Dependency("x", "target"));
  }
}

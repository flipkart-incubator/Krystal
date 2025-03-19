package com.flipkart.krystal.vajram.samples;

import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.visualization.StaticCallGraphGenerator;
import com.flipkart.krystal.visualization.models.GraphGenerationResult;
import org.junit.jupiter.api.Test;

class StaticCallGraphTest {

  @Test
  void testStaticCallGraphContainsGivenStartVajram() throws Exception {
    try (VajramKryonGraph vajramKryonGraph =
        VajramKryonGraph.builder()
            .loadFromPackage("com.flipkart.krystal.vajram.samples.calculator")
            .build()) {

      // Generate static call graph for A2MinusB2
      GraphGenerationResult result =
          StaticCallGraphGenerator.generateStaticCallGraphContent(vajramKryonGraph, "A2MinusB2");

      String htmlContent = result.getHtml();

      // Check if the HTML contains the main Vajram name
      assertThat(htmlContent)
          .as("HTML should contain the A2MinusB2 Vajram name")
          .contains("A2MinusB2");

      assertThat(htmlContent)
          .as("HTML should contain Subtractor as a dependency")
          .contains("Subtractor");

      assertThat(htmlContent)
          .as("HTML should contain Multiplier as a dependency")
          .contains("Multiplier");

      // Verify that a non-existent Vajram is not present in the HTML
      assertThat(htmlContent)
          .as("HTML should not contain NonExistentVajram")
          .doesNotContain("NonExistentVajram");

      // Check for UI elements and functionality in the HTML
      assertThat(htmlContent)
          .as("HTML should contain search functionality")
          .contains("search-container");

      assertThat(htmlContent)
          .as("HTML should contain Collapse All functionality")
          .contains("collapseAll");

      assertThat(htmlContent)
          .as("HTML should contain Expand All functionality")
          .contains("expandAll");

      // Check for node type indicators
      assertThat(htmlContent).as("HTML should indicate COMPUTE type Vajrams").contains("COMPUTE");

      assertThat(htmlContent).as("HTML should indicate IO type Vajrams").contains("IO");

      // Check for SVG and graph rendering elements
      assertThat(htmlContent).as("HTML should contain SVG elements for the graph").contains("<svg");

      assertThat(htmlContent).as("HTML should support zoom functionality").contains("zoom");
    }
  }

  @Test
  void testCompleteStaticCallGraph() throws Exception {

    try (VajramKryonGraph vajramKryonGraph =
        VajramKryonGraph.builder()
            .loadFromPackage("com.flipkart.krystal.vajram.samples.calculator")
            .build()) {

      // Generate complete static call graph (no specific Vajram filter)
      GraphGenerationResult result =
          StaticCallGraphGenerator.generateStaticCallGraphContent(vajramKryonGraph, null);

      String htmlContent = result.getHtml();

      // Check that all expected Vajrams are present
      assertThat(htmlContent).as("Complete graph should contain A2MinusB2").contains("A2MinusB2");

      assertThat(htmlContent).as("Complete graph should contain Add2And3").contains("Add2And3");

      assertThat(htmlContent).as("Complete graph should contain AddZero").contains("AddZero");

      assertThat(htmlContent).as("Complete graph should contain ChainAdder").contains("ChainAdder");

      // Check for UI functionality in the HTML
      assertThat(htmlContent)
          .as("HTML should contain interaction handling")
          .contains("interactionHandler");

      assertThat(htmlContent)
          .as("HTML should contain node controller functionality")
          .contains("nodeController");

      assertThat(htmlContent)
          .as("HTML should contain search controller functionality")
          .contains("searchController");

      // Check for information display
      assertThat(htmlContent)
          .as("HTML should contain tooltip functionality for node details")
          .contains("tooltip");
    }
  }

  @Test
  void testEmptyGraphShowsNoVajramsMessage() throws Exception {

    try (VajramKryonGraph emptyGraph = VajramKryonGraph.builder().build()) {

      // Generate static call graph for the empty graph
      GraphGenerationResult result =
          StaticCallGraphGenerator.generateStaticCallGraphContent(emptyGraph, null);

      String htmlContent = result.getHtml();

      // Verify the HTML contains the empty state message
      assertThat(htmlContent)
          .as("HTML should contain empty state message element")
          .contains("id=\"emptyStateMessage\"");

      // Verify the empty state message text
      assertThat(htmlContent)
          .as("HTML should contain 'No Vajrams found' message")
          .contains("No Vajrams found in this graph");

      // Verify that the graph data object should be empty
      assertThat(htmlContent)
          .as("HTML should contain an empty nodes array in the graphData")
          .contains("graphData = {\"nodes\":[],\"links\":[]}");
    }
  }
}

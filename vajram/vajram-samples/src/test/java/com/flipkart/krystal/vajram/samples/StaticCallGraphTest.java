package com.flipkart.krystal.vajram.samples;

import static org.junit.jupiter.api.Assertions.*;

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
      assertTrue(
          htmlContent.contains("A2MinusB2"), "HTML should contain the A2MinusB2 Vajram name");

      assertTrue(
          htmlContent.contains("Subtractor"), "HTML should contain Subtractor as a dependency");
      assertTrue(
          htmlContent.contains("Multiplier"), "HTML should contain Multiplier as a dependency");

      // Check for UI elements and functionality in the HTML
      assertTrue(
          htmlContent.contains("search-container"), "HTML should contain search functionality");
      assertTrue(
          htmlContent.contains("collapseAll"), "HTML should contain Collapse All functionality");
      assertTrue(htmlContent.contains("expandAll"), "HTML should contain Expand All functionality");

      // Check for node type indicators
      assertTrue(htmlContent.contains("COMPUTE"), "HTML should indicate COMPUTE type Vajrams");
      assertTrue(htmlContent.contains("IO"), "HTML should indicate IO type Vajrams");

      // Check for SVG and graph rendering elements
      assertTrue(htmlContent.contains("<svg"), "HTML should contain SVG elements for the graph");
      assertTrue(htmlContent.contains("zoom"), "HTML should support zoom functionality");
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
      assertTrue(htmlContent.contains("A2MinusB2"), "Complete graph should contain A2MinusB2");
      assertTrue(htmlContent.contains("Add2And3"), "Complete graph should contain Add2And3");
      assertTrue(htmlContent.contains("AddZero"), "Complete graph should contain AddZero");
      assertTrue(htmlContent.contains("ChainAdder"), "Complete graph should contain ChainAdder");

      // Check for UI functionality in the HTML
      assertTrue(
          htmlContent.contains("interactionHandler"), "HTML should contain interaction handling");
      assertTrue(
          htmlContent.contains("nodeController"),
          "HTML should contain node controller functionality");
      assertTrue(
          htmlContent.contains("searchController"),
          "HTML should contain search controller functionality");

      // Check for information display
      assertTrue(
          htmlContent.contains("tooltip"),
          "HTML should contain tooltip functionality for node details");
    }
  }
}

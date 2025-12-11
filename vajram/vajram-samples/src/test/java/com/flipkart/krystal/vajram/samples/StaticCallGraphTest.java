package com.flipkart.krystal.vajram.samples;

import static com.flipkart.krystal.traits.matchers.InputValueMatcher.equalsEnum;
import static com.flipkart.krystal.traits.matchers.InputValueMatcher.isAnyValue;
import static com.flipkart.krystal.traits.matchers.InputValueMatcher.isInstanceOf;
import static com.flipkart.krystal.vajram.samples.calculator.add.MultiAdd.MultiAddType.CHAIN;
import static com.flipkart.krystal.vajram.samples.calculator.add.MultiAdd.MultiAddType.SIMPLE;
import static com.flipkart.krystal.vajram.samples.calculator.add.MultiAdd.MultiAddType.SPLIT;
import static com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent.AgentType.L1;
import static com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent.AgentType.L2;
import static com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent.AgentType.L3;
import static com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent_Req.agentType_s;
import static com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent_Req.initialCommunication_s;
import static com.flipkart.krystal.vajramexecutor.krystex.traits.PredicateDispatchUtil.dispatchTrait;
import static com.flipkart.krystal.vajramexecutor.krystex.traits.PredicateDispatchUtil.when;
import static com.flipkart.krystal.visualization.StaticCallGraphGenerator.generateStaticCallGraphContent;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.vajram.guice.traitbinding.StaticDispatchPolicyImpl;
import com.flipkart.krystal.vajram.guice.traitbinding.TraitBinder;
import com.flipkart.krystal.vajram.samples.calculator.add.Add;
import com.flipkart.krystal.vajram.samples.calculator.add.AddUsingTraits;
import com.flipkart.krystal.vajram.samples.calculator.add.ChainAdd;
import com.flipkart.krystal.vajram.samples.calculator.add.ChainAdd_Req;
import com.flipkart.krystal.vajram.samples.calculator.add.MultiAdd;
import com.flipkart.krystal.vajram.samples.calculator.add.MultiAdd.MultiAddQualifier;
import com.flipkart.krystal.vajram.samples.calculator.add.MultiAdd_Req;
import com.flipkart.krystal.vajram.samples.calculator.add.SimpleAdd;
import com.flipkart.krystal.vajram.samples.calculator.add.SimpleAdd_Req;
import com.flipkart.krystal.vajram.samples.calculator.add.SplitAdd;
import com.flipkart.krystal.vajram.samples.calculator.add.SplitAdd_Req;
import com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent.Call;
import com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent.Email;
import com.flipkart.krystal.vajram.samples.customer_service.CustomerServiceAgent_Req;
import com.flipkart.krystal.vajram.samples.customer_service.DefaultCallAgent_Req;
import com.flipkart.krystal.vajram.samples.customer_service.DefaultCustomerServiceAgent_Req;
import com.flipkart.krystal.vajram.samples.customer_service.DefaultEmailAgent_Req;
import com.flipkart.krystal.vajram.samples.customer_service.L1CallAgent_Req;
import com.flipkart.krystal.vajram.samples.customer_service.L1EmailAgent_Req;
import com.flipkart.krystal.vajram.samples.customer_service.L2CallAgent_Req;
import com.flipkart.krystal.vajram.samples.customer_service.L3EmailAgent_Req;
import com.flipkart.krystal.vajramexecutor.krystex.VajramGraph;
import com.flipkart.krystal.visualization.models.GraphGenerationResult;
import org.junit.jupiter.api.Test;

class StaticCallGraphTest {

  @Test
  void testStaticCallGraphContainsGivenStartVajram() throws Exception {
    try (VajramGraph vajramGraph =
        VajramGraph.builder()
            .loadFromPackage("com.flipkart.krystal.vajram.samples.calculator")
            .build()) {

      // Generate static call graph for A2MinusB2
      GraphGenerationResult result = generateStaticCallGraphContent(vajramGraph, "A2MinusB2");

      String htmlContent = result.html();

      // Check if the HTML contains the main Vajram name
      assertThat(htmlContent)
          .as("HTML should contain the A2MinusB2 Vajram name")
          .contains("A2MinusB2");

      assertThat(htmlContent)
          .as("HTML should contain Subtract as a dependency")
          .contains("Subtract");

      assertThat(htmlContent)
          .as("HTML should contain Multiply as a dependency")
          .contains("Multiply");

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

    try (VajramGraph graph =
        VajramGraph.builder().loadFromPackage("com.flipkart.krystal.vajram.samples").build()) {
      TraitBinder traitBinder = new TraitBinder();
      traitBinder
          .bindTrait(MultiAdd_Req.class)
          .annotatedWith(MultiAddQualifier.Creator.create(SIMPLE))
          .to(SimpleAdd_Req.class);
      traitBinder
          .bindTrait(MultiAdd_Req.class)
          .annotatedWith(MultiAddQualifier.Creator.create(CHAIN))
          .to(ChainAdd_Req.class);
      traitBinder
          .bindTrait(MultiAdd_Req.class)
          .annotatedWith(MultiAddQualifier.Creator.create(SPLIT))
          .to(SplitAdd_Req.class);
      graph.registerTraitDispatchPolicies(
          new StaticDispatchPolicyImpl(
              graph, graph.getVajramIdByVajramDefType(MultiAdd.class), traitBinder),
          dispatchTrait(CustomerServiceAgent_Req.class, graph)
              .conditionally(
                  when(agentType_s, equalsEnum(L1))
                      .and(initialCommunication_s, isInstanceOf(Call.class))
                      .to(L1CallAgent_Req.class),
                  when(agentType_s, equalsEnum(L1))
                      .and(initialCommunication_s, isInstanceOf(Email.class))
                      .to(L1EmailAgent_Req.class),
                  when(agentType_s, equalsEnum(L2))
                      .and(initialCommunication_s, isInstanceOf(Call.class))
                      .to(L2CallAgent_Req.class),
                  when(agentType_s, equalsEnum(L3))
                      .and(initialCommunication_s, isInstanceOf(Email.class))
                      .to(L3EmailAgent_Req.class),
                  when(initialCommunication_s, isInstanceOf(Call.class))
                      .to(DefaultCallAgent_Req.class),
                  when(initialCommunication_s, isInstanceOf(Email.class))
                      .to(DefaultEmailAgent_Req.class),
                  // Default fallback
                  when(agentType_s, isAnyValue())
                      .and(initialCommunication_s, isAnyValue())
                      .to(DefaultCustomerServiceAgent_Req.class)));
      // Generate complete static call graph (no specific Vajram filter)
      GraphGenerationResult result = generateStaticCallGraphContent(graph, null);

      String htmlContent = result.html();
      System.out.println(htmlContent);

      // Check that all expected Vajrams are present
      assertThat(htmlContent).as("Complete graph should contain A2MinusB2").contains("A2MinusB2");

      assertThat(htmlContent).as("Complete graph should contain Add2And3").contains("Add2And3");

      assertThat(htmlContent).as("Complete graph should contain AddZero").contains("AddZero");

      assertThat(htmlContent).as("Complete graph should contain ChainAdd").contains("ChainAdd");

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
  void graphWithTraits_expandsDispatchTarget() throws Exception {
    try (VajramGraph graph =
        VajramGraph.builder()
            .loadClasses(
                AddUsingTraits.class,
                MultiAdd.class,
                SimpleAdd.class,
                ChainAdd.class,
                SplitAdd.class,
                Add.class)
            .build()) {
      TraitBinder traitBinder = new TraitBinder();
      traitBinder
          .bindTrait(MultiAdd_Req.class)
          .annotatedWith(MultiAddQualifier.Creator.create(SIMPLE))
          .to(SimpleAdd_Req.class);
      traitBinder
          .bindTrait(MultiAdd_Req.class)
          .annotatedWith(MultiAddQualifier.Creator.create(CHAIN))
          .to(ChainAdd_Req.class);
      traitBinder
          .bindTrait(MultiAdd_Req.class)
          .annotatedWith(MultiAddQualifier.Creator.create(SPLIT))
          .to(SplitAdd_Req.class);
      graph.registerTraitDispatchPolicies(
          new StaticDispatchPolicyImpl(
              graph, graph.getVajramIdByVajramDefType(MultiAdd.class), traitBinder));
      // Generate complete static call graph (no specific Vajram filter)
      GraphGenerationResult result = generateStaticCallGraphContent(graph, null);

      String htmlContent = result.html();

      // Check that all expected Vajrams are present
      assertThat(htmlContent)
          .as("Complete graph should contain A2MinusB2")
          .contains("AddUsingTraits");

      assertThat(htmlContent)
          .as("Complete graph should contain AddUsingTraits")
          .contains("AddUsingTraits");
      assertThat(htmlContent).as("Complete graph should contain MultiAdd").contains("MultiAdd");
      assertThat(htmlContent).as("Complete graph should contain SimpleAdd").contains("SimpleAdd");
      assertThat(htmlContent).as("Complete graph should contain ChainAdd").contains("ChainAdd");
      assertThat(htmlContent).as("Complete graph should contain SplitAdd").contains("SplitAdd");

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

    try (VajramGraph emptyGraph = VajramGraph.builder().build()) {

      // Generate static call graph for the empty graph
      GraphGenerationResult result = generateStaticCallGraphContent(emptyGraph, null);

      String htmlContent = result.html();

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

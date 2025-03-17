// main.js - Entry point for the graph visualization
import * as d3 from "https://cdn.skypack.dev/d3@7.8.4";
import * as d3dag from "https://cdn.skypack.dev/d3-dag@1.0.0-1";
import { processGraphData, createEdgePaths } from './dataProcessor.js';
import { NodeController } from './nodeController.js';
import { GraphRenderer } from './graphRenderer.js';
import { SearchController } from './searchController.js';
import { setupInteractionHandlers } from './interactionHandler.js';

// Initialize the application when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
  // The graphData is injected into the HTML template as a global variable.
  const graphData = window.graphData;
  
  // Process the graph data
  const { allNodes, filteredLinks, dag } = processGraphData(graphData);
  
  // Apply layout to DAG
  const sugiyama = d3dag.sugiyama()
    .nodeSize([60, 60])
    .gap([80, 120]);
  
  const { width, height } = sugiyama(dag);
  
  // Create SVG element
  const svg = d3.select("#graph-container")
    .append("svg")
    .attr("width", window.innerWidth)
    .attr("height", window.innerHeight);
  
  // Create node controller to manage node visibility and expansion state
  const nodeController = new NodeController(allNodes, filteredLinks);
  
  // Create edge paths for visualization
  const uniqueEdgePaths = createEdgePaths(filteredLinks, dag);
  
  // Create graph renderer
  const graphRenderer = new GraphRenderer(
    svg, dag, allNodes, filteredLinks, uniqueEdgePaths, nodeController
  );
  
  // Render edges and nodes
  graphRenderer.renderEdges();
  graphRenderer.renderNodes();
  
  // Set up search controller
  const searchController = new SearchController(
    svg, graphRenderer.getContainer(), nodeController
  );
  
  // Set up interaction handlers
  setupInteractionHandlers(svg, graphRenderer, nodeController);
}); 
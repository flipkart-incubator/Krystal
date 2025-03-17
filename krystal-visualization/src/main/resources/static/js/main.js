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
  const graphData = window.graphData;
  
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

  const nodeController = new NodeController(allNodes, filteredLinks);
  
  const uniqueEdgePaths = createEdgePaths(filteredLinks, dag);
  
  const graphRenderer = new GraphRenderer(
    svg, dag, allNodes, filteredLinks, uniqueEdgePaths, nodeController
  );

  graphRenderer.renderEdges();
  graphRenderer.renderNodes();
  
  const searchController = new SearchController(
    svg, graphRenderer.getContainer(), nodeController
  );
  
  setupInteractionHandlers(svg, graphRenderer, nodeController);
}); 
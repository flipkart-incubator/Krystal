// interactionHandler.js - Functions for handling user interactions with the graph
import * as d3 from "https://cdn.skypack.dev/d3@7.8.4";
import { CONFIG } from './config.js';
import { hideTooltip } from './tooltip.js';

/**
 * Set up interaction handlers for the graph visualization
 * @param {Object} svg - D3 selection of the SVG element
 * @param {Object} graphRenderer - GraphRenderer instance
 * @param {Object} nodeController - NodeController instance
 */
export function setupInteractionHandlers(svg, graphRenderer, nodeController) {
  // Global click handler to close tooltips and remove node action buttons
  svg.on("click", () => {
    hideTooltip();
    graphRenderer.getContainer().selectAll(".node-action-button").remove();
  });
  
  // Set up control buttons
  setupControlButtons(graphRenderer, nodeController);
  
  // Handle window resize
  setupWindowResize(graphRenderer);
}

/**
 * Set up control button event handlers
 * @param {Object} graphRenderer - GraphRenderer instance
 * @param {Object} nodeController - NodeController instance
 */
function setupControlButtons(graphRenderer, nodeController) {
  // Expand All button
  d3.select("#expandAll").on("click", () => {
    hideTooltip(); // Close any open tooltips
    nodeController.expandAll();
    graphRenderer.updateGraphVisibility();
  });
  
  // Contract All button
  d3.select("#contractAll").on("click", () => {
    hideTooltip(); // Close any open tooltips
    nodeController.contractAll();
    graphRenderer.updateGraphVisibility();
  });
  
  // Reset View button
  d3.select("#resetView").on("click", () => {
    graphRenderer.resetView();
  });
}

/**
 * Set up window resize handler
 * @param {Object} graphRenderer - GraphRenderer instance
 */
function setupWindowResize(graphRenderer) {
  window.addEventListener('resize', () => {
    graphRenderer.resize();
  });
} 
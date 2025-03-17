// searchController.js - Functions for search functionality
import { CONFIG } from './config.js';
import { hideTooltip } from './tooltip.js';
import * as d3 from "https://cdn.skypack.dev/d3@7.8.4";

/**
 * Handles the search functionality for the graph
 */
export class SearchController {
  /**
   * Initialize the search controller
   * @param {Object} svg - D3 selection of the SVG element
   * @param {Object} g - D3 selection of the container group
   * @param {Object} nodeController - NodeController instance 
   */
  constructor(svg, g, nodeController) {
    this.svg = svg;
    this.g = g;
    this.nodeController = nodeController;
    this.setupSearchHandlers();
  }
  
  /**
   * Sets up event handlers for search functionality
   */
  setupSearchHandlers() {
    // Remove all existing search event handlers and UI
    d3.select("#searchInput").on("input", null);
    d3.select("#searchInput").on("blur", null);
    d3.select("#searchInput").on("keydown", null);
    
    d3.select("#clearSearchBtn").on("click", () => {
      hideTooltip();
      d3.select("#searchInput").property("value", "");
      this.svg.classed("searching", false);
      this.g.selectAll(".node").classed("search-match", false);
      this.updateGraphVisibility();
    });
    
    d3.select("#searchInput").on("input", (event) => {
      const searchTerm = event.target.value.toLowerCase().trim();
      const nodeController = this.nodeController;
      
      hideTooltip();
      
      if (searchTerm.length < CONFIG.search.minLength) {
        this.svg.classed("searching", false);
        this.g.selectAll(".node").classed("search-match", false);
        return;
      }
      
      this.svg.classed("searching", true);
      
      // Mark matching vs non-matching nodes
      this.g.selectAll(".node").each(function(d) {
        const node = d3.select(this);
        const nodeText = d.data.name.toLowerCase();
        const searchableText = d.data.isDuplicate ? nodeText + " self" : nodeText;
        
        if (searchableText.includes(searchTerm)) {
          node.classed("search-match", true);
          // Make sure matching nodes are visible in case they were hidden
          if (d.data.id) {
            nodeController.visibleNodeIds.add(d.data.id);
          }
        } else {
          node.classed("search-match", false);
        }
      });
      
      this.updateGraphVisibility();
    });
    
    // Add ESC key support
    d3.select("#searchInput").on("keydown", (event) => {
      if (event.key === "Escape") {
        d3.select("#searchInput").property("value", "");
        this.svg.classed("searching", false);
        this.g.selectAll(".node").classed("search-match", false);
        this.updateGraphVisibility();
      }
    });
  }
  
  /**
   * Update graph visibility based on current search and node controller state
   */
  updateGraphVisibility() {
    const nodeController = this.nodeController;
    
    this.g.selectAll("g.node")
      .style("display", d => {
        return nodeController.visibleNodeIds.has(d.data.id) ? 
          CONFIG.display.block : CONFIG.display.none;
      })
      .each(function(d) {
        const node = d3.select(this);
        const isLeafNode = nodeController.isLeafNode(d.data.id);
        const isContracted = nodeController.explicitlyContractedNodes.has(d.data.id) && !isLeafNode;
        
        node.classed("contracted", isContracted);
        node.classed("expanded", nodeController.expandedNodes.has(d.data.id));
        node.classed("compute", d.data.vajramType === "COMPUTE");
        node.classed("io", d.data.vajramType === "IO");
        node.classed("abstract", d.data.vajramType === "ABSTRACT");
        node.classed("external-invocation", 
                     nodeController.externalInvocationAllowedNodes.has(d.data.id));
        
        node.selectAll(".node-state-indicator").remove();
      });
    
    // Update links visibility
    this.g.selectAll("path.link")
      .style("display", d => {
        const linkId = `${d.sourceId}--${d.targetId}`;
        return nodeController.visibleLinkIds.has(linkId) && 
               nodeController.visibleNodeIds.has(d.sourceId) && 
               nodeController.visibleNodeIds.has(d.targetId) ? 
                 CONFIG.display.block : CONFIG.display.none;
      });
    
    // Update link-related elements visibility
    this.g.selectAll("[data-link-id]").each(function() {
      const element = d3.select(this);
      const linkId = element.attr("data-link-id");
      const [sourceId, targetId] = linkId.split("--");
      element.style("display", 
        nodeController.visibleLinkIds.has(linkId) && 
        nodeController.visibleNodeIds.has(sourceId) && 
        nodeController.visibleNodeIds.has(targetId) ? 
          CONFIG.display.block : CONFIG.display.none);
    });
    
    this.g.selectAll(".node-action-button").remove();
  }
} 
// graphRenderer.js - Functions for rendering the graph
import { CONFIG } from './config.js';
import * as d3 from "https://cdn.skypack.dev/d3@7.8.4";
import * as d3dag from "https://cdn.skypack.dev/d3-dag@1.0.0-1";
import { showTooltip, hideTooltip } from './tooltip.js';
import { getIntersectionPoint, createEdgePaths } from './dataProcessor.js';

/**
 * GraphRenderer class for handling all graph rendering functionality
 */
export class GraphRenderer {
  /**
   * Initialize the graph renderer
   * @param {Object} svg - D3 selection of the SVG element 
   * @param {Object} dag - DAG layout object
   * @param {Array} allNodes - All graph nodes
   * @param {Array} filteredLinks - All graph links
   * @param {Array} uniqueEdgePaths - Processed edge paths 
   * @param {Object} nodeController - NodeController instance
   */
  constructor(svg, dag, allNodes, filteredLinks, uniqueEdgePaths, nodeController) {
    this.svg = svg;
    this.dag = dag;
    this.allNodes = allNodes;
    this.filteredLinks = filteredLinks;
    this.uniqueEdgePaths = uniqueEdgePaths;
    this.nodeController = nodeController;
    
    // Store a reference to this instance for use in callbacks
    const self = this;
    // Use a non-enumerable property to avoid serialization issues
    Object.defineProperty(svg.node(), '__graphRenderer', {
      value: this,
      writable: false,
      configurable: true
    });
    
    // Set dimensions
    this.windowWidth = window.innerWidth;
    this.windowHeight = window.innerHeight;
    
    // Create the SVG container inside the graph container
    this.svg
      .attr("width", this.windowWidth)
      .attr("height", this.windowHeight);
    
    // Apply layout to DAG
    const sugiyama = d3dag.sugiyama()
      .nodeSize(CONFIG.layout.nodeSize)
      .gap(CONFIG.layout.gap);
    
    const { width, height } = sugiyama(this.dag);
    this.width = width;
    this.height = height;
    
    // Invert y coordinates for a bottom-to-top layout
    this.dag.nodes().forEach(node => { node.y = height - node.y; });
    this.dag.links().forEach(link => {
      link.points = link.points.map(p => [p[0], height - p[1]]);
    });

    // Recalculate unique edge paths using the updated dag
    this.uniqueEdgePaths = createEdgePaths(this.filteredLinks, this.dag);


    // Create marker definitions and container group
    this.createDefs();
    
    // Create the main container group
    this.g = this.svg.append("g")
      .attr("transform", `translate(${this.windowWidth/2 - width/2}, ${this.windowHeight/2 - height/2})`);
    
    // Set up zoom
    this.setupZoom();
  }
  
  /**
   * Create marker definitions for arrows
   */
  createDefs() {
    const defs = this.svg.append("defs");
    
    defs.append("marker")
      .attr("id", "arrow")
      .attr("viewBox", CONFIG.marker.viewBox)
      .attr("refX", CONFIG.marker.refX)
      .attr("refY", CONFIG.marker.refY)
      .attr("markerUnits", "userSpaceOnUse")
      .attr("markerWidth", CONFIG.marker.width)
      .attr("markerHeight", CONFIG.marker.height)
      .attr("orient", "auto-start-reverse")
      .append("path")
      .attr("d", CONFIG.marker.path)
      .attr("fill", "var(--color-link-mandatory)");
    
    defs.append("marker")
      .attr("id", "arrow-fanout")
      .attr("viewBox", CONFIG.fanoutMarker.viewBox)
      .attr("refX", CONFIG.fanoutMarker.refX)
      .attr("refY", CONFIG.fanoutMarker.refY)
      .attr("markerUnits", "userSpaceOnUse")
      .attr("markerWidth", CONFIG.fanoutMarker.width)
      .attr("markerHeight", CONFIG.fanoutMarker.height)
      .attr("orient", "auto-start-reverse")
      .append("path")
      .attr("d", CONFIG.fanoutMarker.path)
      .attr("fill", "none")
      .attr("stroke", "var(--color-link-fanout-mandatory)")
      .attr("stroke-width", CONFIG.fanoutMarker.strokeWidth);
  }
  
  /**
   * Set up zooming and panning
   */
  setupZoom() {
    const zoom = d3.zoom()
      .scaleExtent(CONFIG.zoomScaleExtent)
      .on("zoom", event => { 
        this.g.attr("transform", event.transform); 
        hideTooltip(); // Close any open tooltips when panning/zooming
      });
    
    this.svg.call(zoom);
    this.zoom = zoom;
  }
  
  /**
   * Create a line generator for edges
   * @return {Function} Line generator function
   */
  createLineGenerator() {
    return d3.line()
      .curve(d3.curveCatmullRom.alpha(0.7))
      .x(d => d[0])
      .y(d => d[1]);
  }
  
  /**
   * Render edges and their labels
   */
  renderEdges() {
    const lineGenerator = this.createLineGenerator();
    const self = this; // Store reference to GraphRenderer instance
    
    // Draw edges
    this.g.selectAll("path.link")
      .data(this.uniqueEdgePaths)
      .join("path")
      .attr("class", d => {
        let className = d.edgeData.mandatory ? "link mandatory" : "link optional";
        if (d.edgeData.canFanout) { className += " canFanout"; }
        return className;
      })
      .attr("d", d => {
        const points = d.path.slice();
        
        const sourceNode = {x: points[0][0], y: points[0][1]};
        const towardSource = {x: points[1][0], y: points[1][1]};
        
        const targetNode = {x: points[points.length-1][0], y: points[points.length-1][1]};
        const towardTarget = {x: points[points.length-2][0], y: points[points.length-2][1]};
        
        const sourcePoint = getIntersectionPoint(
            sourceNode,
            towardSource,
            CONFIG.nodeWidth,
            CONFIG.nodeHeight
        );
        
        const targetPoint = getIntersectionPoint(
            targetNode,
            towardTarget,
            CONFIG.nodeWidth,
            CONFIG.nodeHeight
        );
        
        points[0] = [sourcePoint.x, sourcePoint.y];
        points[points.length-1] = [targetPoint.x, targetPoint.y];
        
        return lineGenerator(points);
      })
      .attr("marker-end", d => d.edgeData.canFanout ? "url(#arrow-fanout)" : "url(#arrow)")
      .attr("data-source", d => d.sourceId)
      .attr("data-target", d => d.targetId)
      .attr("data-edge-id", d => d.edgeData.name)
      .each(function(d) {
        if (d.edgeData.canFanout) {
          d3.select(this).append("title").text("This dependency can fan out to multiple targets");
        }
      })
      .on("click", (event, d) => {
        event.stopPropagation();
        if (d.edgeData && d.edgeData.documentation && d.edgeData.documentation.trim() !== "") {
          let info = `<strong>Dependency: ${d.edgeData.name}</strong><br/><strong>From:</strong> ${d.sourceId}<br/><strong>To:</strong> ${d.targetId}<br/><strong>Mandatory:</strong> ${d.edgeData.mandatory ? "Yes" : "No"}<br/><strong>Can Fanout:</strong> ${d.edgeData.canFanout ? "<span style='color:#3f51b5;font-weight:bold;'>Yes</span>" : "No"}<br/><br/><strong>Documentation:</strong><div class="documentation">${d.edgeData.documentation}</div>`;
          showTooltip(info, event);
        }
      });
    
    // Add labels for edges
    this.g.selectAll("text.link-label")
      .data(this.uniqueEdgePaths)
      .join("text")
      .attr("class", "link-label")
      .attr("text-anchor", "middle")
      .text(d => d.edgeData.name)
      .each(function(d) {
        try {
          const element = this; // Store reference to current DOM element
          const text = d3.select(element);
          
          // Helper function to escape CSS selector special characters
          const escapeSelector = (selector) => {
            if (!selector) return "";
            return selector.replace(/[ !"#$%&'()*+,.\/:;<=>?@\[\\\]^`{|}~]/g, '\\$&');
          };
          
          // Escape the IDs and names for CSS selectors
          const safeSourceId = escapeSelector(d.sourceId);
          const safeTargetId = escapeSelector(d.targetId);
          const safeEdgeName = escapeSelector(d.edgeData.name);
          
          // Try to find the corresponding path (with escaped selectors)
          const linkPath = self.g.select(`path.link[data-source="${safeSourceId}"][data-target="${safeTargetId}"][data-edge-id="${safeEdgeName}"]`);
          
          // Get the actual SVG path element for more accurate position calculation
          const pathNode = linkPath.node();
          if (!pathNode) {
            console.warn("Path not found for edge", d);
            return;
          }
          
          try {
            const pathLength = pathNode.getTotalLength();
            
            const labelPosition = d.totalEdges > 1 ? d.labelPosition : 0.5;
            
            const point = pathNode.getPointAtLength(pathLength * labelPosition);
            
            text
              .attr("x", point.x)
              .attr("y", point.y - 5);
            
            const textBBox = element.getBBox();
            const linkId = `${d.sourceId}--${d.targetId}`;
            
            // Insert label background rect
            d3.select(element.parentNode)
              .insert("rect", "text.link-label")
              .attr("class", "link-label-bg")
              .attr("data-link-id", linkId)
              .attr("data-edge-id", d.edgeData.name)
              .attr("x", textBBox.x - 4)
              .attr("y", textBBox.y - 2)
              .attr("width", textBBox.width + 8)
              .attr("height", textBBox.height + 4)
              .attr("rx", 3)
              .attr("ry", 3);
          } catch (pathError) {
            console.error("Error calculating path position:", pathError);
          }
        } catch (error) {
          console.error("Error rendering edge label:", error, d);
        }
      })
      .attr("data-link-id", d => `${d.sourceId}--${d.targetId}`)
      .attr("data-edge-id", d => d.edgeData.name);
    
    // Add hover effects for edges
    this.g.selectAll("path.link")
      .on("mouseenter", (event, d) => {
        this.g.selectAll(".link, .link-label, .link-label-bg, .node")
          .classed("faded", true);
        const edgeId = d.edgeData.name;
        const linkId = `${d.sourceId}--${d.targetId}`;
        
        const highlightedLink = this.g.selectAll(`path.link[data-edge-id="${edgeId}"][data-source="${d.sourceId}"][data-target="${d.targetId}"]`);
        const highlightedLabel = this.g.selectAll(`text.link-label[data-edge-id="${edgeId}"][data-link-id="${linkId}"]`);
        const highlightedBg = this.g.selectAll(`rect.link-label-bg[data-edge-id="${edgeId}"][data-link-id="${linkId}"]`);
        
        highlightedLink
          .classed("faded", false)
          .classed("highlighted", true);
        highlightedLabel
          .classed("faded", false)
          .classed("highlighted", true);
        highlightedBg
          .classed("faded", false)
          .classed("highlighted", true);
          
        highlightedBg.each(function() { 
          const parent = this.parentNode;
          parent.appendChild(this); 
        });
        highlightedLabel.each(function() { 
          const parent = this.parentNode;
          parent.appendChild(this); 
        });
        
        this.g.selectAll(`.node[data-id="${d.sourceId}"], .node[data-id="${d.targetId}"]`)
          .classed("faded", false);
      })
      .on("mouseleave", () => {
        this.g.selectAll(".link, .link-label, .link-label-bg, .node")
          .classed("faded", false)
          .classed("highlighted", false);
      });
    
    // Add hover effects for edge labels
    this.g.selectAll("text.link-label, rect.link-label-bg")
      .on("mouseenter", function(event, d) {
        try {
          const element = this;
          let sourceId, targetId, edgeName;
          
          const escapeSelector = (selector) => {
            if (!selector) return "";
            return selector.replace(/[ !"#$%&'()*+,.\/:;<=>?@\[\\\]^`{|}~]/g, '\\$&');
          };
          
          if (d3.select(element).classed("link-label")) {
            sourceId = d.sourceId;
            targetId = d.targetId;
            edgeName = d.edgeData.name;
          } else {
            const linkId = d3.select(element).attr("data-link-id");
            const edgeId = d3.select(element).attr("data-edge-id");
            if (!linkId || !edgeId) return;
            
            const parts = linkId.split("--");
            if (parts.length !== 2) return;
            
            [sourceId, targetId] = parts;
            edgeName = edgeId;
          }
          
          if (!sourceId || !targetId || !edgeName) return;
          
          const safeSourceId = escapeSelector(sourceId);
          const safeTargetId = escapeSelector(targetId);
          const safeEdgeName = escapeSelector(edgeName);
          
          self.g.selectAll(".link, .link-label, .link-label-bg, .node")
            .classed("faded", true);
          const linkId = `${sourceId}--${targetId}`;
          const safeLinkId = escapeSelector(linkId);
          
          const highlightedLink = self.g.selectAll(`path.link[data-edge-id="${safeEdgeName}"][data-source="${safeSourceId}"][data-target="${safeTargetId}"]`);
          const highlightedLabel = self.g.selectAll(`text.link-label[data-edge-id="${safeEdgeName}"][data-link-id="${safeLinkId}"]`);
          const highlightedBg = self.g.selectAll(`rect.link-label-bg[data-edge-id="${safeEdgeName}"][data-link-id="${safeLinkId}"]`);
          
          highlightedLink
            .classed("faded", false)
            .classed("highlighted", true);
          highlightedLabel
            .classed("faded", false)
            .classed("highlighted", true);
          highlightedBg
            .classed("faded", false)
            .classed("highlighted", true);
            
          highlightedBg.each(function() { 
            const parent = this.parentNode;
            parent.appendChild(this); 
          });
          highlightedLabel.each(function() { 
            const parent = this.parentNode;
            parent.appendChild(this); 
          });
          
          self.g.selectAll(`.node[data-id="${safeSourceId}"], .node[data-id="${safeTargetId}"]`)
            .classed("faded", false);
        } catch (error) {
          console.error("Error in edge label hover effect:", error);
          self.g.selectAll(".link, .link-label, .link-label-bg, .node")
            .classed("faded", false)
            .classed("highlighted", false);
        }
      })
      .on("mouseleave", () => {
        this.g.selectAll(".link, .link-label, .link-label-bg, .node")
          .classed("faded", false)
          .classed("highlighted", false);
      });
  }
  
  /**
   * Render nodes with their labels and input badges
   */
  renderNodes() {
    // Create node groups
    const nodeG = this.g.selectAll("g.node")
      .data(this.dag.nodes())
      .join("g")
      .attr("class", d => {
        let className = "node";
        const isLeafNode = this.nodeController.isLeafNode(d.data.id);
        if (this.nodeController.explicitlyContractedNodes.has(d.data.id) && !isLeafNode) {
          className += " contracted";
        }
        if (this.nodeController.expandedNodes.has(d.data.id)) {
          className += " expanded";
        }
        if (d.data.isDuplicate) {
          className += " duplicate";
        }
        if (d.data.vajramType === "COMPUTE") className += " compute";
        else if (d.data.vajramType === "IO") className += " io";
        else if (d.data.vajramType === "ABSTRACT") className += " abstract";
        return className;
      })
      .attr("transform", d => `translate(${d.x},${d.y})`)
      .attr("data-id", d => d.data.id)
      .on("click", (event, d) => this.handleNodeClick(event, d));
    
    // Draw node shapes
    nodeG.append("rect")
      .attr("class", "node-shape")
      .attr("width", CONFIG.nodeWidth)
      .attr("height", CONFIG.nodeHeight)
      .attr("x", -CONFIG.nodeWidth/2)
      .attr("y", -CONFIG.nodeHeight/2)
      .attr("rx", CONFIG.nodeRadius)
      .attr("ry", CONFIG.nodeRadius);
    
    // Add input badge for nodes with inputs
    nodeG.each((d) => this.addInputBadge(d));
    
    // Add text labels for nodes with wrapping
    nodeG.append("text")
      .attr("text-anchor", "middle")
      .attr("dominant-baseline", "central")
      .style("font-size", `${CONFIG.text.fontSize}px`)
      .style("font-weight", CONFIG.text.fontWeight)
      .style("fill", "var(--color-text-primary)")
      .text(d => d.data.name)
      .each(function(d) {
        const text = d3.select(this);
        let nodeName = d.data.name;
        
        // For duplicate nodes, we'll display the name with "(Self)" on a new line
        const isDuplicate = d.data.isDuplicate;
        
        // Clear the current text
        text.text('');
        
        const words = nodeName.split(/\s+/);
        const lineHeight = CONFIG.text.lineHeight;
        let tspans = [];
        
        if (words.length > 1 || nodeName.length > CONFIG.text.maxNameLength) {
          if (words.length === 1) {
            const word = words[0];
            if (word.length <= CONFIG.text.maxWordLength) {
              tspans.push(text.append("tspan")
                .attr("x", 0)
                .attr("dy", 0)
                .text(word));
            } else {
              const breakpoints = ['.', '_', '-'];
              let foundBreak = false;
              for (let bp of breakpoints) {
                if (word.includes(bp)) {
                  let parts = word.split(bp);
                  let currentPart = '';
                  for (let i = 0; i < parts.length; i++) {
                    const testPart = currentPart + (currentPart ? bp : '') + parts[i];
                    if (testPart.length > CONFIG.text.maxLineLength && currentPart) {
                      tspans.push(text.append("tspan")
                        .attr("x", 0)
                        .attr("dy", tspans.length ? lineHeight : 0)
                        .text(currentPart));
                      currentPart = parts[i];
                    } else {
                      currentPart = testPart;
                    }
                    if (i === parts.length - 1 && currentPart) {
                      tspans.push(text.append("tspan")
                        .attr("x", 0)
                        .attr("dy", tspans.length ? lineHeight : 0)
                        .text(currentPart));
                    }
                  }
                  foundBreak = true;
                  break;
                }
              }
              if (!foundBreak) {
                let i = 0;
                while (i < word.length) {
                  const end = Math.min(i + CONFIG.text.maxLineLength, word.length);
                  tspans.push(text.append("tspan")
                    .attr("x", 0)
                    .attr("dy", tspans.length ? lineHeight : 0)
                    .text(word.substring(i, end)));
                  i = end;
                }
              }
            }
          } else {
            let line = '';
            words.forEach((word, i) => {
              const testLine = line + (line ? ' ' : '') + word;
              if (testLine.length > CONFIG.text.maxLineLength && line) {
                tspans.push(text.append("tspan")
                  .attr("x", 0)
                  .attr("dy", tspans.length ? lineHeight : -((words.length > 3 ? 3 : words.length-1)*lineHeight/2))
                  .text(line));
                line = word;
              } else {
                line = testLine;
              }
              if (i === words.length - 1) {
                tspans.push(text.append("tspan")
                  .attr("x", 0)
                  .attr("dy", tspans.length ? lineHeight : -((words.length > 3 ? 3 : words.length-1)*lineHeight/2))
                  .text(line));
              }
            });
          }
          if (tspans.length > 1) {
            const offset = -(tspans.length - 1) * lineHeight / 2;
            tspans[0].attr("dy", offset);
          }
        } else {
          // Simple case - just one short word
          tspans.push(text.append("tspan")
            .attr("x", 0)
            .attr("dy", 0)
            .text(nodeName));
        }
        
        // Add "(Self)" on a new line for duplicate nodes
        if (isDuplicate) {
          const selfSpan = text.append("tspan")
            .attr("x", 0)
            .attr("dy", lineHeight)
            .text(CONFIG.text.selfText)
            .style("font-style", CONFIG.text.fontStyle)
            .style("font-size", `${CONFIG.text.selfFontSize}px`);
          tspans.push(selfSpan);
          
          // Adjust vertical positioning to center all lines
          if (tspans.length > 1) {
            const offset = -(tspans.length - 1) * lineHeight / 2;
            tspans[0].attr("dy", offset);
          }
        }
      });
    
    // Add node hover effects
    nodeG.on("mouseenter", (event, d) => {
      const nodeId = d.data.id;
      this.g.selectAll(".link, .link-label, .link-label-bg, .node")
        .classed("faded", true);
      d3.select(event.currentTarget)
        .classed("faded", false)
        .classed("highlighted", true);
      const connectedEdges = [];
      const connectedNodes = new Set();
      connectedNodes.add(nodeId);
      
      this.g.selectAll("path.link")
        .each(function(linkData) {
          if (linkData.sourceId === nodeId) {
            connectedEdges.push({
              edge: d3.select(this),
              linkId: `${linkData.sourceId}--${linkData.targetId}`,
              edgeId: linkData.edgeData.name
            });
            connectedNodes.add(linkData.targetId);
          } else if (linkData.targetId === nodeId) {
            connectedEdges.push({
              edge: d3.select(this),
              linkId: `${linkData.sourceId}--${linkData.targetId}`,
              edgeId: linkData.edgeData.name
            });
            connectedNodes.add(linkData.sourceId);
          }
        });
      
      connectedEdges.forEach(item => {
        item.edge.classed("faded", false).classed("highlighted", true);
        this.g.selectAll(`text.link-label[data-edge-id="${item.edgeId}"][data-link-id="${item.linkId}"]`)
          .classed("faded", false)
          .classed("highlighted", true);
        this.g.selectAll(`rect.link-label-bg[data-edge-id="${item.edgeId}"][data-link-id="${item.linkId}"]`)
          .classed("faded", false)
          .classed("highlighted", true);
      });
      
      connectedNodes.forEach(id => {
        this.g.selectAll(`.node[data-id="${id}"]`)
          .classed("faded", false);
      });
    })
    .on("mouseleave", () => {
      this.g.selectAll(".link, .link-label, .link-label-bg, .node")
        .classed("faded", false)
        .classed("highlighted", false);
    });
  }
  
  /**
   * Handle click events on nodes
   * @param {Event} event - Mouse event
   * @param {Object} d - Node data 
   */
  handleNodeClick(event, d) {
    if (event) event.stopPropagation();
    // Remove the selection from all nodes and then mark the clicked node as selected
    this.g.selectAll(".node").classed("selected", false);
    d3.select(event ? event.currentTarget : null).classed("selected", true);
    // Remove any existing action buttons
    this.g.selectAll(".node-action-button").remove();
    this.renderActionButtons(d);
  }
  
  /**
   * Render action buttons for the currently selected node.
   * This method re-creates the buttons so that their labels reflect the current state.
   * @param {Object} d - Node data
   */
  renderActionButtons(d) {
    const buttonRadius = 12;
    const positions = [];
    const hasOutgoingLinks = this.nodeController.hasOutgoingLinks(d.data.id);
    
    if (hasOutgoingLinks) {
      const isExpanded = this.nodeController.expandedNodes.has(d.data.id);
      // Use "-" for expanded (contract action) and "+" for collapsed (expand action)
      const label = isExpanded ? "-" : "+";
      const action = isExpanded ? "contract" : "expand";
      positions.push({
        x: d.x,
        y: d.y - CONFIG.nodeHeight/2 - 25,
        label: label,
        action: action
      });
    }
    
    positions.push({
      x: d.x,
      y: d.y + CONFIG.nodeHeight/2 + 25,
      label: "i",
      action: "info"
    });
    
    positions.forEach(pos => {
      const buttonG = this.g.append("g")
        .attr("class", "node-action-button")
        .attr("transform", `translate(${Math.round(pos.x)},${Math.round(pos.y)})`)
        .style("cursor", "pointer")
        .on("click", event => {
          event.stopPropagation();
          if (pos.action === "info") {
            let info = `<strong>Node: ${d.data.name}</strong><br/><br/>`;
            if (d.data.annotationTags && d.data.annotationTags.length > 0) {
              info += `<strong>Tags:</strong><br/><ul>`;
              d.data.annotationTags.forEach(annotation => {
                info += `<li><strong>${annotation.name}</strong>`;
                if (annotation.attributes && Object.keys(annotation.attributes).length > 0) {
                  info += `<ul>`;
                  Object.entries(annotation.attributes).forEach(([key, value]) => {
                    info += `<li><strong>${key}:</strong> ${value}</li>`;
                  });
                  info += `</ul>`;
                }
                info += `</li>`;
              });
              info += `</ul>`;
            }
            showTooltip(info, event);
          } else if (pos.action === "expand") {
            this.nodeController.expandNode(d.data.id);
            this.updateGraphVisibility();
          } else if (pos.action === "contract") {
            this.nodeController.contractNode(d.data.id);
            this.updateGraphVisibility();
          }
        });
      
      buttonG.append("circle")
        .attr("r", buttonRadius)
        .attr("cx", 0)
        .attr("cy", 0)
        .attr("fill", "#f8f8f8")
        .attr("stroke", "#666")
        .attr("stroke-width", 1.5)
        .attr("filter", CONFIG.filter.nodeShadow);
      
      buttonG.append("text")
        .attr("x", 0)
        .attr("y", 0)
        .attr("text-anchor", "middle")
        .attr("dominant-baseline", "central")
        .attr("font-size", "14px")
        .attr("font-weight", "bold")
        .attr("fill", "#333")
        .style("pointer-events", "none")
        .text(pos.label);
    });
  }
  
  /**
   * Add input badge to nodes with inputs
   * @param {Object} d - Node data
   */
  addInputBadge(d) {
    if (d.data.inputs && d.data.inputs.length > 0) {
      const node = this.g.select(`.node[data-id="${d.data.id}"]`);
      // Position the badge to just touch the node
      const badgeX = Math.round(-CONFIG.nodeWidth/2 - CONFIG.inputBadge.xOffset);
      const badgeY = Math.round(-CONFIG.inputBadge.yOffset);
      
      const badgeGroup = node.append("g")
        .attr("class", "input-badge-container")
        .attr("transform", `translate(0,0)`)
        .style("cursor", "pointer")
        .on("click", (event) => {
          event.stopPropagation();
          let info = `<strong>Inputs for ${d.data.name}:</strong><br/><br/>`;
          info += d.data.inputs.map(input => {
            let inputInfo = `<div class="input-item">`;
            inputInfo += `<div class="input-name">${input.name}`;
            if (input.mandatory) { 
              inputInfo += `<span class="input-mandatory">*</span>`; 
            }
            inputInfo += `<span class="input-type">${input.type ? input.type : "N/A"}</span>`;
            inputInfo += `</div>`;
            if (input.documentation && input.documentation.trim() !== "") {
              inputInfo += `<div class="documentation">${input.documentation}</div>`;
            }
            inputInfo += "</div>";
            return inputInfo;
          }).join("");
          showTooltip(info, event, true);
        });
  
      badgeGroup.append("rect")
        .attr("class", "input-badge")
        .attr("width", CONFIG.inputBadge.width)
        .attr("height", CONFIG.inputBadge.height)
        .attr("x", badgeX)
        .attr("y", badgeY)
        .attr("rx", CONFIG.inputBadge.radius)
        .attr("ry", CONFIG.inputBadge.radius)
        .attr("fill", "var(--color-secondary)")
        .attr("stroke", "var(--color-surface)")
        .attr("stroke-width", CONFIG.inputBadge.strokeWidth);
  
      badgeGroup.append("text")
        .attr("class", "input-badge-arrow")
        .attr("x", badgeX + CONFIG.inputBadge.width/2)
        .attr("y", badgeY + CONFIG.inputBadge.height/2)
        .attr("text-anchor", "middle")
        .attr("dominant-baseline", "central")
        .attr("font-size", `${CONFIG.inputBadge.fontSize}px`)
        .attr("font-weight", CONFIG.inputBadge.fontWeight)
        .attr("fill", "var(--color-surface)")
        .text("→");
    }
  }
  
  /**
   * Update graph visibility based on node controller state
   */
  updateGraphVisibility() {
    // First pass: update node visibility based on the visibleNodeIds set
    this.g.selectAll("g.node")
      .style("display", d => {
        return this.nodeController.visibleNodeIds.has(d.data.id) ? 
          CONFIG.display.block : CONFIG.display.none;
      })
      .each((d) => {
        const node = this.g.select(`.node[data-id="${d.data.id}"]`);
        const isLeafNode = this.nodeController.isLeafNode(d.data.id);
        const isContracted = this.nodeController.explicitlyContractedNodes.has(d.data.id) && !isLeafNode;
        
        node.classed("contracted", isContracted);
        node.classed("expanded", this.nodeController.expandedNodes.has(d.data.id));
        node.classed("compute", d.data.vajramType === "COMPUTE");
        node.classed("io", d.data.vajramType === "IO");
        node.classed("abstract", d.data.vajramType === "ABSTRACT");
        node.classed("external-invocation", 
                     this.nodeController.externalInvocationAllowedNodes.has(d.data.id));
        
        node.selectAll(".node-state-indicator").remove();
      });
    
    // Update links visibility
    this.g.selectAll("path.link")
      .style("display", d => {
        const linkId = `${d.sourceId}--${d.targetId}`;
        return this.nodeController.visibleLinkIds.has(linkId) && 
               this.nodeController.visibleNodeIds.has(d.sourceId) && 
               this.nodeController.visibleNodeIds.has(d.targetId) ? 
                 CONFIG.display.block : CONFIG.display.none;
      });
    
    // Store reference to the current instance for use in the callback
    const self = this;
    
    // Update link-related elements visibility
    this.g.selectAll("[data-link-id]").each(function(d) {
      const element = d3.select(this);
      const linkId = element.attr("data-link-id");
      const [sourceId, targetId] = linkId.split("--");
      
      element.style("display", 
        self.nodeController.visibleLinkIds.has(linkId) && 
        self.nodeController.visibleNodeIds.has(sourceId) && 
        self.nodeController.visibleNodeIds.has(targetId) ? 
          CONFIG.display.block : CONFIG.display.none);
    });
    
    // Remove all node action buttons
    this.g.selectAll(".node-action-button").remove();
    
    // Re-render the action button for the currently selected node (if any)
    const selectedNodeElem = this.g.select("g.node.selected");
    if (!selectedNodeElem.empty()) {
      const selectedData = selectedNodeElem.datum();
      
      // Only render action buttons if the node is still visible
      if (this.nodeController.visibleNodeIds.has(selectedData.data.id)) {
        this.renderActionButtons(selectedData);
      } else {
        // If the selected node is no longer visible, remove the "selected" class
        selectedNodeElem.classed("selected", false);
      }
    }
  }
  
  /**
   * Resets the view to fit all visible nodes
   */
  resetView() {
    hideTooltip();
    
    // Get all currently visible nodes
    const visibleNodes = this.g.selectAll("g.node")
      .filter(function() {
        return d3.select(this).style("display") !== CONFIG.display.none;
      });
    
    if (visibleNodes.empty()) {
      // If no visible nodes, reset to original position
      this.svg.transition().duration(CONFIG.resetAnimationDuration).call(
        this.zoom.transform,
        d3.zoomIdentity.translate(this.windowWidth/2 - this.width/2, this.windowHeight/2 - this.height/2)
      );
      return;
    }
    
    // Calculate the bounding box of all visible nodes
    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
    
    visibleNodes.each(function(d) {
      const bbox = this.getBBox();
      const transform = this.getAttribute("transform");
      let x = d.x;
      let y = d.y;
      
      // Account for node width and height
      minX = Math.min(minX, x - CONFIG.nodeWidth/2);
      minY = Math.min(minY, y - CONFIG.nodeHeight/2);
      maxX = Math.max(maxX, x + CONFIG.nodeWidth/2);
      maxY = Math.max(maxY, y + CONFIG.nodeHeight/2);
    });
    
    // Add padding around the bounding box
    const padding = CONFIG.resetViewPadding;
    minX -= padding;
    minY -= padding;
    maxX += padding;
    maxY += padding;
    
    // Calculate the scale to fit the bounding box
    const boxWidth = maxX - minX;
    const boxHeight = maxY - minY;
    const scale = Math.min(
      this.windowWidth / boxWidth,
      this.windowHeight / boxHeight
    ) * CONFIG.resetViewMarginFactor;
    
    // Calculate the transform to center the bounding box
    const centerX = (minX + maxX) / 2;
    const centerY = (minY + maxY) / 2;
    
    // Apply the transform
    this.svg.transition().duration(CONFIG.resetAnimationDuration).call(
      this.zoom.transform,
      d3.zoomIdentity
        .translate(this.windowWidth / 2, this.windowHeight / 2)
        .scale(scale)
        .translate(-centerX, -centerY)
    );
  }
  
  /**
   * Resize the SVG when window size changes
   */
  resize() {
    this.windowWidth = window.innerWidth;
    this.windowHeight = window.innerHeight;
    this.svg.attr("width", this.windowWidth).attr("height", this.windowHeight);
  }
  
  /**
   * Get the container group for rendering
   * @return {Object} The container group
   */
  getContainer() {
    return this.g;
  }
} 
// tooltip.js - Functions for managing tooltips

import { CONFIG } from './config.js';
import * as d3 from "https://cdn.skypack.dev/d3@7.8.4";

// Create tooltip element once
const tooltip = d3.select("#tooltip");

/**
 * Display tooltip with content at specified event coordinates
 * @param {string} content - HTML content to show in tooltip
 * @param {Event} event - Mouse event object with page coordinates
 * @param {boolean} isInputTooltip - Whether this is an input tooltip with special behavior
 */
export function showTooltip(content, event, isInputTooltip = false) {
  // Reset any ongoing transitions and clear previous classes
  tooltip.interrupt()
    .classed("input-tooltip", isInputTooltip)
    .style("transform", "translateY(10px)")
    .style("opacity", 0);

  // Set content and position
  tooltip.html(content)
    .style("left", (event.pageX + 15) + "px")
    .style("top", (event.pageY + 15) + "px");
  
  // Apply the animation
  setTimeout(() => {
    tooltip.transition()
      .duration(CONFIG.animationDuration)
      .style("opacity", 1)
      .style("transform", "translateY(0)");
      
    // Add scroll event handling for input tooltips to prevent hiding
    if (isInputTooltip) {
      // Prevent mouseleave from hiding tooltip when scrolling
      tooltip.on("wheel", function(event) {
        event.stopPropagation();
      });
      
      // Prevent the tooltip from closing when mouse is over it
      tooltip.on("mouseenter", function() {
        tooltip.classed("tooltip-active", true);
      });
      
      tooltip.on("mouseleave", function() {
        tooltip.classed("tooltip-active", false);
        // Only hide if mouse has left the tooltip
        hideTooltip();
      });
    }
  }, 5);
}

/**
 * Hide tooltip with animation
 */
export function hideTooltip() {
  // Don't hide if mouse is still over an active tooltip
  if (tooltip.classed("tooltip-active")) {
    return;
  }
  
  tooltip.transition()
    .duration(CONFIG.animationDuration)
    .style("opacity", 0)
    .style("transform", "translateY(10px)")
    .on("end", () => {
      tooltip.classed("input-tooltip", false);
      tooltip.on("wheel", null);
      tooltip.on("mouseenter", null);
      tooltip.on("mouseleave", null);
    });
} 
// config.js - Configuration constants for the graph visualization

// Export the configuration settings
export const CONFIG = {
  
  // Node dimensions and appearance
  nodeWidth: 100,
  nodeHeight: 60,
  nodeRadius: 6,
  
  // Input badge dimensions
  inputBadge: {
    width: 20,
    height: 16,
    radius: 3,
    xOffset: 20,
    yOffset: 8,
    strokeWidth: 1,
    fontSize: 12,
    fontWeight: "bold"
  },
  
  // Zoom and animation settings
  zoomScaleExtent: [0.1, 10],
  animationDuration: 200,
  transitionDuration: 300,
  resetAnimationDuration: 750,
  
  // Layout configuration
  layout: {
    nodeSize: [60, 60],
    gap: [80, 120]
  },
  
  // Padding values
  resetViewPadding: 50,
  resetViewMarginFactor: 0.95, // 95% of available space
  
  // Line styling
  lineStrokeWidth: {
    default: 2,
    hover: 2.5,
    mandatory: 3.5,
    mandatoryHover: 4
  },
  
  // Marker dimensions
  marker: {
    viewBox: "0 0 10 10",
    refX: 8,
    refY: 5,
    width: 12,
    height: 12,
    path: "M 0 0 L 10 5 L 0 10 z"
  },
  
  fanoutMarker: {
    viewBox: "0 0 20 10",
    refX: 8,
    refY: 5,
    width: 22,
    height: 12,
    strokeWidth: 1.8,
    path: "M 0 0 L 5 5 L 0 10 M 5 0 L 10 5 L 5 10 M 10 0 L 15 5 L 10 10"
  },
  
  // Text formatting
  text: {
    fontSize: 12,
    fontWeight: 500,
    selfText: "(Recursive)",
    selfFontSize: 10,
    fontStyle: "italic",
    lineHeight: 14,
    maxLineLength: 15,
    maxNameLength: 12,
    maxWordLength: 18
  },
  
  // Search settings
  search: {
    minLength: 2
  },
  
  // Filter, shadow and opacity values
  filter: {
    nodeShadow: "drop-shadow(0px 3px 5px rgba(0,0,0,0.2))",
    searchHighlight: "drop-shadow(0px 3px 6px rgba(0,0,0,0.5))"
  },
  
  // Display options
  display: {
    block: "block",
    none: "none"
  }
}; 
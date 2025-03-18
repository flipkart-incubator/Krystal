// dataProcessor.js - Functions for processing graph data
import * as d3dag from "https://cdn.skypack.dev/d3-dag@1.0.0-1";

/**
 * Process the raw graph data to prepare it for visualization
 * @param {Object} graphData - Raw graph data with nodes and links
 * @return {Object} Processed data with nodes, links, and DAG
 */
export function processGraphData(graphData) {
  const nodes = graphData.nodes;
  const links = graphData.links;
  
  // Build adjacency list for efficient traversal
  const adjacencyList = {};
  nodes.forEach(node => {
    adjacencyList[node.id] = [];
  });
  
  links.forEach(link => {
    if (adjacencyList[link.source]) {
      adjacencyList[link.source].push(link.target);
    }
  });
  
  // Function to detect cycles using DFS
  function detectRecursion(start) {
    const visited = new Set();
    const recursionStack = new Set();
    const recursiveNodes = new Set();
    
    function dfs(nodeId) {
      visited.add(nodeId);
      recursionStack.add(nodeId);
      
      for (const neighbor of adjacencyList[nodeId] || []) {
        if (!visited.has(neighbor)) {
          if (dfs(neighbor)) {
            recursiveNodes.add(neighbor);
          }
        } else if (recursionStack.has(neighbor)) {
          // Found a cycle, mark the node as recursive
          recursiveNodes.add(neighbor);
          return true;
        }
      }
      
      recursionStack.delete(nodeId);
      return false;
    }
    
    dfs(start);
    return recursiveNodes;
  }
  
  // Find all recursive nodes from all starting points
  const allRecursiveNodes = new Set();
  nodes.forEach(node => {
    const recursiveNodes = detectRecursion(node.id);
    recursiveNodes.forEach(id => allRecursiveNodes.add(id));
  });
  
  // Separate self-loops from regular links and create duplicate nodes for recursive references
  const selfLoops = links.filter(l => l.source === l.target);
  const regularLinks = links.filter(l => l.source !== l.target);
  const duplicatedNodes = [];
  const updatedSelfLoopLinks = [];

  // Process self-loops by creating duplicate nodes
  selfLoops.forEach((selfLoop, index) => {
    const originalNode = nodes.find(n => n.id === selfLoop.source);
    if (originalNode) {
      const duplicateNodeId = `${originalNode.id}_dup_${index}`;
      const duplicateNode = { ...originalNode, id: duplicateNodeId, isDuplicate: true };
      duplicatedNodes.push(duplicateNode);
      updatedSelfLoopLinks.push({ ...selfLoop, target: duplicateNodeId });
    }
  });
  
  // Process nodes that are part of a recursive chain
  links.forEach((link, index) => {
    if (link.source !== link.target && allRecursiveNodes.has(link.target)) {
      const targetNode = nodes.find(n => n.id === link.target);
      const sourceNode = nodes.find(n => n.id === link.source);
      
      // Check if this link creates a recursive dependency (source depends on target through other paths)
      const path = findPath(adjacencyList, link.target, link.source);
      if (path && path.length > 0) {
        // Create duplicate node
        const duplicateNodeId = `${targetNode.id}_rec_${index}`;
        const duplicateNode = { ...targetNode, id: duplicateNodeId, isDuplicate: true };
        duplicatedNodes.push(duplicateNode);
        
        // Update link to point to duplicate
        const linkIndex = regularLinks.findIndex(l => 
          l.source === link.source && l.target === link.target);
        if (linkIndex !== -1) {
          regularLinks[linkIndex] = { ...regularLinks[linkIndex], target: duplicateNodeId };
        }
      }
    }
  });
  
  // Helper function to find a path between two nodes
  function findPath(adjacencyList, start, end) {
    const visited = new Set();
    const path = [];
    
    function dfs(current, target) {
      visited.add(current);
      path.push(current);
      
      if (current === target) {
        return true;
      }
      
      for (const neighbor of adjacencyList[current] || []) {
        if (!visited.has(neighbor)) {
          if (dfs(neighbor, target)) {
            return true;
          }
        }
      }
      
      path.pop();
      return false;
    }
    
    dfs(start, end);
    return path;
  }

  const allNodes = [...nodes, ...duplicatedNodes];
  const filteredLinks = regularLinks.concat(updatedSelfLoopLinks);

  // Build a map for nodes and prepare for DAG construction
  const nodeById = new Map();
  allNodes.forEach(n => nodeById.set(n.id, { ...n, parentIds: [] }));
  filteredLinks.forEach(l => {
    if (nodeById.has(l.target)) nodeById.get(l.target).parentIds.push(l.source);
  });

  const processedNodes = Array.from(nodeById.values());
  
  // Build the DAG structure
  const dagBuilder = d3dag.graphStratify();
  const dag = dagBuilder(processedNodes);
  
  return {
    allNodes,
    filteredLinks,
    dag
  };
}

/**
 * Creates unique visual paths for multiple edges between the same nodes
 * @param {Array} filteredLinks - Processed links
 * @param {Object} dag - DAG structure
 * @return {Array} Unique edge paths
 */
export function createEdgePaths(filteredLinks, dag) {
  // Create unique visual paths for multiple edges between the same nodes
  const edgesByPair = {};
  filteredLinks.forEach(link => {
    const key = `${link.source}--${link.target}`;
    if (!edgesByPair[key]) { edgesByPair[key] = []; }
    edgesByPair[key].push(link);
  });
  
  const uniqueEdgePaths = [];
  Object.entries(edgesByPair).forEach(([key, edges]) => {
    const [sourceId, targetId] = key.split('--');
    const dagLink = dag.links().find(link => 
      link.source.data.id === sourceId && link.target.data.id === targetId
    );
    
    if (dagLink && edges.length > 0) {
      uniqueEdgePaths.push({
        path: dagLink.points.slice(),
        sourceId: sourceId,
        targetId: targetId,
        edgeData: edges[0],
        offset: 0,
        totalEdges: edges.length,
        labelPosition: 0.5
      });
      
      if (edges.length > 1) {
        const baseOffset = Math.max(40, edges.length * 20);
        for (let i = 1; i < edges.length; i++) {
          const curveOffset = baseOffset * i;
          const newPath = dagLink.points.slice().map((point, idx) => {
            if (idx > 0 && idx < dagLink.points.length - 1) {
              const prevPoint = dagLink.points[idx - 1];
              const dx = point[0] - prevPoint[0];
              const dy = point[1] - prevPoint[1];
              const len = Math.sqrt(dx * dx + dy * dy);
              const perpX = len > 0 ? -dy / len : 0;
              const perpY = len > 0 ? dx / len : 0;
              const pathProgress = idx / (dagLink.points.length - 1);
              const scaleFactor = 4 * pathProgress * (1 - pathProgress);
              return [
                point[0] + perpX * curveOffset * scaleFactor,
                point[1] + perpY * curveOffset * scaleFactor
              ];
            }
            return point;
          });
          
          const labelPos = 0.5 + (i % 2 === 0 ? 1 : -1) * 0.12 * (Math.floor(i/2) + 1);
          const labelPosition = Math.max(0.3, Math.min(0.7, labelPos));
          
          uniqueEdgePaths.push({
            path: newPath,
            sourceId: sourceId,
            targetId: targetId,
            edgeData: edges[i],
            offset: curveOffset,
            totalEdges: edges.length,
            labelPosition: labelPosition
          });
        }
      }
    }
  });
  
  return uniqueEdgePaths;
}

/**
 * Compute intersection point of a line with a rectangle boundary
 * @param {Object} source - Source point {x, y}
 * @param {Object} target - Target point {x, y}
 * @param {number} width - Rectangle width
 * @param {number} height - Rectangle height
 * @return {Object} Intersection point {x, y}
 */
export function getIntersectionPoint(source, target, width, height) {
  const dx = target.x - source.x;
  const dy = target.y - source.y;
  const length = Math.sqrt(dx * dx + dy * dy);
  const ndx = dx / length;
  const ndy = dy / length;
  const halfWidth = width / 2;
  const halfHeight = height / 2;
  
  // Calculate intersections with all 4 sides of the rectangle
  // We'll find which side is intersected first based on the direction
  
  // Time to hit vertical boundaries (left/right sides)
  const tx = ndx === 0 ? Infinity : halfWidth / Math.abs(ndx);
  
  // Time to hit horizontal boundaries (top/bottom sides)
  const ty = ndy === 0 ? Infinity : halfHeight / Math.abs(ndy);
  
  // Use the shorter time - that's the side we hit first
  const t = Math.min(tx, ty);
  
  // Calculate final intersection point
  return {
    x: source.x + ndx * t,
    y: source.y + ndy * t
  };
} 
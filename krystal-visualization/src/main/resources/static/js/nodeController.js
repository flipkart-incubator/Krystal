// nodeController.js - Functions for controlling node expand/contract actions
import { hideTooltip } from './tooltip.js';

// Visibility and state management for nodes and links
export class NodeController {
  constructor(allNodes, filteredLinks) {
    this.allNodes = allNodes;
    this.filteredLinks = filteredLinks;
    
    // Node visibility and expand/contract state
    this.visibleNodeIds = new Set(allNodes.map(n => n.id));
    this.visibleLinkIds = new Set(filteredLinks.map(l => `${l.source}--${l.target}`));
    this.explicitlyContractedNodes = new Set();
    this.expandedNodes = new Set();
    
    // Set to track nodes with ExternalInvocation annotation and allow=true
    this.externalInvocationAllowedNodes = new Set();
    this.trueSourceNodes = new Set();
    
    // Identify nodes with ExternalInvocation annotation and allow=true
    this.allNodes.forEach(node => {
      // Skip duplicate nodes
      if (node.isDuplicate) return;
      
      if (node.annotationTags && node.annotationTags.length > 0) {
        const externalInvocationAnnotation = node.annotationTags.find(annotation => 
          annotation.name === "ExternalInvocation" && 
          annotation.attributes && 
          annotation.attributes.allow === "true"
        );
        if (externalInvocationAnnotation) {
          this.externalInvocationAllowedNodes.add(node.id);
          console.log("Node marked as always visible (ExternalInvocation allow=true):", node.id);
        }
      }
    });
    
    // Initially expand nodes with outgoing links
    this.filteredLinks.forEach(link => {
      this.expandedNodes.add(link.source);
    });
    
    // Build map of node connections
    this.nodeConnections = new Map();
    this.filteredLinks.forEach(link => {
      if (!this.nodeConnections.has(link.source)) { 
        this.nodeConnections.set(link.source, new Set()); 
      }
      this.nodeConnections.get(link.source).add(link.target);
      if (!this.nodeConnections.has(link.target)) { 
        this.nodeConnections.set(link.target, new Set()); 
      }
    });
    
    // Identify true source nodes (nodes with no incoming links)
    this.allNodes.forEach(node => {
      // A true source node has no incoming links in the original graph structure
      const hasTrueIncomingLinks = this.filteredLinks.some(link => link.target === node.id);
      if (!hasTrueIncomingLinks) {
        this.trueSourceNodes.add(node.id);
        console.log("True source node identified:", node.id);
      }
    });
  }
  
  /**
   * Expand a node to show its connections
   * @param {string} nodeId - ID of the node to expand
   */
  expandNode(nodeId) {
    console.log("Expanding node:", nodeId);
    hideTooltip(); // Close any open tooltips when expanding nodes
    this.expandedNodes.add(nodeId);
    this.explicitlyContractedNodes.delete(nodeId);
    this.visibleNodeIds.add(nodeId);
    const connectedTargets = new Set();
    
    // Find all direct targets of this node
    this.filteredLinks.forEach(link => {
      if (link.source === nodeId) {
        connectedTargets.add(link.target);
      }
    });
    
    // Make all direct targets visible
    connectedTargets.forEach(targetId => {
      this.visibleNodeIds.add(targetId);
      const directLinkId = `${nodeId}--${targetId}`;
      this.visibleLinkIds.add(directLinkId);
      
      // Check if this node is a leaf node (has no outgoing connections)
      const isLeafNode = !this.filteredLinks.some(l => l.source === targetId);
      
      if (isLeafNode) { 
        // Leaf nodes should never appear contracted
        this.explicitlyContractedNodes.delete(targetId); 
      } else {
        // For non-leaf nodes, check if any of their outgoing edges are hidden
        const hasHiddenOutgoingEdges = this.filteredLinks.some(link => {
          if (link.source === targetId) {
            const linkId = `${link.source}--${link.target}`;
            return !this.visibleLinkIds.has(linkId);
          }
          return false;
        });
        
        // Only mark as contracted if it has at least one hidden outgoing edge
        if (hasHiddenOutgoingEdges) {
          this.explicitlyContractedNodes.add(targetId);
          this.expandedNodes.delete(targetId);
        } else {
          // If all outgoing edges are visible, don't mark as contracted
          this.explicitlyContractedNodes.delete(targetId);
        }
      }
    });
  }
  
  /**
   * Contract a node to hide its connections
   * @param {string} nodeId - ID of the node to contract
   */
  contractNode(nodeId) {
    console.log("Contracting node:", nodeId);
    hideTooltip();
    this.explicitlyContractedNodes.add(nodeId);
    this.expandedNodes.delete(nodeId);
    
    // First hide direct outgoing links from this node
    this.filteredLinks.forEach(link => {
      if (link.source === nodeId) {
        const linkId = `${link.source}--${link.target}`;
        this.visibleLinkIds.delete(linkId);
      }
    });
    
    // Start with a new set of visible nodes
    const newVisibleNodes = new Set();
    
    // Add true source nodes and external invocation nodes as always visible
    this.trueSourceNodes.forEach(id => newVisibleNodes.add(id));
    this.externalInvocationAllowedNodes.forEach(id => newVisibleNodes.add(id));
    
    // Iteratively find all nodes reachable through visible and expanded nodes
    let changed = true;
    while (changed) {
      changed = false;
      
      // For each link
      this.filteredLinks.forEach(link => {
        // If source is visible and expanded, and target isn't already added
        if (newVisibleNodes.has(link.source) && 
            this.expandedNodes.has(link.source) && 
            !this.explicitlyContractedNodes.has(link.source) &&
            !newVisibleNodes.has(link.target)) {
          
          // The linkId for this connection
          const linkId = `${link.source}--${link.target}`;
          
          // Only add if this link hasn't been explicitly hidden
          if (this.visibleLinkIds.has(linkId)) {
            newVisibleNodes.add(link.target);
            changed = true;
          }
        }
      });
    }
    
    // Create new set of visible links
    const newVisibleLinks = new Set();
    this.filteredLinks.forEach(link => {
      if (newVisibleNodes.has(link.source) && 
          newVisibleNodes.has(link.target) && 
          this.expandedNodes.has(link.source) && 
          !this.explicitlyContractedNodes.has(link.source)) {
        
        const linkId = `${link.source}--${link.target}`;
        newVisibleLinks.add(linkId);
      }
    });
    
    // Update the visibility state
    this.visibleNodeIds.clear();
    newVisibleNodes.forEach(id => this.visibleNodeIds.add(id));
    
    this.visibleLinkIds.clear();
    newVisibleLinks.forEach(id => this.visibleLinkIds.add(id));
    
    console.log("Contract complete. Visible nodes:", [...this.visibleNodeIds]);
  }
  
  /**
   * Expand all nodes in the graph
   */
  expandAll() {
    hideTooltip();
    this.allNodes.forEach(node => { this.visibleNodeIds.add(node.id); });
    this.explicitlyContractedNodes.clear();
    this.allNodes.forEach(node => { this.expandedNodes.add(node.id); });
    this.filteredLinks.forEach(link => {
      const linkId = `${link.source}--${link.target}`;
      this.visibleLinkIds.add(linkId);
    });
  }
  
  /**
   * Contract all nodes in the graph
   */
  contractAll() {
    hideTooltip();
    // First ensure all external invocation nodes are visible
    this.externalInvocationAllowedNodes.forEach(nodeId => {
      this.visibleNodeIds.add(nodeId);
    });
    
    // Contract all non-duplicated nodes
    this.allNodes.forEach(node => {
      // Skip duplicate nodes
      if (!node.isDuplicate) {
        this.contractNode(node.id);
      }
    });
    
    // Hide all links - we want nodes visible but not their connections
    this.filteredLinks.forEach(link => {
      const linkId = `${link.source}--${link.target}`;
      this.visibleLinkIds.delete(linkId);
    });
  }
  
  /**
   * Check if a node is a leaf node (no outgoing connections)
   * @param {string} nodeId - Node ID to check
   * @return {boolean} Whether the node is a leaf node
   */
  isLeafNode(nodeId) {
    return !this.filteredLinks.some(link => link.source === nodeId);
  }
  
  /**
   * Check if a node has outgoing connections
   * @param {string} nodeId - Node ID to check
   * @return {boolean} Whether the node has outgoing connections
   */
  hasOutgoingLinks(nodeId) {
    return this.filteredLinks.some(link => link.source === nodeId);
  }
} 
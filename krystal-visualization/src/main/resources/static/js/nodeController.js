// nodeController.js - Functions for controlling node expand/collapse actions
import { hideTooltip } from './tooltip.js';

// Visibility and state management for nodes and links
export class NodeController {
  constructor(allNodes, filteredLinks) {
    this.allNodes = allNodes;
    this.filteredLinks = filteredLinks;
    
    // Node visibility and expand/collapse state
    this.visibleNodeIds = new Set(allNodes.map(n => n.id));
    this.visibleLinkIds = new Set(filteredLinks.map(l => `${l.source}--${l.target}`));
    this.explicitlyCollapsedNodes = new Set();
    this.expandedNodes = new Set();
    
    // Set to track nodes with ExternalInvocation annotation and allow=true
    this.externalInvocationAllowedNodes = new Set();
    this.trueSourceNodes = new Set();
    
    // Identify nodes with ExternalInvocation annotation and allow=true
    this.allNodes.forEach(node => {
      if (node.isDuplicate) return;
      
      if (node.annotationTags && node.annotationTags.length > 0) {
        const hasExternalInvocation = node.annotationTags.some(annotation => 
          annotation.includes("ExternalInvocation") && 
          annotation.includes("allow=true")
        );
        if (hasExternalInvocation) {
          this.externalInvocationAllowedNodes.add(node.id);
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
      const hasTrueIncomingLinks = this.filteredLinks.some(link => link.target === node.id);
      if (!hasTrueIncomingLinks) {
        this.trueSourceNodes.add(node.id);
      }
    });
  }
  
  /**
   * Expand a node to show its connections
   * @param {string} nodeId - ID of the node to expand
   */
  expandNode(nodeId) {
    hideTooltip();
    this.expandedNodes.add(nodeId);
    this.explicitlyCollapsedNodes.delete(nodeId);
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
      
      const isLeafNode = !this.filteredLinks.some(l => l.source === targetId);
      
      if (isLeafNode) { 
        this.explicitlyCollapsedNodes.delete(targetId); 
      } else {
        const hasHiddenOutgoingEdges = this.filteredLinks.some(link => {
          if (link.source === targetId) {
            const linkId = `${link.source}--${link.target}`;
            return !this.visibleLinkIds.has(linkId);
          }
          return false;
        });
        
        if (hasHiddenOutgoingEdges) {
          this.explicitlyCollapsedNodes.add(targetId);
          this.expandedNodes.delete(targetId);
        } else {
          this.explicitlyCollapsedNodes.delete(targetId);
        }
      }
    });
  }
  
  /**
   * Collapse a node to hide its connections
   * @param {string} nodeId - ID of the node to collapse
   */
  collapseNode(nodeId) {
    hideTooltip();
    this.explicitlyCollapsedNodes.add(nodeId);
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
      
      this.filteredLinks.forEach(link => {
        if (newVisibleNodes.has(link.source) && 
            this.expandedNodes.has(link.source) && 
            !this.explicitlyCollapsedNodes.has(link.source) &&
            !newVisibleNodes.has(link.target)) {
          
          const linkId = `${link.source}--${link.target}`;
          
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
          !this.explicitlyCollapsedNodes.has(link.source)) {
        
        const linkId = `${link.source}--${link.target}`;
        newVisibleLinks.add(linkId);
      }
    });
    
    // Update the visibility state
    this.visibleNodeIds.clear();
    newVisibleNodes.forEach(id => this.visibleNodeIds.add(id));
    
    this.visibleLinkIds.clear();
    newVisibleLinks.forEach(id => this.visibleLinkIds.add(id));
  }
  
  /**
   * Expand all nodes in the graph
   */
  expandAll() {
    hideTooltip();
    this.allNodes.forEach(node => { this.visibleNodeIds.add(node.id); });
    this.explicitlyCollapsedNodes.clear();
    this.allNodes.forEach(node => { this.expandedNodes.add(node.id); });
    this.filteredLinks.forEach(link => {
      const linkId = `${link.source}--${link.target}`;
      this.visibleLinkIds.add(linkId);
    });
  }
  
  /**
   * Collapse all nodes in the graph
   */
  collapseAll() {
    hideTooltip();
    this.externalInvocationAllowedNodes.forEach(nodeId => {
      this.visibleNodeIds.add(nodeId);
    });
    
    this.allNodes.forEach(node => {
      if (!node.isDuplicate) {
        this.collapseNode(node.id);
      }
    });
    
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
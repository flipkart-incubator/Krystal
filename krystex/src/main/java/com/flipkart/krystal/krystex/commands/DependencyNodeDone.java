package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.Node;

/**
 * @param node the node whose dependency is done
 * @param depNodeId the node Id of the dependency which is done.
 */
public record DependencyNodeDone(Node<?> node, String depNodeId) implements NodeCommand {}

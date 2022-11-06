package com.flipkart.krystal.krystex;

public record DependencyDone(Node<?> node, String depNodeId) implements NodeCommand {}

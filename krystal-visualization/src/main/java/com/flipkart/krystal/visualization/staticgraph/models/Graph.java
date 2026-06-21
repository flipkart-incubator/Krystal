package com.flipkart.krystal.visualization.staticgraph.models;

import java.util.List;
import lombok.Builder;

@Builder
public record Graph(List<Node> nodes, List<Link> links) {}

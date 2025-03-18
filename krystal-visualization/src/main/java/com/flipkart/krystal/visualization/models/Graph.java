package com.flipkart.krystal.visualization.models;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Graph {
  private final List<Node> nodes;
  private final List<Link> links;
}

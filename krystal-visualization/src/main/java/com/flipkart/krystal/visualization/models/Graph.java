package com.flipkart.krystal.visualization.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Graph {
  @JsonProperty private final List<Node> nodes;
  @JsonProperty private final List<Link> links;
}

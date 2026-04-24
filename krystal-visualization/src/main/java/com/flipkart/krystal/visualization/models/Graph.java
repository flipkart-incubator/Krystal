package com.flipkart.krystal.visualization.models;

import java.util.List;
import lombok.Builder;

@Builder
public record Graph(List<Node> nodes, List<Link> links) {}

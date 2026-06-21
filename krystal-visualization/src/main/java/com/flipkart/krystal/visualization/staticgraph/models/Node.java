package com.flipkart.krystal.visualization.staticgraph.models;

import java.util.List;
import lombok.Builder;

@Builder
public record Node(
    String id,
    String name,
    VajramType vajramType,
    List<Input> inputs,
    List<String> annotationTags) {}

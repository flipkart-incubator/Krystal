package com.flipkart.krystal.visualization.models;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GraphGenerationResult {
  private final String html;
}

package com.flipkart.krystal.visualization.models;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GraphGenerationResult {
  private final String html;
  private final String fileName;
  private final Map<String, String> visualizationResources;
}

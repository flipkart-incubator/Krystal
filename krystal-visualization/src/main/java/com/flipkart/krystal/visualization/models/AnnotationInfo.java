package com.flipkart.krystal.visualization.models;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnnotationInfo {
  private final String name;
  private final Map<String, String> attributes;
}

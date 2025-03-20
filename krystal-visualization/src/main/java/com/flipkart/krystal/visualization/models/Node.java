package com.flipkart.krystal.visualization.models;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Node {
  private final String id;
  private final String name;
  private final VajramType vajramType;
  private final List<Input> inputs;
  private final List<String> annotationTags;
}

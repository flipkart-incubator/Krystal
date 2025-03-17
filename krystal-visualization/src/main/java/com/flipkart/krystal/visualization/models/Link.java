package com.flipkart.krystal.visualization.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Link {
  private final String source;
  private final String target;
  private final String name;
  private final boolean isMandatory;
  private final boolean canFanout;
  private final String documentation;
}

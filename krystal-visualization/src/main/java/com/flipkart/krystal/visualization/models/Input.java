package com.flipkart.krystal.visualization.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Input {
  private final String name;
  private final String type;
  private final boolean isMandatory;
  private final String documentation;
}

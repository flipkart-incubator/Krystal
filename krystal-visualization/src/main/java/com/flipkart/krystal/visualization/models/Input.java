package com.flipkart.krystal.visualization.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Input {
  @JsonProperty private final String name;
  @JsonProperty private final String type;
  @JsonProperty private final boolean isMandatory;
  @JsonProperty private final String documentation;
}

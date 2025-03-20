package com.flipkart.krystal.visualization.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Link {
  @JsonProperty private final String source;
  @JsonProperty private final String target;
  @JsonProperty private final String name;
  @JsonProperty private final boolean isMandatory;
  @JsonProperty private final boolean canFanout;
  @JsonProperty private final String documentation;
}

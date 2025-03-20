package com.flipkart.krystal.visualization.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Node {
  @JsonProperty private final String id;
  @JsonProperty private final String name;
  @JsonProperty private final VajramType vajramType;
  @JsonProperty private final List<Input> inputs;
  @JsonProperty private final List<String> annotationTags;
}

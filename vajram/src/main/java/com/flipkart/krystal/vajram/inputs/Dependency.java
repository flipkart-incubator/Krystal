package com.flipkart.krystal.vajram.inputs;

import com.flipkart.krystal.vajram.das.DataAccessSpec;
import lombok.Builder;

@Builder
public record Dependency<T>(
    String name,
    DataAccessSpec dataAccessSpec,
    boolean isMandatory,
    boolean canFanout,
    String documentation)
    implements VajramInputDefinition {

  @Override
  public boolean needsModulation() {
    return false;
  }
}

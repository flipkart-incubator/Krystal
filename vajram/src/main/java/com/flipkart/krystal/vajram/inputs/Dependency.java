package com.flipkart.krystal.vajram.inputs;

import com.flipkart.krystal.vajram.das.DataAccessSpec;
import lombok.Builder;

@Builder
public record Dependency(String name, DataAccessSpec dataAccessSpec, boolean isMandatory)
    implements VajramInputDefinition {

  @Override
  public boolean needsModulation() {
    return false;
  }

  public static class DependencyBuilder<T> {
    public DependencyBuilder<T> isMandatory() {
      return isMandatory(true);
    }

    public DependencyBuilder<T> isMandatory(boolean isMandatory) {
      this.isMandatory = isMandatory;
      return this;
    }
  }
}

package com.flipkart.krystal.vajram.inputs;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.vajram.das.DataAccessSpec;
import lombok.Builder;

@Builder
public record Dependency<T>(
    String name, DataAccessSpec dataAccessSpec, DataType<? extends T> type, boolean isMandatory)
    implements VajramInputDefinition<T> {

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

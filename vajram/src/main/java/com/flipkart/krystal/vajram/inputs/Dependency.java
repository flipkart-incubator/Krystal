package com.flipkart.krystal.vajram.inputs;

import com.flipkart.krystal.vajram.DependencySpec;
import lombok.Builder;

@Builder
public record Dependency(String name, DependencySpec dependencySpec, boolean mandatory)
    implements VajramInputDefinition {

  @Override
  public boolean needsModulation() {
    return false;
  }

  public static class DependencyBuilder<T> {
    public DependencyBuilder<T> isMandatory() {
      this.mandatory = true;
      return this;
    }
  }
}

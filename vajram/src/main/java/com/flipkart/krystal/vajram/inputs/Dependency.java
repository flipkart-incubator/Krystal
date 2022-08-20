package com.flipkart.krystal.vajram.inputs;

import com.flipkart.krystal.vajram.DependencySpec;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
public record Dependency(String name, DependencySpec dependencySpec, boolean mandatory)
    implements VajramDependencyDefinition {

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

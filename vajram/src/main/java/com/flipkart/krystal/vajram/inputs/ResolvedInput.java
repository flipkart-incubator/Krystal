package com.flipkart.krystal.vajram.inputs;

import com.flipkart.krystal.vajram.DependencySpec;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
public record ResolvedInput(String name, DependencySpec dependencySpec, boolean mandatory)
    implements VajramInputDefinition {

  @Override
  public boolean needsModulation() {
    return false;
  }

  public static <T> ResolvedInputBuilder<T> builder() {
    return new ResolvedInputBuilder<T>();
  }

  public static class ResolvedInputBuilder<T> {
    public ResolvedInputBuilder<T> isMandatory() {
      this.mandatory = true;
      return this;
    }

  }

}

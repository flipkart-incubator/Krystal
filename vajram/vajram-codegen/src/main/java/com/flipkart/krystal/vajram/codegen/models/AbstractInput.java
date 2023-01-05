package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract sealed class AbstractInput extends TypeDefinition permits InputDef, DependencyDef {
  private String name;
  private boolean mandatory;
  private String doc = "";

  public abstract VajramInputDefinition toInputDefinition();
}

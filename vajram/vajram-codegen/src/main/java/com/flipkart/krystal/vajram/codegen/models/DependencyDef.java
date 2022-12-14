package com.flipkart.krystal.vajram.codegen.models;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.flipkart.krystal.vajram.das.DataAccessSpec;
import com.flipkart.krystal.vajram.inputs.Dependency;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@JsonTypeInfo(use = Id.NAME, property = "depType")
@JsonSubTypes({@Type(value = VajramDependencyDef.class)})
public abstract sealed class DependencyDef extends AbstractInput permits VajramDependencyDef {
  abstract DataAccessSpec toDataAccessSpec();

  @Override
  public Dependency<?> toInputDefinition() {
    return Dependency.builder()
        .name(getName())
        .isMandatory(isMandatory())
        .documentation(getDoc())
        .dataAccessSpec(toDataAccessSpec())
        .build();
  }
}

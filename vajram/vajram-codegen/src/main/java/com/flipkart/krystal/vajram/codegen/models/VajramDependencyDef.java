package com.flipkart.krystal.vajram.codegen.models;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.das.DataAccessSpec;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("vajram")
public final class VajramDependencyDef extends DependencyDef {
  private String vajramClass;

  private boolean canFanout;

  @Override
  public DataAccessSpec toDataAccessSpec() {
    return VajramID.vajramID(vajramClass);
  }

  @Override
  public boolean canFanout() {
    return canFanout;
  }
}

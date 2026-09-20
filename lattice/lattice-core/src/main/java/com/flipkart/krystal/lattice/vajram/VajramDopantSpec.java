package com.flipkart.krystal.lattice.vajram;

import static com.flipkart.krystal.lattice.vajram.VajramDopant.DOPANT_TYPE;

import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpec;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpecBuilder;
import lombok.Builder;

@Builder(buildMethodName = "_buildSpec")
public record VajramDopantSpec() implements SimpleDopantSpec<VajramDopant> {

  @Override
  public Class<VajramDopant> dopantClass() {
    return VajramDopant.class;
  }

  @Override
  public String _dopantType() {
    return DOPANT_TYPE;
  }

  @DopantType(DOPANT_TYPE)
  public static final class VajramDopantSpecBuilder
      extends SimpleDopantSpecBuilder<VajramDopantSpec> {}
}

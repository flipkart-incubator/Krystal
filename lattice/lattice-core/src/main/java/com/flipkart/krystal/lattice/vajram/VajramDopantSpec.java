package com.flipkart.krystal.lattice.vajram;

import static com.flipkart.krystal.lattice.vajram.VajramDopant.DOPANT_TYPE;

import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpec;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpecBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.VajramGraph.VajramGraphBuilder;
import lombok.Builder;
import lombok.NonNull;

public record VajramDopantSpec(VajramGraphBuilder vajramGraphBuilder)
    implements SimpleDopantSpec<VajramDopant> {

  @Builder(buildMethodName = "_buildSpec")
  public static VajramDopantSpec create(@NonNull VajramGraphBuilder vajramGraphBuilder) {
    return new VajramDopantSpec(vajramGraphBuilder);
  }

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

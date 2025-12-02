package com.flipkart.krystal.lattice.graphql.rest.dopant;

import static com.flipkart.krystal.lattice.ext.rest.RestServiceDopant.restService;
import static com.flipkart.krystal.lattice.vajram.VajramDopant.vajramGraph;

import com.flipkart.krystal.lattice.core.doping.DopantSpecBuilder;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpec;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpecBuilder;
import java.util.List;
import lombok.Builder;

@Builder(buildMethodName = "_buildSpec")
record GraphQlOverRestDopantSpec() implements SimpleDopantSpec<GraphQlOverRestDopant> {

  @Override
  public Class<? extends GraphQlOverRestDopant> dopantClass() {
    return GraphQlOverRestDopant.class;
  }

  public static final class GraphQlOverRestDopantSpecBuilder
      extends SimpleDopantSpecBuilder<GraphQlOverRestDopantSpec> {

    @Override
    public String _dopantType() {
      return GraphQlOverRestDopant.DOPANT_TYPE;
    }

    @Override
    public List<DopantSpecBuilder<?, ?, ?>> getAdditionalDopants() {
      return List.of(vajramGraph(), restService());
    }
  }
}

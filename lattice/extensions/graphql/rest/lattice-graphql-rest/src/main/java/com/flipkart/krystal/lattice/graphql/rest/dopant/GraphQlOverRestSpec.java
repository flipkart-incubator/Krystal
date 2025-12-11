package com.flipkart.krystal.lattice.graphql.rest.dopant;

import com.flipkart.krystal.lattice.core.doping.AutoConfigure;
import com.flipkart.krystal.lattice.core.doping.DopantSpecBuilder;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpec;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpecBuilder;
import com.flipkart.krystal.lattice.ext.rest.RestServiceDopantSpec;
import com.flipkart.krystal.lattice.graphql.rest.dispatch.GraphQlOperationExecutor;
import com.flipkart.krystal.lattice.krystex.KrystexDopantSpec.KrystexDopantSpecBuilder;
import com.flipkart.krystal.lattice.vajram.VajramDopantSpec;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationDispatch;
import java.util.List;
import lombok.Builder;

@Builder(buildMethodName = "_buildSpec")
public record GraphQlOverRestSpec(
    GraphQlOperationExecutor graphQlOperationExecutor,
    GraphQlOperationDispatch graphQlOperationDispatch)
    implements SimpleDopantSpec<GraphQlOverRestDopant> {

  @Override
  public Class<? extends GraphQlOverRestDopant> dopantClass() {
    return GraphQlOverRestDopant.class;
  }

  public void _configure(
      @AutoConfigure KrystexDopantSpecBuilder vajramDopantSpecBuilder,
      GraphQlOperationExecutor graphQlOperationExecutor,
      GraphQlOperationDispatch graphQlOperationDispatch) {
    vajramDopantSpecBuilder.configureExecutorWith(graphQlOperationExecutor);
    vajramDopantSpecBuilder.addTraitDispatchPolicies(graphQlOperationDispatch);
  }

  @Override
  public String _dopantType() {
    return GraphQlOverRestDopant.DOPANT_TYPE;
  }

  public static final class GraphQlOverRestSpecBuilder
      extends SimpleDopantSpecBuilder<GraphQlOverRestSpec> {

    @Override
    public List<DopantSpecBuilder<?, ?, ?>> getAdditionalDopants() {
      return List.of(VajramDopantSpec.builder(), RestServiceDopantSpec.builder());
    }
  }
}

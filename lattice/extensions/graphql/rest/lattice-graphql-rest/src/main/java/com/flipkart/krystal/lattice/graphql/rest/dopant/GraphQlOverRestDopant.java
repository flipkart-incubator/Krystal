package com.flipkart.krystal.lattice.graphql.rest.dopant;

import com.flipkart.krystal.lattice.core.doping.SimpleDopant;
import com.flipkart.krystal.lattice.graphql.rest.dispatch.GraphQlOperationExecutor;
import com.flipkart.krystal.lattice.graphql.rest.dopant.GraphQlOverRestDopantSpec.GraphQlOverRestDopantSpecBuilder;
import com.flipkart.krystal.lattice.vajram.VajramDopant;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationAggregate_Req;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationDispatch;
import graphql.schema.idl.TypeDefinitionRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Set;

public abstract class GraphQlOverRestDopant implements SimpleDopant {

  public static final String DOPANT_TYPE = "krystal.lattice.graphql.overRest";

  protected GraphQlOverRestDopant(
      GraphQlOverRestDopantInitData initData,
      Set<Class<? extends GraphQlOperationAggregate_Req>> graphQlOperationVajrams) {
    initData
        .vajramDopant()
        .graph()
        .registerTraitDispatchPolicies(
            new GraphQlOperationDispatch(
                initData.vajramDopant().graph(),
                initData.typeDefinitionRegistry(),
                graphQlOperationVajrams));
    initData.vajramDopant().configureKryonExecutor(initData.graphQlOperationExecutor());
  }

  public static GraphQlOverRestDopantSpecBuilder graphQlOverRest() {
    return GraphQlOverRestDopantSpec.builder();
  }

  @Singleton
  public record GraphQlOverRestDopantInitData(
      VajramDopant vajramDopant,
      TypeDefinitionRegistry typeDefinitionRegistry,
      GraphQlOperationExecutor graphQlOperationExecutor) {
    @Inject
    public GraphQlOverRestDopantInitData {}
  }
}

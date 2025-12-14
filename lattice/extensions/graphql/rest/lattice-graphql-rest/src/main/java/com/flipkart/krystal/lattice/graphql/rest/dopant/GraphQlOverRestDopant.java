package com.flipkart.krystal.lattice.graphql.rest.dopant;

import static com.flipkart.krystal.lattice.graphql.rest.dopant.GraphQlOverRestDopant.DOPANT_TYPE;

import com.flipkart.krystal.lattice.core.di.Produces;
import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.lattice.core.doping.SimpleDopant;
import com.flipkart.krystal.vajram.graphql.api.schema.GraphQlLoader;
import graphql.GraphQL;
import graphql.schema.idl.TypeDefinitionRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@DopantType(DOPANT_TYPE)
@Singleton
public final class GraphQlOverRestDopant implements SimpleDopant {

  public static final String DOPANT_TYPE = "krystal.lattice.graphql.overRest";
  private final GraphQlLoader graphQlLoader;

  @Inject
  public GraphQlOverRestDopant() {
    this.graphQlLoader = new GraphQlLoader();
  }

  @Produces(inScope = Singleton.class)
  public GraphQL graphQL() {
    return graphQlLoader.getGraphQl();
  }

  @Produces(inScope = Singleton.class)
  public TypeDefinitionRegistry typeDefinitionRegistry() {
    return graphQlLoader.getTypeDefinitionRegistry();
  }
}

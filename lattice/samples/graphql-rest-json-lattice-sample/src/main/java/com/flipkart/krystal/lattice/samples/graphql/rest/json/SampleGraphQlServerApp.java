package com.flipkart.krystal.lattice.samples.graphql.rest.json;

import static com.flipkart.krystal.lattice.core.execution.ThreadingStrategy.POOLED_NATIVE_THREAD_PER_REQUEST;

import com.flipkart.krystal.krystex.decoration.DecorationOrdering;
import com.flipkart.krystal.lattice.core.LatticeApp;
import com.flipkart.krystal.lattice.core.LatticeApplication;
import com.flipkart.krystal.lattice.core.doping.DopeWith;
import com.flipkart.krystal.lattice.core.execution.ThreadingStrategySpec;
import com.flipkart.krystal.lattice.core.execution.ThreadingStrategySpec.ThreadingStrategySpecBuilder;
import com.flipkart.krystal.lattice.ext.cdi.CdiFramework;
import com.flipkart.krystal.lattice.ext.quarkus.app.QuarkusApplicationSpec;
import com.flipkart.krystal.lattice.ext.quarkus.app.QuarkusApplicationSpec.QuarkusApplicationSpecBuilder;
import com.flipkart.krystal.lattice.ext.rest.RestService;
import com.flipkart.krystal.lattice.ext.rest.RestServiceDopantSpec;
import com.flipkart.krystal.lattice.ext.rest.RestServiceDopantSpec.RestServiceDopantSpecBuilder;
import com.flipkart.krystal.lattice.graphql.rest.dispatch.GraphQlOperationExecutor;
import com.flipkart.krystal.lattice.graphql.rest.dopant.GraphQlOverRestSpec;
import com.flipkart.krystal.lattice.graphql.rest.dopant.GraphQlOverRestSpec.GraphQlOverRestSpecBuilder;
import com.flipkart.krystal.lattice.graphql.rest.restapi.HttpPostGraphQl;
import com.flipkart.krystal.lattice.krystex.KrystexDopantSpec;
import com.flipkart.krystal.lattice.krystex.KrystexDopantSpec.KrystexDopantSpecBuilder;
import com.flipkart.krystal.lattice.vajram.VajramDopantSpec;
import com.flipkart.krystal.lattice.vajram.VajramDopantSpec.VajramDopantSpecBuilder;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationAggregate;
import com.flipkart.krystal.vajramexecutor.krystex.VajramGraph;
import com.flipkart.krystal.vajramexecutor.krystex.traits.DefaultTraitDispatcher;

@LatticeApp(
    description = "A sample graphql server on rest+json",
    dependencyInjectionFramework = CdiFramework.class)
@RestService(resourceVajrams = HttpPostGraphQl.class)
public abstract class SampleGraphQlServerApp extends LatticeApplication {

  @DopeWith
  public static ThreadingStrategySpecBuilder threading() {
    return ThreadingStrategySpec.builder().threadingStrategy(POOLED_NATIVE_THREAD_PER_REQUEST);
  }

  @DopeWith
  static VajramDopantSpecBuilder vajrams() {
    return VajramDopantSpec.builder()
        .vajramGraphBuilder(
            VajramGraph.builder()
                .loadFromPackage("com.flipkart.krystal.lattice.samples.graphql.rest.json")
                .loadClasses(GraphQlOperationAggregate.class, HttpPostGraphQl.class));
  }

  @DopeWith
  static KrystexDopantSpecBuilder krystex() {
    return KrystexDopantSpec.builder()
        .configureExecutorWith(
            configBuilder ->
                configBuilder.decorationOrdering(
                    new DecorationOrdering(
                        GraphQlOperationExecutor.DECORATOR_TYPE,
                        DefaultTraitDispatcher.DECORATOR_TYPE)));
  }

  @DopeWith
  static RestServiceDopantSpecBuilder rest() {
    return RestServiceDopantSpec.builder();
  }

  @DopeWith
  static QuarkusApplicationSpecBuilder quarkusRest() {
    return QuarkusApplicationSpec.builder();
  }

  @DopeWith
  static GraphQlOverRestSpecBuilder graphQlServer() {
    return GraphQlOverRestSpec.builder();
  }
}

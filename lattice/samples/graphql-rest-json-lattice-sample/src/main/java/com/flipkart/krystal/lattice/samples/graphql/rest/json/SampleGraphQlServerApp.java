package com.flipkart.krystal.lattice.samples.graphql.rest.json;

import static com.flipkart.krystal.lattice.core.execution.ThreadingStrategy.NATIVE_THREAD_PER_REQUEST;
import static com.flipkart.krystal.lattice.core.execution.ThreadingStrategyDopant.threadingStrategy;
import static com.flipkart.krystal.lattice.ext.rest.quarkus.restServer.QuarkusRestServerDopant.quarkusRestServer;
import static com.flipkart.krystal.lattice.graphql.rest.dopant.GraphQlOverRestDopant.graphQlOverRest;
import static com.flipkart.krystal.lattice.vajram.VajramDopant.vajramGraph;

import com.flipkart.krystal.krystex.decoration.DecorationOrdering;
import com.flipkart.krystal.lattice.core.LatticeApp;
import com.flipkart.krystal.lattice.core.LatticeAppBootstrap;
import com.flipkart.krystal.lattice.core.LatticeApplication;
import com.flipkart.krystal.lattice.ext.guice.GuiceModuleBinder;
import com.flipkart.krystal.lattice.ext.guice.servlet.GuiceServletModuleBinder;
import com.flipkart.krystal.lattice.ext.rest.RestService;
import com.flipkart.krystal.lattice.graphql.rest.dispatch.GraphQlOperationExecutor;
import com.flipkart.krystal.lattice.graphql.rest.restapi.HttpPostGraphQl;
import com.flipkart.krystal.lattice.vajram.VajramDopantSpec.VajramDopantSpecBuilder;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationAggregate;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.traits.DefaultTraitDispatcher;

@LatticeApp(
    description = "A sample graphql server on rest+json",
    dependencyInjectionBinder = GuiceServletModuleBinder.class)
@RestService(resourceVajrams = HttpPostGraphQl.class)
public abstract class SampleGraphQlServerApp extends LatticeApplication {
  @Override
  public void bootstrap(LatticeAppBootstrap app) {
    app.dopeWith(threadingStrategy().threadingStrategy(NATIVE_THREAD_PER_REQUEST))
        .dopeWith(configured(vajramGraph()))
        .dopeWith(quarkusRestServer())
        .dopeWith(graphQlOverRest());
  }

  private static VajramDopantSpecBuilder configured(
      VajramDopantSpecBuilder vajramDopantSpecBuilder) {
    return vajramDopantSpecBuilder
        .graphBuilder(
            VajramKryonGraph.builder()
                .loadFromPackage("com.flipkart.krystal.lattice.samples.graphql.rest.json")
                .loadClasses(GraphQlOperationAggregate.class, HttpPostGraphQl.class))
        .configureKryonExecutor(
            configBuilder ->
                configBuilder.decorationOrdering(
                    new DecorationOrdering(
                        GraphQlOperationExecutor.DECORATOR_TYPE,
                        DefaultTraitDispatcher.DECORATOR_TYPE)));
  }

  @Override
  public GuiceModuleBinder getDependencyInjectionBinder() {
    return new GuiceModuleBinder(new SampleGraphQlServerModule());
  }
}

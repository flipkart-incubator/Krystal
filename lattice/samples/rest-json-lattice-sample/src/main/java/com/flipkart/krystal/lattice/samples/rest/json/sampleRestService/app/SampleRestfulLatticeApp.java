package com.flipkart.krystal.lattice.samples.rest.json.sampleRestService.app;

import static com.flipkart.krystal.lattice.core.execution.ThreadingStrategy.NATIVE_THREAD_PER_REQUEST;
import static com.flipkart.krystal.lattice.core.execution.ThreadingStrategyDopant.threadingStrategy;
import static com.flipkart.krystal.lattice.vajram.VajramDopant.vajramGraph;
import static flipkart.krystal.lattice.ext.rest.quarkus.restServer.QuarkusRestServerDopant.quarkusRestServer;

import com.flipkart.krystal.lattice.core.LatticeApp;
import com.flipkart.krystal.lattice.core.LatticeAppBootstrap;
import com.flipkart.krystal.lattice.core.LatticeApplication;
import com.flipkart.krystal.lattice.ext.guice.GuiceModuleBinder;
import com.flipkart.krystal.lattice.ext.guice.servlet.GuiceServletModuleBinder;
import com.flipkart.krystal.lattice.samples.rest.json.sampleRestService.RestLatticeSample;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import flipkart.krystal.lattice.ext.rest.quarkus.restServer.RestService;

@RestService(pathPrefix = "sample", resourceVajrams = RestLatticeSample.class)
@LatticeApp(
    description = "A sample Lattice Application",
    dependencyInjectionBinder = GuiceServletModuleBinder.class)
public abstract class SampleRestfulLatticeApp extends LatticeApplication {

  @Override
  public void bootstrap(LatticeAppBootstrap app) {
    app.dopeWith(threadingStrategy().threadingStrategy(NATIVE_THREAD_PER_REQUEST))
        .dopeWith(
            vajramGraph()
                .graphBuilder(
                    VajramKryonGraph.builder()
                        .loadFromPackage(
                            "com.flipkart.krystal.lattice.samples.proto3.sampleProtoService.app")
                        .loadClasses(RestLatticeSample.class)))
        .dopeWith(quarkusRestServer());
  }

  @Override
  public GuiceModuleBinder getDependencyInjectionBinder() {
    return new GuiceModuleBinder(new CustomGuiceModule());
  }
}

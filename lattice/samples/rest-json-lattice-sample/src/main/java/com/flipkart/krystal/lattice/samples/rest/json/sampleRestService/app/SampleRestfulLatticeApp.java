package com.flipkart.krystal.lattice.samples.rest.json.sampleRestService.app;

import static com.flipkart.krystal.lattice.core.execution.ThreadingStrategy.NATIVE_THREAD_PER_REQUEST;
import static com.flipkart.krystal.lattice.core.execution.ThreadingStrategyDopant.threadingStrategy;
import static com.flipkart.krystal.lattice.ext.rest.quarkus.restServer.QuarkusRestServerDopant.quarkusRestServer;
import static com.flipkart.krystal.lattice.vajram.VajramDopant.vajramGraph;

import com.flipkart.krystal.lattice.core.LatticeApp;
import com.flipkart.krystal.lattice.core.LatticeAppBootstrap;
import com.flipkart.krystal.lattice.core.LatticeApplication;
import com.flipkart.krystal.lattice.ext.guice.GuiceModuleBinder;
import com.flipkart.krystal.lattice.ext.guice.servlet.GuiceServletModuleBinder;
import com.flipkart.krystal.lattice.rest.RestService;
import com.flipkart.krystal.lattice.samples.rest.json.sampleRestService.logic.RestGetMappingLatticeSample;
import com.flipkart.krystal.lattice.samples.rest.json.sampleRestService.logic.RestHeadMappingLatticeSample;
import com.flipkart.krystal.lattice.samples.rest.json.sampleRestService.logic.RestLatticeSample;
import com.flipkart.krystal.lattice.samples.rest.json.sampleRestService.logic.RestPostComplexPathMatching;
import com.flipkart.krystal.lattice.samples.rest.json.sampleRestService.logic.RestPostMappingLatticeSample;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;

@LatticeApp(
    description = "A sample Lattice Application",
    dependencyInjectionBinder = GuiceServletModuleBinder.class)
@RestService(
    resourceVajrams = {
      RestLatticeSample.class,
      RestGetMappingLatticeSample.class,
      RestPostMappingLatticeSample.class,
      RestPostComplexPathMatching.class,
      RestHeadMappingLatticeSample.class
    })
public abstract class SampleRestfulLatticeApp extends LatticeApplication {

  @Override
  public void bootstrap(LatticeAppBootstrap app) {
    app.dopeWith(threadingStrategy().threadingStrategy(NATIVE_THREAD_PER_REQUEST))
        .dopeWith(
            vajramGraph()
                .graphBuilder(
                    VajramKryonGraph.builder()
                        .loadClasses(
                            RestLatticeSample.class,
                            RestGetMappingLatticeSample.class,
                            RestPostMappingLatticeSample.class,
                            RestPostComplexPathMatching.class,
                            RestHeadMappingLatticeSample.class)))
        .dopeWith(quarkusRestServer());
  }

  @Override
  public GuiceModuleBinder getDependencyInjectionBinder() {
    return new GuiceModuleBinder(new CustomGuiceModule());
  }
}

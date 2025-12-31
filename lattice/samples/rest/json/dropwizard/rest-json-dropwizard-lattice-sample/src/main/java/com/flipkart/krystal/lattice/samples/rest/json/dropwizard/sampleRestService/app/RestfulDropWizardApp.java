package com.flipkart.krystal.lattice.samples.rest.json.dropwizard.sampleRestService.app;

import static com.flipkart.krystal.lattice.core.execution.ThreadingStrategy.POOLED_NATIVE_THREAD_PER_REQUEST;

import com.flipkart.krystal.lattice.core.LatticeApp;
import com.flipkart.krystal.lattice.core.LatticeApplication;
import com.flipkart.krystal.lattice.core.doping.DopeWith;
import com.flipkart.krystal.lattice.core.execution.ThreadingStrategySpec;
import com.flipkart.krystal.lattice.core.execution.ThreadingStrategySpec.ThreadingStrategySpecBuilder;
import com.flipkart.krystal.lattice.ext.guice.GuiceFramework;
import com.flipkart.krystal.lattice.ext.guice.servlet.GuiceServletFramework;
import com.flipkart.krystal.lattice.ext.rest.RestService;
import com.flipkart.krystal.lattice.ext.rest.RestServiceDopantSpec;
import com.flipkart.krystal.lattice.ext.rest.RestServiceDopantSpec.RestServiceDopantSpecBuilder;
import com.flipkart.krystal.lattice.ext.rest.dropwizard.DropwizardServerDopantSpec;
import com.flipkart.krystal.lattice.ext.rest.dropwizard.DropwizardServerDopantSpec.DropwizardServerDopantSpecBuilder;
import com.flipkart.krystal.lattice.krystex.KrystexDopantSpec;
import com.flipkart.krystal.lattice.krystex.KrystexDopantSpec.KrystexDopantSpecBuilder;
import com.flipkart.krystal.lattice.samples.rest.json.dropwizard.sampleRestService.logic.RestGetMappingLatticeSample;
import com.flipkart.krystal.lattice.samples.rest.json.dropwizard.sampleRestService.logic.RestHeadMappingLatticeSample;
import com.flipkart.krystal.lattice.samples.rest.json.dropwizard.sampleRestService.logic.RestLatticeSample;
import com.flipkart.krystal.lattice.samples.rest.json.dropwizard.sampleRestService.logic.RestPostComplexPathMatching;
import com.flipkart.krystal.lattice.samples.rest.json.dropwizard.sampleRestService.logic.RestPostMappingLatticeSample;
import com.flipkart.krystal.lattice.samples.rest.json.dropwizard.sampleRestService.logic.RestStreamingSample;
import com.flipkart.krystal.lattice.vajram.VajramDopantSpec;
import com.flipkart.krystal.lattice.vajram.VajramDopantSpec.VajramDopantSpecBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.VajramGraph;

@LatticeApp(
    description = "A sample Lattice Application",
    dependencyInjectionFramework = GuiceServletFramework.class)
@RestService(
    resourceVajrams = {
      RestLatticeSample.class,
      RestGetMappingLatticeSample.class,
      RestPostMappingLatticeSample.class,
      RestPostComplexPathMatching.class,
      RestHeadMappingLatticeSample.class,
      RestStreamingSample.class
    })
public abstract class RestfulDropWizardApp extends LatticeApplication {

  @DopeWith
  public static ThreadingStrategySpecBuilder threading() {
    return ThreadingStrategySpec.builder().threadingStrategy(POOLED_NATIVE_THREAD_PER_REQUEST);
  }

  @DopeWith
  public static VajramDopantSpecBuilder vajramGraph() {
    return VajramDopantSpec.builder()
        .vajramGraphBuilder(
            VajramGraph.builder()
                .loadClasses(
                    RestLatticeSample.class,
                    RestGetMappingLatticeSample.class,
                    RestPostMappingLatticeSample.class,
                    RestPostComplexPathMatching.class,
                    RestHeadMappingLatticeSample.class,
                    RestStreamingSample.class));
  }

  @DopeWith
  public static KrystexDopantSpecBuilder krystex() {
    return KrystexDopantSpec.builder();
  }

  @DopeWith
  public static RestServiceDopantSpecBuilder rest() {
    return RestServiceDopantSpec.builder();
  }

  @DopeWith
  public static DropwizardServerDopantSpecBuilder dropwizardServer() {
    return DropwizardServerDopantSpec.builder();
  }

  @Override
  public GuiceFramework getDependencyInjector() {
    return new GuiceFramework(this);
  }
}

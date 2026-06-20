package com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.app;

import static com.flipkart.krystal.lattice.core.execution.ThreadingStrategy.POOLED_NATIVE_THREAD_PER_REQUEST;

import com.flipkart.krystal.krystex.VajramGraph;
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
import com.flipkart.krystal.lattice.krystex.KrystexDopantSpec;
import com.flipkart.krystal.lattice.krystex.KrystexDopantSpec.KrystexDopantSpecBuilder;
import com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.logic.ForyGetSample;
import com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.logic.ForyPostSample;
import com.flipkart.krystal.lattice.vajram.VajramDopantSpec;
import com.flipkart.krystal.lattice.vajram.VajramDopantSpec.VajramDopantSpecBuilder;

@LatticeApp(
    description = "A sample Lattice Application demonstrating Apache Fory serde",
    dependencyInjectionFramework = CdiFramework.class)
@RestService(resourceVajrams = {ForyGetSample.class, ForyPostSample.class})
public abstract class ForyRestApp extends LatticeApplication {

  @DopeWith
  public static ThreadingStrategySpecBuilder threading() {
    return ThreadingStrategySpec.builder().threadingStrategy(POOLED_NATIVE_THREAD_PER_REQUEST);
  }

  @DopeWith
  public static VajramDopantSpecBuilder vajramGraph() {
    return VajramDopantSpec.builder()
        .vajramGraphBuilder(
            VajramGraph.builder().loadClasses(ForyGetSample.class, ForyPostSample.class));
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
  static QuarkusApplicationSpecBuilder quarkusApplication() {
    return QuarkusApplicationSpec.builder();
  }
}

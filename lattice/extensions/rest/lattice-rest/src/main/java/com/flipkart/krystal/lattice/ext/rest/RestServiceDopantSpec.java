package com.flipkart.krystal.lattice.ext.rest;

import static com.flipkart.krystal.lattice.ext.rest.RestServiceDopant.REST_SERVICE_DOPANT_TYPE;

import com.flipkart.krystal.lattice.core.doping.AutoConfigure;
import com.flipkart.krystal.lattice.core.doping.DopantSpec;
import com.flipkart.krystal.lattice.core.doping.DopantSpecBuilder;
import com.flipkart.krystal.lattice.core.execution.ThreadingStrategySpec.ThreadingStrategySpecBuilder;
import com.flipkart.krystal.lattice.ext.rest.config.RestServiceDopantConfig;
import java.util.List;
import lombok.Builder;
import lombok.Singular;

@Builder(buildMethodName = "_buildSpec")
public record RestServiceDopantSpec(
    boolean serveStaticKrystalCallGraph, @Singular List<Object> customJakartaResources)
    implements DopantSpec<RestService, RestServiceDopantConfig, RestServiceDopant> {

  @Override
  public Class<RestServiceDopant> dopantClass() {
    return RestServiceDopant.class;
  }

  public void autoConfigure(
      @AutoConfigure ThreadingStrategySpecBuilder threadingStrategyDopantBuilder) {}

  @Override
  public Class<RestServiceDopantConfig> _configurationType() {
    return RestServiceDopantConfig.class;
  }

  @Override
  public String _dopantType() {
    return REST_SERVICE_DOPANT_TYPE;
  }

  public static final class RestServiceDopantSpecBuilder
      implements DopantSpecBuilder<RestService, RestServiceDopantConfig, RestServiceDopantSpec> {

    public RestServiceDopantSpecBuilder serveStaticKrystalCallGraph() {
      return this.serveStaticKrystalCallGraph(true);
    }

    @SuppressWarnings("SameParameterValue")
    private RestServiceDopantSpecBuilder serveStaticKrystalCallGraph(
        boolean serveStaticKrystalCallGraph) {
      this.serveStaticKrystalCallGraph = serveStaticKrystalCallGraph;
      return this;
    }

    @Override
    public Class<RestService> _annotationType() {
      return RestService.class;
    }
  }
}

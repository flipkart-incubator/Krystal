package com.flipkart.krystal.lattice.ext.rest;

import static com.flipkart.krystal.lattice.ext.rest.RestServiceDopant.REST_SERVICE_DOPANT_TYPE;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.lattice.core.doping.DopantSpec;
import com.flipkart.krystal.lattice.core.doping.DopantSpecBuilder;
import com.flipkart.krystal.lattice.ext.rest.config.RestServiceDopantConfig;
import lombok.Builder;
import org.checkerframework.checker.nullness.qual.Nullable;

@Builder
record RestServiceDopantSpec(boolean serveStaticKrystalCallGraph, RestServiceDopantConfig config)
    implements DopantSpec<RestService, RestServiceDopantConfig, RestServiceDopant> {

  @Override
  public Class<RestServiceDopant> dopantClass() {
    return RestServiceDopant.class;
  }

  @SuppressWarnings("ClassEscapesDefinedScope")
  public static class RestServiceDopantSpecBuilder
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
    public RestServiceDopantSpec _buildSpec(
        @Nullable RestService annotation, RestServiceDopantConfig config) {
      return new RestServiceDopantSpec(
          serveStaticKrystalCallGraph,
          requireNonNull(config, "RestServiceDopantConfig cannot be null"));
    }

    @Override
    public Class<RestService> _annotationType() {
      return RestService.class;
    }

    @Override
    public Class<RestServiceDopantConfig> _configurationType() {
      return RestServiceDopantConfig.class;
    }

    @Override
    public String _dopantType() {
      return REST_SERVICE_DOPANT_TYPE;
    }
  }
}

package com.flipkart.krystal.lattice.rest;

import static com.flipkart.krystal.lattice.rest.RestServiceDopant.REST_SERVICE_DOPANT_TYPE;

import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoConfiguration;
import com.flipkart.krystal.lattice.core.doping.DopantSpec;
import com.flipkart.krystal.lattice.core.doping.DopantSpecBuilderWithAnnotation;
import lombok.Builder;
import org.checkerframework.checker.nullness.qual.Nullable;

@Builder
record RestServiceDopantSpec(boolean serveStaticKrystalCallGraph)
    implements DopantSpec<RestService, NoConfiguration, RestServiceDopant> {

  @Override
  public Class<RestServiceDopant> dopantClass() {
    return RestServiceDopant.class;
  }

  @SuppressWarnings("ClassEscapesDefinedScope")
  public static class RestServiceDopantSpecBuilder
      extends DopantSpecBuilderWithAnnotation<RestService, RestServiceDopantSpec> {

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
    public RestServiceDopantSpec _buildSpec(@Nullable RestService annotation) {
      return new RestServiceDopantSpec(serveStaticKrystalCallGraph);
    }

    @Override
    public Class<RestService> _annotationType() {
      return RestService.class;
    }

    @Override
    public String _dopantType() {
      return REST_SERVICE_DOPANT_TYPE;
    }
  }
}

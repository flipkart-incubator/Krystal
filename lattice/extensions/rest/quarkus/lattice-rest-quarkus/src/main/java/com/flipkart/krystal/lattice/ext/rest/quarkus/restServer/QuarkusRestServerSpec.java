package com.flipkart.krystal.lattice.ext.rest.quarkus.restServer;

import static com.flipkart.krystal.lattice.ext.quarkus.app.QuarkusApplicationDopant.quarkusApplication;
import static com.flipkart.krystal.lattice.rest.RestServiceDopant.restService;
import static com.flipkart.krystal.lattice.ext.rest.quarkus.restServer.QuarkusRestServerDopant.REST_SERVER_DOPANT_TYPE;

import com.flipkart.krystal.lattice.core.doping.Dopant;
import com.flipkart.krystal.lattice.core.doping.DopantSpec;
import com.flipkart.krystal.lattice.core.doping.DopantSpecBuilder;
import com.flipkart.krystal.lattice.rest.RestService;
import java.util.List;
import lombok.Builder;
import org.checkerframework.checker.nullness.qual.Nullable;

@Builder
record QuarkusRestServerSpec()
    implements DopantSpec<RestService, QuarkusRestServerConfig, QuarkusRestServerDopant> {

  @Override
  public Class<? extends Dopant<RestService, QuarkusRestServerConfig>> dopantClass() {
    return QuarkusRestServerDopant.class;
  }

  public static class QuarkusRestServerSpecBuilder
      implements DopantSpecBuilder<RestService, QuarkusRestServerConfig, QuarkusRestServerSpec> {

    @Override
    public List<DopantSpecBuilder<?, ?, ?>> getAdditionalDopants() {
      return List.of(quarkusApplication(), restService());
    }

    @Override
    public QuarkusRestServerSpec _buildSpec(
        @Nullable RestService annotation, @Nullable QuarkusRestServerConfig configuration) {
      return new QuarkusRestServerSpec();
    }

    @Override
    public Class<RestService> _annotationType() {
      return RestService.class;
    }

    @Override
    public Class<QuarkusRestServerConfig> _configurationType() {
      return QuarkusRestServerConfig.class;
    }

    @Override
    public String _dopantType() {
      return REST_SERVER_DOPANT_TYPE;
    }
  }
}

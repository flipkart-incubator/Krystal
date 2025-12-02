package com.flipkart.krystal.lattice.ext.rest.quarkus.restServer;

import static com.flipkart.krystal.lattice.ext.quarkus.app.QuarkusApplicationDopant.quarkusApplication;
import static com.flipkart.krystal.lattice.ext.rest.RestServiceDopant.restService;
import static com.flipkart.krystal.lattice.ext.rest.quarkus.restServer.QuarkusRestServerDopant.QUARKUS_REST_SERVER_DOPANT_TYPE;

import com.flipkart.krystal.lattice.core.doping.DopantSpecBuilder;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpec;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpecBuilder;
import java.util.List;
import lombok.Builder;

@Builder
record QuarkusRestServerSpec() implements SimpleDopantSpec<QuarkusRestServerDopant> {

  @Override
  public Class<? extends QuarkusRestServerDopant> dopantClass() {
    return QuarkusRestServerDopant.class;
  }

  @SuppressWarnings("ClassEscapesDefinedScope")
  public static class QuarkusRestServerSpecBuilder
      extends SimpleDopantSpecBuilder<QuarkusRestServerSpec> {

    @Override
    public List<DopantSpecBuilder<?, ?, ?>> getAdditionalDopants() {
      return List.of(quarkusApplication(), restService());
    }

    @Override
    public QuarkusRestServerSpec _buildSpec() {
      return new QuarkusRestServerSpec();
    }

    @Override
    public String _dopantType() {
      return QUARKUS_REST_SERVER_DOPANT_TYPE;
    }
  }
}

package com.flipkart.krystal.lattice.ext.rest.quarkus.restServer;

import static com.flipkart.krystal.lattice.ext.rest.quarkus.restServer.QuarkusRestServerDopant.QUARKUS_REST_SERVER_DOPANT_TYPE;

import com.flipkart.krystal.lattice.core.doping.DopantConfig;
import com.flipkart.krystal.lattice.core.doping.DopantType;

@DopantType(QUARKUS_REST_SERVER_DOPANT_TYPE)
record QuarkusRestServerConfig(int port) implements DopantConfig {

  @Override
  public String _dopantType() {
    return QUARKUS_REST_SERVER_DOPANT_TYPE;
  }
}

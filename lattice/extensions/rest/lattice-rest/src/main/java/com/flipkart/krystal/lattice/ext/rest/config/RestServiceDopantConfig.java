package com.flipkart.krystal.lattice.ext.rest.config;

import static com.flipkart.krystal.lattice.ext.rest.RestServiceDopant.REST_SERVICE_DOPANT_TYPE;

import com.flipkart.krystal.lattice.core.doping.DopantConfig;
import com.flipkart.krystal.lattice.core.doping.DopantType;

/**
 * @param applicationServer config for the "main" application server
 */
@DopantType(REST_SERVICE_DOPANT_TYPE)
public record RestServiceDopantConfig(RestServerConfig applicationServer) implements DopantConfig {

  public RestServiceDopantConfig {
    applicationServer = applicationServer.withNameIfNotNamed("application");
  }

  @Override
  public String _dopantType() {
    return REST_SERVICE_DOPANT_TYPE;
  }
}

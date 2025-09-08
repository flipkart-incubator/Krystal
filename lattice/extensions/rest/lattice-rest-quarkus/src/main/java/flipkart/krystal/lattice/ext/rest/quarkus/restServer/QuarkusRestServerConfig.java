package flipkart.krystal.lattice.ext.rest.quarkus.restServer;

import static flipkart.krystal.lattice.ext.rest.quarkus.restServer.QuarkusRestServerDopant.REST_SERVER_DOPANT_TYPE;

import com.flipkart.krystal.lattice.core.doping.DopantConfig;
import com.flipkart.krystal.lattice.core.doping.DopantType;

@DopantType(REST_SERVER_DOPANT_TYPE)
record QuarkusRestServerConfig(int port) implements DopantConfig {

  @Override
  public String _dopantType() {
    return REST_SERVER_DOPANT_TYPE;
  }
}

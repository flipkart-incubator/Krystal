package com.flipkart.krystal.lattice.ext.a2a;

import static com.flipkart.krystal.lattice.ext.a2a.A2AServerDopant.A2A_SERVER_DOPANT_TYPE;

import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoConfiguration;
import com.flipkart.krystal.lattice.core.doping.DopantSpec;
import com.flipkart.krystal.lattice.core.doping.DopantSpecBuilder;
import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.lattice.ext.a2a.api.A2AServer;
import lombok.Builder;

@DopantType(A2A_SERVER_DOPANT_TYPE)
public record A2AServerDopantSpec()
    implements DopantSpec<A2AServer, NoConfiguration, A2AServerDopant> {

  @Builder(buildMethodName = "_buildSpec")
  public A2AServerDopantSpec {}

  @Override
  public Class<? extends A2AServerDopant> dopantClass() {
    return A2AServerDopant.class;
  }

  @Override
  public String _dopantType() {
    return A2A_SERVER_DOPANT_TYPE;
  }

  @Override
  public Class<NoConfiguration> _configurationType() {
    return NoConfiguration.class;
  }

  public static final class A2AServerDopantSpecBuilder
      implements DopantSpecBuilder<A2AServer, NoConfiguration, A2AServerDopantSpec> {

    @Override
    public Class<A2AServer> _annotationType() {
      return A2AServer.class;
    }
  }
}

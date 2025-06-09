package flipkart.krystal.lattice.ext.rest.quarkus.app;

import static flipkart.krystal.lattice.ext.rest.quarkus.app.QuarkusApplicationDopant.APPLICATION_DOPANT_TYPE;

import com.flipkart.krystal.lattice.core.doping.Dopant;
import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoAnnotation;
import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoConfiguration;
import com.flipkart.krystal.lattice.core.doping.DopantSpec;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpecBuilder;
import lombok.Builder;

@Builder
record QuarkusApplicationSpec()
    implements DopantSpec<NoAnnotation, NoConfiguration, QuarkusApplicationDopant> {

  @Override
  public Class<? extends Dopant<NoAnnotation, NoConfiguration>> dopantClass() {
    return QuarkusApplicationDopant.class;
  }

  public static class QuarkusApplicationSpecBuilder
      extends SimpleDopantSpecBuilder<QuarkusApplicationSpec> {

    @Override
    public QuarkusApplicationSpec _buildSpec() {
      return new QuarkusApplicationSpec();
    }

    @Override
    public String _dopantType() {
      return APPLICATION_DOPANT_TYPE;
    }
  }
}

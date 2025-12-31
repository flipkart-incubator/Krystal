package com.flipkart.krystal.lattice.ext.rest.dropwizard;

import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoAnnotation;
import com.flipkart.krystal.lattice.core.doping.DopantSpec;
import com.flipkart.krystal.lattice.core.doping.DopantSpecBuilderWithConfig;
import com.flipkart.krystal.lattice.core.doping.DopantType;
import lombok.Builder;

@DopantType(DropwizardServerDopant.DOPANT_TYPE)
@Builder(buildMethodName = "_buildSpec")
public final class DropwizardServerDopantSpec
    implements DopantSpec<NoAnnotation, DropwizardServerDopantConfig, DropwizardServerDopant> {

  @Override
  public Class<? extends DropwizardServerDopant> dopantClass() {
    return DropwizardServerDopant.class;
  }

  @Override
  public String _dopantType() {
    return DropwizardServerDopant.DOPANT_TYPE;
  }

  @Override
  public Class<DropwizardServerDopantConfig> _configurationType() {
    return DropwizardServerDopantConfig.class;
  }

  public static final class DropwizardServerDopantSpecBuilder
      extends DopantSpecBuilderWithConfig<
          DropwizardServerDopantConfig, DropwizardServerDopantSpec> {}
}

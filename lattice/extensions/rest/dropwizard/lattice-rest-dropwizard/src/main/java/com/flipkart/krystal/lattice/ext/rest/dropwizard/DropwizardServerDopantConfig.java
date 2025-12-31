package com.flipkart.krystal.lattice.ext.rest.dropwizard;

import com.flipkart.krystal.lattice.core.doping.DopantConfig;
import com.flipkart.krystal.lattice.core.doping.DopantType;
import java.util.List;

@DopantType(DropwizardServerDopant.DOPANT_TYPE)
public record DropwizardServerDopantConfig(List<String> args) implements DopantConfig {

  @Override
  public String _dopantType() {
    return DropwizardServerDopant.DOPANT_TYPE;
  }
}

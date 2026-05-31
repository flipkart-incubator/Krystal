package com.flipkart.krystal.lattice.ext.rest.dropwizard;

import com.flipkart.krystal.annos.NoAnnotation;
import com.flipkart.krystal.lattice.core.doping.DopantSpec;
import com.flipkart.krystal.lattice.core.doping.DopantSpecBuilderWithConfig;
import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;

@DopantType(DropwizardServerDopant.DOPANT_TYPE)
@Builder(buildMethodName = "_buildSpec")
public record DropwizardServerDopantSpec(List<EnvironmentEnricher> environmentEnrichers)
    implements DopantSpec<NoAnnotation, DropwizardServerDopantConfig, DropwizardServerDopant> {

  public DropwizardServerDopantSpec {
    environmentEnrichers = ImmutableList.copyOf(environmentEnrichers);
  }

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
          DropwizardServerDopantConfig, DropwizardServerDopantSpec> {

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") // Used in generated build method
    private final List<EnvironmentEnricher> environmentEnrichers = new ArrayList<>();

    public DropwizardServerDopantSpecBuilder environmentEnrichers(
        EnvironmentEnricher... environmentEnricher) {
      this.environmentEnrichers.addAll(Arrays.asList(environmentEnricher));
      return this;
    }

    public DropwizardServerDopantSpecBuilder environmentEnrichers(
        List<EnvironmentEnricher> environmentEnrichers) {
      this.environmentEnrichers.addAll(environmentEnrichers);
      return this;
    }
  }
}

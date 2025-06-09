package com.flipkart.krystal.lattice.core.execution;

import static com.flipkart.krystal.lattice.core.execution.ThreadingStrategyDopant.DOPANT_TYPE;

import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoAnnotation;
import com.flipkart.krystal.lattice.core.doping.DopantSpec;
import com.flipkart.krystal.lattice.core.doping.DopantSpecBuilderWithConfig;
import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.lattice.core.doping.MissingConfigurationException;
import lombok.Builder;
import org.checkerframework.checker.nullness.qual.Nullable;

@DopantType(DOPANT_TYPE)
@Builder(builderClassName = "ThreadingStrategySpecBuilder")
record ThreadingStrategySpec(ThreadingStrategy threadingStrategy, ThreadStrategyConfig config)
    implements DopantSpec<NoAnnotation, ThreadStrategyConfig, ThreadingStrategyDopant> {

  @Override
  public Class<? extends ThreadingStrategyDopant> dopantClass() {
    return ThreadingStrategyDopant.class;
  }

  @SuppressWarnings("ClassEscapesDefinedScope")
  public static class ThreadingStrategySpecBuilder
      extends DopantSpecBuilderWithConfig<ThreadStrategyConfig, ThreadingStrategySpec> {

    @Override
    public ThreadingStrategySpec _buildSpec(@Nullable ThreadStrategyConfig config) {
      if (config == null) {
        throw new MissingConfigurationException(
            "Configuration is mandatory for dopant '"
                + ThreadingStrategyDopant.class.getSimpleName()
                + "' of dopant type '"
                + DOPANT_TYPE
                + "'");
      }

      return this.config(config).build();
    }

    @Override
    public Class<ThreadStrategyConfig> _configurationType() {
      return ThreadStrategyConfig.class;
    }

    @Override
    public String _dopantType() {
      return DOPANT_TYPE;
    }
  }
}

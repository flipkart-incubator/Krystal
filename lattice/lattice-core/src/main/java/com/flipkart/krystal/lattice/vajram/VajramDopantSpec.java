package com.flipkart.krystal.lattice.vajram;

import com.flipkart.krystal.krystex.kryon.KryonExecutorConfigurator;
import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoAnnotation;
import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoConfiguration;
import com.flipkart.krystal.lattice.core.doping.DopantSpec;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig.KrystexVajramExecutorConfigBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.google.common.collect.ImmutableList;
import java.util.function.Consumer;

record VajramDopantSpec(
    VajramKryonGraph vajramGraph,
    ImmutableList<Consumer<KrystexVajramExecutorConfigBuilder>> krystexExecutorConfigurators,
    ImmutableList<KryonExecutorConfigurator> kryonExecutorConfigurators)
    implements DopantSpec<NoAnnotation, NoConfiguration, VajramDopant> {

  @Override
  public Class<VajramDopant> dopantClass() {
    return VajramDopant.class;
  }
}

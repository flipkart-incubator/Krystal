package com.flipkart.krystal.lattice.krystex;

import static com.flipkart.krystal.lattice.krystex.KrystexDopant.DOPANT_TYPE;
import static java.util.Objects.requireNonNullElse;

import com.flipkart.krystal.krystex.kryon.KryonExecutorConfigurator;
import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpec;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpecBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexGraph.KrystexGraphBuilder;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.Singular;

@Builder(buildMethodName = "_buildSpec")
public record KrystexDopantSpec(
    @Singular("buildKrystexGraphWith") List<Consumer<KrystexGraphBuilder>> buildKrystexGraphWith,
    @Singular("configureExecutorWith")
        ImmutableList<KryonExecutorConfigurator> configureExecutorWith)
    implements SimpleDopantSpec<KrystexDopant> {

  public KrystexDopantSpec {
    buildKrystexGraphWith = requireNonNullElse(buildKrystexGraphWith, List.of());
    configureExecutorWith = requireNonNullElse(configureExecutorWith, ImmutableList.of());
  }

  @Override
  public Class<? extends KrystexDopant> dopantClass() {
    return KrystexDopant.class;
  }

  @Override
  public String _dopantType() {
    return DOPANT_TYPE;
  }

  @DopantType(DOPANT_TYPE)
  public static final class KrystexDopantSpecBuilder
      extends SimpleDopantSpecBuilder<KrystexDopantSpec> {}
}

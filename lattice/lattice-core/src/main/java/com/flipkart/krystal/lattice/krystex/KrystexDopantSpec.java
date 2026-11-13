package com.flipkart.krystal.lattice.krystex;

import static com.flipkart.krystal.lattice.krystex.KrystexDopant.DOPANT_TYPE;
import static java.util.Objects.requireNonNullElse;

import com.flipkart.krystal.krystex.KrystexGraph.KrystexGraphBuilder;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfigurator;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpec;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpecBuilder;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.Singular;

public record KrystexDopantSpec(
    ImmutableList<Consumer<KrystexGraphBuilder>> buildKrystexGraphWith,
    ImmutableList<KryonExecutorConfigurator> configureExecutorWith)
    implements SimpleDopantSpec<KrystexDopant> {

  @Builder(buildMethodName = "_buildSpec")
  public KrystexDopantSpec(
      @Singular("buildKrystexGraphWith") List<Consumer<KrystexGraphBuilder>> buildKrystexGraphWith,
      @Singular("configureExecutorWith") List<KryonExecutorConfigurator> configureExecutorWith) {
    this(
        ImmutableList.copyOf(requireNonNullElse(buildKrystexGraphWith, ImmutableList.of())),
        ImmutableList.copyOf(requireNonNullElse(configureExecutorWith, List.of())));
  }

  @Override
  public Class<? extends KrystexDopant> dopantClass() {
    return KrystexDopant.class;
  }

  @Override
  public String _dopantType() {
    return DOPANT_TYPE;
  }

  public static final class KrystexDopantSpecBuilder
      extends SimpleDopantSpecBuilder<KrystexDopantSpec> {}
}

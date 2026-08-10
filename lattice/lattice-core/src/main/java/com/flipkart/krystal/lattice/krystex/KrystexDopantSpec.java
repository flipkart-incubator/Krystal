package com.flipkart.krystal.lattice.krystex;

import static com.flipkart.krystal.lattice.krystex.KrystexDopant.DOPANT_TYPE;
import static java.util.Objects.requireNonNullElse;

import com.flipkart.krystal.krystex.KrystexGraph.KrystexGraphBuilder;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfigurator;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpec;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpecBuilder;
import com.flipkart.krystal.traits.TraitDispatchPolicy;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.Singular;
import org.checkerframework.checker.nullness.qual.NonNull;

@Builder(buildMethodName = "_buildSpec")
public record KrystexDopantSpec(
    @Singular("buildKrystexGraphWith") List<Consumer<KrystexGraphBuilder>> buildKrystexGraphWith,
    @Singular("configureExecutorWith")
        ImmutableList<KryonExecutorConfigurator> configureExecutorWith,
    Collection<? extends @NonNull TraitDispatchPolicy> traitDispatchPolicies)
    implements SimpleDopantSpec<KrystexDopant> {

  public KrystexDopantSpec(
      @Singular("buildKrystexGraphWith") List<Consumer<KrystexGraphBuilder>> buildKrystexGraphWith,
      @Singular("configureExecutorWith")
          ImmutableList<KryonExecutorConfigurator> configureExecutorWith,
      Collection<? extends @NonNull TraitDispatchPolicy> traitDispatchPolicies) {
    this.buildKrystexGraphWith = requireNonNullElse(buildKrystexGraphWith, List.of());
    this.configureExecutorWith = requireNonNullElse(configureExecutorWith, ImmutableList.of());
    this.traitDispatchPolicies =
        Objects.<Collection<? extends @NonNull TraitDispatchPolicy>>requireNonNullElse(
            traitDispatchPolicies, ImmutableList.of());
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
      extends SimpleDopantSpecBuilder<KrystexDopantSpec> {
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") // Used by lombok
    private final List<@NonNull TraitDispatchPolicy> traitDispatchPolicies = new ArrayList<>();

    public KrystexDopantSpecBuilder traitDispatchPolicies(
        Collection<? extends TraitDispatchPolicy> traitDispatchPolicies) {
      this.traitDispatchPolicies.addAll(traitDispatchPolicies);
      return this;
    }

    public KrystexDopantSpecBuilder traitDispatchPolicies(
        TraitDispatchPolicy... traitDispatchPolicies) {
      this.traitDispatchPolicies.addAll(Arrays.asList(traitDispatchPolicies));
      return this;
    }
  }
}

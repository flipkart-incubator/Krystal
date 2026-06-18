package com.flipkart.krystal.lattice.krystex;

import static com.flipkart.krystal.lattice.krystex.KrystexDopant.DOPANT_TYPE;
import static java.util.Objects.requireNonNullElse;

import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfigurator;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpec;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpecBuilder;
import com.flipkart.krystal.traits.TraitDispatchPolicy;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexGraph.KrystexGraphBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.batching.DepChainBatcherConfig.BatchSizeSupplier;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.Singular;
import org.checkerframework.checker.nullness.qual.Nullable;

@Builder(buildMethodName = "_buildSpec")
public record KrystexDopantSpec(
    @Singular("buildKrystexGraphWith") List<Consumer<KrystexGraphBuilder>> buildKrystexGraphWith,
    @Singular("configureExecutorWith")
        ImmutableList<KryonExecutorConfigurator> configureExecutorWith,
    Collection<? extends TraitDispatchPolicy> traitDispatchPolicies,
    Set<DependentChain> disabledDependentChains,
    @Nullable BatchSizeSupplier batchSizeSupplier,
    boolean enableSharedAutoBatchers)
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

  public static final class KrystexDopantSpecBuilder
      extends SimpleDopantSpecBuilder<KrystexDopantSpec> {
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") // Used by lombok
    private final List<TraitDispatchPolicy> traitDispatchPolicies = new ArrayList<>();

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") // Used by lombok
    private final Set<DependentChain> disabledDependentChains = new LinkedHashSet<>();

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

    public KrystexDopantSpecBuilder disabledDependentChains(
        Collection<? extends DependentChain> disabledDependentChains) {
      this.disabledDependentChains.addAll(disabledDependentChains);
      return this;
    }

    public KrystexDopantSpecBuilder disabledDependentChains(
        DependentChain... disabledDependentChains) {
      this.disabledDependentChains.addAll(Arrays.asList(disabledDependentChains));
      return this;
    }
  }
}

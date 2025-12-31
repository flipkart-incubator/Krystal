package com.flipkart.krystal.lattice.krystex;

import static com.flipkart.krystal.lattice.krystex.KrystexDopant.DOPANT_TYPE;

import com.flipkart.krystal.krystex.kryon.KryonExecutorConfigurator;
import com.flipkart.krystal.lattice.core.di.DependencyInjectionFramework;
import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpec;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpecBuilder;
import com.flipkart.krystal.lattice.vajram.BatchingConfigurator;
import com.flipkart.krystal.lattice.vajram.BatchingConfigurator.BatchingConfiguratorContext;
import com.flipkart.krystal.traits.TraitDispatchPolicies;
import com.flipkart.krystal.traits.TraitDispatchPolicy;
import com.flipkart.krystal.vajram.inputinjection.VajramInjectionProvider;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexGraph;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexGraph.KrystexGraphBuilder;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.Singular;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.returnsreceiver.qual.This;

public record KrystexDopantSpec(
    KrystexGraphBuilder krystexGraphBuilder,
    ImmutableList<KryonExecutorConfigurator> configureExecutorWith)
    implements SimpleDopantSpec<KrystexDopant> {

  @Builder(buildMethodName = "_buildSpec")
  public static KrystexDopantSpec create(
      @Nullable KrystexGraphBuilder krystexGraphBuilder,
      @Singular("configureExecutorWith")
          ImmutableList<KryonExecutorConfigurator> configureExecutorWith,
      DependencyInjectionFramework dependencyInjectionFramework,
      @Singular("buildKrystexGraphWith") List<Consumer<KrystexGraphBuilder>> buildKrystexGraphWith,
      @Nullable List<TraitDispatchPolicy> traitDispatchPolicies,
      BatchingConfigurator batchingConfigurator) {
    KrystexGraphBuilder kGraphBuilder =
        krystexGraphBuilder != null ? krystexGraphBuilder : KrystexGraph.builder();
    if (dependencyInjectionFramework != null) {
      VajramInjectionProvider vajramInjectionProvider =
          dependencyInjectionFramework.toVajramInjectionProvider();
      if (vajramInjectionProvider != null) {
        kGraphBuilder.injectionProvider(vajramInjectionProvider);
      }
    }
    buildKrystexGraphWith.forEach(p -> p.accept(kGraphBuilder));
    TraitDispatchPolicies traitDispatchPoliciesContainer =
        new TraitDispatchPolicies(
            traitDispatchPolicies != null ? traitDispatchPolicies : List.of());
    kGraphBuilder.traitDispatchPolicies(traitDispatchPoliciesContainer);
    if (batchingConfigurator != null) {
      kGraphBuilder.inputBatcherConfig(
          batchingConfigurator.createBatcherConfig(
              new BatchingConfiguratorContext(traitDispatchPoliciesContainer)));
    }
    return new KrystexDopantSpec(kGraphBuilder, configureExecutorWith);
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
      extends SimpleDopantSpecBuilder<KrystexDopantSpec> {
    @This
    public KrystexDopantSpecBuilder addTraitDispatchPolicies(
        TraitDispatchPolicy... traitDispatchPolicies) {
      this.traitDispatchPolicies(List.of(traitDispatchPolicies));
      return this;
    }
  }
}

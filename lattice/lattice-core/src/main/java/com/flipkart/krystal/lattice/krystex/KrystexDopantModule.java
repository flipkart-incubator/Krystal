package com.flipkart.krystal.lattice.krystex;

import static com.flipkart.krystal.lattice.core.di.Util.asOptional;

import com.flipkart.krystal.traits.TraitDispatchPolicies;
import com.flipkart.krystal.traits.TraitDispatchPolicies.TraitDispatchPoliciesBuilder;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

@Singleton
public class KrystexDopantModule {

  @Produces
  @Singleton
  public TraitDispatchPolicies traitDispatchPolicies(
      Provider<TraitDispatchPoliciesBuilder> traitDispatchPoliciesBuilder,
      KrystexDopantSpec krystexDopantSpec) {
    return asOptional(traitDispatchPoliciesBuilder)
        .orElseGet(TraitDispatchPolicies::builder)
        .addTraitDispatchPolicies(krystexDopantSpec.traitDispatchPolicies())
        .build();
  }
}

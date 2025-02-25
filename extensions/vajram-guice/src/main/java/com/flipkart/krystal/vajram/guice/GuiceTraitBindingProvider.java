package com.flipkart.krystal.vajram.guice;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajram.facets.specs.DependencySpec;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.flipkart.krystal.vajram.traitbinding.TraitBindingProvider;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import java.util.Optional;

public class GuiceTraitBindingProvider implements TraitBindingProvider {

  private final VajramKryonGraph vajramKryonGraph;
  private final Injector injector;

  public GuiceTraitBindingProvider(VajramKryonGraph vajramKryonGraph, Injector injector) {
    this.vajramKryonGraph = vajramKryonGraph;
    this.injector = injector;
  }

  @Override
  public VajramID get(VajramID clientVajramId, Dependency dependency) {
    String depName = dependency.name();
    Optional<VajramDefinition> vajramDefinition = vajramKryonGraph.getVajramDefinition(clientVajramId);
    if(vajramDefinition.isEmpty()){
      throw new RuntimeException("VajramDefinition not found for clientVajramId: " + clientVajramId);
    }
    Class<? extends VajramDef<?>> aClass = vajramDefinition.get().vajramDefClass();
    injector.getProvider(Key.get(TypeLiteral.get(aClass),))
  }
}

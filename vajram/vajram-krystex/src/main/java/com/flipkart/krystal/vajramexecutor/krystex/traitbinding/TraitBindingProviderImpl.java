package com.flipkart.krystal.vajramexecutor.krystex.traitbinding;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajram.traitbinding.TraitBindingProvider;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TraitBindingProviderImpl implements TraitBindingProvider {

  private final VajramKryonGraph vajramKryonGraph;

  private final Map<Key, Supplier<VajramID>> bindingsMap = new LinkedHashMap<>();

  public TraitBindingProviderImpl(VajramKryonGraph vajramKryonGraph, TraitBinder bindingsMap) {
    this.vajramKryonGraph = vajramKryonGraph;
  }

  @Override
  public VajramID get(VajramID vajramID, Dependency facetDef, VajramID depVajramId) {
    VajramDefinition vajramDefinition =
        vajramKryonGraph
            .getVajramDefinition(depVajramId)
            .orElseGet(
                () -> {
                  throw new IllegalStateException(
                      "VajramDefinition not found for clientVajramId: " + depVajramId);
                });
    Class<? extends VajramDef<?>> aClass = vajramDefinition.vajramDefClass();

    return null;
  }

  private record Key(VajramID vajramId, @Nullable Annotation qualifier) {}
}

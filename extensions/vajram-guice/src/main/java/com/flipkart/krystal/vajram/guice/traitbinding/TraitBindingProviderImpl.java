package com.flipkart.krystal.vajram.guice.traitbinding;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.tags.ElementTags;
import com.flipkart.krystal.traits.TraitBindingProvider;
import com.flipkart.krystal.vajram.VajramDefRoot;
import com.flipkart.krystal.vajram.VajramTraitDef;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.google.inject.Key;
import jakarta.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Implements a guice-like retrieving conrete vajrams bound to traits.
 *
 * @see TraitBinder
 */
public class TraitBindingProviderImpl implements TraitBindingProvider {

  private final VajramKryonGraph vajramKryonGraph;

  private final Map<Key<? extends VajramTraitDef<?>>, Supplier<VajramID>> traitBindingsMap;

  private final Map<VajramID, Map<Dependency, Supplier<VajramID>>> dependencyBindingsMap =
      new LinkedHashMap<>();

  public TraitBindingProviderImpl(VajramKryonGraph vajramKryonGraph, TraitBinder traitBinder) {
    this.vajramKryonGraph = vajramKryonGraph;
    this.traitBindingsMap =
        traitBinder.traitBindings().stream()
            .collect(
                toImmutableMap(
                    TraitBinding::key, binding -> vajramIdSupplier(binding, vajramKryonGraph)));
  }

  private static Supplier<VajramID> vajramIdSupplier(
      TraitBinding traitBinding, VajramKryonGraph vajramKryonGraph) {
    return () -> {
      return vajramKryonGraph.getVajramId(traitBinding.vajram().get());
    };
  }

  @Override
  public VajramID get(VajramID traitId, Dependency dependency) {
    return dependencyBindingsMap
        .computeIfAbsent(traitId, vajramID -> new LinkedHashMap<>())
        .computeIfAbsent(
            dependency,
            dep -> {
              ElementTags tags = dep.tags();
              List<Annotation> list =
                  tags.annotations().stream()
                      .filter(
                          annotation -> {
                            return annotation.annotationType().getAnnotation(Qualifier.class)
                                != null;
                          })
                      .toList();
              if (list.size() > 1) {
                throw new IllegalStateException("Multiple qualifiers found for " + dep);
              }
              Key<? extends VajramTraitDef<?>> key;
              VajramDefinition vajramDefinition = vajramKryonGraph.getVajramDefinition(traitId);
              Class<? extends VajramDefRoot<?>> aClass = vajramDefinition.vajramDefClass();
              if (!VajramTraitDef.class.isAssignableFrom(aClass)) {
                return () -> traitId;
              }
              @SuppressWarnings("unchecked")
              Class<? extends VajramTraitDef<?>> traitDef =
                  (Class<? extends VajramTraitDef<?>>) aClass.asSubclass(VajramTraitDef.class);
              if (list.isEmpty()) {
                key = Key.get(traitDef);
              } else {
                key = Key.get(traitDef, list.get(0));
              }
              Supplier<VajramID> vajramIDSupplier = traitBindingsMap.get(key);
              if (vajramIDSupplier == null && key.hasAttributes()) {
                vajramIDSupplier = traitBindingsMap.get(key.withoutAttributes());
              }
              if (vajramIDSupplier == null) {
                throw new IllegalStateException(
                    "No binding found for " + key + " corresponding to dependency " + dep);
              }
              return vajramIDSupplier;
            })
        .get();
  }
}

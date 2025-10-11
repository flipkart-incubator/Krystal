package com.flipkart.krystal.vajram.guice.traitbinding;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.tags.ElementTags;
import com.flipkart.krystal.traits.StaticDispatchPolicy;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Key;
import jakarta.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Implements a guice-like retrieving conrete vajrams bound to traits.
 *
 * @see TraitBinder
 */
public class StaticDispatchPolicyImpl extends StaticDispatchPolicy {

  @Getter private final VajramID traitID;
  @Getter private final ImmutableList<Class<? extends Request<?>>> dispatchTargetReqs;

  private final VajramKryonGraph vajramKryonGraph;
  private final ImmutableMap<Key<? extends Request<?>>, VajramID> bindingsByKey;

  private final Map<VajramID, Map<QualWrapper, VajramID>> bindingsByQualifier =
      new LinkedHashMap<>();

  public StaticDispatchPolicyImpl(
      VajramKryonGraph vajramKryonGraph, VajramID traitID, TraitBinder traitBinder) {
    this.vajramKryonGraph = vajramKryonGraph;
    this.traitID = traitID;
    this.bindingsByKey =
        traitBinder.traitBindings().stream()
            .collect(
                toImmutableMap(
                    TraitBinding::key,
                    binding ->
                        vajramKryonGraph.getVajramIdByVajramReqType(
                            binding.concreteVajramRequestType())));
    this.dispatchTargetReqs =
        ImmutableList.copyOf(
            traitBinder.traitBindings().stream()
                .map(TraitBinding::concreteVajramRequestType)
                .toList());
  }

  @Override
  public VajramID getDispatchTarget(Dependency dependency) {
    ElementTags tags = dependency.tags();
    List<Annotation> list =
        tags.annotations().stream()
            .filter(
                annotation -> annotation.annotationType().getAnnotation(Qualifier.class) != null)
            .toList();
    if (list.size() > 1) {
      throw new IllegalStateException("Multiple qualifiers found for " + dependency);
    }
    Annotation qualifier;
    if (list.isEmpty()) {
      qualifier = null;
    } else {
      qualifier = list.get(0);
    }
    return getDispatchTarget(new QualWrapper(qualifier));
  }

  @Override
  public VajramID getDispatchTarget(@Nullable Annotation qualifier) {
    return getDispatchTarget(new QualWrapper(qualifier));
  }

  private VajramID getDispatchTarget(QualWrapper qualWrapper) {
    return bindingsByQualifier
        .computeIfAbsent(traitID, vajramID -> new LinkedHashMap<>())
        .computeIfAbsent(
            qualWrapper,
            qualifierWrapper -> {
              Key<? extends Request<?>> key;
              VajramDefinition vajramDefinition = vajramKryonGraph.getVajramDefinition(traitID);
              Class<? extends Request<?>> traitReq = vajramDefinition.reqRootType();
              Annotation qualifier = qualifierWrapper.qualifier();
              if (qualifier == null) {
                key = Key.get(traitReq);
              } else {
                key = Key.get(traitReq, qualifier);
              }
              VajramID vajramId = bindingsByKey.get(key);
              if (vajramId == null && key.hasAttributes()) {
                vajramId = bindingsByKey.get(key.withoutAttributes());
              }
              if (vajramId == null) {
                throw new IllegalStateException(
                    "No binding found for "
                        + key
                        + " corresponding to dependency "
                        + qualifierWrapper);
              }
              return vajramId;
            });
  }

  @Override
  public ImmutableCollection<VajramID> dispatchTargets() {
    return bindingsByKey.values();
  }

  private record QualWrapper(@Nullable Annotation qualifier) {
    private QualWrapper {
      checkArgument(
          isValidQualifier(qualifier),
          "qualifier annotation must have the @Qualifier annotation on its type");
    }
  }
}

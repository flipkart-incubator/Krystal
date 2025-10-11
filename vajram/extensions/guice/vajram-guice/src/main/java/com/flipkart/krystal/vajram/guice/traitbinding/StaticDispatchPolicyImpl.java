package com.flipkart.krystal.vajram.guice.traitbinding;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.tags.ElementTags;
import com.flipkart.krystal.traits.StaticDispatchPolicy;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Key;
import jakarta.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Implements a guice-like retrieving concrete vajrams bound to traits.
 *
 * @see TraitBinder
 */
public class StaticDispatchPolicyImpl extends StaticDispatchPolicy {

  @Getter private final VajramID traitID;
  @Getter private final ImmutableSet<Class<? extends Request<?>>> dispatchTargetReqs;
  @Getter private final ImmutableSet<VajramID> dispatchTargetIDs;

  private final VajramKryonGraph vajramKryonGraph;
  private final ImmutableMap<Key<? extends Request<?>>, Class<? extends Request<?>>> bindingsByKey;

  private final Map<VajramID, Map<QualWrapper, Class<? extends Request<?>>>> bindingsByQualifier =
      new LinkedHashMap<>();

  public StaticDispatchPolicyImpl(
      VajramKryonGraph vajramKryonGraph, VajramID traitID, TraitBinder traitBinder) {
    this.vajramKryonGraph = vajramKryonGraph;
    this.traitID = traitID;
    this.bindingsByKey =
        traitBinder.traitBindings().stream()
            .collect(toImmutableMap(TraitBinding::key, TraitBinding::concreteVajramRequestType));
    this.dispatchTargetReqs =
        traitBinder.traitBindings().stream()
            .map(TraitBinding::concreteVajramRequestType)
            .collect(toImmutableSet());
    this.dispatchTargetIDs =
        dispatchTargetReqs.stream()
            .map(vajramKryonGraph::getVajramIdByVajramReqType)
            .collect(toImmutableSet());
  }

  @Override
  public Class<? extends Request<?>> getDispatchTarget(Dependency dependency) {
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
  public VajramID getDispatchTargetID(Dependency dependency) {
    return vajramKryonGraph.getVajramIdByVajramReqType(getDispatchTarget(dependency));
  }

  @Override
  public Class<? extends Request<?>> getDispatchTarget(@Nullable Annotation qualifier) {
    return getDispatchTarget(new QualWrapper(qualifier));
  }

  @Override
  public VajramID getDispatchTargetID(@Nullable Annotation qualifier) {
    return vajramKryonGraph.getVajramIdByVajramReqType(getDispatchTarget(qualifier));
  }

  private Class<? extends Request<?>> getDispatchTarget(QualWrapper qualWrapper) {
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
              Class<? extends Request<?>> dispatchTarget = bindingsByKey.get(key);
              if (dispatchTarget == null && key.hasAttributes()) {
                dispatchTarget = bindingsByKey.get(key.withoutAttributes());
              }
              if (dispatchTarget == null) {
                throw new IllegalStateException(
                    "No binding found for "
                        + key
                        + " corresponding to dependency "
                        + qualifierWrapper);
              }
              return dispatchTarget;
            });
  }

  private record QualWrapper(@Nullable Annotation qualifier) {
    private QualWrapper {
      checkArgument(
          isValidQualifier(qualifier),
          "qualifier annotation must have the @Qualifier annotation on its type");
    }
  }
}

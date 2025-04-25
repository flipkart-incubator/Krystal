package com.flipkart.krystal.vajram.exec;

import static com.flipkart.krystal.core.VajramID.vajramID;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.resolution.ResolverDefinition;
import com.flipkart.krystal.tags.ElementTags;
import com.flipkart.krystal.vajram.ComputeDelegationMode;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramDefRoot;
import com.flipkart.krystal.vajram.annos.OutputLogicDelegationMode;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
@UtilityClass
final class Vajrams {

  /**
   * Returns the exact class in the class hierarchy which has the {@link Vajram} or {@link Trait}
   * annotation
   *
   * @param aClass the class in whose class hierarcgy the vajram def class is to be searched for.
   */
  @SuppressWarnings("unchecked")
  static Class<? extends VajramDefRoot<?>> getVajramDefClass(
      @SuppressWarnings("rawtypes") Class<? extends VajramDefRoot> aClass) {
    Class<? extends VajramDefRoot<?>> result = _getVajramDefClass(aClass);
    if (result == null) {
      throw new IllegalArgumentException(
          "@Vajram or @VajramTrait annotation missing in type hierarchy of " + aClass);
    }
    return result;
  }

  static VajramID parseVajramId(VajramDefRoot<?> vajramDef) {
    return vajramID(getVajramDefClass(vajramDef.getClass()).getSimpleName());
  }

  static ElementTags parseVajramTags(VajramDefRoot<?> vajramDef) {
    return ElementTags.of(
        Arrays.stream(getVajramDefClass(vajramDef.getClass()).getAnnotations())
            .flatMap(a -> enrich(a, vajramDef))
            .toList());
  }

  static ImmutableMap<ResolverDefinition, InputResolver> parseInputResolvers(
      VajramDefRoot<?> vajramDef) {
    return vajramDef instanceof VajramDef<?> v
        ? v.getInputResolvers().stream()
            .collect(ImmutableMap.toImmutableMap(InputResolver::definition, Function.identity()))
        : ImmutableMap.of();
  }

  private static FacetSpec inferFacetId(
      Parameter parameter,
      ImmutableMap<String, FacetSpec> facetsByName,
      VajramDefRoot<?> vajramDef) {
    FacetSpec facetSpec = facetsByName.get(parameter.getName());
    if (facetSpec == null) {
      throw new IllegalArgumentException(
          "Unable to infer facet id for parameter %s of vajram %s"
              .formatted(parameter.getName(), vajramDef.getClass()));
    }
    return facetSpec;
  }

  static ElementTags parseOutputLogicTags(VajramDefRoot<?> vajramDef) {
    return vajramDef instanceof VajramDef<?> v
        ? ElementTags.of(
            Arrays.stream(getVajramDefClass(v.getClass()).getDeclaredMethods())
                .filter(method -> method.getAnnotation(Output.class) != null)
                .findFirst()
                .stream()
                .flatMap(method -> Arrays.stream(method.getAnnotations()))
                .toList())
        : ElementTags.emptyTags();
  }

  private static Stream<Annotation> enrich(Annotation annotation, VajramDefRoot<?> vajramDefRoot) {
    List<Annotation> inferredAnnos = new ArrayList<>();
    if (annotation instanceof Vajram && vajramDefRoot instanceof VajramDef<?> vajramDef) {
      inferredAnnos.add(
          OutputLogicDelegationMode.Creator.create(getComputeDelegationType(vajramDef)));
    }
    return Stream.concat(Stream.of(annotation), inferredAnnos.stream());
  }

  private static ComputeDelegationMode getComputeDelegationType(VajramDef<?> vajramDef) {
    if (vajramDef instanceof ComputeVajramDef<?>) {
      return ComputeDelegationMode.NONE;
    } else if (vajramDef instanceof IOVajramDef<?>) {
      return ComputeDelegationMode.SYNC;
    } else {
      throw new IllegalStateException(
          "Unable infer compute delegation type of vajram %s".formatted(vajramDef.getClass()));
    }
  }

  @SuppressWarnings("unchecked")
  private static @Nullable Class<? extends VajramDefRoot<?>> _getVajramDefClass(Class<?> aClass) {
    if (!VajramDefRoot.class.isAssignableFrom(aClass)) {
      return null;
    }
    Annotation annotation = aClass.getAnnotation(Vajram.class);
    if (annotation == null) {
      annotation = aClass.getAnnotation(Trait.class);
    }
    if (annotation != null) {
      return (Class<? extends VajramDefRoot<?>>) aClass;
    }
    List<Class<?>> superTypes = new ArrayList<>(Arrays.asList(aClass.getInterfaces()));
    {
      Class<?> superclass = aClass.getSuperclass();
      if (!Object.class.equals(superclass) && superclass != null) {
        superTypes.add(superclass);
      }
    }
    for (Class<?> superType : superTypes) {
      Class<? extends VajramDefRoot<?>> result = _getVajramDefClass(superType);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  static ImmutableSet<FacetSpec> parseOutputLogicSources(
      VajramDefRoot<?> vajramDefRoot,
      ImmutableSet<FacetSpec> facetSpecs,
      ImmutableMap<String, FacetSpec> facetsByName) {
    Optional<Method> outputLogicMethod =
        Arrays.stream(getVajramDefClass(vajramDefRoot.getClass()).getDeclaredMethods())
            .filter(method -> method.getAnnotation(Output.class) != null)
            .findFirst();
    if (outputLogicMethod.isEmpty()) {
      // It is possible this is a vajram trait which does not have output logic
      return ImmutableSet.of();
    }
    if (facetSpecs.stream().noneMatch(FacetSpec::isBatched)) {
      // This is a vajram which doesn't have batched facets. So we can infer the output logic's
      // sources from the parameters
      Parameter[] outputLogicParams = outputLogicMethod.get().getParameters();
      ImmutableSet.Builder<FacetSpec> facetIds = ImmutableSet.builder();
      for (Parameter param : outputLogicParams) {
        facetIds.add(Vajrams.inferFacetId(param, facetsByName, vajramDefRoot));
      }
      return facetIds.build();
    } else {
      //  The output logic consumes all facets
      return facetSpecs;
    }
  }
}

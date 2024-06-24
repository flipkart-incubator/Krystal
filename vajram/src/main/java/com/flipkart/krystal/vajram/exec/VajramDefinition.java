package com.flipkart.krystal.vajram.exec;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.toMap;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.Output;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.DefaultInputResolverDefinition;
import com.flipkart.krystal.vajram.facets.DependencyDef;
import com.flipkart.krystal.vajram.facets.QualifiedInputs;
import com.flipkart.krystal.vajram.facets.Using;
import com.flipkart.krystal.vajram.facets.VajramFacetDefinition;
import com.flipkart.krystal.vajram.facets.resolution.InputResolverDefinition;
import com.flipkart.krystal.vajram.facets.resolution.sdk.Resolve;
import com.flipkart.krystal.vajram.tags.AnnotationTag;
import com.flipkart.krystal.vajram.tags.AnnotationTagKey;
import com.flipkart.krystal.vajram.tags.AnnotationTags;
import com.flipkart.krystal.vajram.tags.NamedValueTag;
import com.flipkart.krystal.vajram.tags.VajramTags;
import com.flipkart.krystal.vajram.tags.VajramTags.VajramTypes;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class VajramDefinition {

  @Getter private final Vajram<?> vajram;

  @Getter private final ImmutableCollection<InputResolverDefinition> inputResolverDefinitions;

  @Getter private final ImmutableMap<AnnotationTagKey, Tag> outputLogicTags;

  @Getter
  private final ImmutableMap</*FacetName*/ String, ImmutableMap</*TagKey*/ Object, Tag>> facetTags;

  @Getter
  @Accessors(fluent = true)
  private final VajramMetadata vajramMetadata;

  public VajramDefinition(Vajram<?> vajram) {
    this.vajram = vajram;
    this.inputResolverDefinitions = ImmutableList.copyOf(parseInputResolvers(vajram));
    this.outputLogicTags = parseOutputLogicTags(vajram);
    this.facetTags = parseFacetTags(vajram);
    this.vajramMetadata = new VajramMetadata(vajram);
  }

  private static ImmutableMap<String, ImmutableMap<Object, Tag>> parseFacetTags(Vajram<?> vajram) {
    ImmutableMap<String, VajramFacetDefinition> facetDefs =
        vajram.getFacetDefinitions().stream()
            .collect(toImmutableMap(VajramFacetDefinition::name, Function.identity()));
    Map<String, ImmutableMap<Object, Tag>> result = new LinkedHashMap<>();
    for (Field declaredField : vajram.getClass().getDeclaredFields()) {
      Map<Object, Tag> tags = new LinkedHashMap<>();
      String facetName = declaredField.getName();
      VajramFacetDefinition facetDef = facetDefs.get(facetName);
      if (facetDef != null) {
        tags.putAll(facetDef.tags());
      }
      Annotation[] annotations = declaredField.getAnnotations();
      for (Annotation annotation : annotations) {
        boolean isRepeatable = annotation.getClass().getAnnotation(Repeatable.class) != null;
        if (isRepeatable) {
          log.warn("Repeatable annotations are not supported as tags. Ignoring {}", annotation);
        } else {
          AnnotationTag<Annotation> annotationTag = AnnotationTag.from(annotation);
          tags.put(annotationTag.tagKey(), annotationTag);
        }
      }
      result.put(facetName, ImmutableMap.copyOf(tags));
    }
    return ImmutableMap.copyOf(result);
  }

  private static Collection<InputResolverDefinition> parseInputResolvers(Vajram<?> vajram) {
    List<InputResolverDefinition> inputResolvers =
        new ArrayList<>(vajram.getSimpleInputResolvers());
    ImmutableSet<Method> resolverMethods =
        Arrays.stream(getVajramSourceClass(vajram.getClass()).getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(Resolve.class))
            .collect(toImmutableSet());

    ImmutableMap<String, DependencyDef<?>> dependencyDefinitions =
        vajram.getFacetDefinitions().stream()
            .filter(vi -> vi instanceof DependencyDef)
            .map(vi -> (DependencyDef<?>) vi)
            .collect(toImmutableMap(VajramFacetDefinition::name, Function.identity()));

    for (Method resolverMethod : resolverMethods) {
      Resolve resolver = resolverMethod.getAnnotation(Resolve.class);
      if (resolver == null) {
        throw new AssertionError();
      }
      String targetDependency = resolver.depName();
      DependencyDef<?> dependencyDef = dependencyDefinitions.get(targetDependency);
      if (dependencyDef == null) {
        throw new IllegalStateException(
            "Could not find dependency with name %s".formatted(targetDependency));
      }
      String[] targetInputs = resolver.depInputs();
      ImmutableSet<String> sources =
          Arrays.stream(resolverMethod.getParameters())
              .map(
                  parameter ->
                      Arrays.stream(parameter.getAnnotations())
                          .filter(annotation -> annotation instanceof Using)
                          .map(annotation -> (Using) annotation)
                          .findAny()
                          .map(Using::value)
                          .orElseGet(parameter::getName))
              .collect(toImmutableSet());
      inputResolvers.add(
          new DefaultInputResolverDefinition(
              sources,
              new QualifiedInputs(
                  targetDependency,
                  dependencyDef.dataAccessSpec(),
                  ImmutableSet.copyOf(targetInputs))));
    }
    return inputResolvers;
  }

  private static ImmutableMap<AnnotationTagKey, Tag> parseOutputLogicTags(Vajram<?> vajram) {
    Optional<Method> outputLogicMethod =
        Arrays.stream(getVajramSourceClass(vajram.getClass()).getDeclaredMethods())
            .filter(method -> method.getAnnotation(Output.class) != null)
            .findFirst();
    List<AnnotationTag<Annotation>> tagWithArray =
        outputLogicMethod
            .map(method -> Arrays.stream(method.getAnnotations()).map(AnnotationTag::from).toList())
            .orElse(List.of());
    Map<AnnotationTagKey, Tag> collect =
        tagWithArray.stream().collect(toMap(AnnotationTag::tagKey, Function.identity()));
    outputLogicMethod.ifPresent(
        method -> {
          AnnotationTag<NamedValueTag> vajramTypeTag =
              AnnotationTags.newNamedTag(
                  VajramTags.VAJRAM_TYPE,
                  vajram instanceof IOVajram<?>
                      ? VajramTypes.IO_VAJRAM
                      : VajramTypes.COMPUTE_VAJRAM);
          collect.put(vajramTypeTag.tagKey(), vajramTypeTag);
        });
    AnnotationTag<NamedValueTag> vajramIdTag =
        AnnotationTags.newNamedTag(VajramTags.VAJRAM_ID, vajram.getId().vajramId());
    collect.put(vajramIdTag.tagKey(), vajramIdTag);
    return ImmutableMap.copyOf(collect);
  }

  private static Class<?> getVajramSourceClass(Class<?> vajramClass) {
    Class<?> superclass = vajramClass.getSuperclass();
    if (Object.class.equals(superclass) || superclass == null) {
      throw new IllegalArgumentException();
    }
    if (IOVajram.class.equals(superclass) || ComputeVajram.class.equals(superclass)) {
      return vajramClass;
    }
    return getVajramSourceClass(superclass);
  }
}

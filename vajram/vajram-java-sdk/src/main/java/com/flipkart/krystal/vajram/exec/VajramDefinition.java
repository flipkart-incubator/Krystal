package com.flipkart.krystal.vajram.exec;

import static com.flipkart.krystal.vajram.utils.Constants.FACETS_CLASS_NAME_SUFFIX;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.toMap;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.data.FacetContainer;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramDefinitionException;
import com.flipkart.krystal.vajram.facets.InputDef;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.Using;
import com.flipkart.krystal.vajram.facets.VajramFacetDefinition;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajram.facets.resolution.InputResolverDefinition;
import com.flipkart.krystal.vajram.tags.AnnotationTag;
import com.flipkart.krystal.vajram.tags.AnnotationTagKey;
import com.flipkart.krystal.vajram.tags.AnnotationTags;
import com.flipkart.krystal.vajram.tags.NamedValueTag;
import com.flipkart.krystal.vajram.tags.VajramTags;
import com.flipkart.krystal.vajram.tags.VajramTags.VajramTypes;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class VajramDefinition {

  private static final Field[] NO_FACET_FIELDS = {};

  @Getter private final Vajram<Object> vajram;

  @Getter private final ImmutableMap<Integer, InputResolverDefinition> inputResolverDefinitions;

  @Getter private final ImmutableMap<AnnotationTagKey, Tag> outputLogicTags;

  @Getter private final ImmutableSet<Integer> outputLogicSources;

  @Getter
  private final ImmutableMap</*FacetId*/ Integer, ImmutableMap</*TagKey*/ Object, Tag>> facetTags;

  @Getter private final ImmutableMap<String, VajramFacetDefinition> facetsByName;

  @Getter private final ImmutableMap<Integer, VajramFacetDefinition> facetsById;

  public VajramDefinition(Vajram<Object> vajram) {
    this.vajram = vajram;
    this.facetsByName =
        vajram.getFacetDefinitions().stream()
            .collect(toImmutableMap(VajramFacetDefinition::name, Function.identity()));
    this.facetsById =
        vajram.getFacetDefinitions().stream()
            .collect(toImmutableMap(VajramFacetDefinition::id, Function.identity()));
    this.inputResolverDefinitions = parseInputResolvers(vajram);

    this.outputLogicTags = parseOutputLogicTags(vajram);
    this.facetTags = parseFacetTags(vajram);
    this.outputLogicSources = parseOutputLogicSources(vajram, facetsByName);
  }

  private static ImmutableMap<Integer, InputResolverDefinition> parseInputResolvers(
      Vajram<Object> vajram) {
    Map<Integer, InputResolverDefinition> map = new LinkedHashMap<>();
    int i = 0;
    for (InputResolver inputResolver : vajram.getInputResolvers()) {
      map.put(i++, inputResolver);
    }
    return ImmutableMap.copyOf(map);
  }

  private static ImmutableSet<Integer> parseOutputLogicSources(
      Vajram<?> vajram, ImmutableMap<String, VajramFacetDefinition> facetsByName) {
    Optional<Method> outputLogicMethod =
        Arrays.stream(getVajramSourceClass(vajram.getClass()).getDeclaredMethods())
            .filter(method -> method.getAnnotation(Output.class) != null)
            .findFirst();
    ImmutableSet<Integer> allFacetIds =
        vajram.getFacetDefinitions().stream()
            .map(VajramFacetDefinition::id)
            .collect(toImmutableSet());
    if (outputLogicMethod.isPresent()
        && vajram.getFacetDefinitions().stream()
            .noneMatch(f -> f instanceof InputDef<?> input && input.isBatched())) {
      // This is a vajram which doesn't have batched facets. So we can infer the output logic's
      // sources from the parameters
      Parameter[] outputLogicParams = outputLogicMethod.get().getParameters();
      if (outputLogicParams.length == 1
          && FacetContainer.class.isAssignableFrom(outputLogicParams[0].getType())) {
        /*
         This means the output logic is consuming the auto-generated Facets class which implies it
         consumes all the facets
        */
        return allFacetIds;
      } else {
        return Arrays.stream(outputLogicParams)
            .map((Parameter parameter) -> inferFacetId(parameter, facetsByName))
            .collect(toImmutableSet());
      }
    } else {
      // The output logic consumes all facets
      return allFacetIds;
    }
  }

  private static ImmutableMap<Integer, ImmutableMap<Object, Tag>> parseFacetTags(Vajram<?> vajram) {
    ImmutableMap<String, VajramFacetDefinition> facetDefs =
        vajram.getFacetDefinitions().stream()
            .collect(toImmutableMap(VajramFacetDefinition::name, Function.identity()));
    Map<Integer, ImmutableMap<Object, Tag>> result = new LinkedHashMap<>();
    for (Field declaredField :
        getFacetsClass(vajram).map(Class::getDeclaredFields).orElse(NO_FACET_FIELDS)) {
      String facetName = declaredField.getName();
      VajramFacetDefinition facetDef = facetDefs.get(facetName);
      if (facetDef != null) {
        Map<Object, Tag> tags = new LinkedHashMap<>(facetDef.tags());
        Annotation[] annotations = declaredField.getAnnotations();
        for (Annotation annotation : annotations) {
          boolean isRepeatable =
              annotation.annotationType().getAnnotation(Repeatable.class) != null;
          if (isRepeatable) {
            log.warn("Repeatable annotations are not supported as tags. Ignoring {}", annotation);
          } else {
            AnnotationTag<Annotation> annotationTag = AnnotationTag.from(annotation);
            tags.put(annotationTag.tagKey(), annotationTag);
          }
        }
        result.put(facetDef.id(), ImmutableMap.copyOf(tags));
      }
    }
    return ImmutableMap.copyOf(result);
  }

  private static int inferFacetId(
      Parameter parameter, Map<String, VajramFacetDefinition> facetsByName) {
    return Arrays.stream(parameter.getAnnotations())
        .filter(annotation -> annotation instanceof Using)
        .map(annotation -> (Using) annotation)
        .findAny()
        .map(Using::value)
        .map(facetsByName::get)
        .map(VajramFacetDefinition::id)
        .orElseGet(
            () -> {
              String paramName = parameter.getName();
              VajramFacetDefinition vajramFacetDefinition = facetsByName.get(paramName);
              if (vajramFacetDefinition == null) {
                throw new VajramDefinitionException(
                    "Unknown facet '%s' defined as parameter".formatted(paramName));
              }
              return vajramFacetDefinition.id();
            });
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

  private static Optional<Class<?>> getFacetsClass(Vajram<?> vajram) {
    return Arrays.stream(getVajramSourceClass(vajram.getClass()).getDeclaredClasses())
        .filter(aClass -> FACETS_CLASS_NAME_SUFFIX.equals(aClass.getSimpleName()))
        .findFirst();
  }
}

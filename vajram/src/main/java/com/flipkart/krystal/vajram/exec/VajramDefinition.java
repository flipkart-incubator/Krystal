package com.flipkart.krystal.vajram.exec;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.toMap;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.DefaultInputResolverDefinition;
import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.QualifiedInputs;
import com.flipkart.krystal.vajram.inputs.Using;
import com.flipkart.krystal.vajram.inputs.VajramFacetDefinition;
import com.flipkart.krystal.vajram.inputs.resolution.InputResolverDefinition;
import com.flipkart.krystal.vajram.inputs.resolution.Resolve;
import com.flipkart.krystal.vajram.tags.AnnotationTag;
import com.flipkart.krystal.vajram.tags.Service;
import com.flipkart.krystal.vajram.tags.ServiceApi;
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
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class VajramDefinition {

  @Getter private final Vajram<?> vajram;

  @Getter private final ImmutableCollection<InputResolverDefinition> inputResolverDefinitions;

  @Getter private final ImmutableMap<Object, Tag> mainLogicTags;
  @Getter private final ImmutableMap<String, ImmutableMap<Object, Tag>> facetTags;

  public VajramDefinition(Vajram<?> vajram) {
    this.vajram = vajram;
    this.inputResolverDefinitions = ImmutableList.copyOf(parseInputResolvers(vajram));
    this.mainLogicTags = parseVajramLogicTags(vajram);
    this.facetTags = parseFacetTags(vajram);
  }

  private static ImmutableMap<String, ImmutableMap<Object, Tag>> parseFacetTags(Vajram<?> vajram) {
    Map<String, ImmutableMap<Object, Tag>> result = new LinkedHashMap<>();
    for (Field declaredField : vajram.getClass().getDeclaredFields()) {
      Map<Object, Tag> annoTags = new LinkedHashMap<>();
      Annotation[] annotations = declaredField.getAnnotations();
      for (Annotation annotation : annotations) {
        boolean isRepeatable = annotation.getClass().getAnnotation(Repeatable.class) != null;
        if (isRepeatable) {
          log.warn("Repeatable annotations are not supported as tags. Ignoring {}", annotation);
        } else {
          AnnotationTag<Annotation> annotationTag = AnnotationTag.from(annotation);
          annoTags.put(annotationTag.tagKey(), annotationTag);
        }
      }
      result.put(declaredField.getName(), ImmutableMap.copyOf(annoTags));
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

    ImmutableMap<String, Dependency<?>> inputDefinitions =
        vajram.getInputDefinitions().stream()
            .filter(vi -> vi instanceof Dependency)
            .map(vi -> (Dependency<?>) vi)
            .collect(toImmutableMap(VajramFacetDefinition::name, Function.identity()));

    for (Method resolverMethod : resolverMethods) {
      Resolve resolver = resolverMethod.getAnnotation(Resolve.class);
      if (resolver == null) {
        throw new AssertionError();
      }
      String targetDependency = resolver.depName();
      Dependency<?> dependency = inputDefinitions.get(targetDependency);
      if (dependency == null) {
        throw new IllegalStateException(
            "Could not find dependency with name %s".formatted(targetDependency));
      }
      String[] targetInputs = resolver.depInputs();
      ImmutableSet<String> sources =
          Arrays.stream(resolverMethod.getParameters())
              .map(Parameter::getAnnotations)
              .map(
                  annotations ->
                      Arrays.stream(annotations)
                          .filter(annotation -> annotation instanceof Using)
                          .map(annotation -> (Using) annotation)
                          .findAny())
              .filter(Optional::isPresent)
              .map(Optional::orElseThrow)
              .toList()
              .stream()
              .map(Using::value)
              .collect(toImmutableSet());
      inputResolvers.add(
          new DefaultInputResolverDefinition(
              sources,
              new QualifiedInputs(
                  targetDependency,
                  dependency.dataAccessSpec(),
                  ImmutableSet.copyOf(targetInputs))));
    }
    return inputResolvers;
  }

  private static ImmutableMap<Object, Tag> parseVajramLogicTags(Vajram<?> vajram) {
    Optional<Method> vajramLogicMethod =
        Arrays.stream(getVajramSourceClass(vajram.getClass()).getDeclaredMethods())
            .filter(method -> method.getAnnotation(VajramLogic.class) != null)
            .findFirst();
    List<AnnotationTag<Annotation>> tagWithArray =
        vajramLogicMethod
            .map(method -> Arrays.stream(method.getAnnotations()).map(AnnotationTag::from).toList())
            .orElse(List.of());
    Map<Object, Tag> collect =
        tagWithArray.stream().collect(toMap(Tag::tagKey, Function.identity()));
    vajramLogicMethod.ifPresent(
        method -> {
          Service service = method.getAnnotation(Service.class);
          if (service != null) {
            AnnotationTag<Service> serviceTag = AnnotationTag.from(service);
            collect.put(serviceTag.tagKey(), serviceTag);
          }
          ServiceApi serviceApi = method.getAnnotation(ServiceApi.class);
          if (serviceApi != null) {
            AnnotationTag<ServiceApi> serviceApiTag = AnnotationTag.from(serviceApi);
            collect.put(serviceApiTag.tagKey(), serviceApiTag);
          }
          collect.put(
              VajramTags.VAJRAM_TYPE,
              AnnotationTag.newNamedTag(
                  VajramTags.VAJRAM_TYPE,
                  vajram instanceof IOVajram<?>
                      ? VajramTypes.IO_VAJRAM
                      : VajramTypes.COMPUTE_VAJRAM));
        });
    collect.put(
        VajramTags.VAJRAM_ID,
        AnnotationTag.newNamedTag(VajramTags.VAJRAM_ID, vajram.getId().vajramId()));
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

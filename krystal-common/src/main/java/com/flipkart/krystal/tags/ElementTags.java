package com.flipkart.krystal.tags;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Collections.synchronizedMap;
import static java.util.stream.Collectors.groupingBy;

import com.flipkart.krystal.annos.ElementTagUtility;
import com.flipkart.krystal.annos.ElementTagUtilityOf;
import com.flipkart.krystal.annos.Transitive;
import com.flipkart.krystal.core.ElementTagUtils;
import com.google.auto.value.AutoAnnotation;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A container for all tags assigned to a krystal graph element, like a facet, logic, vajram/kryon
 * etc.
 *
 * <p>Tags are the primary way to annotate metadata to various application elements of the krystal
 * graph, like vajrams/kryons, logics, facets, etc. Tags are modelled at build time and runtime as
 * annotations and annotation instances respectively. Each krystal element can have exactly one
 * annotation of a given annotation type. When an element has multiple annotations in the source
 * code which are themselves annotated with {@link Repeatable}, the element is tagged with the
 * individual unpacked annotations (as returned by {@link
 * AnnotatedElement#getAnnotationsByType(Class)}).
 *
 * <p>{@link NamedValueTag} is treated slightly differently (see its documentation for more details)
 */
@Slf4j
@EqualsAndHashCode
public final class ElementTags {

  private static final ElementTags EMPTY_TAGS = new ElementTags(List.of());

  private static final Map<Class<? extends Annotation>, ElementTagUtils<Annotation>>
      ELEMENT_TAG_UTILS_CACHE = synchronizedMap(new HashMap<>());

  private final ImmutableMap<Class<? extends Annotation>, List<Annotation>> annotationTags;
  private final ImmutableMap<String, NamedValueTag> namedValueTags;
  private final ImmutableList<Annotation> allAnnotations;

  private ElementTags(Iterable<Annotation> tags) {
    Map<Class<? extends Annotation>, List<Annotation>> annos = new HashMap<>();
    List<Annotation> allAnnos = new ArrayList<>();
    Map<String, NamedValueTag> namedValueTags = new HashMap<>();
    for (Annotation annotation : tags) {
      allAnnos.add(annotation);
      if (annotation instanceof NamedValueTag namedValueTag) {
        if (namedValueTags.put(namedValueTag.name(), namedValueTag) != null) {
          throw new IllegalArgumentException("Found duplicate named value tag: " + namedValueTag);
        }
      } else {
        List<Annotation> annotations =
            annos.computeIfAbsent(annotation.annotationType(), _k -> new ArrayList<>());
        if (!annotation.annotationType().isAnnotationPresent(Repeatable.class)) {
          if (!annotations.isEmpty()) {
            throw new IllegalArgumentException(
                "Annotation "
                    + annotation
                    + " is not repeatable, but found more than one. This is not allowed.");
          }
        }
        annotations.add(annotation);
      }
    }
    this.annotationTags = ImmutableMap.copyOf(annos);
    this.namedValueTags = ImmutableMap.copyOf(namedValueTags);
    this.allAnnotations = ImmutableList.copyOf(allAnnos);
  }

  private ElementTags(
      Map<Class<? extends Annotation>, List<Annotation>> annotationTags,
      Map<String, NamedValueTag> namedValueTags) {
    this.annotationTags = ImmutableMap.copyOf(annotationTags);
    this.namedValueTags = ImmutableMap.copyOf(namedValueTags);
    this.allAnnotations =
        Stream.concat(
                namedValueTags.values().stream(),
                annotationTags.values().stream().flatMap(Collection::stream))
            .collect(toImmutableList());
  }

  private static void validate(Annotation annotation) {
    Class<? extends Annotation> annotationType = annotation.annotationType();
    ElementTagUtility elementTagUtility = annotationType.getAnnotation(ElementTagUtility.class);
    if (elementTagUtility != null) {
      Class<? extends ElementTagUtils<?>> elementTagUtilsClass = elementTagUtility.value();
      ElementTagUtilityOf elementTagUtilityOf =
          elementTagUtilsClass.getAnnotation(ElementTagUtilityOf.class);
      if (elementTagUtilityOf == null) {
        throw new IllegalStateException(
            elementTagUtilsClass + " does not have a @ElementTagUtilityOf annotation");
      }
      if (elementTagUtilityOf.value() != annotationType) {
        throw new IllegalStateException(
            elementTagUtilityOf
                + " on "
                + elementTagUtilsClass
                + " does not match annotationType "
                + annotationType);
      }
    }
  }

  public static ElementTags of(Annotation... tags) {
    return of(Arrays.asList(tags));
  }

  public static ElementTags of(Collection<Annotation> tags) {
    if (tags.isEmpty()) {
      return emptyTags();
    }
    return new ElementTags(
        tags.stream()
            .map(ElementTags::unpackContainer)
            .flatMap(Collection::stream)
            .peek(ElementTags::validate)
            .toList());
  }

  public static ElementTags emptyTags() {
    return EMPTY_TAGS;
  }

  public <A extends Annotation> Optional<A> getAnnotationByType(Class<? extends A> annotationType) {
    List<A> annotationsByType = getAnnotationsByType(annotationType);
    if (annotationType.isAnnotationPresent(Repeatable.class)) {
      throw new IllegalArgumentException(
          "Annotation type "
              + annotationType
              + " is repeatable. Call getAnnotationsByType instead.");
    }
    if (annotationsByType.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(annotationsByType.get(0));
    }
  }

  @SuppressWarnings("unchecked")
  public <A extends Annotation> List<A> getAnnotationsByType(Class<? extends A> annotationType) {
    return (List<A>) annotationTags.getOrDefault(annotationType, List.of());
  }

  public Optional<NamedValueTag> getNamedValueTag(String name) {
    return Optional.ofNullable(namedValueTags.get(name));
  }

  public ImmutableCollection<Annotation> annotations() {
    return allAnnotations;
  }

  public ElementTags mergeTagsWithOverwrite(ElementTags otherTags) {
    LinkedHashMap<Class<? extends Annotation>, List<Annotation>> merged =
        new LinkedHashMap<>(annotationTags);
    merged.putAll(otherTags.annotationTags);
    LinkedHashMap<String, NamedValueTag> merged2 = new LinkedHashMap<>(namedValueTags);
    merged2.putAll(otherTags.namedValueTags);
    return new ElementTags(merged, merged2);
  }

  public ElementTags filter(Predicate<Annotation> predicate) {
    return ElementTags.of(annotations().stream().filter(predicate).toList());
  }

  public static boolean isTransitive(Annotation annotation) {
    return annotation.annotationType().isAnnotationPresent(Transitive.class);
  }

  public static ElementTags resolveTagConflicts(Collection<ElementTags> tagsIterable) {
    Map<? extends Class<? extends Annotation>, List<Annotation>> annotationsByType =
        tagsIterable.stream()
            .map(ElementTags::annotations)
            .flatMap(Collection::stream)
            .collect(groupingBy(Annotation::annotationType));
    ElementTags resolvedTags =
        ElementTags.of(
            annotationsByType.entrySet().stream()
                .flatMap(
                    listEntry -> {
                      List<Annotation> annotationConflicts = listEntry.getValue();
                      return getElementTagUtilsOrThrow(listEntry.getKey())
                          .resolve(annotationConflicts)
                          .stream();
                    })
                .toList());
    return resolvedTags;
  }

  public static @Nullable ConflictResponse detectConflictingAnnotation(
      ElementTags currentTransitiveTags, ElementTags resolvedDepTags) {
    for (Annotation annotation : currentTransitiveTags.annotations()) {
      List<? extends Annotation> depAnnotationOfSameType =
          resolvedDepTags.getAnnotationsByType(annotation.annotationType());
      ElementTagUtils<Annotation> elementTagUtils =
          getElementTagUtilsOrThrow(annotation.annotationType());
      if (depAnnotationOfSameType.isEmpty()) {
        continue;
      }
      for (Annotation depAnno : depAnnotationOfSameType) {
        int comparison = elementTagUtils.compare(annotation, depAnno);
        if (comparison < 0) {
          return new ConflictResponse(annotation, depAnno);
        }
      }
    }
    return null;
  }

  public static @AutoAnnotation NamedValueTag namedValueTag(String name, String value) {
    return new AutoAnnotation_ElementTags_namedValueTag(name, value);
  }

  private static ElementTagUtils<Annotation> getElementTagUtilsOrThrow(
      Class<? extends Annotation> annotationType) {
    ElementTagUtility elementTagUtility = annotationType.getAnnotation(ElementTagUtility.class);
    if (elementTagUtility == null) {
      throw new IllegalArgumentException(
          "Cannot handle conflicts for annotation type "
              + annotationType
              + " as the annotation type does not have @ElementTagUtility");
    }
    return ELEMENT_TAG_UTILS_CACHE.computeIfAbsent(
        annotationType,
        etu -> {
          try {
            @SuppressWarnings("unchecked")
            ElementTagUtils<Annotation> elementTagUtils =
                (ElementTagUtils<Annotation>)
                    elementTagUtility.value().getConstructor().newInstance();
            return elementTagUtils;
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  public record ConflictResponse(
      Annotation conflictingAnnotation, Annotation transitiveDepAnnotation) {}

  private static List<Annotation> unpackContainer(Annotation annotation) {
    Method containedAnnotationsMethod = getContainedAnnotationsMethod(annotation);
    if (containedAnnotationsMethod == null) {
      return List.of(annotation);
    }
    List<Annotation> unpacked = new ArrayList<>();
    try {
      Annotation[] nestedAnnos = (Annotation[]) containedAnnotationsMethod.invoke(annotation);
      unpacked.addAll(Arrays.asList(nestedAnnos));
    } catch (Exception e) {
      // Fallback in case of reflection exceptions
      unpacked.add(annotation);
    }
    return unpacked;
  }

  /**
   * If {@code annotation} is a container annotation of a @Repeatable annotation, this method
   * returns the "value" method which can be invoked to get the contained annotations. Else returns
   * null
   */
  private static @Nullable Method getContainedAnnotationsMethod(Annotation annotation) {
    try {
      Class<? extends Annotation> containerType = annotation.annotationType();
      Method valueMethod = containerType.getMethod("value");
      Class<?> returnType = valueMethod.getReturnType();

      // 1. Check if the value() method returns an array of Annotations
      Class<?> componentType = returnType.getComponentType();
      if (componentType != null && Annotation.class.isAssignableFrom(componentType)) {

        // 2. Fetch the @Repeatable meta-annotation from the component annotation type
        Repeatable repeatableMeta = componentType.getAnnotation(Repeatable.class);

        // 3. CRITICAL CHECK: Verify that the component's @Repeatable points back to this container
        // type
        if (repeatableMeta != null && Objects.equals(repeatableMeta.value(), containerType)) {
          return valueMethod;
        }
      }
    } catch (NoSuchMethodException e) {
      // No value() method exists; it cannot be a repeatable container
    }
    return null;
  }
}

package com.flipkart.krystal.tags;

import static java.util.Collections.synchronizedMap;
import static java.util.stream.Collectors.groupingBy;

import com.flipkart.krystal.annos.ElementTagUtility;
import com.flipkart.krystal.annos.ElementTagUtilityOf;
import com.flipkart.krystal.annos.Transitive;
import com.flipkart.krystal.core.ElementTagUtils;
import com.google.auto.value.AutoAnnotation;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
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
 * container annotation rather than the individual repeatable annotations (as returned by {@link
 * AnnotatedElement#getAnnotations()}). The only exception to this rule is{@link NamedValueTag} (see
 * its documentation for more details)
 */
@Slf4j
@EqualsAndHashCode
public final class ElementTags {

  private static final ElementTags EMPTY_TAGS = new ElementTags(List.of());

  private static final Map<Class<? extends Annotation>, ElementTagUtils<Annotation>>
      ELEMENT_TAG_UTILS_CACHE = synchronizedMap(new HashMap<>());

  private final ImmutableMap<Class<? extends Annotation>, Annotation> annotationTags;
  private final ImmutableMap<String, NamedValueTag> namedValueTags;

  private ElementTags(Iterable<Annotation> tags) {
    Map<Class<? extends Annotation>, Annotation> annos = new HashMap<>();
    Map<String, NamedValueTag> namedValueTags = new HashMap<>();
    for (Annotation annotation : tags) {
      if (annotation instanceof NamedValueTag namedValueTag) {
        if (namedValueTags.put(namedValueTag.name(), namedValueTag) != null) {
          throw new IllegalArgumentException("Found duplicate named value tag: " + namedValueTag);
        }
      } else {
        if (annos.put(annotation.annotationType(), annotation) != null) {
          throw new IllegalArgumentException(
              "Found duplicate annotation of this type: " + annotation);
        }
      }
    }
    this.annotationTags = ImmutableMap.copyOf(annos);
    this.namedValueTags = ImmutableMap.copyOf(namedValueTags);
  }

  private ElementTags(
      Map<Class<? extends Annotation>, Annotation> annotationTags,
      Map<String, NamedValueTag> namedValueTags) {
    this.annotationTags = ImmutableMap.copyOf(annotationTags);
    this.namedValueTags = ImmutableMap.copyOf(namedValueTags);
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
    return new ElementTags(Arrays.asList(tags));
  }

  public static ElementTags of(Collection<Annotation> tags) {
    if (tags.isEmpty()) {
      return emptyTags();
    }
    tags.forEach(ElementTags::validate);
    return new ElementTags(tags);
  }

  public static ElementTags emptyTags() {
    return EMPTY_TAGS;
  }

  @SuppressWarnings("unchecked")
  public <A extends Annotation> Optional<A> getAnnotationByType(Class<? extends A> annotationType) {
    return Optional.ofNullable((@Nullable A) annotationTags.get(annotationType));
  }

  public Optional<NamedValueTag> getNamedValueTag(String name) {
    return Optional.ofNullable(namedValueTags.get(name));
  }

  public ImmutableCollection<Annotation> annotations() {
    return annotationTags.values();
  }

  public ElementTags mergeAnnotations(Annotation... annotations) {
    if (annotations.length == 0) {
      return this;
    }
    return mergeTagsWithOverwrite(
        new ElementTags(Arrays.stream(annotations).peek(ElementTags::validate).toList()));
  }

  public ElementTags mergeTagsWithOverwrite(ElementTags otherTags) {
    LinkedHashMap<Class<? extends Annotation>, Annotation> merged =
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
                .map(
                    listEntry -> {
                      List<Annotation> annotationConflicts = listEntry.getValue();
                      return getElementTagUtilsOrThrow(listEntry.getKey())
                          .resolve(annotationConflicts);
                    })
                .toList());
    return resolvedTags;
  }

  public static @Nullable ConflictResponse detectConflictingAnnotation(
      ElementTags currentTransitiveTags, ElementTags resolvedDepTags) {
    for (Annotation annotation : currentTransitiveTags.annotations()) {
      Optional<? extends Annotation> depAnnotationOfSameType =
          resolvedDepTags.getAnnotationByType(annotation.annotationType());
      ElementTagUtils<Annotation> elementTagUtils =
          getElementTagUtilsOrThrow(annotation.annotationType());
      if (depAnnotationOfSameType.isEmpty()) {
        continue;
      }
      int comparison = elementTagUtils.compare(annotation, depAnnotationOfSameType.get());
      if (comparison < 0) {
        return new ConflictResponse(annotation, depAnnotationOfSameType.get());
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
      @Nullable Annotation conflictingAnnotation, @Nullable Annotation transitiveDepAnnotation) {}
}

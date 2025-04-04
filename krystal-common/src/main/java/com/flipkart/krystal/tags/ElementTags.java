package com.flipkart.krystal.tags;

import com.google.auto.value.AutoAnnotation;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A container for all tags assigned to an krystal graph element, like a facet, logic, vajram/kryon
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
@EqualsAndHashCode
public final class ElementTags {

  private static final ElementTags EMPTY_TAGS = new ElementTags(List.of());

  private final ImmutableMap<Class<? extends Annotation>, Annotation> annotationTags;
  private final ImmutableMap<String, NamedValueTag> namedValueTags;

  private ElementTags(Iterable<Annotation> tags) {
    ImmutableMap.Builder<Class<? extends Annotation>, Annotation> annos = ImmutableMap.builder();
    ImmutableMap.Builder<String, NamedValueTag> namedValueTags = ImmutableMap.builder();
    for (Annotation annotation : tags) {
      if (annotation instanceof NamedValueTag namedValueTag) {
        namedValueTags.put(namedValueTag.name(), namedValueTag);
      } else {
        annos.put(annotation.annotationType(), annotation);
      }
    }
    this.annotationTags = annos.build();
    this.namedValueTags = namedValueTags.build();
  }

  private ElementTags(
      Map<Class<? extends Annotation>, Annotation> annotationTags,
      Map<String, NamedValueTag> namedValueTags) {
    this.annotationTags = ImmutableMap.copyOf(annotationTags);
    this.namedValueTags = ImmutableMap.copyOf(namedValueTags);
  }

  public static ElementTags of(Annotation... tags) {
    return new ElementTags(Arrays.asList(tags));
  }

  public static ElementTags of(Collection<Annotation> tags) {
    if (tags.isEmpty()) {
      return emptyTags();
    }
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
    return mergeFrom(new ElementTags(Arrays.stream(annotations).toList()));
  }

  private ElementTags mergeFrom(ElementTags otherTags) {
    LinkedHashMap<Class<? extends Annotation>, Annotation> merged =
        new LinkedHashMap<>(annotationTags);
    merged.putAll(otherTags.annotationTags);
    LinkedHashMap<String, NamedValueTag> merged2 = new LinkedHashMap<>(namedValueTags);
    merged2.putAll(otherTags.namedValueTags);
    return new ElementTags(merged, merged2);
  }

  public static @AutoAnnotation NamedValueTag namedValueTag(String name, String value) {
    return new AutoAnnotation_ElementTags_namedValueTag(name, value);
  }
}

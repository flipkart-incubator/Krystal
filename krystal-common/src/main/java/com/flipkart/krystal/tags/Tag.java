package com.flipkart.krystal.tags;

import static com.flipkart.krystal.tags.TagKey.namedValueTagKey;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Tag are the primary way to annotate metadata to various application elements of the krystal
 * graph, like logics, facets, vajrams/kryons etc. Tags are modelled at build time and runtime as
 * annotations and annotation objects. Each element can have exactly one annotation of a given
 * annotation type. When an element has multiple annotations in the source code which are themselves
 * annotated with {@link Repeatable}, the element is tagged with the container annotation rather
 * than the individual repeatable annotations (as returned by {@link
 * AnnotatedElement#getAnnotations()}).
 *
 * <p>The only exception to this rule is{@link NamedValueTag} (see its documentation for more
 * details)
 *
 * @param <T> The annotation
 */
@EqualsAndHashCode
@ToString
@Accessors(fluent = true)
@Value
public final class Tag<T extends Annotation> {
  private final TagKey tagKey;
  private final T tagValue;

  private Tag(Class<T> annotationType, T tagValue) {
    this(new TagKey(annotationType, annotationType), tagValue);
  }

  Tag(TagKey tagKey, T tagValue) {
    this.tagKey = tagKey;
    this.tagValue = tagValue;
  }

  @SuppressWarnings("unchecked")
  public static <A extends Annotation> Tag<A> from(A annotation) {
    if (annotation instanceof NamedValueTag namedValueTag) {
      return (Tag<A>) new Tag<>(namedValueTagKey(namedValueTag.name()), namedValueTag);
    } else {
      return new Tag<>((Class<A>) annotation.annotationType(), annotation);
    }
  }
}

package com.flipkart.krystal.tags;

import com.google.common.base.Preconditions;
import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.util.Objects;

/**
 * A key for a tag. A tag key is a pair of a key and an annotation type. The key is used to make
 * sure that no element can have two tags for the same key.
 *
 * <p>For all annotations except {@link NamedValueTag}, the key just contains the annotation type.
 * For annotations which are {@link Repeatable}, the container annotation is used as the key.
 *
 * <p>For {@link NamedValueTag}, the key contains the name of the {@link NamedValueTag}. This
 * ensures that an element can have multiple tags all of which are {@link NamedValueTag}s, but no
 * two tags can have the same {@link NamedValueTag#name()}.
 *
 * @param key
 * @param annotationType
 */
public record TagKey(Object key, Class<? extends Annotation> annotationType) {

  /**
   * @param annotationType The annotation type for which the TagKey is to be created. If the
   *     annotation type is {@link Repeatable}, then the container annotation needs to be passed
   *     instead.
   * @return A TagKey for the given annotation type.
   * @throws IllegalArgumentException if the annotation type is {@link NamedValueTag}. For {@link
   *     NamedValueTag} use {@link #namedValueTagKey(String)} instead.
   */
  public static TagKey of(Class<? extends Annotation> annotationType) {
    Preconditions.checkArgument(!Objects.equals(annotationType, NamedValueTag.class));
    return new TagKey(annotationType, annotationType);
  }

  /**
   * @param name The name of the {@link NamedValueTag} for which the TagKey is to be created.
   * @return A TagKey for the given {@link NamedValueTag} and name.
   */
  public static TagKey namedValueTagKey(String name) {
    return new TagKey(name, NamedValueTag.class);
  }
}

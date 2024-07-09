package com.flipkart.krystal.vajram.tags;

import com.flipkart.krystal.config.Tag;
import java.lang.annotation.Annotation;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

@EqualsAndHashCode
@ToString
@Accessors(fluent = true)
@Value
public final class AnnotationTag<T extends Annotation> implements Tag {
  private final AnnotationTagKey tagKey;
  private final T tagValue;

  private AnnotationTag(Class<T> annotationType, T tagValue) {
    this(new AnnotationTagKey(annotationType, annotationType), tagValue);
  }

  AnnotationTag(AnnotationTagKey tagKey, T tagValue) {
    this.tagKey = tagKey;
    this.tagValue = tagValue;
  }

  @SuppressWarnings("unchecked")
  public static <A extends Annotation> AnnotationTag<A> from(A annotation) {
    if (annotation instanceof NamedValueTag namedValueTag) {
      return (AnnotationTag<A>) AnnotationTags.newNamedTag(namedValueTag);
    } else {
      return new AnnotationTag<>((Class<A>) annotation.annotationType(), annotation);
    }
  }
}

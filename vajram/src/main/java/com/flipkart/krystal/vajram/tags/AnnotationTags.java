package com.flipkart.krystal.vajram.tags;

import com.flipkart.krystal.config.Tag;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;

public final class AnnotationTags {

  static AnnotationTag<NamedValueTag> newNamedTag(NamedValueTag namedValueTag) {
    return newNamedTag(namedValueTag.name(), namedValueTag.value());
  }

  public static AnnotationTag<NamedValueTag> newNamedTag(String name, String value) {
    return new AnnotationTag<>(
        new AnnotationTagKey(name, NamedValueTag.class),
        new NamedValueTag() {
          @Override
          public String name() {
            return name;
          }

          @Override
          public String value() {
            return value;
          }

          @Override
          public Class<? extends Annotation> annotationType() {
            return NamedValueTag.class;
          }
        });
  }

  public static <A extends Annotation> Optional<A> getAnnotationByType(
      Class<? extends A> annotationType, Map<Object, Tag> tagMap) {
    return getAnnotationByKey(new AnnotationTagKey(annotationType, annotationType), tagMap);
  }

  public static Optional<NamedValueTag> getNamedValueTag(String name, Map<Object, Tag> tagMap) {
    return getAnnotationByKey(new AnnotationTagKey(name, NamedValueTag.class), tagMap);
  }

  private static <A> Optional<A> getAnnotationByKey(
      AnnotationTagKey tagKey, Map<Object, Tag> tagMap) {
    if (tagMap.get(tagKey) instanceof AnnotationTag<?> anno) {
      @SuppressWarnings("unchecked")
      A tagValue = (A) anno.tagValue();
      return Optional.of(tagValue);
    }
    return Optional.empty();
  }

  private AnnotationTags() {}
}

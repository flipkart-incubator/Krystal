package com.flipkart.krystal.vajram.tags;

import com.flipkart.krystal.config.Tag;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;

public record AnnotationTag<T extends Annotation>(
    Class<? extends T> annotationType, T annotation, Object tagKey) implements Tag {

  public AnnotationTag(Class<T> annotationType, T annotation) {
    this(annotationType, annotation, annotationType);
  }

  public static <A extends Annotation> AnnotationTag<A> from(A annotation) {
    //noinspection unchecked
    return new AnnotationTag<>((Class<A>) annotation.getClass(), annotation);
  }

  public static AnnotationTag<NamedValueTag> newNamedTag(String name, String value) {
    return new AnnotationTag<>(
        NamedValueTag.class,
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
        },
        name);
  }

  public static <A> Optional<A> getAnnotationByType(
      Class<? extends A> clazz, Map<Object, Tag> tagMap) {
    return getAnnotationByType(clazz, clazz, tagMap);
  }

  public static <A> Optional<A> getAnnotationByType(
      Object tagKey, Class<? extends A> clazz, Map<Object, Tag> tagMap) {
    Tag tag = tagMap.get(tagKey);
    if (tag instanceof AnnotationTag<?> anno) {
      if (clazz.isAssignableFrom(anno.annotation().getClass())) {
        //noinspection unchecked
        return Optional.of((A) anno.annotation());
      }
    }
    return Optional.empty();
  }
}

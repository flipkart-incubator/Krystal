package com.flipkart.krystal.vajram;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.vajram.tags.AnnotationTag;
import com.google.common.collect.ImmutableMap;
import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Vajrams {

  public static String getVajramIdString(
      @SuppressWarnings("rawtypes") Class<? extends Vajram> aClass) {
    VajramDef annotation;
    Class<?> annotatedClass = aClass;
    do {
      annotation = annotatedClass.getAnnotation(VajramDef.class);
      if (annotation != null) {
        return annotatedClass.getSimpleName();
      }
      annotatedClass = annotatedClass.getSuperclass();
      if (annotatedClass == null) {
        break;
      }
    } while (Vajram.class.isAssignableFrom(annotatedClass));
    throw new IllegalStateException("Unable to find vajramId for class %s".formatted(aClass));
  }

  public static ImmutableMap<Object, Tag> parseFacetTags(Field facetField) {
    Map<Object, Tag> tags = new LinkedHashMap<>();
    Annotation[] annotations = facetField.getAnnotations();
    for (Annotation annotation : annotations) {
      boolean isRepeatable = annotation.getClass().getAnnotation(Repeatable.class) != null;
      if (isRepeatable) {
        log.info("Repeatable annotations are not supported as tags. Ignoring {}", annotation);
      } else {
        AnnotationTag<Annotation> annotationTag = AnnotationTag.from(annotation);
        tags.put(annotationTag.tagKey(), annotationTag);
      }
    }
    return ImmutableMap.copyOf(tags);
  }

  private Vajrams() {}
}

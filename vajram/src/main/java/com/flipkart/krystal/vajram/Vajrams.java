package com.flipkart.krystal.vajram;

import com.flipkart.krystal.tags.ElementTags;
import com.flipkart.krystal.tags.Tag;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Vajrams {

  public static String getVajramIdString(
      @SuppressWarnings("rawtypes") Class<? extends Vajram> aClass) {
    return getVajramSourceClass(aClass).getSimpleName();
  }

  public static ElementTags parseFacetTags(Field facetField) {
    return ElementTags.of(
        Arrays.stream(facetField.getAnnotations())
            .map(Tag::from)
            .collect(Collectors.toMap(Tag::tagKey, Function.identity())));
  }

  public static Class<?> getVajramSourceClass(Class<?> aClass) {
    Annotation annotation = aClass.getAnnotation(VajramDef.class);
    if (annotation != null) {
      return aClass;
    }
    Class<?> superclass = aClass.getSuperclass();
    if (Object.class.equals(superclass) || superclass == null) {
      throw new IllegalArgumentException();
    }
    return getVajramSourceClass(superclass);
  }

  private Vajrams() {}
}
